# 使用多阶段构建
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 复制 pom.xml 和 settings.xml
COPY pom.xml settings.xml ./
# 下载依赖 (使用阿里云镜像加速)
RUN mvn dependency:go-offline -s settings.xml
# 复制源码
COPY src ./src
# 打包
RUN mvn clean package -DskipTests -s settings.xml

# 运行阶段
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
