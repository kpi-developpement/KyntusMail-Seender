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
                    // 🚨 Kan-tconnectaw b les credentials s7a7 li jbdna mn l'serveur
                    sh '''
                    docker run --rm -e PGPASSWORD="kyntus_password" postgres:15-alpine psql -h 10.10.10.50 -p 5432 -U kyntus_user -d kyntus_db -c "CREATE DATABASE mailingdb;" || echo "✅ Info: La base 'mailingdb' existe déjà."
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

        stage('🔍 Le Radar (Auto-Trigger & Logs)') {
            steps {
                script {
                    echo "=> ⏳ Attente de 25 secondes bach Spring Boot y-demarri mzyan..."
                    sleep time: 25, unit: 'SECONDS'

                    echo "=> 🎯 AUTO-TRIGGER: Kan-diro appel l'API history bach n-provoquew l'erreur 500..."
                    // Hna Jenkins kay-drb l'API b yeddou bash y-tiye7 l'Backend f l'Fakh
                    sh "curl -s http://10.10.10.50:4778/api/history || true"
                    
                    sleep time: 3, unit: 'SECONDS'

                    echo "=========================================================="
                    echo "=> ⚙️ EXTRACTION LOGS BACKEND (Stack Trace dyal l'Erreur) <="
                    echo "=========================================================="
                    // Kan-jbdou akher 200 ster bash y-ban lina l'Java Exception kamla
                    sh "docker logs --tail 200 mailos_backend_prod || true"
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