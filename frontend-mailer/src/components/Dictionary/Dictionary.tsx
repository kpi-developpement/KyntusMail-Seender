"use client"; // Darouri f Next.js 14+ hit ghan-khdmo b useState w useEffect

import React, { useState, useEffect } from "react";
import { Anomalie } from "../../types/anomalie";
import { anomalieService } from "../../services/api";
import styles from "./Dictionary.module.css";

export default function Dictionary() {
  const [anomalies, setAnomalies] = useState<Anomalie[]>([]);
  const [code, setCode] = useState("");
  const [description, setDescription] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  // Njib l'data mlli y-tcharger l'composant
  useEffect(() => {
    fetchAnomalies();
  }, []);

  const fetchAnomalies = async () => {
    try {
      const data = await anomalieService.getAll();
      setAnomalies(data);
    } catch (error) {
      console.error("Erreur de chargement", error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code || !description) return;

    setIsLoading(true);
    try {
      await anomalieService.save({ code, description });
      // N-vider l'formulaire w n-chargéw tableau jdid
      setCode("");
      setDescription("");
      await fetchAnomalies();
    } catch (error) {
      console.error("Erreur de sauvegarde", error);
      alert("Erreur lors de la sauvegarde.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number | undefined) => {
    if (!id) return;
    const isConfirmed = window.confirm("Baghi tmsse7 had l'anomalie?");
    if (!isConfirmed) return;

    try {
      await anomalieService.delete(id);
      await fetchAnomalies();
    } catch (error) {
      console.error("Erreur de suppression", error);
    }
  };

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>Dictionnaire des Anomalies</h2>

      {/* Formulaire d'ajout */}
      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.inputGroup}>
          <label>Code (Abréviation)</label>
          <input
            type="text"
            className={styles.input}
            placeholder="ex: NOK_AFTER_PB"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            required
          />
        </div>
        <div className={styles.inputGroup}>
          <label>Description Complète</label>
          <input
            type="text"
            className={styles.input}
            placeholder="La phrase complète li ghadi t-remplacer l'code..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
          />
        </div>
        <button type="submit" className={styles.btnSubmit} disabled={isLoading}>
          {isLoading ? "Chargement..." : "Ajouter"}
        </button>
      </form>

      {/* Tableau d'affichage */}
      <div className={styles.tableContainer}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Code</th>
              <th>Description</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {anomalies.map((anomalie) => (
              <tr key={anomalie.id}>
                <td>
                  <span className={styles.codeBadge}>{anomalie.code}</span>
                </td>
                <td>{anomalie.description}</td>
                <td>
                  <button
                    onClick={() => handleDelete(anomalie.id)}
                    className={styles.btnDelete}
                  >
                    Supprimer
                  </button>
                </td>
              </tr>
            ))}
            {anomalies.length === 0 && (
              <tr>
                <td colSpan={3} style={{ textAlign: "center", color: "#64748b" }}>
                  Aucune anomalie dans le dictionnaire. Ajoutez-en une !
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}