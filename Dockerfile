FROM ubuntu

VOLUME /tmp
COPY target/bunc /app/bunc
ENTRYPOINT ["/app/bunc", "-Dorg.springframework.boot.logging.LoggingSystem=org.springframework.boot.logging.java.JavaLoggingSystem", "-Dspring.functional.enabled=true"]