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