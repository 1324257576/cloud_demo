索引（Index）：满足特定查找算法又维护着表数据引用的数据结构，使用索引能高效获取表数据。

索引的优势与劣势

优势：提高数据检索效率，降低数据库的IO成本；降低数据排序的成本，降低CPU消耗。

劣势：占用存储空间；降低表更新的速度。



索引结构

索引是在MySQL的存储引擎层实现的。MySQL提供了以下4种索引：

1. BTREE索引：最常见的索引类型
2. HASH索引：只有Memory引擎支持
3. RTREE索引：空间索引，是MyISAM存储引擎的一个特殊索引类型，主要用于地理空间数据类型。
4. Full-Text索引：全文索引



<center><b>MyISAM、InnoDB、Memory三种存储引擎对各种索引类型的支持</b></center>

| 索引        | InnoDB引擎      | MyISAM引擎 | Memory引擎 |
| ----------- | --------------- | ---------- | ---------- |
| BTREE索引   | 支持            | 支持       | 支持       |
| HASH 索引   | 不支持          | 不支持     | 支持       |
| R-tree 索引 | 不支持          | 支持       | 不支持     |
| Full-text   | 5.6版本之后支持 | 支持       | 不支持     |



这里的索引，如果没有特别说明，都是指B+树（多路平衡搜索树）结构组织的索引。



B树与B+树的结构

B树，又叫多路平衡搜索树，一棵m叉的B树特性如下：

- 树中的每个节点最多包含m个孩子

- 除根节点和叶子节点外，每个节点至少有ceil(m/2)个孩子

- 若根节点不是叶子节点，则至少有两个孩子

- 所有的叶子节点都在同一层

- 每个非叶子节点由n个key和n+1个指针组成，其中ceil(m/2)-1<=n<=m-1

  

以5叉BTree为例，key的数量：公式推导[ceil(m/2)-1] <= n <= m-1。所以 2 <= n <=4 。当n>4时，中间节点分裂到父节点，两边节点分裂。

插入 C N G A H E K Q M F W L T Z D P R X Y S 数据为例。

演变过程如下：

1). 插入前4个字母 C N G A 

![1555944126588](D:\Temp\cloud_demo\note\mysql\1555944126588.png) 

2). 插入H，n>4，中间元素G字母向上分裂到新的节点

![1555944549825](D:\Temp\cloud_demo\note\mysql\1555944549825.png) 

3). 插入E，K，Q不需要分裂

![1555944596893](D:\Temp\cloud_demo\note\mysql\1555944596893.png) 

4). 插入M，中间元素M字母向上分裂到父节点G

![1555944652560](D:\Temp\cloud_demo\note\mysql\1555944652560.png) 

5). 插入F，W，L，T不需要分裂

![1555944686928](D:\Temp\cloud_demo\note\mysql\1555944686928.png) 

6). 插入Z，中间元素T向上分裂到父节点中 

![1555944713486](D:\Temp\cloud_demo\note\mysql\1555944713486.png) 

7). 插入D，中间元素D向上分裂到父节点中。然后插入P，R，X，Y不需要分裂

![1555944749984](D:\Temp\cloud_demo\note\mysql\1555944749984.png) 

8). 最后插入S，NPQR节点n>5，中间节点Q向上分裂，但分裂后父节点DGMT的n>5，中间节点M向上分裂

![1555944848294](D:\Temp\cloud_demo\note\mysql\1555944848294.png) 

到此，该BTREE树就已经构建完成了， BTREE树 和 二叉树 相比， 查询数据的效率更高， 因为对于相同的数据量来说，BTREE的层级结构比二叉树小，因此搜索速度快。





B+树

B+树是B树的变种，区别是：

1. n叉B+树最多可以含有n个key，而B树最多含有n-1个key。
2. B+树的中间节点不保存数据，只用来索引，所有数据都保存在叶子节点中。而B树的每个节点都保存着数据。
3. B+Tree的叶子节点保存所有的key信息（包括了父节点的数据），，按照key大小的顺序自小而大排列（父节点的数据在子节点所有key中是最大或最小）。
4. B+树所有叶子节点形成有序链表，便于范围查询。



索引分类

1. 单值索引：一个索引只包含一个列
2. 唯一索引：索引列的值必须唯一，但允许有NULL，NULL可能出现多次
3. 复合索引：一个索引包含多个列



索引相关语法：

```sql
#创建
CREATE [QUIQUE|FULLTEXT|SPATIAL] INDEX index_name
[USING index_type]
ON table_name(col_name,...)
#查看
show index from table_name
#删除
drop index index_name on table_name

#alter
添加主键，索引值必须是唯一且不能为NULL
alter table table_name add primary key(col_list) 
添加唯一索引，索引值必须是唯一，允许存在多个NULL
alter table table_name add unique index_name(col_list)
添加普通索引
alter table table_name add index index_name(col_list)
```





视图创建或修改语法

```sql
CREATE OR REPLACE VIEW view_name
AS
select_statement;

DROP VIEW [IF EXISTS] view_name ;
```











































