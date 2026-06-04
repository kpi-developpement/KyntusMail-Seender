"use client";

import React, { useState } from "react";
import { excelService } from "../../services/api";
import styles from "./MailingForm.module.css";

export default function MailingForm() {
  const [file, setFile] = useState<File | null>(null);
  const [depFile, setDepFile] = useState<File | null>(null);
  const [message, setMessage] = useState(
    "Bonjour {{NOM_PARTENAIRE}},\n\nVeuillez trouver ci-joint le rapport des anomalies.\n\nCordialement,\nL'équipe Kyntus."
  );
  const [isLoading, setIsLoading] = useState(false);
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) setFile(e.target.files[0]);
  };

  const handleDepFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) setDepFile(e.target.files[0]);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFeedback(null);

    if (!file) {
      setFeedback({ type: "error", text: "Veuillez sélectionner le fichier principal." });
      return;
    }

    if (!message.includes("{{NOM_PARTENAIRE}}")) {
      const confirm = window.confirm("Le message ne contient pas {{NOM_PARTENAIRE}}. Continuer ?");
      if (!confirm) return;
    }

    setIsLoading(true);
    try {
      const responseMsg = await excelService.uploadAndSend(file, message, depFile);
      setFeedback({ type: "success", text: responseMsg });
      
      setFile(null);
      setDepFile(null);
      (document.getElementById('excelUpload') as HTMLInputElement).value = '';
      const depInput = document.getElementById('depUpload') as HTMLInputElement;
      if (depInput) depInput.value = '';

    } catch (error: any) {
      setFeedback({ type: "error", text: error.message });
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
        
        {/* INPUT 1 CUSTOM */}
        <div className={styles.formGroup}>
          <label>Fichier Source Principal</label>
          <div className={styles.customUploadWrapper}>
            <input
              id="excelUpload"
              type="file"
              accept=".xlsx, .xls, .csv"
              onChange={handleFileChange}
              className={styles.hiddenInput}
              required
            />
            <label htmlFor="excelUpload" className={styles.customUploadBtn}>
              <span className={styles.uploadIcon}>+</span>
              <span className={styles.uploadText}>
                {file ? file.name : "Sélectionner un fichier"}
              </span>
            </label>
          </div>
        </div>

        {/* INPUT 2 CUSTOM */}
        <div className={styles.formGroup}>
          <label>Mapping des Départements (Cc)</label>
          <div className={styles.customUploadWrapper}>
            <input
              id="depUpload"
              type="file"
              accept=".xlsx, .xls, .csv"
              onChange={handleDepFileChange}
              className={styles.hiddenInput}
            />
            <label htmlFor="depUpload" className={styles.customUploadBtn}>
              <span className={styles.uploadIcon}>+</span>
              <span className={styles.uploadText}>
                {depFile ? depFile.name : "Fichier optionnel"}
              </span>
            </label>
          </div>
        </div>

        <div className={styles.formGroup} style={{ gridColumn: "1 / -1" }}>
          <label htmlFor="messageTemplate">Code d'Injection (Message)</label>
          <div className={styles.textareaWrapper}>
            <textarea
              id="messageTemplate"
              className={styles.textarea}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              required
            />
          </div>
        </div>

        <button type="submit" className={styles.btnSubmit} disabled={isLoading || !file}>
          {isLoading ? "Synchronisation en cours..." : "Initialiser le Déploiement"}
        </button>
      </form>
    </div>
  );
}