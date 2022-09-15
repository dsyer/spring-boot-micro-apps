FROM ubuntu:bionic
ENV JAVA_HOME /opt/jdk/jdk-18
ENV PATH $JAVA_HOME/bin:$PATH

ADD https://download.java.net/java/early_access/jdk18/21/GPL/openjdk-18-ea+21_linux-x64_bin.tar.gz /opt/jdk/jdk.tgz
RUN tar -xzvf /opt/jdk/jdk.tgz -C /opt/jdk/

ARG USER
ARG USER_ID
ARG USER_GID

# Configure apt, install packages and tools
RUN (groupadd --gid $USER_GID $USER || echo "Group already exists" ) \
    && useradd -s /bin/bash --uid $USER_ID --gid $USER_GID -m $USER

ENV PATH $PATH:/opt/jdk/bin
