package com.seender.mail.service;

import com.seender.mail.entity.MailHistory;
import com.seender.mail.event.PartnerMailEvent;
import com.seender.mail.repository.MailHistoryRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailQueueService {

    private final JavaMailSender mailSender;
    private final MailHistoryRepository mailHistoryRepository;
    private final Queue<PartnerMailEvent> mailQueue = new ConcurrentLinkedQueue<>();

    @EventListener
    public void handlePartnerMailEvent(PartnerMailEvent event) {
        mailQueue.add(event);
    }

    @Scheduled(fixedDelay = 60000)
    public void processQueue() {
        if (!mailQueue.isEmpty()) {
            PartnerMailEvent event = mailQueue.poll();
            sendEmail(event);
        }
    }

    private void sendEmail(PartnerMailEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String finalMessage = event.getMessageTemplate().replace("{{NOM_PARTENAIRE}}", event.getPartenaireName());

            helper.setFrom("communication.bytel@kyntus.com");
            helper.setTo(event.getEmailTo());

            if (event.getCcEmails() != null && event.getCcEmails().length > 0) {
                helper.setCc(event.getCcEmails());
            }

            helper.setSubject("TR: Informations et accès - " + event.getPartenaireName());
            helper.setText(finalMessage, false);

            // Attachement Excel (Data partenaire)
            helper.addAttachment("Donnees_" + event.getPartenaireName() + ".xlsx", new ByteArrayResource(event.getExcelAttachment()));

            // Attachement PDF (Document Global)
            if (event.getPdfAttachment() != null && event.getPdfAttachment().length > 0) {
                helper.addAttachment("Document_Global.pdf", new ByteArrayResource(event.getPdfAttachment()));
            }

            mailSender.send(message);
            updateHistory(event.getHistoryId(), "ENVOYE", null);

            log.info("✅ Email envoyé à {} (Reste : {})", event.getEmailTo(), mailQueue.size());
        } catch (Exception e) {
            updateHistory(event.getHistoryId(), "ERREUR", e.getMessage());
            log.error("❌ Erreur d'envoi à {}", event.getEmailTo());
        }
    }

    private void updateHistory(Long historyId, String status, String error) {
        mailHistoryRepository.findById(historyId).ifPresent(history -> {
            history.setStatus(status);
            history.setErrorMessage(error);
            mailHistoryRepository.save(history);
        });
    }
}