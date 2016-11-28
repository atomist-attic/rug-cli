FROM java:8-jre

# First copy over the static Spring Boot launcher classes and all the project
# dependencies
COPY org /opt/build/org
COPY BOOT-INF/lib /opt/build/BOOT-INF/lib

# Copy over the changing project classes
COPY META-INF /opt/build/META-INF
COPY BOOT-INF/classes /opt/build/BOOT-INF/classes

WORKDIR /opt/build/

EXPOSE 8080

CMD ["java", "-Xmx1g", "org.springframework.boot.loader.JarLauncher"]
