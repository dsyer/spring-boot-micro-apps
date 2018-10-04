FROM ubuntu

VOLUME /tmp
COPY target/demo /app/demo
ENTRYPOINT ["/app/demo", "-Dorg.springframework.boot.logging.LoggingSystem=org.springframework.boot.logging.java.JavaLoggingSystem", "-Dspring.functional.enabled=true"]