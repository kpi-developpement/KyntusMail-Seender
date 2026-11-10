"use client";

import React, { useState, useEffect } from "react";
// 🛡️ THE SHIELD: 7eyedna 'Anomalie' mn l'import bach ma-tiye7ch l'build
import { anomalieService } from "../../services/api";
import styles from "./Dictionnaire.module.css";

// 🚀 L'FIX: Déclarina l'Interface hna directement bach TypeScript y-rdekh
export interface Anomalie {
  id?: number;
  code: string;
  description: string;
}

export default function DictionnairePage() {
  const [anomalies, setAnomalies] = useState<Anomalie[]>([]);
  const [code, setCode] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(true);

  const loadAnomalies = async () => {
    try {
      const data = await anomalieService.getAll();
      setAnomalies(data);
    } catch (error) {
      console.error("Erreur de chargement", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAnomalies();
  }, []);

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code || !description) return;

    try {
      await anomalieService.save({ code, description });
      setCode("");
      setDescription("");
      loadAnomalies();
    } catch (error) {
      console.error("Erreur d'ajout", error);
    }
  };

  const handleDelete = async (id?: number) => {
    if (!id) return;
    const confirm = window.confirm("Confirmer la suppression ?");
    if (!confirm) return;

    try {
      await anomalieService.delete(id);
      loadAnomalies();
    } catch (error) {
      console.error("Erreur de suppression", error);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.headerSection}>
        <h1 className={styles.title}>Base de Données</h1>
        <p className={styles.subtitle}>Gestion des règles de remplacement et dictionnaire des anomalies.</p>
      </div>

      <div className={styles.terminal}>
        <form className={styles.addForm} onSubmit={handleAdd}>
          <div className={styles.inputGroup}>
            <label htmlFor="code">Code Original</label>
            <div className={styles.inputWrapper}>
              <input
                id="code"
                type="text"
                className={styles.input}
                placeholder="Ex: NOK_AFTER_PB"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                required
              />
            </div>
          </div>
          <div className={styles.inputGroup}>
            <label htmlFor="description">Description Cible</label>
            <div className={styles.inputWrapper}>
              <input
                id="description"
                type="text"
                className={styles.input}
                placeholder="Ex: Câble manquant"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
              />
            </div>
          </div>
          <button type="submit" className={styles.btnAdd}>
            Injecter la règle
          </button>
        </form>

        <div className={styles.tableContainer}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>ID</th>
                <th>Code de Référence</th>
                <th>Description</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={4} className={styles.emptyState}>Chargement des données...</td></tr>
              ) : anomalies.length === 0 ? (
                <tr><td colSpan={4} className={styles.emptyState}>Aucune règle dans la base.</td></tr>
              ) : (
                anomalies.map((anomalie, index) => (
                  <tr key={anomalie.id} style={{ animationDelay: `${0.05 * index}s` }}>
                    <td style={{ color: "var(--text-dim)" }}>#{anomalie.id}</td>
                    <td><span className={styles.codeBadge}>{anomalie.code}</span></td>
                    <td>{anomalie.description}</td>
                    <td>
                      <button onClick={() => handleDelete(anomalie.id)} className={styles.btnDelete}>
                        Supprimer
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}