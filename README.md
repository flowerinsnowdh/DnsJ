# DnsJ
一个基于 Netty 的 DNS 请求转发服务器，可以将 UDP DNS 请求转发给 DNS-over-HTTPS

需要 Java21

# 三个用途
- 提供日志以审计
- 将不支持安全 DNS 的请求转发给安全 DNS
- 允许使用代理服务器（Socks/HTTP）

# 配置方式
编辑[config.groovy](src/main/resources/config.groovy)

# 编译方式
## 编译jar
```shell
./gradlew shadowJar
```

## 编译 Application（.zip）
```shell
./gradlew shadowDistZip
```

## 编译 Application（.tar）
```shell
./gradlew shadowDistTar
```

# 第三方类库（排名不分前后）
- [apache/groovy](https://github.com/apache/groovy) ([Apache-2.0 license](https://github.com/apache/groovy#Apache-2.0-1-ov-file))
- [netty/netty](https://github.com/netty/netty) ([Apache-2.0 license](https://github.com/netty/netty#Apache-2.0-1-ov-file))
- [square/okhttp](https://github.com/square/okhttp) ([Apache-2.0 license](https://github.com/square/okhttp#Apache-2.0-1-ov-file)) 
- [apache/logging-log4j2](https://github.com/apache/logging-log4j2) ([Apache-2.0 license](https://github.com/apache/logging-log4j2#Apache-2.0-1-ov-file)) 
- [JetBrains/kotlin](https://github.com/JetBrains/kotlin) ([Apache-2.0 license](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt)) 
- [qos-ch/slf4j](https://github.com/qos-ch/slf4j) ([MIT license](https://github.com/qos-ch/slf4j#MIT-1-ov-file)) 
