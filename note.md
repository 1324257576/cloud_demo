

H版的使用：

坑1：

eureka的server和client都需要添加以下依赖：

其中，server需要标记`@EnableEurekaServer`注解，而client不需要显式地加上注解（自动配置）

```java
<dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>
    </dependencies>
```



server的`application.yml`

```java
server:
  port: 8761
      
spring:
  application:
    name: eureka 
        
eureka:
  instance:
    hostname: localhost
  client:
    registerWithEureka: false
    fetchRegistry: false
    serviceUrl:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```



client的`application.yml`

```java
server:
  port: 8005
      
spring:
  application:
    name: order-service
        
eureka:
  instance:
    prefer-ip-address: true
  client:
    serviceUrl:
      defaultZone: http://${eureka.host:localhost}:${eureka.port:8761}/eureka/
```



坑2：

`spring.application.name`不能带下划线：错误示例`order_service`。在通过ribbon方式使用`restTemplate`进行服务调用时出现。



坑3：

使用`restTemplate`的方法进行服务调用时，第一个参数`url`需要指定`http://`，后面带上被调用服务的名称。

例如：`http://order-service/get`



