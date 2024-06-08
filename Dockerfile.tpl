FROM openjdk:17-jdk
COPY target/lanraragi_tag-jar-with-dependencies.jar /usr/app/lanraragi_tag-jar-with-dependencies.jar
WORKDIR /usr/app
ENV HOST="http://192.168.5.4:7788"
ENV KEY=""
ENV CRON="0 1 * * *"
ENV RUN="TRUE"
ENV TZ="Asia/Shanghai"
ENV THREAD_NUM="2"
CMD ["java", "-jar", "lanraragi_tag-jar-with-dependencies.jar"]
