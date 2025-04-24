# YourCarYourWay (P13 PoC de Chat WebSocket) - Mono-Repo

![Java](https://img.shields.io/badge/Java-17-%23ED8B00?style=&logo=openjdk&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-3.9.9-%23C71A36?style=&logo=apachemaven&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-%236DB33F?style=&logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-%236DB33F?style=&logo=spring&logoColor=white) ![JPA](https://img.shields.io/badge/JPA-%E2%89%A58.0-%23007B9D?style=&logo=hibernate&logoColor=white) ![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-%2375BAEB?style=&logo=websockets&logoColor=white) ![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.11-%23FF6600?style=&logo=rabbitmq&logoColor=white) ![MySQL](https://img.shields.io/badge/MySQL-8.0-%234479A1?style=&logo=mysql&logoColor=white) ![Swagger UI](https://img.shields.io/badge/Swagger%20UI-v3-%2385EA2D?style=&logo=swagger&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-20.10-%230249ED?style=&logo=docker&logoColor=white)

## 📝 Description

Ce dépôt **mono-repo** contient :
- **Backend** : PoC de plateforme de location de voitures avec chat en temps réel (WebSocket STOMP) et persistance MySQL.
- **Front-end** : Application Angular qui consomme l’API REST et les endpoints WebSocket du backend.

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

- Java 17+
- Maven 3.9+
- Node.js 18+ & npm 9+
- MySQL 8.0+
- RabbitMQ 3.x
- (Optionnel) Docker & Docker Compose

---

## ⚙️ Installation & configuration

### 1. Cloner le dépôt

```bash
git clone <URL_DU_REPO>
cd <nom_du_repo>
```

---

### 2. Backend

#### 2.1 Préparation de la base de données

1. Créez la base **YCYW** :
   ```sql
   CREATE DATABASE YCYW;
   ```
2. Importez les scripts depuis `backend/bdd/` :
   ```bash
   mysql -u root -p YCYW < backend/bdd/script.sql
   mysql -u root -p YCYW < backend/bdd/test_populate.sql
   ```

#### 2.2 Configuration des secrets

Ajustez : `application-secrets.properties` :

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

#### 2.3 Lancer le backend

```bash
cd back/poc_chat
mvn clean spring-boot:run
```

Ou via Docker Compose (préconfiguré) :

```bash
docker-compose up -d
```

---

### 3. Front-end

#### 3.1 Dépendances

```bash
cd front/ycyw
npm install
```

#### 3.2 Configuration des environnements

Dans `front/src/environments/environment.ts` et `environment.prod.ts` :

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  wsUrl: 'ws://localhost:8080/ws-chat'
};
```

#### 3.3 Lancer le front-end

```bash
npm start
```

Accessible sur `http://localhost:4200`.

---

## 📖 Documentation & WebSocket

- **Swagger UI** : `http://localhost:8080/swagger-ui/index.html`
- **WebSocket (STOMP)** :
  - URL WS : `ws://localhost:8080/ws-chat`
  - Topics : `/topic/dialog/{dialogId}`
  - Envoi : `/app/dialog/{dialogId}/message`

---

## 🚀 Endpoints Clés (Backend)

**Auth**  
- `POST /auth/register`  
- `POST /auth/login`  

**Profils**  
- `GET /api/profile/me`  
- `GET /api/profile/{userId}`  
- `PUT /api/profile/{userId}`  

**Dialogues & Messages**  
- `POST /api/dialog/`  
- `GET /api/dialog/{id}`  
- `POST /api/dialog/{dialogId}/message`  
- `POST /api/dialog/{dialogId}/{senderId}/markasread`  
- `POST /api/dialog/{dialogId}/invite/{userId}`  
- `POST /api/dialog/{dialogId}/close`  
- `GET /api/dialog/all`  
- `GET /api/dialog/status/{status}`  
- `GET /api/dialog/sender/{senderId}`  

---

## 📦 Scripts (Front-end)

| Script         | Description                                       |
| -------------- | ------------------------------------------------- |
| `npm start`    | Dev server (http://localhost:4200)                |
| `npm run build`| Build production (dist/)                          |
| `npm run watch`| Watch & rebuild                                  |
| `npm test`     | Tests unitaires (Karma/Jasmine)                   |

---

## 🗂️ Architecture

- **Backend** : Spring Boot, JPA, RabbitMQ, WebSocket STOMP, Swagger.
- **Front-end** : Angular 19, Material, RxJS, STOMP/SockJS.

---

## 📣 Notes

Ce projet en phase de développement est une ébauche à compléter et tester avant production.

## 🏡 Merci pour votre intérêt ! 😊