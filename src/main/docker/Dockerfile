################################################################################
# Docker image for the Rug CLI
#
# Please refer to its documentation to start the container:
# http://docs.atomist.com/rug/rug-cli/rug-cli-install/
#
# Read then the Rug documentation on how to use the CLI itself:
# http://docs.atomist.com/quick-starts/rug-cli/
#
################################################################################
FROM openjdk:8-jdk
LABEL maintainer="Atomist, Inc. <oss@atomist.com>" \
      org.label-schema.vendor="Atomist, Inc." \
      org.label-schema.license="Commercial" \
      org.label-schema.schema-version="1.0" \
      org.label-schema.name="rug-cli" \
      org.label-schema.url="https://github.com/atomist/rug-cli" \
      org.label-schema.name="${version}" \
      org.label-schema.description="${description}" \
      org.label-schema.build-date="${timestamp}"

# we don't want to run the CLI as root
RUN groupadd -r atomist && \
    useradd --home-dir=/home/atomist --create-home -r -g atomist atomist && \
    mkdir -p /home/atomist/.atomist/repository && \
    mkdir -p /home/atomist/project
ENV PATH="$PATH:/home/atomist/rug-cli-${version}/bin"
ADD rug-cli-${version}-bin.tar.gz /home/atomist
RUN chown -R atomist.atomist /home/atomist
WORKDIR /home/atomist/project
USER atomist
CMD ["rug", "shell", "-l"]
