FROM node:22-alpine AS frontend
WORKDIR /workspace

COPY frontend/package*.json ./frontend/
RUN cd frontend && npm ci

COPY frontend ./frontend
RUN cd frontend && npm run build

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY . .
COPY --from=frontend /workspace/src/main/resources/static ./src/main/resources/static

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/target/campus-erp-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java -jar app.jar --server.port=${PORT:-8080}"]
