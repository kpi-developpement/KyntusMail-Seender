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

        stage('🛡️ Frappe Chirurgicale (Arrêt Gracieux)') {
            steps {
                script {
                    echo "=> [ÉTAPE 2] Arrêt GRACIEUX des containers (Prévention des connexions Zombies)..."
                    
                    // L'Arrêt gracieux: Kan-3tiw l'Spring Boot 15 secondes bash y-gta3 l'DB n9i
                    sh "docker stop -t 15 mailos_backend_prod || true"
                    sh "docker stop -t 10 mailos_frontend_prod || true"
                    
                    echo "=> Suppression des anciens containers..."
                    sh "docker rm mailos_backend_prod mailos_frontend_prod || true"
                }
            }
        }

        stage('🚀 Build & Deploy (MailOS)') {
            steps {
                script {
                    echo "=> [ÉTAPE 3] Lancement de l'écosystème MailOS..."
                    // Build w Lancement b docker-compose
                    sh "docker compose up -d --build"
                }
            }
        }

        stage('🔍 Audit Logs (Le Radar)') {
            steps {
                script {
                    echo "=> ⏳ Attente de 25 secondes bach Spring Boot y-connecta m3a PostgreSQL..."
                    sleep time: 25, unit: 'SECONDS'

                    echo "=========================================================="
                    echo "=> ⚙️ EXTRACTION LOGS BACKEND (Spring Boot - 100 lignes) <="
                    echo "=========================================================="
                    sh "docker logs --tail 100 mailos_backend_prod || true"

                    echo "=========================================================="
                    echo "=> 🌐 EXTRACTION LOGS FRONTEND (Next.js - 50 lignes) <="
                    echo "=========================================================="
                    sh "docker logs --tail 50 mailos_frontend_prod || true"
                }
            }
        }

        stage('🧹 Clean Up (Optimisation Serveur)') {
            steps {
                script {
                    echo "=> [ÉTAPE 4] Nettoyage des images Docker obsolètes..."
                    sh "docker image prune -f"
                }
            }
        }
    }

    post {
        always {
            echo "🏁 Fin de l'analyse. Le Radar a capturé les derniers signaux."
        }
        success {
            echo "========================================================"
            echo "✅ DÉPLOIEMENT MAILOS RÉUSSI (100% SÉCURISÉ) !"
            echo "🌐 Frontend: http://10.10.10.50:1193"
            echo "⚙️ Backend:  http://10.10.10.50:4778"
            echo "🛡️ Architecture: Spring Boot + Next.js (Pure CSS)"
            echo "========================================================"
        }
        failure {
            echo "❌ ÉCHEC DU DÉPLOIEMENT. Vérifiez le Radar (Logs) ci-dessus."
        }
    }
}