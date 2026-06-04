import MailingForm from "../../components/Mailing/MailingForm";
import MailHistoryTable from "../../components/Mailing/MailHistoryTable";

export default function MailingPage() {
  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "3rem 1.5rem", position: "relative" }}>
      
      {/* Orbs de lumière pour le côté "Premium" */}
      <div style={{ position: "absolute", top: "10%", left: "20%", width: "300px", height: "300px", background: "rgba(212, 175, 55, 0.1)", filter: "blur(100px)", zIndex: -1, borderRadius: "50%" }}></div>
      <div style={{ position: "absolute", top: "40%", right: "10%", width: "400px", height: "400px", background: "rgba(30, 58, 138, 0.15)", filter: "blur(120px)", zIndex: -1, borderRadius: "50%" }}></div>

      <div style={{ 
        marginBottom: "3rem", 
        paddingBottom: "1.5rem",
        borderBottom: "1px solid var(--elite-border)",
        animation: "eliteFadeUp 0.8s cubic-bezier(0.16, 1, 0.3, 1)"
      }}>
        <h1 style={{ 
          fontSize: "2.4rem", 
          color: "var(--text-main)",
          fontWeight: 300,
          letterSpacing: "-0.5px",
          display: "flex", alignItems: "center", gap: "15px"
        }}>
          Déploiement Automatisé
        </h1>
        <p style={{ color: "var(--text-dim)", fontSize: "0.95rem", fontWeight: 400, marginTop: "0.5rem" }}>
          Traitement des données de bout en bout. Cryptage des canaux et injection des matrices de contact.
        </p>
      </div>
      
      <MailingForm />
      <MailHistoryTable /> 
    </div>
  );
}