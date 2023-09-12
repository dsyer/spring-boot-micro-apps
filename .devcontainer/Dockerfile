# See here for image contents: https://github.com/microsoft/vscode-dev-containers/tree/v0.245.2/containers/java/.devcontainer/base.Dockerfile

# [Choice] Java version (use -bullseye variants on local arm64/Apple Silicon): 11, 17, 11-bullseye, 17-bullseye, 11-buster, 17-buster
ARG VARIANT=17-bullseye
FROM mcr.microsoft.com/vscode/devcontainers/java:0-${VARIANT}

ARG USER=vscode

VOLUME /home/$USER/.m2

RUN bash -lc '. /usr/local/sdkman/bin/sdkman-init.sh && sdk install java 19-amzn && sdk use java 19-amzn'
RUN sudo mkdir /home/$USER/.m2 && sudo chown $USER:$USER /home/$USER/.m2
