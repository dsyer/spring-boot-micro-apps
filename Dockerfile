FROM danny02/graalvm as native-image-builder
COPY target/micro*.jar /app.jar
COPY graal.json /graal.json
RUN native-image -Dio.netty.noUnsafe=true -H:Name=micro -H:ReflectionConfigurationFiles=graal.json --report-unsupported-elements-at-runtime \
  -cp $(java -jar /app.jar --thin.classpath --thin.profile=graal) com.example.micro.MicroApplication

FROM ubuntu:18.04
COPY --from=native-image-builder /micro /micro
EXPOSE 8080
ENTRYPOINT [ "/micro" ]
