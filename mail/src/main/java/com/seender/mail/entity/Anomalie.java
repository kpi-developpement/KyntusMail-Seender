package com.seender.mail.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "anomalies_dictionary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anomalie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Le code abréviation (ex: "NOK_AFTER_PB")
    @Column(nullable = false, unique = true)
    private String code;

    // La description complète (ex: "La photo du signal NOK est aprés...")
    // Derna length = 1000 bach la kant chi phrase twila ma-y-w9e3ch mochkil f DB
    @Column(nullable = false, length = 1000)
    private String description;
}