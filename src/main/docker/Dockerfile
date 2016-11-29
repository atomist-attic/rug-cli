FROM java:8-jre
LABEL org.label-schema.vendor="Atomist, Inc."\
	org.label-schema.name="rug-cli"\
	org.label-schema.description="CLI to build and run rugs"\
	org.label-schema.url="https://github.com/atomist/rug-cli"\
	org.label-schema.vcs-url="https://github.com/atomist/rug-cli.git"\
	org.label-schema.vcs-type="git"\
	org.label-schema.license="GPLv3"

COPY lib /opt/build/lib/
COPY @project.build.finalName@.jar /opt/build/
COPY .atomist-profile /root/

VOLUME /opt/atomist

WORKDIR /opt/build

ENTRYPOINT ["java", "-Xmx1g", "-cp", "lib/*:@project.build.finalName@.jar:.", "com.atomist.projectoperations.cli.Main"]
