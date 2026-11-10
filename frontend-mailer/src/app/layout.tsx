import type { Metadata } from "next";
import Navbar from "../components/Navigation/Navbar";
import "./globals.css";

export const metadata: Metadata = {
  title: "RiskMail OS | Dynamic Mailing",
  description: "Système d'envoi d'emails dynamiques et filtrage Excel",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr">
      <body>
        {/* Navbar dima kayna */}
        <Navbar /> 
        {/* L'contenu dyal les pages ghay-tbeddel hna */}
        <main className="main-container">
          {children}
        </main>
      </body>
    </html>
  );
}