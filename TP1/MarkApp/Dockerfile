FROM openjdk:11

RUN mkdir /usr/MarkApp
WORKDIR /usr/MarkApp
copy MarkAppImage-1.0-jar-with-dependencies.jar .

ENTRYPOINT ["java", "-jar", "MarkAppImage-1.0-jar-with-dependencies.jar"]