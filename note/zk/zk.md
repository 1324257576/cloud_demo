##### `zookeeper`

###### 简介

`Zookeeper`是分布式应用程序协调服务，是一个为分布式应用提供一致性服务的软件，使用场景主要有：配置维护、分布式同步、集群管理、生成唯一ID等。

###### 数据模型

`zookeeper`的数据结构可以视为树形结构，树中的节点`znode`维护着节点数据、节点状态、子节点等信息。

节点类型

1. `PERSISTENT` 持久化节点：在节点创建后，就一直存在，直到进行删除操作来主动删除该节点。不会因创建该节点的客户端的会话失效而消失。
2. `PERSISTENT_SEQUENTIAL`持久顺序节点：在`ZK`中，父节点会为第一级子节点维护一份时序，用于记录每个子节点的创建先后顺序。当创建顺序节点时，`ZK`会自动为给定节点的名称加上一个时序数字后缀，作为该节点的名称。
3. `EPHEMERAL`临时节点：临时节点的生命周期和创建该节点的客户端的会话绑定。当会话失效后（并非会话断开连接），当前节点会自动被清除掉。临时节点下面不能创建子节点。
4. `EPHEMERAL_SEQUENTIAL`临时顺序节点









##### 安装

获取对应的安装文件：

` wget http://mirror.bit.edu.cn/apache/zookeeper/zookeeper-3.4.14/zookeeper-3.4.14.tar.gz `

修改`config/zoo.cfg`的`dataDir`属性

开放端口：

```shell

firewall-cmd --zone=public --add-port=2181/tcp --permanent 

firewall-cmd --reload   # 配置立即生效
```



##### 常用SHELL命令



![1585993834496](D:\Temp\cloud_demo\note\zk\1585993834496.png)





新增节点：

```shell
 #-s：顺序节点 -e：临时节点 path：指定绝对路径 data:字符串
 create [-s] [-e] path data  
 create -s -e /znode "abc"
```

修改节点：

```shell
#version:提交version等于节点的dataVersion值才能set成功。指定为-1则忽略version的对比
set path data [version]
create -e /znode "abc"
set /znode 'xyz' #每次提交set，都会使得node的dataVersion+1
```

删除节点：

```shell
#version:提交version等于节点的dataVersion值才能set成功。指定为-1则忽略version的对比
delete path [version]
```

查看节点：

```shell
get path [watch]

#加上watch后，其他客户端进行触发Wathcer事件的操作后，会将事件类型响应到当前客户端连接
get /node watch

get /node 
a #节点内容
cZxid = 0x9e #节点创建时的zxid
ctime = Sat Apr 04 18:01:53 CST 2020 #节点创建时间
mZxid = 0x9e #节点最后一次更新时的zxid
mtime = Sat Apr 04 18:01:53 CST 2020 #节点最后一次更新时的时间
pZxid = 0x9e #节点的子节点最后一次被更新的zxid
cversion = 0 #子节点的更新次数
dataVersion = 0 #节点数据的更新次数
aclVersion = 0  #节点acl的更新次数
ephemeralOwner = 0x0 #0表示持久节点，如果是临时节点该值为创建节点的会话SessionId
dataLength = 1  #数据内容的长度
numChildren = 0 #节点当前的子节点个数
```



查看子节点列表

```shell
ls path 
ls2 path 
```

![1585995133618](D:\Temp\cloud_demo\note\zk\1585995133618.png)

















##### JavaAPI

坑1：需要排除`Zookeeper`自带的日志`API`，替换成常用的`logback`

```java
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
    <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>
```



`org.apache.zookeeper.ZooKeeper`构造函数传入`connectionString`，`timeout`，`watch`。

借助`CountDownLatch`进行同步等待连接结果。

