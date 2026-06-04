import { Anomalie } from '../types/anomalie';

// 🚨 THE FIX: Frappe Chirurgicale 3la Localhost
// Kan-9raw l'URL mn l'environnement (Docker), awla kan-pointiw nichan 3la l'serveur f l'production.
const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://10.10.10.50:4778';
const API_URL = `${BASE_URL}/api`;

export const anomalieService = {
  getAll: async (): Promise<Anomalie[]> => {
    const response = await fetch(`${API_URL}/anomalies`);
    if (!response.ok) throw new Error('Erreur réseau');
    return response.json();
  },
  save: async (anomalie: Anomalie): Promise<Anomalie> => {
    const response = await fetch(`${API_URL}/anomalies`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(anomalie),
    });
    if (!response.ok) throw new Error('Erreur de sauvegarde');
    return response.json();
  },
  delete: async (id: number): Promise<void> => {
    const response = await fetch(`${API_URL}/anomalies/${id}`, { method: 'DELETE' });
    if (!response.ok) throw new Error('Erreur de suppression');
  },
};

export const excelService = {
  // L'ajout dyal depFile hnaya (fichier facultatif)
  uploadAndSend: async (file: File, message: string, depFile?: File | null): Promise<string> => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("message", message);
    if (depFile) {
      formData.append("depFile", depFile);
    }

    const response = await fetch(`${API_URL}/excel/upload-and-send`, {
      method: "POST",
      body: formData,
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || "Erreur lors de l'envoi du fichier");
    }
    return response.text();
  },
};

export interface MailHistory {
  id: number;
  partenaire: string;
  emailTo: string;
  status: string;
  errorMessage?: string;
  createdAt: string;
}

export const historyService = {
  getHistory: async (): Promise<MailHistory[]> => {
    const response = await fetch(`${API_URL}/history`);
    if (!response.ok) throw new Error('Erreur réseau');
    return response.json();
  }
};