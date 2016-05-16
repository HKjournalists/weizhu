# 军用微信构建测试平台搭建手册

## 一. 安装CentOS 6.x操作系统
1. 启动服务器，安装CentOS操作系统
    * 使用语言为英文，时区为上海
    * 选择删除所有数据，全新安装
    * 设置好root用户密码
    * **一定要设置好 hostname**
    * 其他选项使用默认值即可
    * 安装完成后重启
2. 操作系统初始配置
    * 打开网络
        - 配置网卡静态ip。修改`/etc/sysconfig/network-scripts/ifcfg-eth0`文件，更改或添加以下选项，其他不要动
        
            ```
            # 将ONBOOT=no修改为ONBOOT=yes
            ONBOOT=yes
            NM_CONTROLLED=yes
            BOOTPROTO=static
            IPADDR=192.168.56.10
            NETMASK=255.255.255.0
            BROADCAST=192.168.56.255
            ```
        - 重启网络`service network restart`
    * 关闭防火墙
        - 执行命令`chkconfig iptables off`, `service iptables stop`
    * 关闭SELINUX
        - 修改`/etc/selinux/config`,将`SELINUX=enforcing`改为`SELINUX=disabled`
    * 安装更新
        - 执行命令`yum update -y`
    * 创建weizhu数据存放目录 `/data`
        - 确保`/data`目录在存放数据的分区上。可软连接到存放数据的分区
        - 如果有数据盘，可将数据盘挂载到该目录上
    * 将后续安装的软件放到`/data/weizhu/weizhu-build/software`目录下
    * 重启虚拟机
        - 执行命令`reboot`

