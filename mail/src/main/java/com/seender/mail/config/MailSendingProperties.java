package com.seender.mail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Paramètres d'envoi (délivrabilité / anti-ban).
 * Tout est surchargeable via application.properties ou variables d'environnement.
 */
@Data
@Component
@ConfigurationProperties(prefix = "kyntus.mail")
public class MailSendingProperties {

    /** Adresse expéditrice (doit être couverte par le SPF/DKIM du domaine). */
    private String from = "communication.bytel@kyntus.com";

    /** Nom affiché de l'expéditeur. */
    private String fromName = "Kyntus";

    /** Adresse de réponse. Vide = on utilise 'from'. */
    private String replyTo = "";

    /** Destinataires toujours ajoutés en copie (séparés par , ou ;). */
    private String defaultCc = "zouheir.belhouji@kyntus.com";

    /** Adresse de désinscription publiée dans l'entête List-Unsubscribe. Vide = entête omise. */
    private String unsubscribeMailto = "";

    /** Domaine utilisé pour générer les Message-ID. */
    private String messageIdDomain = "kyntus.com";

    /** Nombre maximum d'emails envoyés par heure glissante. */
    private int maxPerHour = 60;

    /** Délai minimum entre deux envois (ms). */
    private long minDelayMs = 20_000;

    /** Délai maximum entre deux envois (ms) — un délai aléatoire est tiré entre min et max. */
    private long maxDelayMs = 45_000;

    /** Nombre de tentatives en cas d'erreur temporaire (greylisting, throttling serveur...). */
    private int maxAttempts = 3;

    /** Nombre maximum d'adresses en copie par message. */
    private int maxCc = 25;

    /** Taille maximale acceptée pour un fichier uploadé (octets). */
    private long maxUploadBytes = 25L * 1024 * 1024;
}
