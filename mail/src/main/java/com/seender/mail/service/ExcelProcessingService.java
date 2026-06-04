package com.seender.mail.service;

import com.seender.mail.entity.Anomalie;
import com.seender.mail.entity.MailHistory;
import com.seender.mail.event.PartnerMailEvent;
import com.seender.mail.repository.AnomalieRepository;
import com.seender.mail.repository.MailHistoryRepository;
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

    public void processAndDispatch(MultipartFile file, MultipartFile depFile, String messageTemplate) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) throw new RuntimeException("Fichier invalide.");

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
            throw new RuntimeException("Format non supporté. Veuillez uploader un fichier .xlsx ou .csv");
        }

        // 4. Détection des colonnes
        int partenaireCol = -1, anomalieCol = -1, emailCol = -1, depColMain = -1;
        for (int i = 0; i < headers.size(); i++) {
            String headerName = headers.get(i).toUpperCase();
            if (headerName.contains("PARTENAIRE")) partenaireCol = i;
            if (headerName.contains("ANOMALIE")) anomalieCol = i;
            if (headerName.contains("MAIL") || headerName.contains("EMAIL")) emailCol = i;
            if (headerName.contains("DEP")) depColMain = i;
        }

        if (partenaireCol == -1 || anomalieCol == -1 || emailCol == -1) {
            throw new RuntimeException("Les colonnes 'Partenaire', 'Anomalie', et 'Email' sont obligatoires.");
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
        Map<String, PartnerData> groupedData = new HashMap<>();

        for (List<String> rowData : allDataRows) {
            if (rowData.size() <= Math.max(partenaireCol, Math.max(anomalieCol, emailCol))) continue;

            String partenaire = rowData.get(partenaireCol).trim();
            String email = rowData.get(emailCol).trim();
            String anomalieCode = rowData.get(anomalieCol).toUpperCase().trim();

            if (partenaire.isEmpty() || email.isEmpty()) continue;

            // Remplacement dynamique
            String finalAnomalie = dictionary.getOrDefault(anomalieCode, anomalieCode);

            // Construction dyal la ligne n9iya (bla Email w bla DEP)
            List<String> updatedCleanRow = new ArrayList<>();
            for (int idx : colsToKeep) {
                if (idx == anomalieCol) {
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
            String[] ccArray = pData.getCcEmails().toArray(new String[0]);

            // Sauvegarder f l'historique "EN ATTENTE"
            MailHistory history = MailHistory.builder()
                    .partenaire(partenaireName)
                    .emailTo(pData.getEmail())
                    .status("EN ATTENTE")
                    .createdAt(LocalDateTime.now())
                    .build();
            history = mailHistoryRepository.save(history);

            log.info("Dispatching Mail Event l'partenaire: " + partenaireName);
            // Sayfet l'Event b l'Id dyal l'historique
            eventPublisher.publishEvent(new PartnerMailEvent(this, history.getId(), partenaireName, pData.getEmail(), ccArray, excelBytes, messageTemplate));
        }
    }

    // ==========================================
    // UTILS & PARSING
    // ==========================================

    private Map<String, Set<String>> parseDepMapping(MultipartFile depFile) throws Exception {
        Map<String, Set<String>> depMap = new HashMap<>();
        if (depFile == null || depFile.isEmpty()) return depMap;

        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        String filename = depFile.getOriginalFilename().toLowerCase();

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

            String[] splitEmails = emailStr.split("[,;]");
            depMap.putIfAbsent(depVal, new HashSet<>());
            for(String e : splitEmails) {
                if(!e.trim().isEmpty()) depMap.get(depVal).add(e.trim());
            }
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
        Sheet sheet = wb.createSheet("Anomalies");
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
        private Set<String> ccEmails = new HashSet<>();

        public PartnerData(String email) {
            this.email = email;
        }
    }
}