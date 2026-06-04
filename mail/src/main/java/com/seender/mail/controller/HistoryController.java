package com.seender.mail.controller;

import com.seender.mail.entity.MailHistory;
import com.seender.mail.repository.MailHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HistoryController {

    private final MailHistoryRepository mailHistoryRepository;

    @GetMapping
    public ResponseEntity<List<MailHistory>> getHistory() {
        return ResponseEntity.ok(mailHistoryRepository.findAllByOrderByCreatedAtDesc());
    }
}