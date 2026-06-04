"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import styles from "./Navbar.module.css";

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 50) {
        setScrolled(true);
      } else {
        setScrolled(false);
      }
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <div className={`${styles.navWrapper} ${scrolled ? styles.wrapperScrolled : ""}`}>
      <nav className={`${styles.nav} ${scrolled ? styles.navScrolled : ""}`}>
        
        {/* L'Animation dyal l'Gelsa (Scanner d'Or) */}
        <div className={styles.shimmerBorder}></div>

        <div className={styles.navContainer}>
          <div className={styles.logoBox}>
            {/* L'Orb d'énergie au lieu du réacteur */}
            <div className={styles.goldOrb}></div>
            <div className={styles.logo}>RiskMail OS</div>
          </div>
          
          <ul className={styles.menu}>
            <li className={styles.menuItem} style={{ animationDelay: "0.2s" }}>
              <Link href="/" className={styles.link}>Système</Link>
            </li>
            <li className={styles.menuItem} style={{ animationDelay: "0.3s" }}>
              <Link href="/dictionnaire" className={styles.link}>Base de Données</Link>
            </li>
            <li className={styles.menuItem} style={{ animationDelay: "0.4s" }}>
              <Link href="/mailing" className={styles.link}>Déploiement</Link>
            </li>
          </ul>
        </div>
      </nav>
    </div>
  );
}