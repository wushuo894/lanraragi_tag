FROM eclipse-temurin:17-jre
COPY target/lanraragi_tag-jar-with-dependencies.jar /usr/app/lanraragi_tag-jar-with-dependencies.jar
WORKDIR /usr/app
ENV HOST="http://192.168.5.4:7788"
ENV KEY=""
ENV CRON="0 1 * * *"
ENV RUN="TRUE"
ENV TZ="Asia/Shanghai"
ENV THREAD_NUM="2"
RUN mkdir /usr/java
RUN ln -s /opt/java/openjdk /usr/java/openjdk-17
CMD ["java", "-jar", "lanraragi_tag-jar-with-dependencies.jar"]
