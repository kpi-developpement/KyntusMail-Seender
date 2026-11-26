package com.seender.mail.controller;

import com.seender.mail.service.AccountMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class AccountController {

    private final AccountMailService accountMailService;

    @PostMapping("/upload-and-send")
    public ResponseEntity<String> uploadAndSend(
            @RequestParam("file") MultipartFile file,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message) {
        try {
            AccountMailService.DispatchResult result = accountMailService.processAndDispatch(file, subject, message);
            StringBuilder sb = new StringBuilder()
                    .append(result.queued())
                    .append(" email(s) mis en file d'attente. L'envoi se fait en arrière-plan (1 email par minute).");
            if (!result.skipped().isEmpty()) {
                sb.append(" Ignorés (adresse invalide) : ").append(String.join(", ", result.skipped()));
            }
            return ResponseEntity.ok(sb.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur : " + e.getMessage());
        }
    }
}
