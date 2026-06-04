package com.seender.mail.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "mail_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String partenaire;
    private String emailTo;

    // Status possibles: "EN ATTENTE", "ENVOYE", "ERREUR"
    private String status;

    @Column(length = 1000)
    private String errorMessage;

    private LocalDateTime createdAt;
}