# Kepler-Trace
@See [[wiki]](https://github.com/Kepler-Framework/Kepler-Trace-Collector/wiki)

### 构建项目 ###
1. ```git https://github.com/Kepler-Framework/Kepler-Trace.git```
2. ```cd kepler-trace-interface && maven install -DskipTests```

### 运行Trace collector service ###
1. ```cd kepler-trace-service```
2. ```mvn clean package```
3. ```cd target/srv-kepler-trace-collector-service```
4. ```java -Dconf=kepler配置文件路径 -jar kepler-trace-collector-service-kepler.jar```
