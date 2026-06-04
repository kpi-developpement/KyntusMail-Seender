package com.seender.mail.controller;

import com.seender.mail.service.ExcelProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExcelController {

    private final ExcelProcessingService excelProcessingService;

    @PostMapping("/upload-and-send")
    public ResponseEntity<String> uploadAndSend(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "depFile", required = false) MultipartFile depFile,
            @RequestParam("message") String messageTemplate) {

        try {
            excelProcessingService.processAndDispatch(file, depFile, messageTemplate);
            return ResponseEntity.ok("Fichier traité avec succès ! Les emails sont en cours d'envoi en arrière-plan.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur : " + e.getMessage());
        }
    }
}