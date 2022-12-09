# MuziDB
一个用Java简单实现的数据库

## 前言
该项目写完也有一段时间了，为了避免以后忘记该项目的一些实现的原理，所以写下这篇博客来记录一下该项目的设计等
## 项目整体
MuziDB分为前端与后端，前后端交互通过socket进行交互，前端的作用就是读取用户输入并发送到后端进行执行然后输出返回结果，并等待下一次的输入，后端则需要解析SQL，尝试执行并返回结果。MuziDB的后端分为五个模块

> **Transaction Manager (TM )
> Data Manager (DM) 
> Version Manager (VM) 
> Index Manager (IM) 
> Table Manager (TBM)**

## 项目结构
![在这里插入图片描述](https://img-blog.csdnimg.cn/0f4136332f944455b7e27eb66425df43.png)


TM：维护XID文件来维护事务的状态，并提供接口给其它模块来查询某个事务的状态
DM：直接管理数据的DB文件和日志文件
VM：基于两段锁协议实现调度序列的可串行化，并实现了MVCC消除读写阻塞
IM：实现了B+树的索引
TBM：实现了对字段和表的管理，同时解析SQL语句并根据语句操作表

## 项目涉及四个文件
![在这里插入图片描述](https://img-blog.csdnimg.cn/0661dd745eed4b8f8ba8e104a4289963.png)

> 根据**UID**可以定位到是那个页面多少偏移量，因为pgno是int类型，offset是short类型，而我们返回的UID是long类型所以long占八个字节而UID = ((long)pgno << 32) | (long)offset，所以前四个字节是pgno所占用的，最后2个字节是offset占用的，所以只需要再经过简单换算就可以换算出来对应的pgno与offset，所以.db中的每个资源都有着唯一的UID，所以**UID的作用可以相当于一个资源定位符**

**.bt文件**
该文件只有8个字节，当创建一张新表的时候就会把新表的UID复制到这个文件保存

**.xid文件**
这个文件就是管理事务的文件，里面会保存首先8个字节的XIDCounter，然后接下来的每个字节就是保存一个事务的状态
![在这里插入图片描述](https://img-blog.csdnimg.cn/efe0e53fed44496b955c666e8c8d527d.png)

这个文件对应的功能就是比较简单的TM，只需要保证事务的开启、提交、取消即可
事务对应着三种状态

> **0 - active 
> 1 - committed 
> 2 - aborted**



**.log文件**

为什么一直不说.db文件呢，因为.db文件中的要素过多留到最后来讲
.log文件就是记录操作过程中产生的日志，为什么要记录日志呢，有了.db文件，.db文件中不是就已经存储了数据了吗?那MySQL数据中为啥又会存在redo、undo log日志呢，而且记录日志有助于后面实现可重复读，假如不记录日志的话，那么可重复读就不能够实现

日志文件格式如下 前四个字节记录所有日志的校验和，后面就是一个一个的[Log]对象即 [xchecksum] [log1] [log2] ... [logn] [BadTail] ，badTail有可能会出现，比如当你记录最后一条日志的时候但是你没有记录完但是数据库宕机了那么这就是badTail
每个日志对象即[log]的形式是 [size][checksum][data]
其中size占四个字节，checksum占四个字节，data所占字节就是size所描述的

![在这里插入图片描述](https://img-blog.csdnimg.cn/83816ef0860341bfa2615ac015c72a26.png)

**.db文件**

.db文件是最重要的一个文件了，里面包含表结构数据、表中数据、Node节点数据等，.db文件中保存的都是一个一个的DataItem结构,.db文件是以页来区分，每个页面大小为8k但是你也可以设置更大的容量，每个页面的前2个字节为该页面的偏移量(便宜量就是当前页面要从哪里开始写入新数据)，
DataItem中存在三个字段
ValidFlag 1字节的标志位代表是否有效
Size 2字节Data的字段的大小
Data 就是数据

![在这里插入图片描述](https://img-blog.csdnimg.cn/c6b6e9c35aa24598856bbeb34d7a2376.png)
