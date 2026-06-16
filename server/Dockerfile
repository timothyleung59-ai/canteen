# ============================================================
#  饭堂订餐后端 - 多阶段构建
#  第一阶段：用 JDK8 + Maven 在容器内编译打包（主机无需装 Java/Maven）
#  第二阶段：只保留 JRE8 + jar，镜像更小
# ============================================================

# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /app
# 阿里云镜像，国内构建更快更稳
COPY deploy/settings.xml /root/.m2/settings.xml
COPY . .
RUN mvn -DskipTests -B clean package

# ---------- run stage ----------
FROM eclipse-temurin:8-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
    && mkdir -p /data/tmp /data/logs
COPY --from=build /app/server-bc/target/server-bc-1.0.0.RELEASE.jar /app/app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