## 二. 安装gitlab, 并创建相关用户和工程
1. 官网安装说明：[https://about.gitlab.com/downloads/#centos6](https://about.gitlab.com/downloads/#centos6)
2. 安装依赖包

    ```
    sudo yum install curl openssh-server openssh-clients postfix cronie
    sudo service postfix start
    sudo chkconfig postfix on
    sudo lokkit -s http -s ssh
    ```
4. 因为gitlab RPM包在AmazonS3上，需要先配置yum翻墙http代理下载
    * 修改`/etc/yum.conf`
    * 添加http代理,`proxy=http://xxx.xxx.xxx.xxx:yyyy` 
3. 安装gitlab repo和gitlab

    ```
    curl -sS https://packages.gitlab.com/install/repositories/gitlab/gitlab-ce/script.rpm.sh | sudo bash
    sudo yum install gitlab-ce
    ```
4. 配置并启动gitlab

    ```
    sudo gitlab-ctl reconfigure
    ```
5. 登录并创建工程weizhu_server,weizhu_config,weizhu_android

## 三. 迁移代码
1. 切换当前目录到weizhu_server
2. 执行命令，切换origin库地址`git remote set-url origin git@test.weizhu.dev:root/weizhu_server.git`
3. 执行命令，将代码推送到origin库`git push origin master`
4. 依次将`weizhu_config`,`weizhu_android`库代码迁移完成

## 四. 安装weizhu_server构建所需组件
1. rpm安装jdk
2. 安装maven到 `/opt/apache-maven-3.3.9`目录。修改配置`/opt/apache-maven-3.3.9/conf/setting.xml`，将maven本地库放到 `/data/weizhu/apache-maven-repository`目录
3. 安装protoc命令. 将`protoc-2.6.1-build2-linux-x86_64.exe`文件拷贝到`/usr/local/bin/protoc`,并修改权限`chmod a+x /usr/local/bin/protoc`
4. 安装mysql. 参考构建文档
5. 安装redis. 参考构建文档
6. 安装ansible. 先执行`yum install epel-release`, 再执行命令`yum install ansible -y`
6. 配置好 MAVEN_HOME, PATH环境变量.

## 五. 安装weizhu_android构建所需组件
1. 安装android sdk到目录`/opt/android-sdk-linux`
2. 执行命令插看所有可下载组件。需要翻墙http代理

    ```
    /opt/android-sdk-linux/tools/android list sdk --all --proxy-host 192.168.56.1 --proxy-port 8848 
    ```
3. 执行命令安装或更新所需组件

    ```
    /opt/android-sdk-linux/tools/android update sdk --no-ui --proxy-host 192.168.56.1 --proxy-port 8848 --no-https --all --filter 1,2,3,4,5,6,7,8,10,12,18,19,29,34,35,36,37,38,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167
    ```
4. 安装依赖组件

    ```
    yum install glibc.i686 zlib.i686 libstdc++.i686 ncurses-libs.i686
    ```
4. 安装gradle
5. 安装nginx，参考单机部署文档
    * 修改端口为8080
    * 将root目录设为`/data/weizhu/weizhu-build`
    * 打开autoindex
    * 访问此nginx地址可下载编译完成的android下载包

## 六. 安装配置jenkins
1. 使用rpm安装
2. 下载jenkins插件 `Dynamic Parameter Plug-in`, `Git Parameter Plug-In`
3. 配置jenkins环境变量 `ANDROID_HOME`,`PATH`
4. 配置`weizhu_server`构建
    * 设置参数，使用`GIT Parameter`, `GIT_NAME` `Branch or tag`
    * 设置构建脚本
    
        ```
        GIT_HASH=`git rev-parse --short HEAD`
        GIT_TIME=`git show -s --format=%ci HEAD | cut -d':' -f 1,2 | sed 's/[- :]//g'`
        GIT_AUTHOR=`git show -s --format=%ce HEAD`
        
        BUILD_DIR=/data/weizhu/weizhu-build/weizhu_server
        
        if [[ ${GIT_NAME} == origin/master ]]; then
        
          BUILD_NAME=master-${GIT_TIME}-${GIT_HASH}
          BUILD_DIR=${BUILD_DIR}/master
          
        elif [[ ${GIT_NAME} == origin/* ]]; then
        
          BRANCH_NAME=`echo ${GIT_NAME} | sed 's/^origin\///g'`
          BUILD_NAME=branch-${BRANCH_NAME}-${GIT_TIME}-${GIT_HASH}
          BUILD_DIR=${BUILD_DIR}/branch
        
        else
        
          BUILD_NAME=${GIT_NAME}
          BUILD_DIR=${BUILD_DIR}/tag
          
        fi
        
        mvn package
        
        if [[ -d ${BUILD_DIR}/${BUILD_NAME} ]]; then
          rm -frv ${BUILD_DIR}/${BUILD_NAME}
        fi
        
        mv -v build ${BUILD_DIR}/${BUILD_NAME}
        ```

5. 配置`weizhu_server`测试环境部署构建
    * 设置构建参数`Dynamic Choice Parameter`, `BUILD_NAME`
    
        ```
        def ver_keys = [ 'bash', '-c', 'cd /data/weizhu/weizhu-build/weizhu_server; ls -t master/ | head -10; ls -t branch/ | head -10; ls -t tag/ ;' ]
        ver_keys.execute().text.tokenize('\n')
        ```
    * 设置构建参数`Choice`, `DEPLOY_SERVER`
    
        ```
        all
        weizhu-all-server
        weizhu-admin-webapp
        weizhu-mobile-webapp
        weizhu-demo-webapp
        weizhu-upload-webapp
        weizhu-boss-webapp
        ```
    * 设置构建脚本
    
        ```
        BUILD_DIR=/Users/weizhu/Develop/weizhu_build/server
        
        if [[ ${BUILD_NAME} == master-* ]]; then
          BUILD_DIR=${BUILD_DIR}/master/${BUILD_NAME}
        elif [[ ${BUILD_NAME} == branch-* ]]; then
          BUILD_DIR=${BUILD_DIR}/branch/${BUILD_NAME}
        else
          BUILD_DIR=${BUILD_DIR}/tag/${BUILD_NAME}
        fi
        
        sh ./server/weizhu_deploy.sh development ${BUILD_DIR} ${DEPLOY_SERVER}
        ```
6. 配置`weizhu_android`构建
    * 设置构建参数`GIT Parameter`, `GIT_NAME` `Branch or tag`
    * 设置构建脚本
    
        ```
        GIT_HASH=`git rev-parse --short HEAD`
        GIT_TIME=`git show -s --format=%ci HEAD | cut -d':' -f 1,2 | sed 's/[- :]//g'`
        GIT_AUTHOR=`git show -s --format=%ce HEAD`
        
        VERSION_CODE=`sed -n "s/versionCode \(.*\)/\1/p" app/build.gradle | tr -d ' '`
        VERSION_NAME=`sed -n "s/versionName '\(.*\)'/\1/p" app/build.gradle | tr -d ' '`
        
        BUILD_DIR=/data/weizhu/weizhu-build/weizhu-android
        
        if [[ ${GIT_NAME} == origin/master ]]; then
          
          sed -i '' "s#versionNameSuffix '.*'#versionNameSuffix '_master-${GIT_TIME}-${GIT_HASH}'#" app/build.gradle
          BUILD_NAME=${VERSION_NAME}_${VERSION_CODE}_master-${GIT_TIME}-${GIT_HASH}
          BUILD_DIR=${BUILD_DIR}/master
        
        elif [[ ${GIT_NAME} == origin/* ]]; then

          BRANCH_NAME=`echo ${GIT_NAME} | sed 's/^origin\///g'`
          
          sed -i '' "s#versionNameSuffix '.*'#versionNameSuffix '_branch-${BRANCH_NAME}-${GIT_TIME}-${GIT_HASH}'#" app/build.gradle
          BUILD_NAME=${VERSION_NAME}_${VERSION_CODE}_branch-${BRANCH_NAME}-${GIT_TIME}-${GIT_HASH}
          BUILD_DIR=${BUILD_DIR}/branch
        
        else
        
          if [[ ${GIT_NAME} != ${VERSION_NAME}_${VERSION_CODE} ]]; then
            echo "Tag名称和版本名不匹配！"
            exit -1
          fi
  
          BUILD_NAME=${GIT_NAME}
          BUILD_DIR=${BUILD_DIR}/tag
          
        fi

        gradle build
        
        APK_FILES=$(cd app/build/outputs/apk && ls app-*-release.apk)

        mkdir build_tmp
        
        for APK_FILE in ${APK_FILES}
        do

        COMPANY_NAME=`echo ${APK_FILE} | sed -n "s/app-\(.*\)-release.apk/\1/p" | tr -d ' '`
        mv app/build/outputs/apk/app-${COMPANY_NAME}-release.apk build_tmp/${COMPANY_NAME}_${BUILD_NAME}.apk
        
        done
        
        if [[ -d ${BUILD_DIR}/${BUILD_NAME} ]]; then
          rm -frv ${BUILD_DIR}/${BUILD_NAME}
        fi
        
        mv -v build_tmp ${BUILD_DIR}/${BUILD_NAME}
        ```
