# syntax=docker/dockerfile:1
# O JDK 21 compila os módulos Java; o Maven é o definido pelo wrapper do projeto.
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace
COPY mvnw ./
COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/
# Separar o wrapper preserva esta camada quando somente os fontes mudarem.
RUN chmod +x mvnw && ./mvnw -B --version

# O agregador lista todos os POMs; -am inclui a biblioteca de contratos.
COPY pom.xml ./
COPY contratos-grpc/pom.xml contratos-grpc/
COPY pedido-service/pom.xml pedido-service/
COPY api-gateway/pom.xml api-gateway/
COPY contratos-grpc/src contratos-grpc/src
COPY api-gateway/src api-gateway/src
# O POM inclui ../web no JAR para servir a interface pela mesma origem da API.
COPY web web
# Reutiliza downloads sem empacotar o cache. O teste de contexto é executado.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -pl api-gateway -am verify

# O estágio final dispensa JDK, Maven e código-fonte separado do JAR.
FROM eclipse-temurin:21-jre-noble AS runtime
WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
# Caminho explícito identifica o JAR repackaged pelo Spring Boot.
COPY --from=build --chown=10001:10001 /workspace/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001:10001
# EXPOSE é documentação: -p publica a porta e PORT configura o listener.
EXPOSE 8080
# Forma exec preserva sinais; o próprio Spring resolve as variáveis de ambiente.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
