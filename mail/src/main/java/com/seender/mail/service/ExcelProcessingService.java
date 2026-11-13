package com.seender.mail.service;

import com.seender.mail.entity.Anomalie;
import com.seender.mail.entity.MailHistory;
import com.seender.mail.event.PartnerMailEvent;
import com.seender.mail.repository.AnomalieRepository;
import com.seender.mail.config.MailSendingProperties;
import com.seender.mail.repository.MailHistoryRepository;
import com.seender.mail.util.MailSafety;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelProcessingService {

    private final AnomalieRepository anomalieRepository;
    private final MailHistoryRepository mailHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MailSendingProperties props;

    /**
     * @param manualCcEmails adresses saisies dans le formulaire (séparées par , ; ou espace).
     *                       Elles s'ajoutent au CC par défaut et au mapping par département.
     * @return le nombre d'emails mis en file d'attente.
     */
    public int processAndDispatch(MultipartFile file, MultipartFile depFile, MultipartFile pdfFile,
                                  String messageTemplate, String manualCcEmails) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) throw new IllegalArgumentException("Fichier invalide.");

        // CC communs à tous les partenaires : saisie du formulaire + CC par défaut de l'application.
        Set<String> baseCc = new LinkedHashSet<>(MailSafety.parseEmailList(manualCcEmails));
        baseCc.addAll(MailSafety.parseEmailList(props.getDefaultCc()));

        // Extraction du PDF (Optionnel)
        byte[] pdfBytes = null;
        if (pdfFile != null && !pdfFile.isEmpty()) {
            pdfBytes = pdfFile.getBytes();
        }

        // 1. Dictionnaire (DB)
        List<Anomalie> anomalies = anomalieRepository.findAll();
        Map<String, String> dictionary = new HashMap<>();
        for (Anomalie a : anomalies) {
            dictionary.put(a.getCode().toUpperCase().trim(), a.getDescription());
        }

        // 2. Mapping dyal les DEP (Fichier Cc)
        Map<String, Set<String>> depMapping = parseDepMapping(depFile);

        List<String> headers = new ArrayList<>();
        List<List<String>> allDataRows = new ArrayList<>();

        // 3. Routing CSV/Excel l'fichier principal
        if (filename.toLowerCase().endsWith(".csv")) {
            parseCsv(file, headers, allDataRows);
        } else if (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls")) {
            parseExcel(file, headers, allDataRows);
        } else {
            throw new IllegalArgumentException("Format non supporté. Veuillez uploader un fichier .xlsx ou .csv");
        }

        // 4. Détection des colonnes (Adapté pour Ancien + Nouveau Format)
        int partenaireCol = -1, anomalieCol = -1, emailCol = -1, depColMain = -1;
        for (int i = 0; i < headers.size(); i++) {
            String headerName = headers.get(i).toUpperCase().trim();
            if (headerName.contains("PARTENAIRE") || headerName.contains("ENTREPRISE")) partenaireCol = i;
            if (headerName.contains("ANOMALIE")) anomalieCol = i;
            if (headerName.contains("MAIL") || headerName.contains("EMAIL")) emailCol = i;
            if (headerName.contains("DEP")) depColMain = i;
        }

        if (partenaireCol == -1 || emailCol == -1) {
            throw new IllegalArgumentException("Les colonnes 'Entreprise/Partenaire' et 'Mail/Email' sont obligatoires.");
        }

        // ==========================================
        // LE FILTRE DES COLONNES (Clean Data Strategy)
        // ==========================================
        List<Integer> colsToKeep = new ArrayList<>();
        List<String> finalHeaders = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            String headerName = headers.get(i).toUpperCase();
            // Kan-7eydou ay colonne dyal l'email awla l'DEP mn l'Excel final
            if (!headerName.contains("MAIL") && !headerName.contains("EMAIL") && !headerName.contains("DEP")) {
                colsToKeep.add(i);
                finalHeaders.add(headers.get(i));
            }
        }

        // 5. Groupement par partenaire
        Map<String, PartnerData> groupedData = new LinkedHashMap<>();
        Set<String> invalidEmails = new LinkedHashSet<>();

        for (List<String> rowData : allDataRows) {
            // Check minimum bounds for mandatory columns
            if (rowData.size() <= Math.max(partenaireCol, emailCol)) continue;

            String partenaire = rowData.get(partenaireCol).trim();
            String email = rowData.get(emailCol).trim();

            if (partenaire.isEmpty() || email.isEmpty()) continue;

            // Une adresse invalide = un bounce, et les bounces sont la première cause de blacklistage.
            if (!MailSafety.isValidEmail(email)) {
                log.warn("Adresse ignorée (format invalide) pour le partenaire {}", partenaire);
                invalidEmails.add(partenaire);
                continue;
            }

            // Remplacement dynamique Seulement si la colonne Anomalie existe
            String finalAnomalie = "";
            if (anomalieCol != -1 && rowData.size() > anomalieCol) {
                String anomalieCode = rowData.get(anomalieCol).toUpperCase().trim();
                finalAnomalie = dictionary.getOrDefault(anomalieCode, anomalieCode);
            }

            // Construction dyal la ligne n9iya (bla Email w bla DEP)
            List<String> updatedCleanRow = new ArrayList<>();
            for (int idx : colsToKeep) {
                if (idx == anomalieCol && anomalieCol != -1) {
                    updatedCleanRow.add(finalAnomalie);
                } else {
                    updatedCleanRow.add(idx < rowData.size() ? rowData.get(idx) : "");
                }
            }

            PartnerData pData = groupedData.computeIfAbsent(partenaire, k -> new PartnerData(email));
            pData.getRows().add(updatedCleanRow);

            // Gestion dyal Cc par département
            if (depColMain != -1 && rowData.size() > depColMain) {
                String rowDep = rowData.get(depColMain).trim();
                if (depMapping.containsKey(rowDep)) {
                    pData.getCcEmails().addAll(depMapping.get(rowDep));
                }
            }
        }

        // 6. Génération de l'Historique et Dispatch
        for (Map.Entry<String, PartnerData> entry : groupedData.entrySet()) {
            String partenaireName = entry.getKey();
            PartnerData pData = entry.getValue();

            byte[] excelBytes = generateExcelFile(finalHeaders, pData.getRows());

            // CC final = mapping département + CC du formulaire + CC par défaut, dédoublonné et plafonné.
            Set<String> allCc = new LinkedHashSet<>(pData.getCcEmails());
            allCc.addAll(baseCc);
            String[] ccArray = MailSafety.buildCcArray(allCc, pData.getEmail(), props.getMaxCc());

            // Sauvegarder f l'historique "EN ATTENTE"
            MailHistory history = MailHistory.builder()
                    .partenaire(partenaireName)
                    .emailTo(pData.getEmail())
                    .status("EN ATTENTE")
                    .createdAt(LocalDateTime.now())
                    .build();
            history = mailHistoryRepository.save(history);

            log.info("Dispatching Mail Event l'partenaire: " + partenaireName);
            // Sayfet l'Event b l'Id dyal l'historique + pdfBytes
            eventPublisher.publishEvent(new PartnerMailEvent(this, history.getId(), partenaireName, pData.getEmail(), ccArray, excelBytes, pdfBytes, messageTemplate));
        }

        if (!invalidEmails.isEmpty()) {
            log.warn("{} partenaire(s) ignoré(s) pour adresse invalide : {}", invalidEmails.size(), invalidEmails);
        }
        return groupedData.size();
    }

    // ==========================================
    // UTILS & PARSING
    // ==========================================

    private Map<String, Set<String>> parseDepMapping(MultipartFile depFile) throws Exception {
        Map<String, Set<String>> depMap = new LinkedHashMap<>();
        if (depFile == null || depFile.isEmpty()) return depMap;

        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        String original = depFile.getOriginalFilename();
        if (original == null) return depMap;
        String filename = original.toLowerCase();

        if (filename.endsWith(".csv")) parseCsv(depFile, headers, rows);
        else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) parseExcel(depFile, headers, rows);

        int depCol = -1, emailCol = -1;
        for(int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).toUpperCase();
            if (h.contains("DEP")) depCol = i;
            if (h.contains("MAIL") || h.contains("EMAIL")) emailCol = i;
        }

        if (depCol == -1) depCol = 0;
        if (emailCol == -1) emailCol = 1;

        for (List<String> row : rows) {
            if (row.size() <= Math.max(depCol, emailCol)) continue;
            String depVal = row.get(depCol).trim();
            String emailStr = row.get(emailCol).trim();
            if(depVal.isEmpty() || emailStr.isEmpty()) continue;

            Set<String> valid = MailSafety.parseEmailList(emailStr);
            if (valid.isEmpty()) continue;
            depMap.computeIfAbsent(depVal, k -> new LinkedHashSet<>()).addAll(valid);
        }
        return depMap;
    }

    private void parseCsv(MultipartFile file, List<String> headers, List<List<String>> allDataRows) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            br.mark(1024);
            String firstLine = br.readLine();
            br.reset();
            char delimiter = (firstLine != null && firstLine.contains(";")) ? ';' : ',';

            CSVFormat format = CSVFormat.Builder.create()
                    .setDelimiter(delimiter)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build();

            try (CSVParser parser = new CSVParser(br, format)) {
                headers.addAll(parser.getHeaderNames());
                for (CSVRecord record : parser) {
                    List<String> row = new ArrayList<>();
                    for (String header : headers) {
                        row.add(record.isMapped(header) ? record.get(header) : "");
                    }
                    allDataRows.add(row);
                }
            }
        }
    }

    private void parseExcel(MultipartFile file, List<String> headers, List<List<String>> allDataRows) throws Exception {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            workbook.close();
            throw new RuntimeException("Fichier Excel sans Header.");
        }
        for (Cell cell : headerRow) {
            headers.add(getCellStringValue(cell));
        }
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            List<String> rowData = new ArrayList<>();
            for (int c = 0; c < headers.size(); c++) {
                rowData.add(getCellStringValue(row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)));
            }
            allDataRows.add(rowData);
        }
        workbook.close();
    }

    private byte[] generateExcelFile(List<String> headers, List<List<String>> dataRows) throws Exception {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Donnees");
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            header.createCell(i).setCellValue(headers.get(i));
        }
        int rowIdx = 1;
        for (List<String> rowData : dataRows) {
            Row row = sheet.createRow(rowIdx++);
            for (int i = 0; i < rowData.size(); i++) {
                row.createCell(i).setCellValue(rowData.get(i));
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        wb.close();
        return bos.toByteArray();
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    @lombok.Data
    private static class PartnerData {
        private String email;
        private List<List<String>> rows = new ArrayList<>();
        private Set<String> ccEmails = new LinkedHashSet<>();

        public PartnerData(String email) {
            this.email = email;
        }
    }
}