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

# Don't run CLI as root; create a atomist user
RUN groupadd -r atomist && \
    useradd --home-dir=/home/atomist --create-home -r -g atomist atomist && \
    mkdir -p /home/atomist/.atomist/repository && \
    mkdir -p /home/atomist/project

# Setup and install CLI
ENV PATH="$PATH:/home/atomist/rug-cli-${version}/bin"
ADD rug-cli-${version}-bin.tar.gz /home/atomist

# Change owner and use our atomist user
RUN chown -R atomist.atomist /home/atomist
USER atomist

# Run a basic search to populate repository and catalog cache
RUN ["rug", "search", "-qX"]

WORKDIR /home/atomist/project
CMD ["rug", "shell", "-l"]
