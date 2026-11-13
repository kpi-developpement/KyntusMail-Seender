package com.seender.mail.controller;

import com.seender.mail.config.MailSendingProperties;
import com.seender.mail.service.ExcelProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final ExcelProcessingService excelProcessingService;
    private final MailSendingProperties props;

    private static final List<String> DATA_EXTENSIONS = List.of(".xlsx", ".xls", ".csv");

    @PostMapping("/upload-and-send")
    public ResponseEntity<String> uploadAndSend(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "depFile", required = false) MultipartFile depFile,
            @RequestParam(value = "pdfFile", required = false) MultipartFile pdfFile,
            @RequestParam(value = "ccEmails", required = false) String ccEmails,
            @RequestParam("message") String messageTemplate) {

        try {
            validate(file, "Base de données", DATA_EXTENSIONS, true);
            validate(depFile, "Mapping CC", DATA_EXTENSIONS, false);
            validate(pdfFile, "Document global", List.of(".pdf"), false);

            if (messageTemplate == null || messageTemplate.isBlank()) {
                return ResponseEntity.badRequest().body("Le message ne peut pas être vide.");
            }
            if (messageTemplate.length() > 20_000) {
                return ResponseEntity.badRequest().body("Le message est trop long (20 000 caractères maximum).");
            }

            int queued = excelProcessingService.processAndDispatch(file, depFile, pdfFile, messageTemplate, ccEmails);
            return ResponseEntity.ok(queued + " email(s) mis en file d'attente. L'envoi est étalé dans le temps "
                    + "(max " + props.getMaxPerHour() + "/heure) pour préserver la réputation du domaine.");

        } catch (IllegalArgumentException e) {
            // Erreur de saisie : le message est destiné à l'utilisateur.
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Pas de détail technique renvoyé au navigateur : il part uniquement dans les logs serveur.
            log.error("Echec du traitement du fichier", e);
            return ResponseEntity.internalServerError()
                    .body("Le traitement du fichier a échoué. Contactez l'administrateur si le problème persiste.");
        }
    }

    private void validate(MultipartFile file, String label, List<String> extensions, boolean required) {
        if (file == null || file.isEmpty()) {
            if (required) throw new IllegalArgumentException("Le fichier « " + label + " » est obligatoire.");
            return;
        }
        if (file.getSize() > props.getMaxUploadBytes()) {
            throw new IllegalArgumentException("Le fichier « " + label + " » dépasse la taille autorisée ("
                    + (props.getMaxUploadBytes() / (1024 * 1024)) + " Mo).");
        }
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le fichier « " + label + " » n'a pas de nom valide.");
        }
        String lower = name.toLowerCase(Locale.ROOT);
        boolean ok = extensions.stream().anyMatch(lower::endsWith);
        if (!ok) {
            throw new IllegalArgumentException("Le fichier « " + label + " » doit être au format "
                    + String.join(" / ", extensions) + ".");
        }
    }
}
