import AccountsForm from "../../components/Accounts/AccountsForm";
import MailHistoryTable from "../../components/Mailing/MailHistoryTable";

export default function ComptesPage() {
  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "3rem 1.5rem", position: "relative" }}>
      <div
        style={{
          marginBottom: "3rem",
          paddingBottom: "1.5rem",
          borderBottom: "1px solid var(--elite-border)",
          animation: "eliteFadeUp 0.8s cubic-bezier(0.16, 1, 0.3, 1)",
        }}
      >
        <h1 style={{ fontSize: "2.4rem", color: "var(--text-main)", fontWeight: 300, letterSpacing: "-0.5px" }}>
          Envoi des Comptes
        </h1>
        <p style={{ color: "var(--text-dim)", fontSize: "0.95rem", fontWeight: 400, marginTop: "0.5rem" }}>
          Chaque personne du fichier reçoit un email avec ses propres identifiants en pièce jointe et le lien de l&apos;application.
        </p>
      </div>

      <AccountsForm />
      <MailHistoryTable />
    </div>
  );
}
