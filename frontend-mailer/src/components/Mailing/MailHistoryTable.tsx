"use client";
import React, { useEffect, useState } from "react";
import { historyService, MailHistory } from "../../services/api";
import styles from "./MailHistoryTable.module.css";

export default function MailHistoryTable() {
  const [history, setHistory] = useState<MailHistory[]>([]);

  const fetchHistory = async () => {
    try {
      const data = await historyService.getHistory();
      setHistory(data);
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    fetchHistory();
    const interval = setInterval(fetchHistory, 10000); 
    return () => clearInterval(interval);
  }, []);

  const getBadgeClass = (status: string) => {
    if (status === "EN ATTENTE") return styles.badgeAttente;
    if (status === "ENVOYE") return styles.badgeEnvoye;
    return styles.badgeErreur;
  };

  return (
    <div className={styles.container}>
      <div className={styles.shimmerTop}></div>
      
      <div className={styles.header}>
        <h3 className={styles.title}>
          <span className={styles.liveIndicator}></span>
          Moniteur de Transmissions
        </h3>
        <button onClick={fetchHistory} className={styles.btnRefresh}>
          Synchroniser
        </button>
      </div>

      <div className={styles.tableWrapper}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Horodatage</th>
              <th>Cible (Partenaire)</th>
              <th>Canal (Email)</th>
              <th>État</th>
              <th>Logs</th>
            </tr>
          </thead>
          <tbody>
            {history.length === 0 ? (
              <tr><td colSpan={5} className={styles.emptyText}>En attente de déploiement.</td></tr>
            ) : (
              history.map((h, index) => (
                <tr key={h.id} style={{ animationDelay: `${0.05 * index}s` }}>
                  <td>{new Date(h.createdAt).toLocaleString()}</td>
                  <td className={styles.targetCell}>{h.partenaire}</td>
                  <td>{h.emailTo}</td>
                  <td><span className={`${styles.badge} ${getBadgeClass(h.status)}`}>{h.status}</span></td>
                  <td className={styles.logError}>{h.errorMessage || "Opération réussie"}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}