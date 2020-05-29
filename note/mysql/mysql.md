#### `MySQL`优化



###### 应用层面的优化思路

1. 资源池化：使用连接池
2. 减少`mysql`访问：
   1. 避免对数据进行重复检索
   2. 增加cache层
3. 负载均衡
   1. 通过主从复制，实现读写分离
   2. 分布式数据库架构



###### `mysql`内部的查询缓存优化

开启查询缓存后，当执行完全相同的`sql`语句时，服务器就会直接从缓存中读取结果，当数据被修改，则之前缓存会失效。



![1586588644602](D:\Temp\cloud_demo\note\mysql\1586588644602.png)



流程：

1. 客户端发送一条查询给服务器，根据是否开启支持缓存查询的配置或查询语句中是否指定进行查询进行判断是否进行检查查询缓存
2. 检查查询缓存，如果命中缓存则立即返回存储在缓存中的结果
3. 服务端进行`sql`解析、预处理、查询优化后，生成查询执行计划
4. 根据查询执行计划调用存储引擎的`Api`来执行查询
5. 根据是否支持缓存的配置进行缓存，并返回结果

查询缓存配置：`SHOW VARIABLES LIKE '%cache%'`

开启查询缓存：通过指定配置参数`query_cache_type`为ON或者1，来开启查询缓存。

| 值          | 含义                                                     |
| ----------- | -------------------------------------------------------- |
| OFF或者0    | 关闭查询缓存功能                                         |
| ON或者1     | 开启查询缓存功能，除非select语句中显式指定`SQL_NO_CACHE` |
| DEMAND或者2 | 按需进行查询缓存，必须在select语句中显式指定`SQL_CACHE`  |

例如：`SELECT SQL_CACHE id,name FROM t_user`或者`SELECT SQL_NO_CACHE id,name FROM t_user`



查询缓存失效的常见情况

1. 只有查询语句完全一致才能命中查询缓存
2. 当查询语句中包含不确定值时，则不会缓存。如包含now()函数
3. 当表结构修改或行数据增删改、清除，则使用该表的所有缓存查询都会被清除。



###### SELECT语句关键字的执行顺序

SELECT FROM left_table JOIN right_table

ON join_condition

WHERE where_condition

GROUP BY group_by

HAVING having_condition

ORDER BY order_by

LIMIT offset,size





###### `mysql`常见并发参数说明

1. max_connections

   允许连接到`mysql`数据库的最大数量

2. back_log

   积压请求栈的等待连接的数量。如果连接数达到max_connections时，新来的请求将会被存在堆栈中，以等待某一连接释放资源，该堆栈的数据即back_log。如果等待连接数量超过back_log，则不被授予连接将会报错。

3. thread_cache_size

   `mysql`缓存客户端线程的数量，当连接断开后缓存该线程，可以重用连接线程去处理下一个连接。

4. table_open_cache

   所有`sql`语句执行线程可打开表缓存的数量，根据max_connections以及每个连接执行关联查询中涉及表的最大数量来决定：`max_connections * N`

5. `innodb_lock_wait_timeout`

   `InnoDB`事务等待行锁的时间。对于需要快速反馈的业务系统可以将行锁的等待时间调小，对于批量处理的程序可以将等待时间调大，以避免发生大的回滚操作。



###### `mysql`锁

表级锁：加锁快，锁定粒度大，不会出现死锁，并发度低。

行级锁：加锁慢，锁粒度小，会出现死锁，并发度高。





事务具有ACID特性：原子性（Atomicity）、一致性（Consistent）、隔离性（Isolation）、持久性（Durable）。

| ACID属性 | 含义                                                         |
| -------- | ------------------------------------------------------------ |
| 原子性   | 事务是一个原子操作的逻辑单元，其对数据的更新，要么全部成功，要么全部失败。 |
| 一致性   | 数据在事务执行后由一个一致性状态变成另一个一致性状态。在一致性状态下，所有事务对一个数据的读取结果都是相同的。 |
| 隔离性   | 当前事务不会被其他外部并发事务所影响                         |
| 持久性   | 事务完成之后，对于数据的修改是永久的                         |



并发事务问题：丢失更新、脏读、不可重复读、幻读。

