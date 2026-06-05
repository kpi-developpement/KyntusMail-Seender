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
                    sh "docker stop -t 15 mailos_backend_prod || true"
                    sh "docker stop -t 10 mailos_frontend_prod || true"
                    sh "docker rm mailos_backend_prod mailos_frontend_prod || true"
                }
            }
        }

        stage('🛠️ Initialisation DB (Création mailingdb)') {
            steps {
                script {
                    echo "=> [ÉTAPE 2.5] Forçage de la création de la base 'mailingdb' dans le serveur..."
                    
                    // 🚨 THE FIX: Kan-tconnectaw l'base par défaut (postgres) bach n-creyiw l'base dyalna. 
                    // '|| true' bach ila kanet déjà kayna, l'pipeline ma-y-crachich.
                    sh '''
                    docker run --rm -e PGPASSWORD="waeloujdiastral481123456" postgres:15-alpine psql -h 10.10.10.50 -p 5432 -U postgres -d postgres -c "CREATE DATABASE mailingdb;" || echo "✅ Info: La base 'mailingdb' existe déjà."
                    '''
                }
            }
        }

        stage('🚀 Build & Deploy (MailOS)') {
            steps {
                script {
                    echo "=> [ÉTAPE 3] Lancement de l'écosystème MailOS..."
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
            echo "✅ DÉPLOIEMENT MAILOS RÉUSSI (ISOLATION 100%) !"
            echo "🌐 Frontend: http://10.10.10.50:1193"
            echo "⚙️ Backend:  http://10.10.10.50:4778"
            echo "🛡️ Base de données propre: mailingdb"
            echo "========================================================"
        }
        failure {
            echo "❌ ÉCHEC DU DÉPLOIEMENT. Vérifiez le Radar (Logs) ci-dessus."
        }
    }
}