###### 存储引擎

存储引擎就是存储数据、建立索引、更新查询数据等等技术的实现方式。存储引擎是基于表而不是基于库的。



`mysql`5.5之前默认的存储引擎是`MyISAM`，自5.5及5.5以后就改为了`InnoDB`。创建新表时如果不指定存储引擎则系统使用默认的存储引擎。



`InnoDB`

1. 事务控制

   `InnoDB`存储引擎提供了具有提交、回滚、崩溃恢复能力的事务安全。
   但是对比`MyISAM`存储引擎，`InnoDB`写的处理效率差一些，并且会占用更多的磁盘空间以保存数据和索引。

2. 外键约束

   支持外键的存储引擎只有`InnoDB`。在创建外键时可以指定在删除父表时、更新父表时，对子表进行相应操作，包括RESTRICT（默认）、CASCADE、SET NULL、NO ACTION。

   其中RESTRICT、NO ACTION相同，表示限制在子表有关联记录的情况下，父表不能进行相应的删除或更新操作。

   CASCADE表示在父表进行更新或删除时，子表级联地更新或删除对应的记录。

   SET NULL表示父表在更新或删除时，子表的对应字段被设置为NULL。

3. 文件存储方式

   使用多表空间存储的方式时，创建表的结构保存在.frm文件中，每个表的数据和索引单独保存在.idb中。

`MyISAM`

1. 访问速度快，适用于SELECT、INSERT为主的场景，不支持事务，也不支持外键。
2. 文件存储方式：每个表的文件存储为`.frm`存储表定义，`.MYD`存储数据，`.MYI`存储索引。

MEMORY

 Memory存储引擎将表的数据放在内存中，把表定义存储在`.frm`磁盘文件中。默认使用HASH索引，访问速度非常快，但服务一旦关闭，表数据就会丢失。



定位低效率执行的sql

1. 慢查询日志：慢查询日志中记录了执行时间超过long_query_time秒的`sql`语句。

2. `show processlist`：使用`show processlist`命令查询当前mysql在进行的线程，可以实时查看`sql`的执行情况。

   ![1586698335954](D:\Temp\cloud_demo\note\mysql\1586698335954.png)

   1. id：系统分配的`connection_id`
   2. user：当前用户
   3. host：执行语句的host：port
   4. db：连接的库
   5. command：当前连接的执行命令
   6. time：当前状态持续时间
   7. state：语句执行中的当前状态
   8. info：当前执行语句



explain分析执行计划

`EXPLAIN mysql_statement`

![1586699802409](D:\Temp\cloud_demo\note\mysql\1586699802409.png)

1. id：执行顺序，id值越大，执行的优先级越高
2. select_type：常见取值：SIMPLE（简单查询）、PRIMARY（复杂查询的最外层查询）、UNION（UNION后面的查询）、`SUBQUERY`（子查询）、DERIVED（衍生查询，使用临时表存放结果）、UNION RESULT（从UNION表获取结果的查询）
3. table：结果集对应的表
4. type：表连接的类型，取值：NULL（不访问任何表及索引，直接返回结果）、`const`（使用主键或唯一索引的字段作为where条件，并且只匹配一行数据）、eq_ref（使用主键或唯一索引的字段进行关联查询，并且只匹配一行数据）、ref（非唯一性索引的扫描，返回匹配某个单独值的所有行）、range（使用索引字段，where后出现范围查询）、index（遍历索引树）、all（遍历全表）
5. possible_keys：可能用到的索引
6. key：实际用到的索引
7. key_len：索引字段长度
8. rows：扫描行的数量
9. extra：执行情况的描述：`using filesort`（使用`sort byffer`或文件排序）、`using temporary`（使用临时表）、`using index`（使用覆盖索引）





