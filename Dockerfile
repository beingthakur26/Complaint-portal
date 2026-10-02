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
# Keep memory low for free hosting plans
ENV JAVA_OPTS="-Xmx300m"
EXPOSE 8080
CMD ["catalina.sh", "run"]
