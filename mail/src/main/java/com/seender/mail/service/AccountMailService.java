package com.seender.mail.service;

import com.seender.mail.entity.MailHistory;
import com.seender.mail.event.PartnerMailEvent;
import com.seender.mail.repository.MailHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Envoi des comptes : koul ligne f l'Excel = wa7ed l'utilisateur.
 * Koul wa7ed kaywslo mail fih fichier Excel b la ligne dyalo bo7dha (identifiant + mot de passe)
 * w lien dyal l'application f le corps du mail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountMailService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final MailHistoryRepository mailHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public record DispatchResult(int queued, List<String> skipped) {}

    public DispatchResult processAndDispatch(MultipartFile file, String subjectTemplate, String messageTemplate) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls"))) {
            throw new RuntimeException("Format non supporté. Veuillez uploader un fichier .xlsx");
        }

        DataFormatter formatter = new DataFormatter(Locale.FRANCE);
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new RuntimeException("Fichier Excel sans en-tête.");
            for (Cell cell : headerRow) headers.add(formatter.formatCellValue(cell).trim());

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                List<String> data = new ArrayList<>();
                boolean empty = true;
                for (int c = 0; c < headers.size(); c++) {
                    String v = formatter.formatCellValue(row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)).trim();
                    if (!v.isEmpty()) empty = false;
                    data.add(v);
                }
                if (!empty) rows.add(data);
            }
        }

        // Détection des colonnes (insensible aux accents / majuscules)
        int nomCol = -1, prenomCol = -1, emailCol = -1, lienCol = -1, identCol = -1, mdpCol = -1;
        for (int i = 0; i < headers.size(); i++) {
            String h = normalize(headers.get(i));
            if (h.contains("PRENOM")) prenomCol = i;
            else if (h.equals("NOM") || h.startsWith("NOM ")) nomCol = i;
            else if (h.contains("MAIL")) emailCol = i;
            else if (h.contains("LIEN") || h.contains("URL") || h.contains("LINK")) lienCol = i;
            else if (h.contains("IDENTIFIANT") || h.contains("LOGIN")) identCol = i;
            else if (h.contains("MOT DE PASSE") || h.contains("PASSWORD") || h.contains("MDP")) mdpCol = i;
        }
        if (emailCol == -1 || identCol == -1 || mdpCol == -1) {
            throw new RuntimeException("Les colonnes 'Email', 'Identifiant' et 'Mot de passe' sont obligatoires.");
        }

        int queued = 0;
        List<String> skipped = new ArrayList<>();

        for (List<String> row : rows) {
            String email = row.get(emailCol);
            String nom = nomCol != -1 ? row.get(nomCol) : "";
            String prenom = prenomCol != -1 ? row.get(prenomCol) : "";
            String lien = lienCol != -1 ? row.get(lienCol) : "";
            String fullName = (prenom + " " + nom).trim();
            if (fullName.isEmpty()) fullName = email;

            if (!isDeliverable(email)) {
                skipped.add(fullName + " (" + (email.isEmpty() ? "email vide" : email) + ")");
                continue;
            }

            byte[] excelBytes = generateSingleRowExcel(headers, row);

            String subject = render(subjectTemplate, nom, prenom, lien, email);
            String message = render(messageTemplate, nom, prenom, lien, email);
            String attachmentName = "Identifiants_" + safeFileName(fullName) + ".xlsx";

            MailHistory history = mailHistoryRepository.save(MailHistory.builder()
                    .partenaire(fullName)
                    .emailTo(email)
                    .status("EN ATTENTE")
                    .createdAt(LocalDateTime.now())
                    .build());

            eventPublisher.publishEvent(new PartnerMailEvent(this, history.getId(), fullName, email,
                    new String[0], excelBytes, message, subject, attachmentName));
            queued++;
        }

        log.info("Comptes : {} mails en file d'attente, {} ignorés", queued, skipped.size());
        return new DispatchResult(queued, skipped);
    }

    private static boolean isDeliverable(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) return false;
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        return !domain.endsWith(".local") && !domain.startsWith("sans-mail");
    }

    private static String render(String template, String nom, String prenom, String lien, String email) {
        return template
                .replace("{{NOM}}", nom)
                .replace("{{PRENOM}}", prenom)
                .replace("{{LIEN}}", lien)
                .replace("{{EMAIL}}", email);
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .trim();
    }

    private static String safeFileName(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private byte[] generateSingleRowExcel(List<String> headers, List<String> row) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Mes identifiants");

            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            Row data = sheet.createRow(1);
            for (int i = 0; i < headers.size(); i++) {
                Cell h = header.createCell(i);
                h.setCellValue(headers.get(i));
                h.setCellStyle(headerStyle);
                data.createCell(i).setCellValue(i < row.size() ? row.get(i) : "");
            }
            for (int i = 0; i < headers.size(); i++) sheet.autoSizeColumn(i);

            wb.write(bos);
            return bos.toByteArray();
        }
    }
}
