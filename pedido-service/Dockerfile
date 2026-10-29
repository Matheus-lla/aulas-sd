# syntax=docker/dockerfile:1
# JDK 21 mantido pelo Eclipse Temurin, compatível com o Java definido no POM.
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace

# O wrapper fixa o Maven do projeto; esta camada sobrevive a mudanças no código.
COPY mvnw ./
COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/
RUN chmod +x mvnw && ./mvnw -B --version

# Todos os POMs são necessários para o Maven ler o agregador da raiz.
COPY pom.xml ./
COPY contratos-grpc/pom.xml contratos-grpc/
COPY pedido-service/pom.xml pedido-service/
COPY api-gateway/pom.xml api-gateway/
COPY contratos-grpc/src contratos-grpc/src
COPY pedido-service/src pedido-service/src
# O cache guarda dependências entre builds, sem copiá-las para a imagem final.
# -am compila também os contratos; verify mantém os testes habilitados.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -pl pedido-service -am verify

# Só o JRE e o JAR executável são necessários para atender chamadas gRPC.
FROM eclipse-temurin:21-jre-noble AS runtime
WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app \
    && chown app:app /app
# Nome exato evita selecionar o .jar.original ou o JAR de contratos.
COPY --from=build --chown=10001:10001 /workspace/pedido-service/target/pedido-service-0.0.1-SNAPSHOT.jar /app/app.jar
# A pasta permite o perfil local anterior; em cloud os dados ficam no Cloud SQL.
RUN mkdir /app/data && chown app:app /app/data
USER 10001:10001
# Documenta o padrão local; PORT/GRPC_PORT configuram o servidor em execução.
EXPOSE 9090
# Java recebe SIGTERM diretamente e envia logs para stdout/stderr.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
