# YourCarYourWay (P13 PoC de Chat WebSocket)

![Java](https://img.shields.io/badge/Java-17-%23ED8B00?style=&logo=openjdk&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-3.9.9-%23C71A36?style=&logo=apachemaven&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-%236DB33F?style=&logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-%236DB33F?style=&logo=spring&logoColor=white) ![Spring Data JPA](https://img.shields.io/badge/JPA-%E2%89%A58.0-%23007B9D?style=&logo=hibernate&logoColor=white) ![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-%2375BAEB?style=&logo=websockets&logoColor=white) ![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.11-%23FF6600?style=&logo=rabbitmq&logoColor=white) ![MySQL](https://img.shields.io/badge/MySQL-8.0-%234479A1?style=&logo=mysql&logoColor=white) ![Swagger UI](https://img.shields.io/badge/Swagger%20UI-v3-%2385EA2D?style=&logo=swagger&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-20.10-%230249ED?style=&logo=docker&logoColor=white)

## 📝 Description

YourCarYourWay est un **Proof-of-Concept** backend de plateforme de location de voitures, focalisé sur le chat en temps réel via **WebSocket** (STOMP) et la persistance en base de données **MySQL**. C'est un projet pédagogique pour le projet 13 du **cursus Full-Stack Java Angular d'OpenClassrooms**.
L'application expose une API REST sécurisée (JWT) pour la gestion des utilisateurs, profils et dialogues, et utilise **RabbitMQ** comme broker de messages.

Le front-end associé, situé dans le dossier `/front`, consomme cette API et la couche WebSocket.

---

## 🛠️ Technologies & dépendances principales

- Langage : **Java 17**
- Framework : **Spring Boot** 3.x
- Sécurité : **Spring Security**, JWT (JJWT)
- Persistance : **Spring Data JPA**, **MySQL 8.0**
- Messaging : **Spring AMQP** (RabbitMQ), **STOMP over WebSocket**
- Documentation : **springdoc-openapi-ui** (Swagger v3)
- Build : **Maven 3.9.9**
- Containerisation (optionnel) : **Docker**, **Docker Compose**

---

## 🔧 Prérequis

- Java 17 ou supérieur
- Maven 3.9+
- MySQL 8.0+
- RabbitMQ 3.x

---

## ⚙️ Installation & configuration

### 1. Cloner le dépôt

```bash
git clone <URL_DU_REPO>
cd <nom_du_repo>
```

### 2. Préparation de la base de données

- Les scripts SQL (`script.sql` et `test_populate.sql`) se trouvent dans le dossier `/bdd`.
- Créez la base de données **YCYW** :
  ```sql
  CREATE DATABASE YCYW;
  ```
- Importez les scripts :
  ```bash
  mysql -u root -p YCYW < bdd/script.sql
  mysql -u root -p YCYW < bdd/test_populate.sql
  ```

### 3. Configuration des secrets

Ajoutez un fichier `application-secrets.properties` dans `src/main/resources/` :

```properties
# ----------------------------------------
# Credentials MySQL
# ----------------------------------------
database.name=YCYW
spring.datasource.username=<votre_user_mysql>
spring.datasource.password=<votre_password_mysql>

# ----------------------------------------
# Credentials RabbitMQ
# ----------------------------------------
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# ----------------------------------------
# JWT Secret & expiration
# ----------------------------------------
jwt.secret=<votre_cle_secrete>
jwt.expiration-ms=3600000
```

### 4. Lancer l’application

```bash
mvn spring-boot:run
```

Ou via Docker Compose si vous avez un fichier `docker-compose.yml` défini en amont pour MySQL & RabbitMQ.

---

## 📖 Documentation Swagger & utilisation du chat

- La documentation Swagger est disponible dès que le serveur tourne à l’adresse :  
  `http://localhost:8080/swagger-ui/index.html`
- Pour les endpoints protégés :
  1. Générez un token avec `POST /auth/login` (email/password dans la BDD de test).
  2. Cliquez sur **Authorize** et renseignez `Bearer <votre_token>`.
- **WebSocket/STOMP** :
  - Endpoint WS : `ws://localhost:8080/ws-chat`
  - Topics : `/topic/dialog/{dialogId}`
  - Destination d’envoi : `/app/dialog/{dialogId}/message`

---

## 🚀 Endpoints clés

### Authentification (routes publiques)

- **POST** `/auth/register` : Création d'un nouvel utilisateur
- **POST** `/auth/login` : Authentification & JWT

### Profils utilisateurs

- **GET** `/api/profile/me` : Profil de l'utilisateur connecté
- **GET** `/api/profile/{userId}` : Récupérer un profil par ID (AGENT/ADMIN)
- **PUT** `/api/profile/{userId}` : Mettre à jour un profil (AGENT/ADMIN)

### Dialogues & messages

- **POST** `/api/dialog/`: Créer un nouveau dialogue
- **GET** `/api/dialog/{id}`: Obtenir un dialogue avec ses messages
- **POST** `/api/dialog/{dialogId}/message`: Envoyer un message dans un dialogue
- **POST** `/api/dialog/{dialogId}/{senderId}/markasread` : Marquer comme lus tous les messages non lus de relatifs à l'expéditeur
- **POST** `/api/dialog/{dialogId}/invite/{userId}` : Inviter un utilisateur dans le dialogue
- **POST** `/api/dialog/{dialogId}/close` : Fermer un dialogue (status → CLOSED)
- **GET** `/api/dialog/all` : Lister tous les dialogues
- **GET** `/api/dialog/status/{status}` : Filtrer par statut (OPEN, PENDING, CLOSED)
- **GET** `/api/dialog/sender/{senderId}`: Dialogues où l'utilisateur a envoyé au moins un msg

---

## 🔧 Front-end associé

Le front-end, situé dans le dossier `/front`, permet de tester l’UI du chat et consomme à la fois l’API REST et le WebSocket.

---

## 🤝 Contribution & Contact

N’hésitez pas à ouvrir des issues ou à proposer des Pull Requests !

---

## 📄 Licence

Ce projet est distribué sous licence MIT.