```java
 CountDownLatch cdl = new CountDownLatch(1);

        log.info("try connecting zk");
        String connectionString = String.format("%s:%s", "129.204.21.157", "2181");
        int timeout = 5 * 1000;
        ZooKeeper zooKeeper = new ZooKeeper(connectionString, timeout,
                (event) -> {
                    log.info("event={}", event);
                    if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                        log.info("connect success");
                        cdl.countDown();
                    }
                });
        cdl.await();

        log.info("sessionId={}", zooKeeper.getSessionId());
        zooKeeper.close();
```





##### Watcher事件监听机制

watcher特性：

1. 一次性：一旦被触发就会移除，再次使用需要重新注册。(`new ZooKeeper(watcher)`传入的watcher为default watcher，不是一次性的，只进行client的连接状态改变的监听。)
2. 顺序回调：回调是顺序串行化执行的。
3. 轻量级：`watchEvent`是最小的通信单元，只包含通知状态、时间类型和节点路径，不包含变化前后的数据内容。

![1585975123963](D:\Temp\cloud_demo\note\zk\1585975123963.png)

4.时效性：watcher只有在当前会话失效（并非会话断开连接）才会失效。



watcher接口

`abstract public void process(WatchedEvent event);`

`WatchedEvent`见上图，主要查看其`KeeperState`和`EventType`的枚举定义



![1585975550843](.\1585975550843.png)

一般使用`EventType.Node`进行判断`KeepState`,当`eventType==EventType.None`时

![](.\1585975517345.png)



watcher的使用与触发

除了Default Watcher，可以注册watcher的方法有

`exists() `/ `getData() `/`getChildren()`

<img src="D:\Temp\cloud_demo\note\zk\1585977613484.png" alt="1585977613484"  />



而可以触发watcher的方法则有：

1. 创建节点：`create()` 对应事件：`NodeCreated`和`NodeChildrenChanged`
2. 删除节点：`delete()`对应事件：`NodeDeleted`和`NodeChildrenChanged`
3. 设置节点数据`setData()`对应事件：`NodeDataChanged`









##### `ZooKeeper Atomic Broadcast`

`zookeeper`通过`ZAB`协议来保证分布式事务的最终一致性。

`ZAB`协议定义了事务请求的处理方式：

1. 所有的事务请求必须由全局唯一的Leader服务器处理
2. Leader负责将事务请求转换为事务提议，并为该事务提议分配一个全局ID（`ZXID`），并将提议分发到集群中的所有的Follower，也就是向所有的Follower节点发送数据复制。
3. 分发后Leader服务器需要等待所有的Follower服务器的反馈，Follower接收到提议后，首先将其以事务日志的方法写入本地磁盘中，写入成功后返回`Ack`响应消息。在`ZAB`协议中规定超过半数的Follower进行正确的反馈后，Leader则再次向所有的Follower发送`Commit`消息，同时自身对事务提议进行提交。

Leader与每一个Follower之间都维护着一个FIFO消息队列进行收发消息，使用队列消息进行异步解耦。

`ZXID`的高32位为Epoch编号，代表当前集群所处的周期，低32位为当前事务的计数。

基于`ZAB`协议，`ZooKeeper`集群中的角色主要有以下三类：

| 角色     | 描述                                                       |
| -------- | ---------------------------------------------------------- |
| Leader   | 负责所有的事务请求，负责进行投票的发起和决议，更新系统状态 |
| Follower | 负责接收客户端请求和返回响应，在选主过程中参与投票         |
| Observer | 负责接收客户端请求和返回响应，不参与投票，只同步Leader状态 |

`ZAB`的三个阶段

1. 选举`Leader Election`

   选出准Leader，选举比较规则：`ZXID`的高32位Epoch-》`ZXID`的低32位事务编号-》server_id(`zoo.cfg`中的`myid`)，当有一个节点的得票超过半数则成为准Leader

2. 恢复`Recovery`

   Leader会将自身提交（广播`commit`消息的同时进行自身提交）的最大ZXID发送给Follower，Follower节点进行回退或数据同步操作，保证集群中的所有节点的数据副本保持一致。

3. 广播`Broadcast`

   具体见前面的说明

