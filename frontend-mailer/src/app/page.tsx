import Link from "next/link";
import styles from "./Home.module.css";

export default function Home() {
  return (
    <div className={styles.heroSection}>
      {/* Background Orbs pour le luxe */}
      <div className={styles.goldOrbLarge}></div>
      <div className={styles.blueOrbLarge}></div>
      
      <div className={styles.contentBox}>
        <div className={styles.badge}>SYSTÈME OPÉRATIONNEL</div>
        <h1 className={styles.title}>RiskMail OS</h1>
        <p className={styles.subtitle}>
          Plateforme de gestion de risques et distribution automatisée. <br/>
          Sécurité maximale, précision absolue.
        </p>
        
        <div className={styles.actionGrid}>
          <Link href="/dictionnaire" className={`${styles.btnElite} ${styles.btnSecondary}`}>
            <span className={styles.btnDesc}>Module 01</span>
            Base de Données
          </Link>
          
          <Link href="/mailing" className={styles.btnElite}>
            <span className={styles.btnDesc}>Module 02</span>
            Déploiement
          </Link>
        </div>
      </div>
    </div>
  );
}