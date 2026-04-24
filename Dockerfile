FROM node:22-bullseye AS frontend-build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm install

COPY index.html vite.config.js ./
COPY src ./src
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS backend-build
WORKDIR /app

COPY pom.xml ./
COPY src/main ./src/main
COPY src/test ./src/test
COPY --from=frontend-build /app/dist ./dist
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=backend-build /app/target/expense-tracker-1.0.0.jar app.jar

EXPOSE 10000
ENTRYPOINT ["java", "-Dserver.port=10000", "-jar", "/app/app.jar"]
