FROM gcr.io/google-appengine/openjdk:8

ENV JAVA_OPTS -agentpath:/opt/cdbg/cdbg_java_agent.so \
              -Dcom.google.cdbg.module=hello \
              -Dcom.google.cdbg.version=1.0

COPY hello-1.0.jar /hello.jar

CMD ["/usr/bin/java", "-jar", "/hello.jar"]
