FROM alpine:latest
ENV JAVA_HOME /opt/jdk/jdk-17
ENV PATH $JAVA_HOME/bin:$PATH

ADD https://download.java.net/java/early_access/alpine/14/binaries/openjdk-17-ea+14_linux-x64-musl_bin.tar.gz /opt/jdk/
RUN tar -xzvf /opt/jdk/openjdk-17-ea+14_linux-x64-musl_bin.tar.gz -C /opt/jdk/

ARG USER
ARG USER_ID
ARG USER_GID

RUN (addgroup --gid "${USER_GID}" "${USER}" || echo "No groupadd needed") && \
    adduser \
     --disabled-password \
      --uid ${USER_ID} \
      --ingroup ${USER} \
      --shell /bin/sh ${USER}