pipeline {
    agent any

    environment {
        COMPOSE_PROJECT_NAME = "mailos-prod"
    }

    stages {
        stage('🧹 Clean & Checkout') {
            steps {
                script {
                    echo "=> [ÉTAPE 1] Récupération dyal l'code jdid mn GitHub (MailOS)..."
                    cleanWs()
                    checkout scm
                }
            }
        }

        stage('💥 Frappe Chirurgicale (Risk Management)') {
            steps {
                script {
                    echo "=> [ÉTAPE 2] N-tiy7ou GHIR l-containers dyal MailOS..."
                    sh "docker rm -f mailos_backend_prod mailos_frontend_prod || true"
                }
            }
        }

        stage('🚀 Build & Deploy (MailOS)') {
            steps {
                script {
                    echo "=> [ÉTAPE 3] Lancement dyal l'ecosysteme MailOS..."
                    sh "docker compose up -d --build"
                }
            }
        }

        stage('🔍 Audit Logs (Le Radar)') {
            steps {
                script {
                    echo "=> ⏳ Attente de 15 secondes bach Spring Boot y-demarri awla y-crashi..."
                    sleep time: 15, unit: 'SECONDS'

                    echo "=========================================================="
                    echo "=> ⚙️ EXTRACTION LOGS BACKEND (Spring Boot - 150 lignes) <="
                    echo "=========================================================="
                    // Kan-jibou l'logs dyal l'backend
                    sh "docker logs --tail 150 mailos_backend_prod || true"

                    echo "=========================================================="
                    echo "=> 🌐 EXTRACTION LOGS FRONTEND (Next.js - 50 lignes) <="
                    echo "=========================================================="
                    // Kan-jibou l'logs dyal l'frontend
                    sh "docker logs --tail 50 mailos_frontend_prod || true"
                }
            }
        }

        stage('🛡️ Clean Up (Images)') {
            steps {
                script {
                    echo "=> [ÉTAPE 4] Nettoyage dyal les images l-qdam..."
                    sh "docker image prune -f"
                }
            }
        }
    }

    post {
        always {
            echo "🏁 Fin de l'analyse des signaux. Vérifiez les logs ci-dessus."
        }
        success {
            echo "========================================================"
            echo "✅ DÉPLOIEMENT MAILOS RÉUSSI !"
            echo "🌐 Frontend: http://10.10.10.50:1193"
            echo "⚙️ Backend: http://10.10.10.50:4778"
            echo "🛡️ BASE DE DONNÉES SÉCURISÉE (Volume Intact 100%)"
            echo "========================================================"
        }
        failure {
            echo "❌ ÉCHEC DU DÉPLOIEMENT."
        }
    }
}