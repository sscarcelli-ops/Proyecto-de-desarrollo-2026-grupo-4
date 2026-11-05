# Frontend en contenedor (T-09). Lo usa `docker compose --profile app up`; para desarrollar se sigue usando
# `npm run dev`.

# Etapa 1: compilar con Vite
FROM node:22-alpine AS build
WORKDIR /app
# Primero solo las dependencias: si no cambian, Docker reutiliza esta capa
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Vite escribe la dirección de la API dentro del código al compilar: se pasa como argumento desde el compose
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# Etapa 2: servir los archivos compilados con nginx
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
