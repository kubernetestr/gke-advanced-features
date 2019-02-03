FROM gcr.io/google-appengine/openjdk:8

COPY hello-1.0.jar /hello.jar
	
CMD ["/usr/bin/java", "-agentpath:/opt/cprof/profiler_java_agent.so=-cprof_service=hello,-cprof_service_version=1.0", "-jar", "/hello.jar"]
