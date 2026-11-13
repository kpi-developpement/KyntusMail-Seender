package com.seender.mail.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validation et assainissement de tout ce qui finit dans un message SMTP.
 * Objectif : éviter l'injection d'entêtes (CRLF), les adresses invalides
 * qui font grimper le taux de bounce, et les noms de fichiers dangereux.
 */
public final class MailSafety {

    private MailSafety() {
    }

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$");

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}]");

    /** Une adresse est valide si elle est bien formée, raisonnablement courte et sans caractère de contrôle. */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String trimmed = email.trim();
        if (trimmed.isEmpty() || trimmed.length() > 254) return false;
        if (CONTROL_CHARS.matcher(trimmed).find()) return false;
        if (trimmed.contains("..")) return false;
        return EMAIL.matcher(trimmed).matches();
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Découpe une liste d'adresses séparées par , ; ou espace, en ne gardant
     * que les adresses valides, dédoublonnées et dans l'ordre d'apparition.
     */
    public static Set<String> parseEmailList(String raw) {
        Set<String> result = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) return result;
        for (String part : raw.split("[,;\\s]+")) {
            String candidate = part.trim();
            if (isValidEmail(candidate)) {
                result.add(normalizeEmail(candidate));
            }
        }
        return result;
    }

    /**
     * Nettoie une valeur destinée à un entête (sujet, nom de fichier) :
     * suppression des retours chariot pour bloquer l'injection d'entêtes SMTP.
     */
    public static String sanitizeHeader(String value) {
        if (value == null) return "";
        String cleaned = value.replaceAll("[\\r\\n\\u0000]", " ").trim();
        cleaned = cleaned.replaceAll("\\s{2,}", " ");
        return cleaned.length() > 200 ? cleaned.substring(0, 200) : cleaned;
    }

    /** Nom de fichier sûr pour une pièce jointe (pas de séparateur de chemin, pas de CRLF). */
    public static String sanitizeFilename(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String cleaned = value.replaceAll("[\\r\\n\\u0000/\\\\:*?\"<>|]", "_").trim();
        if (cleaned.isEmpty() || cleaned.equals(".") || cleaned.equals("..")) return fallback;
        return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
    }

    /** Retire de la liste de copie le destinataire principal et limite le nombre d'adresses. */
    public static String[] buildCcArray(Set<String> cc, String mainRecipient, int max) {
        String main = normalizeEmail(mainRecipient);
        List<String> out = new ArrayList<>();
        for (String address : cc) {
            String normalized = normalizeEmail(address);
            if (normalized.equals(main) || !isValidEmail(normalized)) continue;
            out.add(normalized);
            if (out.size() >= max) break;
        }
        return out.toArray(new String[0]);
    }
}
