"use client";

import React, { useState } from "react";
import { excelService } from "../../services/api";
import styles from "./MailingForm.module.css";

// Adresse(s) mise(s) en copie par défaut sur chaque envoi.
const DEFAULT_CC = ["zouheir.belhouji@kyntus.com"];

const EMAIL_RE = /^[^\s@,;]+@[^\s@,;.]+(\.[^\s@,;.]+)+$/;

export default function MailingForm() {
  const [file, setFile] = useState<File | null>(null);
  const [pdfFile, setPdfFile] = useState<File | null>(null);
  const [depFile, setDepFile] = useState<File | null>(null);
  const [ccList, setCcList] = useState<string[]>(DEFAULT_CC);
  const [ccDraft, setCcDraft] = useState("");
  const [ccError, setCcError] = useState<string | null>(null);
  const [message, setMessage] = useState(
    "Bonjour {{NOM_PARTENAIRE}},\n\nVeuillez trouver ci-joint vos accès et documents sécurisés.\n\nCordialement,\nL'équipe Kyntus."
  );
  const [isLoading, setIsLoading] = useState(false);
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>, setter: React.Dispatch<React.SetStateAction<File | null>>) => {
    if (e.target.files && e.target.files.length > 0) setter(e.target.files[0]);
  };

  /** Ajoute une ou plusieurs adresses saisies (séparées par , ; ou espace). Retourne false si l'une est invalide. */
  const commitCcDraft = (raw: string): boolean => {
    const parts = raw.split(/[,;\s]+/).map((p) => p.trim()).filter(Boolean);
    if (parts.length === 0) {
      setCcDraft("");
      return true;
    }
    const invalid = parts.filter((p) => !EMAIL_RE.test(p));
    if (invalid.length > 0) {
      setCcError(`Adresse invalide : ${invalid[0]}`);
      return false;
    }
    setCcList((prev) => {
      const seen = new Set(prev.map((e) => e.toLowerCase()));
      const added = parts.filter((p) => !seen.has(p.toLowerCase()));
      return [...prev, ...added];
    });
    setCcDraft("");
    setCcError(null);
    return true;
  };

  const handleCcKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" || e.key === "," || e.key === ";" || e.key === " " || e.key === "Tab") {
      if (ccDraft.trim() === "") return;
      e.preventDefault();
      commitCcDraft(ccDraft);
    } else if (e.key === "Backspace" && ccDraft === "" && ccList.length > 0) {
      setCcList((prev) => prev.slice(0, -1));
    }
  };

  const removeCc = (email: string) => {
    setCcList((prev) => prev.filter((e) => e !== email));
    setCcError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFeedback(null);

    if (!file) {
      setFeedback({ type: "error", text: "Le fichier Excel Source (Base) est obligatoire." });
      return;
    }

    // Une adresse encore dans le champ de saisie doit être prise en compte.
    if (ccDraft.trim() !== "" && !commitCcDraft(ccDraft)) return;

    if (!message.includes("{{NOM_PARTENAIRE}}")) {
      const confirm = window.confirm("Le message ne contient pas la balise {{NOM_PARTENAIRE}}. Continuer ?");
      if (!confirm) return;
    }

    const ccToSend = ccDraft.trim() !== ""
      ? [...ccList, ...ccDraft.split(/[,;\s]+/).map((p) => p.trim()).filter(Boolean)]
      : ccList;

    setIsLoading(true);
    try {
      // 🚨 Passina l'pdfFile hna bach y-mchi f la requête POST
      const responseMsg = await excelService.uploadAndSend(file, message, depFile, pdfFile, ccToSend.join(","));
      setFeedback({ type: "success", text: responseMsg });

      // Reset
      setFile(null);
      setPdfFile(null);
      setDepFile(null);
      setCcList(DEFAULT_CC);
      setCcDraft("");
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
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            {feedback.type === "success" 
              ? <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              : <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            }
          </svg>
          {feedback.text}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        
        {/* LES CARTES D'UPLOAD (Dropzones) */}
        <div className={styles.formGrid}>
          
          {/* CARTE 1 : EXCEL BASE */}
          <label htmlFor="excelUpload" className={`${styles.uploadCard} ${file ? styles.hasFile : ''}`}>
            <div className={styles.iconWrapper}>
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="8" y1="13" x2="16" y2="13"></line>
                <line x1="8" y1="17" x2="16" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
            </div>
            <div className={styles.cardTitle}>Base de Données</div>
            <div className={styles.cardDesc}>.xlsx, .csv (Obligatoire)</div>
            {file && <div className={styles.fileName}>{file.name}</div>}
            <input id="excelUpload" type="file" accept=".xlsx, .xls, .csv" onChange={(e) => handleFileChange(e, setFile)} className={styles.hiddenInput} required />
          </label>

          {/* CARTE 2 : PDF GLOBAL */}
          <label htmlFor="pdfUpload" className={`${styles.uploadCard} ${pdfFile ? styles.hasFile : ''}`}>
            <div className={styles.iconWrapper}>
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <path d="M9 15v-4"></path><path d="M12 15v-4"></path><path d="M15 15v-4"></path>
              </svg>
            </div>
            <div className={styles.cardTitle}>Document Global</div>
            <div className={styles.cardDesc}>.pdf (Optionnel)</div>
            {pdfFile && <div className={styles.fileName}>{pdfFile.name}</div>}
            <input id="pdfUpload" type="file" accept=".pdf" onChange={(e) => handleFileChange(e, setPdfFile)} className={styles.hiddenInput} />
          </label>

          {/* CARTE 3 : MAPPING CC */}
          <label htmlFor="depUpload" className={`${styles.uploadCard} ${depFile ? styles.hasFile : ''}`}>
            <div className={styles.iconWrapper}>
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
              </svg>
            </div>
            <div className={styles.cardTitle}>Mapping CC (Réseau)</div>
            <div className={styles.cardDesc}>.xlsx (Optionnel)</div>
            {depFile && <div className={styles.fileName}>{depFile.name}</div>}
            <input id="depUpload" type="file" accept=".xlsx, .xls, .csv" onChange={(e) => handleFileChange(e, setDepFile)} className={styles.hiddenInput} />
          </label>

        </div>

        {/* CARTE : DESTINATAIRES EN COPIE (CC) */}
        <div className={styles.ccSection}>
          <div className={styles.editorHeader}>
            <span className={styles.editorLabel}>Destinataires en copie (CC)</span>
            <span className={styles.ccCount}>
              {ccList.length} adresse{ccList.length > 1 ? "s" : ""}
            </span>
          </div>

          <div className={`${styles.ccBox} ${ccError ? styles.ccBoxError : ""}`}>
            {ccList.map((email) => (
              <span key={email} className={styles.chip}>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                  <polyline points="22,6 12,13 2,6"></polyline>
                </svg>
                {email}
                <button
                  type="button"
                  className={styles.chipRemove}
                  onClick={() => removeCc(email)}
                  aria-label={`Retirer ${email}`}
                >
                  ×
                </button>
              </span>
            ))}

            <input
              type="text"
              className={styles.ccInput}
              value={ccDraft}
              placeholder={ccList.length === 0 ? "nom@kyntus.com" : "Ajouter une adresse…"}
              onChange={(e) => {
                setCcDraft(e.target.value);
                if (ccError) setCcError(null);
              }}
              onKeyDown={handleCcKeyDown}
              onBlur={() => { if (ccDraft.trim() !== "") commitCcDraft(ccDraft); }}
              onPaste={(e) => {
                const text = e.clipboardData.getData("text");
                if (/[,;\s]/.test(text)) {
                  e.preventDefault();
                  commitCcDraft(text);
                }
              }}
            />
          </div>

          <div className={ccError ? styles.ccErrorText : styles.ccHint}>
            {ccError ?? "Entrée, virgule ou point-virgule pour valider. Ces adresses sont en copie de chaque email, en plus du mapping CC par département."}
          </div>
        </div>

        {/* EDITOR DE MESSAGE */}
        <div className={styles.editorSection}>
          <div className={styles.editorHeader}>
            <span className={styles.editorLabel}>Moteur d'Injection (Message)</span>
          </div>
          <div className={styles.textareaWrapper}>
            <textarea
              className={styles.textarea}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              required
            />
          </div>
        </div>

        <button type="submit" className={styles.btnSubmit} disabled={isLoading || !file}>
          {isLoading ? "Déploiement en cours..." : "Initialiser la Transmission"}
        </button>
      </form>
    </div>
  );
}