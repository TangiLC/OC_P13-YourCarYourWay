# YourCarYourWay (P13 PoC de Chat WebSocket) - Mono-Repo

![Java](https://img.shields.io/badge/Java-17-%23ED8B00?style=&logo=openjdk&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-3.9.9-%23C71A36?style=&logo=apachemaven&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-%236DB33F?style=&logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-%236DB33F?style=&logo=spring&logoColor=white) ![JPA](https://img.shields.io/badge/JPA-%E2%89%A58.0-%23007B9D?style=&logo=hibernate&logoColor=white) ![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-%2375BAEB?style=&logo=websockets&logoColor=white) ![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.11-%23FF6600?style=&logo=rabbitmq&logoColor=white) ![MySQL](https://img.shields.io/badge/MySQL-8.0-%234479A1?style=&logo=mysql&logoColor=white) ![Swagger UI](https://img.shields.io/badge/Swagger%20UI-v3-%2385EA2D?style=&logo=swagger&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-20.10-%230249ED?style=&logo=docker&logoColor=white)

## 📝 Description

Ce dépôt **mono-repo** contient :
- **Backend** : PoC de plateforme de location de voitures avec chat en temps réel (WebSocket STOMP) et persistance MySQL.
- **Front-end** : Application Angular qui consomme l'API REST et les endpoints WebSocket du backend.

YourCarYourWay est un projet full-stack développé en Java 17 (Spring Boot 3.3) et Typescript (Angular 19) dans un cadre pédagogique pour le **cursus Full-Stack Java Angular d'OpenClassrooms**. 

---

## 📁 Structure du dépôt

```
.
├── bdd                           # Scripts SQL (script.sql, test_populate.sql)
├── backend/poc_chat
│   ├── src/                      # Code Java Spring Boot
│   ├── pom.xml                   # Configuration Maven
│   └── src/main/resources/
│       └── application-secrets.properties
├── front/ycyw                    # Application Angular
│   ├── src/
│   ├── angular.json
│   ├── package.json
│   └── tsconfig.json
└── README.md                    # Ce fichier
```

---

## 🔧 Prérequis

- Java 17+
- Maven 3.9+
- Node.js 18+ & npm 9+
- MySQL 8.0+
- RabbitMQ 3.x
- Docker & Docker Compose (optionnel, pour l'installation via conteneurs)

---

## ⚙️ Installation & configuration

### 1. Cloner le dépôt

```bash
git clone <URL_DU_REPO>
cd <nom_du_repo>
```

---

## 🚀 Méthode 1 : Installation sans Docker

### 1. Backend

#### 1.1 Préparation de la base de données

1. Créez la base **YCYW** :
   ```sql
   CREATE DATABASE YCYW;
   ```
2. Importez les scripts depuis `bdd/` :
   ```bash
   mysql -u root -p YCYW < bdd/script.sql
   mysql -u root -p YCYW < bdd/test_populate.sql
   ```

#### 1.2 Configuration des secrets

Ajustez : `application-secrets.properties` :

```properties
# MySQL
database.name=YCYW
spring.datasource.username=<mysql_user>
spring.datasource.password=<mysql_password>

# RabbitMQ
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# JWT
jwt.secret=<votre_cle_secrete>
jwt.expiration-ms=3600000
```

#### 1.3 Lancer le backend

```bash
cd back/poc_chat
mvn clean spring-boot:run
```

### 2. Front-end

#### 2.1 Dépendances

```bash
cd front/ycyw
npm install
```

#### 2.2 Configuration des environnements

Dans `front/src/environments/environment.ts` et `environment.prod.ts` :

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  wsUrl: 'ws://localhost:8080/ws-chat'
};
```

#### 2.3 Lancer le front-end

```bash
npm start
```

Accessible sur `http://localhost:4200`.

---

## 🐳 Méthode 2 : Installation avec Docker

### 1. Préparation des fichiers

#### 1.1 Créer un fichier `.env` à la racine du projet

Créez un fichier `.env` contenant les variables d'environnement nécessaires :

```
# MySQL
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=YCYW
MYSQL_USER=<mysql_user>
MYSQL_PASSWORD=<mysql_password>

# RabbitMQ
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest

# JWT
JWT_SECRET=votre_cle_secrete_tres_longue_et_complexe
JWT_EXPIRATION_MS=3600000
```

#### 1.2 Assurez-vous que vos fichiers SQL sont présents

Vérifiez que les fichiers SQL nécessaires sont présents dans le dossier `./bdd/` :
- `script.sql` (structure de la base de données)
- `test_populate.sql` (données de test)

### 2. Lancer l'application avec Docker Compose

```bash
docker-compose up -d
```

Cette commande va :
1. Construire les images Docker pour le backend et le frontend
2. Créer et configurer les conteneurs MySQL et RabbitMQ
3. Lancer tous les services en mode détaché

### 3. Accès aux services

Une fois les conteneurs démarrés, vous pouvez accéder aux services :

- **Frontend** : http://localhost:4201
- **Backend API** : http://localhost:8081/api
- **Swagger UI** : http://localhost:8081/swagger-ui/index.html
- **RabbitMQ Admin** : http://localhost:15672 (utilisateur/mot de passe : guest/guest)

### 4. Gestion des conteneurs Docker

**Voir les logs**
```bash
docker-compose logs -f
```

**Arrêter les services**
```bash
docker-compose down
```

**Reconstruire après modifications**
```bash
docker-compose up --build -d
```

---

## 📖 Documentation & WebSocket

- **Swagger UI** : 
  - Sans Docker : `http://localhost:8080/swagger-ui/index.html`
  - Avec Docker : `http://localhost:8081/swagger-ui/index.html`
  
- **WebSocket (STOMP)** :
  - URL WS (sans Docker) : `ws://localhost:8080/ws-chat`
  - URL WS (avec Docker) : `ws://localhost:8081/ws-chat`
  - Topics : `/topic/dialog/{dialogId}`
  - Envoi : `/app/dialog/{dialogId}/message`

---

## Infos complémentaires

Des fichiers readme complémentaires sont disponibles dans `/back` et `front` pour plus de documentation.
---

## 🔍 Dépannage Docker

- **Problème de ports** : Si vous avez des erreurs indiquant que les ports sont déjà utilisés, assurez-vous qu'aucune application n'utilise les ports 8081, 4201, 3307 (MySQL modifié) et 5672/15672 (RabbitMQ).

- **Base de données non initialisée** : Si la base de données ne semble pas initialisée, vérifiez les logs du conteneur MySQL avec `docker-compose logs mysql`.

- **Erreurs de compilation** : Pour voir les erreurs de compilation backend ou frontend, vérifiez les logs des conteneurs correspondants avec `docker-compose logs backend` ou `docker-compose logs front`.

---

## 📣 Notes

Ce projet en phase de développement est une ébauche à compléter et tester avant production.

## 🏡 Merci pour votre intérêt ! 😊