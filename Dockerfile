# syntax=docker/dockerfile:1

# --- Stage 0: ETL do seed ------------------------------------------------
# Nenhum dado é versionado: o seed é gerado aqui, a partir de datasets
# públicos fixados por commit (ver docs/etl.md). Precisa de internet só no
# build; a camada fica em cache enquanto tools/etl não mudar.
FROM python:3.12-alpine AS seed-build
WORKDIR /etl
COPY tools/etl/generate_seed.py ./
RUN python3 generate_seed.py --out /seed --cache-dir /tmp/etl-cache

# --- Stage 1: build do Angular -------------------------------------------
FROM node:20-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npx ng build --configuration production

# --- Stage 2: build do Spring Boot -----------------------------------------
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-build
WORKDIR /backend
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
COPY --from=seed-build /seed/ ./src/main/resources/seed/
# Estáticos do Angular entram dentro do jar do Spring Boot (docs/spec/original-spec.md seção 9).
COPY --from=frontend-build /frontend/dist/frontend/browser/ ./src/main/resources/static/
RUN mvn -q -B package -DskipTests

# --- Stage 3: runtime só com o JRE -----------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /backend/target/app.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