| 问题       | 含义                                                         |
| ---------- | ------------------------------------------------------------ |
| 丢失更新   | 当多个事务选择同一行，最初的事务修改的值，被后面的事务修改的值覆盖。 |
| 脏读       | 当一个事务正在修改数据，而另一个事务访问并使用了修改后的数据，但是修改的数据没有提交到数据库中 |
| 不可重复读 | 一个事务在读取某一行数据后，再次读取时发现两次读取的数据不一致。 |
| 幻读       | 一个事务在读取一批数据后，再次读取时发现两次读取的数据行数不一致。 |



事务隔离级别

| 隔离级别                     | 丢失更新 | 脏读 | 不可重复读 | 幻读 |
| ---------------------------- | -------- | ---- | ---------- | ---- |
| 读未提交（read uncommitted） | √        | x    | x          | x    |
| 读已提交（read committed）   | √        | √    | x          | x    |
| 可重复读（repeatable read）  | √        | √    | √          | x    |
| 串行化（`serializable`）     | √        | √    | √          | √    |

√表示能解决对应的问题



`mysql`数据库中默认事务隔离级别为可重复读（repeatable read），而oracle数据的事务隔离级别只有读已提交（oracle默认级别）和串行化。

查看隔离级别方式：`SHOW VARIABLES LIKE 'tx_isolation'`







##### 

###### `InnoDB`的行锁模式

`InnoDB`存储引擎支持行级锁。

行锁的类型：共享锁（读锁/S锁）、排它锁（写锁/X锁）

共享锁：多个事务对于同一行数据都可以获取共享锁进行访问数据，但只能读不能修改。

排它锁：一个事务获取了一个数据行的排它锁，其他事务就不能再获取该行的共享锁或者排它锁。

| 行锁类型兼容 | S锁  | X锁  |
| ------------ | ---- | ---- |
| **S锁**      | √    | x    |
| **X锁**      | x    | x    |



对于INSERT、UPDATE、DELETE语句，`InnoDB`会自动给涉及数据行加上排它锁

对于普通SELECT语句，`InnoDB`不会加上任何锁。

可以通过以下语句显式对SELECT语句加上共享锁或排它锁：

```java
SELECT * FROM t_user WHERE where_condition LOCK IN SHARE MODE
SELECT * FROM t_user WHERE where_condition FOR UPDATE
```



如果SELECT语句显式加上共享锁或排它锁，但WHERE条件是不通过索引条件检索数据，那么`InnoDB`将会对表中的所有记录加锁，实际效果和表锁一样。



间隙锁

当使用范围条件而不是使用相等条件进行`sql`操作，并请求共享或排它锁时，`InnoDB`会给符合条件的数据行加上行锁，并且会对于在条件范围 内但并不存在的记录的间隙加上间隙锁。

```java
//数据表有id=1，2，4，6的数据
//SESSION1:
BEGIN;
SELECT * FROM t_user WHRER id BETWEEN 2 AND 4 LOCK IN SHARE MODE;

//此时SESSION2：
INSERT INTO t_user VALUES (8,'name'); //操作成功


INSERT INTO t_user VALUES (5,'name'); //阻塞，包含两端的间隙
INSERT INTO t_user VALUES (3,'name'); //阻塞

//当SESSION1进行COMMIT时，阻塞解除并成功增加id=3，5的记录。

```



###### 表级锁

适用于`InnoDB`、`MyISAM`、`MEMORY`存储引擎。表级锁有表锁、元数据锁。



表锁

在查询操作（SELECT语句）前，会自动给涉及的所有表加上读锁。在执行更新操作（INSERT、UPDATE、DELETE等）前，会自动给涉及的表加上写锁。

加锁过程是自动的不需要用户干预，显式加表锁或释放锁的语法：

```java
LOCK TABLES table_name READ; //加读锁
LOCK TABLES table_name WRITE; //加写锁

//为多个表加表锁
LOCK TABLES table_name1 READ, table_name2 READ, table_name3 WRITE;
UNLOCK TABLES; //释放锁
```

当显式对指定表加锁时，只能进行指定表的操作。如果此时对非指定表进行操作时，会提示` 1100 - Table 'msg' was not locked with LOCK TABLES`。



元数据锁（`MDL`，`metadata lock`）

`MDL`在访问表的时候会自动加上，当表进行增删改查时，加的是`MDL`读锁，当表进行结构表更时，加`MDL`写锁。

`MDL`的作用是保证读写的正确性。`MDL`读锁与读锁不互斥，但写锁之间、读写锁互斥，第二个锁操作需要等待第一个执行完才能继续。