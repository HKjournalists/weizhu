# 微助数据统计分析相关

### 安装zookeeper

1. [下载zookeeper-3.4.6.tar.gz](http://zookeeper.apache.org/releases.html#download)
2. 解压并创建软连接
    * `tar -zxvf zookeeper-3.4.6.tar.gz -C /usr/local/weizhu/`
    * `ln -s /usr/local/weizhu/zookeeper-3.4.6 /usr/local/weizhu/zookeeper`
3. 创建数据和日志存放目录
    * `mkdir -p /data/weizhu/zookeeper/data /data/weizhu/zookeeper/logs`
4. 配置zookeeper
    * 拷贝sample配置
    
        ```
        cd /usr/local/weizhu/zookeeper/conf
        cp zoo_sample.cfg zoo.cfg
        ```
    * 修改zoo.cfg `dataDir=/data/weizhu/zookeeper/data`
5. 修改环境脚本`/usr/local/weizhu/zookeeper/bin/zkEnv.sh`
    * 在脚本开头注释后添加`ZOO_LOG_DIR=/data/weizhu/zookeeper/logs`
6. 配置zookeeper随机启动
    * 创建文件`/etc/init.d/zookeeper`
    
        ```
        #!/bin/bash
        # 
        # ZooKeeper
        # 
        # chkconfig: 2345 89 9 
        # description: zookeeper
        
        exec /usr/local/weizhu/zookeeper/bin/zkServer.sh $@
        
        case "$1" in
          start)
            touch /var/lock/subsys/zookeeper
          ;;
          stop)
            if [ -f /var/lock/subsys/zookeeper ]; then
              rm -f /var/lock/subsys/zookeeper
            fi
          ;;
        esac
        ```
    * 添加自动启动 `chkconfig --add zookeeper`
    * 启动zookeeper `service zookeeper start` 

### 安装kafka

1. [下载kafka_2.11-0.9.0.0.tgz](http://kafka.apache.org/downloads.html)
2. 解压并创建软连接
    * `tar -zxvf kafka_2.11-0.9.0.0.tgz -C /usr/local/weizhu/`
    * `ln -s /usr/local/weizhu/kafka_2.11-0.9.0.0 /usr/local/weizhu/kafka`
3. 创建数据和日志存放目录
    * `mkdir -p /data/weizhu/kafka/data /data/weizhu/kafka/logs`
4. 配置kafka`/usr/local/weizhu/kafka/config/server.properties`
    
    ```
    # 改为外部可访问的ip地址
    advertised.host.name=192.168.56.11
    # kafka数据存放目录
    log.dirs=/data/weizhu/kafka/data
    # 数据保存时间，永久(10年)
    log.retention.hours=87600
    # zookeeper地址
    zookeeper.connect=192.168.56.11:2181
    ```
5. 修改kafka启动脚本`/usr/local/weizhu/kafka/bin/kafka-server-start.sh`
    * 在脚本开头注释后添加`export LOG_DIR=/data/weizhu/kafka/logs`
6. 配置kafka随机启动
    * 创建文件`/etc/init.d/kafka`
    
        ```
        #!/bin/bash
        # 
        # kafka
        # 
        # chkconfig: 2345 90 8 
        # description: kafka

        KAFKA_HOME=/usr/local/weizhu/kafka
        
        function kafka_start() {
          ${KAFKA_HOME}/bin/kafka-server-start.sh -daemon ${KAFKA_HOME}/config/server.properties
          
          touch /var/lock/subsys/kafka
        }
        
        function kafka_stop() {
          ${KAFKA_HOME}/bin/kafka-server-stop.sh
          
          while [ `ps ax | grep -i 'kafka\.Kafka' | grep java | grep -v grep | wc -l` -gt 0 ]
          do
            echo "kafka waiting shutdown"
        	sleep 1
          done
          
          if [ -f /var/lock/subsys/kafka ]; then
            rm -f /var/lock/subsys/kafka
          fi
        }
        
        case "$1" in
          start)
            kafka_start
          ;;
          stop)
            kafka_stop
          ;;
          restart)
            kafka_stop
            kafka_start
          ;;
          *)
            echo "service kafka start|stop|restart "
          ;;
        esac
        ```
    * 添加自动启动 `chkconfig --add kafka`
    * 启动zookeeper `service kafka start` 
    
### 安装postgresql

1. [下载postgresql相关rpm](http://yum.postgresql.org/9.5/redhat/rhel-6-x86_64/repoview/postgresqldbserver95.group.html)
2. 安装rpm文件

    ```
    yum -y install libxslt
    rpm -ivh postgresql95-libs-9.5.0-1PGDG.rhel6.x86_64.rpm
    rpm -ivh postgresql95-9.5.0-1PGDG.rhel6.x86_64.rpm
    rpm -ivh postgresql95-server-9.5.0-1PGDG.rhel6.x86_64.rpm
    rpm -ivh postgresql95-contrib-9.5.0-1PGDG.rhel6.x86_64.rpm
    ```
3. 创建存储数据目录

    ```
    mkdir -p /data/weizhu/pgsql/9.5
    chown -R postgres:postgres /data/weizhu/pgsql
    ```
4. 修改自动启动文件`/etc/init.d/postgresql-9.5`以下配置
   
    ```
    PGDATA=/data/weizhu/pgsql/9.5/data
    PGLOG=/data/weizhu/pgsql/9.5/pgstartup.log
    PGUPLOG=/data/weizhu/pgsql/$PGMAJORVERSION/pgupgrade.log
    ```
5. 初始化数据目录，并启动postgresql

    ```
    service postgresql-9.5 initdb
    service postgresql-9.5 start
    chkconfig postgresql-9.5 on
    ```
    
