FROM java:8-jre

COPY lib /opt/build/lib/
COPY @project.build.finalName@.jar /opt/build/
COPY .atomist-profile /root/

VOLUME /opt/atomist

WORKDIR /opt/build

ENTRYPOINT ["java", "-Xmx1g", "-cp", "lib/*:@project.build.finalName@.jar:.", "com.atomist.projectoperations.cli.Main"]