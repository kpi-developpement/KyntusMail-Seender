"use client";

import React, { useState } from "react";
import { accountService } from "../../services/api";
import styles from "../Mailing/MailingForm.module.css";

const DEFAULT_SUBJECT = "Vos identifiants d'accès à l'application MyKyntus";

const DEFAULT_MESSAGE =
  "Bonjour {{PRENOM}} {{NOM}},\n\n" +
  "Vous trouverez ci-joint un fichier Excel contenant vos identifiants personnels (identifiant et mot de passe) pour accéder à l'application MyKyntus.\n\n" +
  "Lien de l'application : {{LIEN}}\n\n" +
  "Ces informations sont strictement personnelles et confidentielles, merci de ne pas les partager.\n\n" +
  "Cordialement,\nL'équipe Kyntus.";

export default function AccountsForm() {
  const [file, setFile] = useState<File | null>(null);
  const [subject, setSubject] = useState(DEFAULT_SUBJECT);
  const [message, setMessage] = useState(DEFAULT_MESSAGE);
  const [isLoading, setIsLoading] = useState(false);
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) setFile(e.target.files[0]);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFeedback(null);

    if (!file) {
      setFeedback({ type: "error", text: "Sélectionnez le fichier Excel des comptes." });
      return;
    }

    if (!window.confirm("Chaque personne du fichier va recevoir ses identifiants par email. Lancer l'envoi ?")) return;

    setIsLoading(true);
    try {
      const responseMsg = await accountService.uploadAndSend(file, subject, message);
      setFeedback({ type: "success", text: responseMsg });
      setFile(null);
      (document.getElementById("accountsUpload") as HTMLInputElement).value = "";
    } catch (error: unknown) {
      setFeedback({ type: "error", text: error instanceof Error ? error.message : "Erreur inconnue" });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      {feedback && (
        <div className={`${styles.alert} ${feedback.type === "success" ? styles.alertSuccess : styles.alertError}`}>
          {feedback.text}
        </div>
      )}

      <form onSubmit={handleSubmit} className={styles.formGrid}>
        <div className={styles.formGroup}>
          <label htmlFor="accountsUpload">Fichier des comptes (.xlsx)</label>
          <div className={styles.customUploadWrapper}>
            <input
              id="accountsUpload"
              type="file"
              accept=".xlsx, .xls"
              onChange={handleFileChange}
              className={styles.hiddenInput}
              required
            />
            <label htmlFor="accountsUpload" className={styles.customUploadBtn}>
              <span className={styles.uploadIcon}>+</span>
              <span className={styles.uploadText}>{file ? file.name : "Sélectionner un fichier"}</span>
            </label>
          </div>
          <span style={{ color: "var(--text-dim)", fontSize: "0.8rem" }}>
            Colonnes attendues : Nom, Prénom, Email, Lien de l&apos;application, Identifiant, Mot de passe.
          </span>
        </div>

        <div className={styles.formGroup}>
          <label htmlFor="accountsSubject">Objet du mail</label>
          <div className={styles.textareaWrapper}>
            <input
              id="accountsSubject"
              type="text"
              className={styles.textarea}
              style={{ minHeight: "auto", padding: "1rem 1.5rem" }}
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              required
            />
          </div>
        </div>

        <div className={styles.formGroup} style={{ gridColumn: "1 / -1" }}>
          <label htmlFor="accountsMessage">Message</label>
          <div className={styles.textareaWrapper}>
            <textarea
              id="accountsMessage"
              className={styles.textarea}
              style={{ minHeight: "240px" }}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              required
            />
          </div>
          <span style={{ color: "var(--text-dim)", fontSize: "0.8rem" }}>
            Variables : {"{{PRENOM}}"}, {"{{NOM}}"}, {"{{LIEN}}"}, {"{{EMAIL}}"} — remplacées pour chaque personne. Le fichier joint
            contient uniquement la ligne de la personne.
          </span>
        </div>

        <button type="submit" className={styles.btnSubmit} disabled={isLoading || !file}>
          {isLoading ? "Envoi en cours..." : "Envoyer les identifiants"}
        </button>
      </form>
    </div>
  );
}
