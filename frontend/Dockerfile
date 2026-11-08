# Frontend en contenedor (T-09). Lo usan los ambientes todo en Docker (README, "Ambientes"); para desarrollar se
# sigue usando `npm run dev`.

# Etapa 1: compilar con Vite. La compilación no lleva ninguna dirección: el frontend llama a /api en su misma
# dirección y nginx lo reenvía al backend (T-12). La misma imagen sirve para cualquier instalación.
FROM node:22-alpine AS build
WORKDIR /app
# Primero solo las dependencias: si no cambian, Docker reutiliza esta capa
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Etapa 2: servir los archivos compilados con nginx. La plantilla se completa al arrancar con BACKEND_URL
# (variable de entorno del contenedor, definida en docker-compose.yml).
FROM nginx:1.27-alpine
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
