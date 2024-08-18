FROM clojure:temurin-17-tools-deps-bullseye

RUN apt-get update && apt-get install -y \
  curl default-jre \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY src ./src
COPY dev ./dev
COPY resources ./resources
COPY deps.edn .

RUN clj -M:dev uberjar && cp target/jar/app.jar . && rm -r target

EXPOSE 8080

CMD ["/usr/bin/java", "-XX:-OmitStackTraceInFastThrow", "-XX:+CrashOnOutOfMemoryError", "-jar", "app.jar"]
