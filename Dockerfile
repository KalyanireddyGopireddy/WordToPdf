FROM openjdk:8
EXPOSE 8080
ADD target/wordtopdf.jar wordtopdf.jar
ENTRYPOINT ["java","-jar","/wordtopdf.jar"]
