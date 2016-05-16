# JVM监控命令
## jps (java virtual machine process status tool)
直接输入jps,显示进程id和main类或者jar名称。
```
[root@test ~]# jps
31985 weizhu-webapp-server-branch-francislin_discover_cli-201602291920-0c666a9.jar
```

### jps有如下几个命令：
* -l 显示main类或者jar的路径+名称
```
[root@test ~]# jps -l
31985 /usr/local/weizhu/weizhu-admin-webapp-server/bin/weizhu-webapp-server-branch-francislin_discover_cli-201602291920-0c666a9.jar
```
* -m 显示main方法传入的参数
```
[root@test ~]# jps -m
31985 weizhu-webapp-server-branch-francislin_discover_cli-201602291920-0c666a9.jar /usr/local/weizhu/weizhu-admin-webapp-server/bin/weizhu-admin-webapp-branch-francislin_discover_cli-201602291920-0c666a9.war
```
* -q 只显示进程id
```
[root@test ~]# jps –q
31985
```
* -v 显示传入的jvm参数
```
[root@test ~]# jps -v
31985 weizhu-webapp-server-branch-francislin_discover_cli-201602291920-0c666a9.jar -Dserver.name=weizhu-admin-webapp-server -Dserver.conf=/usr/local/weizhu/weizhu-admin-webapp-server/conf/server.conf -Dserver.logs=/usr/local/weizhu/weizhu-admin-webapp-server/logs -Dserver.tmp=/usr/local/weizhu/weizhu-admin-webapp-server/tmp -Dfile.encoding=UTF-8 -Dlogback.configurationFile=/usr/local/weizhu/weizhu-admin-webapp-server/conf/logback.xml -Xms512m -Xmx512m -XX:NewSize=256m -XX:MaxNewSize=256m -Xss1m -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=80 -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintGCApplicationStoppedTime -Xloggc:/usr/local/weizhu/weizhu-admin-webapp-server/logs/jvm.log -XX:ErrorFile=/usr/local/weizhu/weizhu-admin-webapp-server/logs/jvm_err.log
```
**命令合起来：**
```
[root@test ~]# jps -l -m
31985 /usr/local/weizhu/weizhu-admin-webapp-server/bin/weizhu-webapp-server-branch-francislin_discover_cli-201602291920-0c666a9.jar /usr/local/weizhu/weizhu-admin-webapp-server/bin/weizhu-admin-webapp-branch-francislin_discover_cli-201602291920-0c666a9.war
45378 sun.tools.jps.Jps -l -m
```
## jstack (java stack trace)
直接输入jstack pid 打印出线程的堆栈信息
* -l 打印出锁信息，如果某个线程有死锁，可显示。（如图后两行）
```
"service-6" #163 prio=5 os_prio=0 tid=0x00007f7998003800 nid=0x85dc waiting on condition [0x00007f79a7770000]
   java.lang.Thread.State: WAITING (parking)
        at sun.misc.Unsafe.park(Native Method)
        - parking to wait for  <0x00000000efad4f28> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
        at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
        at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2039)
        at java.util.concurrent.ArrayBlockingQueue.take(ArrayBlockingQueue.java:403)
        at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1067)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1127)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
        at java.lang.Thread.run(Thread.java:745)

   Locked ownable synchronizers:
        - None
```
Jstack 是打印线程堆栈信息，所以在排查问题的时候用常用。
我这里写了一个死循环的线程来测试
先用**top**查看哪个进程占cpu最高
```
  PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME+  COMMAND
 1290 root      20   0 2185m  27m  11m S 12.6  2.8   2:03.22 java
    7 root      20   0     0    0    0 R  1.3  0.0   0:16.00 events/0
 1240 root      20   0 99.8m 6320 3476 S  0.7  0.6   0:07.59 sshd
```
这里看到了 pid 1290的占用cpu/memory最高,
然后**shift+h**打开线程查看情况
```
  PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME+  COMMAND
1291 root      20   0 2185m  27m  11m S 12.3  2.8   2:10.25 java
 1231 mysql     20   0 1052m 452m 6120 S  8.9 45.5   0:00.27 mysqld
 1235 mysql     20   0 1052m 452m 6120 S  7.6 45.5   0:00.23 mysqld
 ```
看到了1291的线程cpuz占用最高
```
printf “%x\n” 1291
```
来查看16进数据 50b,然后
```
jstack 1290 | grep 1291
```
"main" #1 prio=5 os_prio=0 tid=0x00007fad08008800 nid=0x50b runnable [0x00007fad105e7000]
我的死循环测试代码所在的main类。

## jmap (java memory map)
jmap用来查看java memory详情
* jmap –histo pid 打印内存使用情况
```
[root@localhost ~]# jmap -histo 1610

 num     #instances         #bytes  class name
----------------------------------------------
   1:         68352        3280896  java.nio.HeapCharBuffer
   2:         35138         843312  java.lang.String
   3:           132         122280  [I
   4:           982          82872  [C
   5:           446          50872  java.lang.Class
   6:             7          24960  [B
```
这里能看出来java.nio.HeapCharBuffer，java.lang.String
内存占用率很高，因为我的测试代码中循环打印System.out.println(new String(“aaaa”));

* jmap –heap 查看当前进程使用的gc方法，和内存的使用情况

* jmap –dump:format=b, file=/tmp/a.dump 1610
* jhat –port 9090 a.dump
```
[root@localhost tmp]# jhat -port 9090 b.map
Reading from b.map...
Dump file created Tue Mar 08 21:38:40 EST 2016
Snapshot read, resolving...
Resolving 4142 objects...
Chasing references, expect 0 dots
Eliminating duplicate references
Snapshot resolved.
Started HTTP server on port 9090
Server is ready.
```
然后在浏览器输入http://127.0.0.1:9090,可以看到当前进程的内存详细信息

* Jmap –finalizerinfo打印正在回收的对象

## Jstat （java state）打印jvm详细状态
* jstat -gc pcid time(间隔时间毫秒为单位) times(打印次数)
```
 S0C（存活0） S1C（存活1） S0U（存活0使用） S1U（存活1使用） EC（伊甸） EU（伊甸使用） OC（old） OU（old使用） MC（类元） MU（类元） CCSC  CCSU  YGC（yang gc次数） YGT（yang gc时间） FGC(full gc次数)    FGCT(full gc时间) GCT
512.0  512.0   0.0    0.0    4480.0   3942.5   10944.0     251.5    4864.0 2459.5 512.0  262.3      18    0.044   0      0.000    0.
512.0  512.0   0.0    0.0    4480.0   3942.5   10944.0     251.5    4864.0 2459.5 512.0  262.3      18    0.044   0      0.000    0.
512.0  512.0   0.0    0.0    4480.0   1971.3   10944.0     251.5    4864.0 2459.5 512.0  262.3      19    0.044   0      0.000    0.
512.0  512.0   0.0    0.0    4480.0   1971.3   10944.0     251.5    4864.0 2459.5 512.0  262.3      19    0.044   0      0.000    0.
```