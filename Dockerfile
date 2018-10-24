FROM openjdk:8-jdk-alpine as build
WORKDIR /workspace/app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY samples samples
COPY library library
RUN ./mvnw dependency:get -Dartifact=org.springframework.boot.experimental:spring-boot-thin-launcher:1.0.17.RELEASE:jar:exec -Dtransitive=false
RUN ./mvnw install -DskipTests
VOLUME /root/.m2

FROM dsyer/graalvm-native-image:1.0.0-rc7 as native
WORKDIR /workspace/app
ARG SAMPLE=json
ARG THINJAR=/root/.m2/repository/org/springframework/boot/experimental/spring-boot-thin-launcher/1.0.17.RELEASE/spring-boot-thin-launcher-1.0.17.RELEASE-exec.jar
COPY --from=build /root/.m2 /root/.m2
COPY --from=build /workspace/app/samples/${SAMPLE}/target/*.jar target/
COPY --from=build /workspace/app/samples/${SAMPLE}/*.json ./
ENV PATH="${PATH}:/usr/lib/graalvm/bin"
RUN mkdir -p target/dependency && (cd target/dependency; /usr/lib/graalvm/bin/jar -xf ../*.jar)
RUN native-image --no-server --static -J-javaagent:/root/.m2/repository/org/aspectj/aspectjweaver/1.9.1/aspectjweaver-1.9.1.jar -Dspring.functional.enabled=true -Dio.netty.noUnsafe=true -Dio.netty.noJdkZlibDecoder=true -Dio.netty.noJdkZlibEncoder=true -H:Name=target/demo -H:ReflectionConfigurationFiles=`echo *.json | tr ' ' ,` -H:ReflectionConfigurationResources=META-INF/micro-library.json -H:IncludeResources='META-INF/.*.json|META-INF/spring.factories|org/springframework/boot/logging/.*' --delay-class-initialization-to-runtime=io.netty.handler.codec.http.HttpObjectEncoder,org.springframework.core.io.VfsUtils,io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator,io.netty.handler.ssl.ReferenceCountedOpenSslEngine  --report-unsupported-elements-at-runtime -cp `java -jar ${THINJAR} --thin.archive=target/dependency --thin.classpath --thin.profile=graal` com.example.DemoApplication

FROM alpine
WORKDIR /workspace/app
VOLUME /tmp
COPY --from=native /workspace/app/target/demo .
EXPOSE 8080
ENTRYPOINT ["./demo"]