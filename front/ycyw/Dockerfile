# Image de développement Angular
FROM node:18-alpine
WORKDIR /app

# Installer les dépendances
COPY package*.json ./
RUN npm install

# Copier le code source
COPY . .

# Exposer le port du serveur dev
EXPOSE 4200

# Lancer en mode dev, binding sur toutes les interfaces
CMD ["npm", "start", "--", "--host", "0.0.0.0"]
