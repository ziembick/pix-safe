# Estágio 1: Build com Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
 
# Copia os scripts do Maven Wrapper e o pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
 
# Da permissão de execução ao mvnw e baixa as dependências
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline
 
# Copia o código-fonte e constrói o JAR
COPY src ./src 
RUN ./mvnw clean package -DskipTests

# Estágio 2: Execução com JRE
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/safe-pix-api-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]