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

        stage('☢️ Frappe Nucléaire (Sniper DB)') {
            steps {
                script {
                    echo "=> [ÉTAPE 2.5] Diagnostic et Nettoyage ciblé des connexions..."
                    
                    // 1. L'Audit: N-choufo chkoun li m-bloki l'Base de données
                    echo "📊 STATISTIQUES DES CONNEXIONS PAR BASE DE DONNÉES :"
                    sh '''
                    docker run --rm -e PGPASSWORD="waeloujdiastral481123456" postgres:15-alpine psql -h 10.10.10.50 -p 5432 -U postgres -d postgres -c "SELECT datname, count(*) FROM pg_stat_activity GROUP BY datname;" || echo "⚠️ Impossible de lire les stats."
                    '''

                    // 2. Le Nettoyage: Kan-9tlou GHIR l-connexions dyal mailingdb w kyntus_db (0% risque 3la les autres)
                    echo "🧹 Libération des connexions pour MailOS..."
                    sh '''
                    docker run --rm -e PGPASSWORD="waeloujdiastral481123456" postgres:15-alpine psql -h 10.10.10.50 -p 5432 -U postgres -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname IN ('mailingdb', 'kyntus_db') AND pid <> pg_backend_pid();" || echo "⚠️ Échec du nettoyage."
                    '''
                }
            }
        }

        stage('🛡️ Nettoyage Sécurisé (Anti-Zombies)') {
            steps {
                script {
                    echo "=> [ÉTAPE 2.5] Libération SÉCURISÉE des connexions inactives dans PostgreSQL..."
                    echo "=> 0% Risque : On ne touche pas aux données, on ferme juste les connexions réseau 'Idle' (endormies)."
                    
                    // 🚨 THE SHIELD FIX: Kan-9tlou GHIR l-connexions li 'idle' (n3ssin). L'Data w l-volumes b3ad w trankil.
                    sh '''
                    docker run --rm -e PGPASSWORD="waeloujdiastral481123456" postgres:15-alpine psql -h 10.10.10.50 -p 5432 -U postgres -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND pid <> pg_backend_pid();" || true
                    '''
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
                    echo "=> ⏳ Attente de 25 secondes bach Spring Boot y-demarri..."
                    sleep time: 25, unit: 'SECONDS'

                    echo "=========================================================="
                    echo "=> ⚙️ EXTRACTION LOGS BACKEND (Spring Boot - 150 lignes) <="
                    echo "=========================================================="
                    sh "docker logs --tail 150 mailos_backend_prod || true"

                    echo "=========================================================="
                    echo "=> 🌐 EXTRACTION LOGS FRONTEND (Next.js - 50 lignes) <="
                    echo "=========================================================="
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
            echo "🛡️ BASE DE DONNÉES SÉCURISÉE (Connexions fantômes libérées)"
            echo "========================================================"
        }
        failure {
            echo "❌ ÉCHEC DU DÉPLOIEMENT."
        }
    }
}