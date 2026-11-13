package com.seender.mail.service;

import com.seender.mail.config.MailSendingProperties;
import com.seender.mail.entity.MailHistory;
import com.seender.mail.event.PartnerMailEvent;
import com.seender.mail.repository.MailHistoryRepository;
import com.seender.mail.util.MailSafety;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File d'attente d'envoi.
 *
 * Stratégie anti-ban :
 *  - un seul message à la fois, avec un délai aléatoire entre deux envois (pas de rafale régulière) ;
 *  - plafond horaire glissant côté application, avant que le fournisseur SMTP ne nous limite ;
 *  - ré-essai avec back-off exponentiel sur les erreurs temporaires, abandon immédiat sur adresse invalide ;
 *  - entêtes complètes (Message-ID, Date, Reply-To, List-Unsubscribe) et corps multipart texte + HTML,
 *    ce qui est ce que les filtres anti-spam attendent d'un expéditeur légitime.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailQueueService {

    private final JavaMailSender mailSender;
    private final MailHistoryRepository mailHistoryRepository;
    private final MailSendingProperties props;

    private final Queue<QueuedMail> mailQueue = new ConcurrentLinkedQueue<>();
    /** Horodatages des envois réussis/tentés de l'heure glissante. */
    private final Deque<Long> recentSends = new ArrayDeque<>();

    private long nextSendAt = 0L;

    @EventListener
    public void handlePartnerMailEvent(PartnerMailEvent event) {
        mailQueue.add(new QueuedMail(event));
    }

    /**
     * Le planificateur tourne souvent, mais l'envoi réel reste limité par
     * {@link #nextSendAt} et par le plafond horaire : c'est le throttle.
     */
    @Scheduled(fixedDelay = 2000)
    public void processQueue() {
        QueuedMail head = mailQueue.peek();
        if (head == null) return;

        long now = System.currentTimeMillis();
        if (now < nextSendAt) return;

        if (!hourlyBudgetAvailable(now)) {
            log.debug("Plafond horaire atteint ({}/h), envoi reporté. En attente : {}",
                    props.getMaxPerHour(), mailQueue.size());
            return;
        }

        if (head.notBefore > now) {
            // Message en back-off : on le remet en fin de file pour ne pas bloquer les autres.
            mailQueue.poll();
            mailQueue.add(head);
            return;
        }

        QueuedMail item = mailQueue.poll();
        if (item == null) return;

        recentSends.addLast(now);
        nextSendAt = now + randomDelay();
        sendEmail(item);
    }

    /** Fenêtre glissante d'une heure : on purge les envois trop anciens puis on compare au plafond. */
    private boolean hourlyBudgetAvailable(long now) {
        long oneHourAgo = now - 3_600_000L;
        while (!recentSends.isEmpty() && recentSends.peekFirst() < oneHourAgo) {
            recentSends.pollFirst();
        }
        return recentSends.size() < Math.max(1, props.getMaxPerHour());
    }

    private long randomDelay() {
        long min = Math.max(0, props.getMinDelayMs());
        long max = Math.max(min + 1, props.getMaxDelayMs());
        return ThreadLocalRandom.current().nextLong(min, max);
    }

    private void sendEmail(QueuedMail item) {
        PartnerMailEvent event = item.event;

        String recipient = MailSafety.normalizeEmail(event.getEmailTo());
        if (!MailSafety.isValidEmail(recipient)) {
            updateHistory(event.getHistoryId(), "ERREUR", "Adresse destinataire invalide : " + recipient);
            log.warn("Adresse invalide ignorée pour le partenaire {}", event.getPartenaireName());
            return;
        }

        try {
            MimeMessage message = buildMessage(event, recipient);
            mailSender.send(message);
            updateHistory(event.getHistoryId(), "ENVOYE", null);
            log.info("Email envoyé au partenaire {} (reste : {})", event.getPartenaireName(), mailQueue.size());
        } catch (Exception e) {
            handleFailure(item, e);
        }
    }

    private MimeMessage buildMessage(PartnerMailEvent event, String recipient) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        String partnerName = MailSafety.sanitizeHeader(event.getPartenaireName());
        String finalMessage = event.getMessageTemplate().replace("{{NOM_PARTENAIRE}}", partnerName);

        helper.setFrom(fromAddress());
        helper.setTo(recipient);

        String rawCc = event.getCcEmails() == null ? "" : String.join(",", event.getCcEmails());
        String[] cc = MailSafety.buildCcArray(
                MailSafety.parseEmailList(rawCc),
                recipient,
                props.getMaxCc());
        if (cc.length > 0) {
            helper.setCc(cc);
        }

        String replyTo = props.getReplyTo().isBlank() ? props.getFrom() : props.getReplyTo();
        if (MailSafety.isValidEmail(replyTo)) {
            helper.setReplyTo(replyTo);
        }

        helper.setSubject(MailSafety.sanitizeHeader("Informations et accès - " + partnerName));
        // Texte brut + HTML : le lien reste lisible même si Outlook (Safe Links) réécrit le href.
        helper.setText(finalMessage, toHtml(finalMessage));

        // Entêtes attendues d'un expéditeur légitime (réduisent le score spam).
        message.setHeader("Message-ID", "<" + UUID.randomUUID() + "@" + props.getMessageIdDomain() + ">");
        message.setHeader("Date", RFC_2822.format(Instant.now().atZone(ZoneId.systemDefault())));
        if (!props.getUnsubscribeMailto().isBlank()) {
            message.setHeader("List-Unsubscribe", "<mailto:" + props.getUnsubscribeMailto() + ">");
            message.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
        }

        String safePartner = MailSafety.sanitizeFilename(partnerName, "Partenaire");
        if (event.getExcelAttachment() != null && event.getExcelAttachment().length > 0) {
            helper.addAttachment("Donnees_" + safePartner + ".xlsx",
                    new ByteArrayResource(event.getExcelAttachment()));
        }
        if (event.getPdfAttachment() != null && event.getPdfAttachment().length > 0) {
            helper.addAttachment("Document_Global.pdf", new ByteArrayResource(event.getPdfAttachment()));
        }

        return message;
    }

    private InternetAddress fromAddress() throws UnsupportedEncodingException, AddressException {
        if (props.getFromName() == null || props.getFromName().isBlank()) {
            return new InternetAddress(props.getFrom());
        }
        return new InternetAddress(props.getFrom(), MailSafety.sanitizeHeader(props.getFromName()), StandardCharsets.UTF_8.name());
    }

    /** Erreur temporaire -> on ré-essaie plus tard. Erreur permanente -> on abandonne tout de suite. */
    private void handleFailure(QueuedMail item, Exception e) {
        String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();

        if (isPermanent(e) || item.attempts + 1 >= props.getMaxAttempts()) {
            updateHistory(item.event.getHistoryId(), "ERREUR", reason);
            log.error("Echec définitif d'envoi au partenaire {} : {}", item.event.getPartenaireName(), reason);
            return;
        }

        item.attempts++;
        // Back-off exponentiel : 1 min, 4 min, 9 min... plus un bruit aléatoire.
        long backoff = (long) item.attempts * item.attempts * 60_000L
                + ThreadLocalRandom.current().nextLong(0, 30_000L);
        item.notBefore = System.currentTimeMillis() + backoff;
        mailQueue.add(item);

        updateHistory(item.event.getHistoryId(), "EN ATTENTE",
                "Tentative " + item.attempts + "/" + props.getMaxAttempts() + " - " + reason);
        log.warn("Erreur temporaire pour {} (tentative {}), nouvel essai dans {} s : {}",
                item.event.getPartenaireName(), item.attempts, backoff / 1000, reason);
    }

    private boolean isPermanent(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof AddressException) return true;
            if (cause instanceof SendFailedException sfe
                    && sfe.getValidUnsentAddresses() == null
                    && sfe.getInvalidAddresses() != null
                    && sfe.getInvalidAddresses().length > 0) {
                return true;
            }
            String msg = cause.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase(Locale.ROOT);
                // Codes SMTP 5xx = refus définitif.
                if (lower.contains("550") || lower.contains("553") || lower.contains("554")
                        || lower.contains("user unknown") || lower.contains("no such user")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static final DateTimeFormatter RFC_2822 =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>\"]+[^\\s<>\".,;:!?)]");

    /** Construit la version HTML : échappement intégral, puis les URL deviennent de vrais liens. */
    private static String toHtml(String text) {
        StringBuilder html = new StringBuilder();
        Matcher m = URL_PATTERN.matcher(text);
        int last = 0;
        while (m.find()) {
            html.append(HtmlUtils.htmlEscape(text.substring(last, m.start())));
            String url = HtmlUtils.htmlEscape(m.group());
            html.append("<a href=\"").append(url).append("\">").append(url).append("</a>");
            last = m.end();
        }
        html.append(HtmlUtils.htmlEscape(text.substring(last)));
        String body = html.toString().replace("\r\n", "\n").replace("\n", "<br>\n");
        return "<html><body style=\"font-family:Calibri,Arial,sans-serif;font-size:11pt;\">"
                + body + "</body></html>";
    }

    private void updateHistory(Long historyId, String status, String error) {
        if (historyId == null) return;
        mailHistoryRepository.findById(historyId).ifPresent(history -> {
            history.setStatus(status);
            history.setErrorMessage(error == null || error.length() <= 1000
                    ? error
                    : error.substring(0, 1000));
            mailHistoryRepository.save(history);
        });
    }

    private static final class QueuedMail {
        private final PartnerMailEvent event;
        private int attempts = 0;
        private long notBefore = 0L;

        private QueuedMail(PartnerMailEvent event) {
            this.event = event;
        }
    }
}
