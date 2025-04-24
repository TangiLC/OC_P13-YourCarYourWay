# YourCarYourWay Front-end

![Angular](https://img.shields.io/badge/Angular-19.x-DD0031?style=&logo=angular&logoColor=white) ![TypeScript](https://img.shields.io/badge/TypeScript-5.7-%23007ACC?style=&logo=typescript&logoColor=white) ![Angular Material](https://img.shields.io/badge/Material-19.x-007FCC?style=&logo=angular&logoColor=white) ![RxJS](https://img.shields.io/badge/RxJS-7.8-B7178C?style=&logo=rxjs&logoColor=white) ![STOMP](https://img.shields.io/badge/STOMP-7.1.1-%231DBF73?style=&logo=websocket&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-20.10-%230249ED?style=&logo=docker&logoColor=white)

## 📖 Description

Ce projet est un **ProofOfConcept** d'application front-end Angular de la plateforme **YourCarYourWay** pour le projet 13 du **cursus Full-Stack Java Angular d'OpenClassrooms**.  
Il fournit une interface utilisateur pour :
- la connexion utilisateurs,
- la navigation des dialogues de chat,
- l'envoi et la réception de messages en temps réel via WebSocket (STOMP)  
Il consomme l'API REST sécurisée (JWT) et les endpoints STOMP exposés par le backend.

## 🛠️ Technologies principales

- Framework : **Angular 19**
- Langage : **TypeScript**
- UI Kit : **Angular Material**
- Reactive Extensions : **RxJS**
- WebSocket : **@stomp/stompjs**, **@stomp/rx-stomp**, **sockjs-client**
- Build & CLI : **Angular CLI**
- Containerisation : **Docker** (optionnel)

## 📋 Prérequis

- Node.js >= 18.x  
- npm >= 9.x  
- (Optionnel) Docker & Docker Compose  

## 🚀 Installation

1. Copier le dépôt et accéder au dossier front :
   ```bash
   git clone <URL_DU_REPO>
   cd <nom_du_repo>/front/ycyw
   ```

2. Installer les dépendances :
   ```bash
   npm install
   ```

3. Configurer l'URL de l'API et du WebSocket  
   Dans `src/environments/environment.ts` (et `environment.prod.ts`), ajustez :
   ```ts
   export const environment = {
     production: false,
     apiUrl: 'http://localhost:8080/api',
     wsUrl: 'ws://localhost:8080/ws-chat'
   };
   ```

## 📦 Scripts disponibles

| Script         | Description                             |
| -------------- | --------------------------------------- |
| `npm start`    | Lance l'application en mode développement (http://localhost:4200) |
| `npm run build`| Compile le projet pour la production (dans `dist/`)    |
| `npm run watch`| Reconstruit à chaque changement (développement) |
| `npm test`     | Lance les tests unitaires avec Karma/Jasmine |

## 🔧 Développement

- Le projet utilise **Angular CLI** pour la création de composants, services, modules, etc.  
- UI basée sur **Angular Material** : thèmes, layouts, composants préfabriqués.  
- Communication WebSocket :  
  - Service `StompService` encapsule la connexion STOMP/SockJS.  
  - Souscriptions aux topics `/topic/dialog/{dialogId}` et publication sur `/app/dialog/{dialogId}/message`.

## 📚 Ressources & liens

- [Angular Documentation](https://angular.io/docs)  
- [Angular Material](https://material.angular.io/)  
- [RxJS](https://rxjs.dev/)  
- [STOMP.js Guide](https://stomp-js.github.io/)  

## 🤝 Contribution

Contributions, issues et PR sont les bienvenus !  

---

## 📄 Licence

Ce projet est distribué sous licence MIT.
