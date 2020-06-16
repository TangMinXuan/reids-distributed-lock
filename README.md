# reids-distributed-lock


##是什么?

是一个基于 Redis 实现的分布式锁

##有哪些特点？

* 可重入锁。基于 Redis 的 Hash 结构实现，记录加锁次数
* 使用Lua脚本来编写加锁、解锁的逻辑，保证这两个动作的原子性。

##如何使用？

在 application.properties 中配置好 Redis 地址、端口号和 uuid 等。

其中uuid是用于标识环境中不同的系统用的
