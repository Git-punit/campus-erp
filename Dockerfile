FROM node:22-alpine AS frontend-build

WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build


FROM eclipse-temurin:25-jdk AS backend-build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

COPY --from=frontend-build \
  /workspace/src/main/resources/static \
  /workspace/src/main/resources/static

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests


FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=backend-build /workspace/target/*.jar /app/campus-erp.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java -jar /app/campus-erp.jar --server.port=${PORT:-8080}"]
