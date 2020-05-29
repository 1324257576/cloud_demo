## `Mysql`复制



复制是将主数据库的`DDL`和`DML`操作通过二进制日志传到从库上，然后在从库上对这些日志进行重做，从而使得从库和主库的数据保持同步。

主从复制的优点：

1. 如果主库出问题，可以切换到从库提供服务。
2. 读写分离，降低主库的访问压力。
3. 在从库执行备份，以避免备份期间影响主库的服务。



复制的基本原理：

1. 主库在事务提交时会把数据变更作为事件Events记录在二进制日志文件`Binlog`中；（主库的`sync_binlog`参数控制`Binlog`日志刷新到磁盘中。）
2. 主库推送二进制日志文件`Binlog`中的事件到从库的中继日志`Replay Log`，从库根据中继日志`Relay Log`重做数据变更操作，通过逻辑复制以完成主从库的数据一致。



`MySQL`通过3个线程来完成主从库的数据复制：其中`BinLog Dump`线程跑在主库上，IO线程和`SQL`线程跑在从库上。当从库启动复制（START SLAVE）时，首先创建IO线程连接到主库，主库随后创建`Binlog Dump`线程去从`BinLog`文件中读取数据库事件并发送给IO线程，IO线程获取到并写入到从库的中继日志`Relay Log`中，之后从库的`SQL`线程读取中继日志中更新数据库的事件并应用。当从库的`SQL`线程在执行完`Relay Log`中的事件后，会负责删除当前的`Relay Log`文件。

![1588081004477](D:\Temp\cloud_demo\note\book_note\深入浅出mysql第二版\1588081004477.png)



为了保证从库Crash重启之后，从库的IO线程和`SQL`线程可以知道从哪里开始复制，从库上默认还会创建`master.info`和`relay-log.info`用于保存复制的进度。



复制的方式

二进制日志文件`Binlog`的格式有以下三种：

1. Statement：基于`SQL`语句级别的`Binlog`。
2. Row:基于行级别，记录每一行数据的变化。在复制时不会因为存储过程、触发器或不确定函数，而造成主从库数据不一致。
3. Mixed：混合Statement和Row模式。

这就对应了`MySQL`复制的三种技术：

1. `binlog_format=Statement`
2. `binlog_format=Row`
3. `binlog_format=Mixed`



## `Mysql`日志



错误日志

错误日志记录了当`mysqld`启动与停止时，以及服务器在运行过程中发生任何严重错误时的相关信息。

可以用`--log-error[=file_name]`选项来指定保存错误日志文件的位置，默认文件名是`host_name.err`。



二进制日志

二进制日志`Binlog`记录了所有的`DDL`（数据定义语言）的语句和`DML`（数据操纵语言）的语句，但不包含数据查询语句。

语句以事件的形式保存，描述着数据的更改过程。

可以用`--log-bin[=file_name]`选项指定保存二进制日志的位置，默认文件名是`host_name-bin`。



查询日志

查询日志记录了客户端的所有语句（并不是只记录SELECT语句），而二进制日志不包含只查询数据的语句。默认文件名是`host_name.log`。



慢查询日志

慢查询日志记录了所有执行时间超过参数`long_query_time`（单位为秒）设置值并且扫描记录数不小于`min_examined_row_limit`的所有`SQL`语句（并不是只记录SELECT语句）。默认文件名是`host_name-slow.log`。

慢查询日志默认是关闭的。可以指定日志输出方式：文件/slow_log表。



## `SQL`优化



定位执行效率低的`SQL`语句：

1. 慢查询日志查看
2. 使用`show processlist`命令查询当前正在进行的线程，实时查看`SQL`的执行情况



