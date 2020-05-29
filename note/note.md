

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
    #instance-id: order-service1:8005
  client:
    serviceUrl:
      defaultZone: http://${eureka.host:localhost}:${eureka.port:8761}/eureka/
```



设置`perfer-ip-address=true`和`instance-id=order-service1:8005`可以使得在eureka的监控界面中对应的服务名的status中显示更清晰的名称和主机地址

![1585926437822](D:\Temp\cloud_demo\note\1585926437822.png)





坑2：

`spring.application.name`不能带下划线：错误示例`order_service`。在通过ribbon方式使用`restTemplate`进行服务调用时出现。



坑3：

使用`restTemplate`的方法进行服务调用时，第一个参数即`url`需要指定`http://`，后面带上被调用服务的名称。

例如：`http://order-service/get`



eureka的server集群

需要在`EurekaServer`的`application.yml`指定`defaultZone`进行server之间的注册，多个值之间使用逗号分隔

![1585925293536](D:\Temp\cloud_demo\note\1585925293536.png)

在`EurekaClient`的`application.yml`中指定所有的server注册中心的地址

![1585925370688](D:\Temp\cloud_demo\note\1585925370688.png)



坑4：

在`IOC`容器中维护着存储eureka注册的服务信息的服务发现Bean，可以通过自动注入`org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient`对象，调用其方法进行获取所有的注册服务的信息。





使用`Zookeeper`作为注册中心

引入依赖，由于安装的`ZK`版本是3.4.14，因此，需要替换starter自带的`zookeeper`的jar，同时由于`zookeeper`依赖的日志框架不是很好用，替换为`logback`。

```java
<dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-zookeeper-discovery</artifactId>
            <version>2.2.1.RELEASE</version>
            <exclusions>
                <exclusion>
                    <groupId>org.apache.zookeeper</groupId>
                    <artifactId>zookeeper</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>org.apache.zookeeper</groupId>
            <artifactId>zookeeper</artifactId>
            <version>3.4.14</version>
            <exclusions>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-log4j12</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>log4j</groupId>
                    <artifactId>log4j</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
```

在`application.yml`文件中指定`zookeeper`的`connectString`

```java
spring:
  cloud:
    zookeeper:
      connect-string: 129.204.21.157:2181
```

启动服务应用后，可以在`zk`中查看到`/services`的子节点中，增加了对应服务的节点。





