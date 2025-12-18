FROM eclipse-temurin:21-jdk-alpine

ADD ./target/crypto-analysis-coin-correlation.jar /app/

CMD sh -c "java -Dspring.profiles.active=bybit \
  -Xmx400m -Xms400m -Xss256k -XX:MaxMetaspaceSize=100m -XX:MaxDirectMemorySize=100m \
  -jar /app/crypto-analysis-coin-correlation.jar"