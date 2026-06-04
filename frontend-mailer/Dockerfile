# Étape 1 : Build
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .

# Kan-passew l'API URL f l'Build time
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
RUN npm run build

# Étape 2 : Production
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV production
# 🚨 Port d'exécution
ENV PORT 1193

# 🚨 L'FIX: Msa7na l'COPY next.config.js li kan kay-fayli
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json

EXPOSE 1193
CMD ["npm", "start"]