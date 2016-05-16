# weizhu_server 单机部署流程文档

1. 下载CentOS-6.x-x86_64-minimal版本的iso文件
    * [官网下载镜像地址列表](http://isoredirect.centos.org/centos/6/isos/x86_64/)
    * 推荐下载完成后校验一下文件的md5，验证下载文件是否正确
2. 使用virtualbox安装CentOS虚拟机
    * **如果是线上实体机安装，请忽略这步**
    * 安装virtualbox
        - 注意安装路径和虚拟机放置路径
        - 安装对应版本的 Extension Pack
    * 新建Host-only Network: vboxnet0
        - Prefrences -> Network -> Host-only Networks -> 新建
    * 打开virtualbox新建虚拟机
        - Name 填写 `weizhu-server`
        - Type 选择 `Linux`
        - Version 选择 `Red Hat (64-bit)`
        - Memory size 设为 `2048MB`
        - Hard Disk 新建并选择 `Dynamically allocated`, 设为 `20GB`
        - CPU 建议设为双核
        - 光盘 使用刚才下载的CentOS iso文件
        - 网络 Adapter1使用Host-only模式，选择刚创建的vboxnet0网络. Adapter2使用NAT模式，确保可以连接外网
3. 启动服务器，安装CentOS操作系统
    * 使用语言为英文，时区为上海
    * 选择删除所有数据，全新安装
    * 设置好root用户密码
    * 其他选项使用默认值即可
    * 安装完成后重启
4. 操作系统初始配置
    * 打开网络
        - 配置网卡1静态ip。修改`/etc/sysconfig/network-scripts/ifcfg-eth0`文件，更改或添加以下选项，其他不要动
        
            ```
            # 将ONBOOT=no修改为ONBOOT=yes
            ONBOOT=yes
            NM_CONTROLLED=yes
            BOOTPROTO=static
            IPADDR=192.168.56.10
            NETMASK=255.255.255.0
            BROADCAST=192.168.56.255
            ```
        - 配置网卡2开机自启动。修改`/etc/sysconfig/network-scripts/ifcfg-eth1`文件，将`ONBOOT=no`修改为`ONBOOT=yes`即可。
        - 重启网络`service network restart`
    * 关闭防火墙
        - 执行命令`chkconfig iptables off`, `service iptables stop`
    * 关闭SELINUX
        - 修改`/etc/selinux/config`,将`SELINUX=enforcing`改为`SELINUX=disabled`
    * 修改打开文件最大参数
        
        ```
        vi /etc/security/limits.conf
        # 在文件末尾添加以下内容
        * soft nofile 65535
        * hard nofile 65535
        # 设置完成并重启后，可以用命令 ulimit -a 查看 open files 验证
        ```
    * 安装更新
        - 执行命令`yum update -y`
    * 创建weizhu数据存放目录 `/data`
        - 确保`/data`目录在存放数据的分区上。可软连接到存放数据的分区
        - 如果有数据盘，可将数据盘挂载到该目录上
    * 重启虚拟机
        - 执行命令`reboot`
5. 安装jdk
    * 下载jdk的linux rpm安装包
    * 执行命令安装 `rpm -ivh jdk-8u66-linux-x64.rpm`
6. 安装redis
    * 使用redis3.x版本
    * 从官网下载[redis-3.0.6.tar.gz](http://download.redis.io/releases/redis-3.0.6.tar.gz)
    * 安装 gcc命令 `yum install gcc -y`
    * 解压缩安装包 `tar -zxvf redis-3.0.6.tar.gz`
    * 切换当前目录到解压缩后的redis目录中,执行命令编译并安装redis `make install`
    * 执行脚本将redis安装为linux服务 `utils/install_server.sh`. 选项都为默认即可
    * 改变原有配置位置`mv /etc/redis/6379.conf /etc/redis/6379_default.conf`
    * 创建配置`/etc/redis/6379.conf`并填入以下内容
    
        ```
        # include default config
        include /etc/redis/6379_default.conf
        
        bind 127.0.0.1
        
        dir /data/weizhu/redis
        maxmemory 1gb
        maxmemory-policy allkeys-lru
        maxmemory-samples 10
        ```
    * 创建数据目录 `mkdir -p /data/weizhu/redis`
    * 重启redis `service redis_6379 restart`
    * 执行命令 `redis-cli INFO` 验证安装是否成功
7. 安装mysql
    * 使用mysql 5.6版本
    * [官网下载页面](http://dev.mysql.com/downloads/mysql/5.6.html#downloads)
    * 请下载`Linux - Generic (glibc 2.5) (x86, 64-bit), RPM Bundle`，下载完成后建议校验MD5
    * yum安装perl命令`yum install perl -y`
    * 删除旧的mysqllib `yum -y remove mysql-libs-5.1*`
    * 解压缩mysql bundle包 `tar -xvf MySQL-5.6.28-1.linux_glibc2.5.i386.rpm-bundle.tar`
    * 安装mysql相关rpm包
    
        ```
        rpm -ivh MySQL-server-5.6.28-1.linux_glibc2.5.x86_64.rpm 
        rpm -ivh MySQL-client-5.6.28-1.linux_glibc2.5.x86_64.rpm
        rpm -ivh MySQL-devel-5.6.28-1.linux_glibc2.5.x86_64.rpm 
        rpm -ivh MySQL-shared-5.6.28-1.linux_glibc2.5.x86_64.rpm 
        rpm -ivh MySQL-shared-compat-5.6.28-1.linux_glibc2.5.x86_64.rpm
        rpm -ivh MySQL-embedded-5.6.28-1.linux_glibc2.5.x86_64.rpm 
        ```
    * 创建`/etc/my.cnf`文件，配置mysql支持utf8mb4字符集
    
        ```
        [client]
        default-character-set = utf8mb4
        
        [mysql]
        default-character-set = utf8mb4
    
        [mysqld]
        bind-address = 127.0.0.1
        
        datadir=/data/weizhu/mysql
        character-set-client-handshake = FALSE
        character-set-server = utf8mb4
        collation-server = utf8mb4_unicode_ci
        init_connect='SET NAMES utf8mb4'
        ```
    * 更改数据存放目录
    
        ```
        # 创建文件数据目录 
        mkdir -p /data/weizhu/mysql
        # 初始化数据目录
        mysql_install_db
        # 更改数据目录所属用户
        chown -R mysql:mysql /data/weizhu/mysql
        ```
    * 启动mysql `service mysql start`
    * 执行命令确认 utf8mb4 更改结果
    
        ```
        mysql -uroot -e "SHOW VARIABLES WHERE Variable_name LIKE 'character\_set\_%' OR Variable_name LIKE 'collation%';"
        ```
    * 执行命令创建db
        
        ```
        mysql -uroot -e "create database weizhu_test;"
        ```
8. 安装nginx
    * 找到`nginx-1.8.0-1.el6.ngx.x86_64.rpm`安装包[nginx yum源](http://nginx.org/packages/centos/6/x86_64/RPMS/)
    * 执行命令安装`rpm -ivh nginx-1.8.0-1.el6.ngx.x86_64.rpm`
    * 将原有配置文件删除`rm /etc/nginx/conf.d/*.conf`
9. 安装ImageMagick
    * 执行命令安装 `yum install ImageMagick`
10. 安装WebRTC turnserver
    * [下载地址](https://github.com/coturn/coturn/wiki/Downloads)
    * 解压缩后执行 `install.sh`
    * 修改配置 `/etc/turnserver/turnserver.conf`
    
        ```
        # ...
        
        # Uncomment to use long-term credential mechanism.
        # By default no credentials mechanism is used (any user allowed).
        #
        lt-cred-mech
        user=weizhu:weizhu2015
        
        # ...
        
        # The default realm to be used for the users when no explicit 
        # origin/realm relationship was found in the database, or if the TURN
        # server is not using any database (just the commands-line settings
        # and the userdb file). Must be used with long-term credentials 
        # mechanism or with TURN REST API.
        #
        realm=weizhu
        
        # ...
        
        # Mobility with ICE (MICE) specs support.
        #
        mobility
        ```
    * 启动turnserver `service turnserver start`
11. 创建文件上传和图片存储访问目录
    * `mkdir /data/weizhu/weizhu-upload/image`
12. 配置服务器免密码登录
    * 请自行参考百度搜索结果配置
13. 命令行构建weizhu源代码
    - 参考[构建文档](build_src.md)配置好环境
    - 执行 `sh build.sh` 构建代码
    - 构建完成的jar/war包在代码跟目录下的build目录中
14. 使用localtest环境配置发布代码
    - 后台代码和服务器配置分开存放不同的代码库，使用ansible工具部署配置和构建好的代码
    - 切换当前目录到weizhu_config项目下
    - mac环境部署
        * 需要安装ansible `brew install ansible`
        * 执行命令 `./weizhu_deploy.sh localtest /path/to/weizhu_server/build/`. 其中`/path/to/weizhu_server/build/`为上一步构建完成的build目录路径
	- windowns环境因为无法安装ansible命令，请参考以下方法部署
	    * 先登录CentOS服务器安装ansible,rsync命令 `yum install ansible rsync -y`
	    * 在CentOS服务器上配置ssh免密码登录自己
	    * 在cygwin环境执行命令 `./weizhu_deploy_windows.sh localtest /path/to/weizhu_server/build/`. 其中`/path/to/weizhu_server/build/`为上一步构建完成的build目录路径
15. 代码部署后，需要初始化db。创建表结构和初始数据
    - 登录服务器，先执行命令停掉`weizhu-all-server`: `service weizhu-all-server stop`
    - 执行命令初始化db `service weizhu-all-server initdb`
    - 重新启动 `service weizhu-all-server start`
16. 管理后台登录,手机软件登录
    - 打开pc浏览器(请使用chrome或者firefox),访问 http://192.168.56.10/admin
    - 初始管理员用户和密码为 `weizhu@wehelpu.cn/123abcABC`
    - 可以使用android虚拟机安装localtest环境的apk登录测试。手机号 18600000000,验证码 666666 
17. 后续开发和测试
    - localtest环境初次部署配置成功后，后续开发测试部署只需重复13，14两个步骤即可
