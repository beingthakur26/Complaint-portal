# ---------- Stage 1: build the WAR with Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- Stage 2: run it on Tomcat 10.1 ----------
FROM tomcat:10.1-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*

# Deploy as ROOT so the site opens at the main URL (no /complaintportal path)
COPY --from=build /app/target/complaintportal.war /usr/local/tomcat/webapps/ROOT.war

# 1. Disable the Tomcat shutdown port (-1) to prevent scanners/Render health checks from sending commands to port 8005
# 2. Make the HTTP connector port configurable via $PORT (defaults to 8080)
RUN sed -i 's/<Server port="8005"/<Server port="-1"/' /usr/local/tomcat/conf/server.xml && \
    sed -i 's/port="8080"/port="${http.port}"/' /usr/local/tomcat/conf/server.xml

# Configure dynamic port binding script in Tomcat setenv.sh
RUN echo '#!/bin/sh' > /usr/local/tomcat/bin/setenv.sh && \
    echo 'export CATALINA_OPTS="$CATALINA_OPTS -Dhttp.port=${PORT:-8080}"' >> /usr/local/tomcat/bin/setenv.sh && \
    chmod +x /usr/local/tomcat/bin/setenv.sh

# Keep memory low for free hosting plans
ENV JAVA_OPTS="-Xmx300m"
EXPOSE 8080

CMD ["catalina.sh", "run"]
