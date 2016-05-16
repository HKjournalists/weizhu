# 微助后台开发大体流程

## Git的使用
1. 用英文名创建自己的开发分支，功能完成并测试ok后，合并到master。保证master分支随时可发布
2. 对于大的功能改动，可以用`英文名` + `功能名` 创建开发分支
3. 开发分支要及时将master上的改动合并过来。使用rebase的方式合并
4. 对于开发中的多次debug提交，可以使用soft reset -> commit -> force push branch的方式合并多次提交为一个提交。
5. 每个service都有一个单独的开发人员负责。保证同一个service不会又多人改动。避免掉不必要的冲突

## 服务开发流程
1. 需求确认。产品文档，开发工期，其他资源等
2. 创建服务目录。maven配置，代码目录等 src/main/java src/main/resource src/test/java src/test/resources pom.xml protoc mvn eclipse:eclipse
3. 编写服务 proto 文件。结构体，服务接口
4. 编写服务 db 文件，设计cache k-v
5. proto/db/cache 设计评审
6. 编写 impl 实现代码
7. 编写单元测试，自测代码。 
8. 实现代码评审。
9. 开发分支发布测试环境，完成web／客户端联调
10. 合并开发分支到主干准备发布
11. 确认db表结构是否有修改，提前升级正式环境db
12. 发布代码到正式环境 jeckins gitlab ansible 
13. 确认是否需要清理cache redis-cli del * 
14. 正式环境功能确认 功能／log／cpu／mem／jvm

## 开发分支合并到主干操作流程

#### 开发分支rebase合并主干代码
1. 确认当前的workspace是自己的开发分支, 并拉取最新的代码
2. 在开发分支上 rebase 远程主干(remote master)，要勾选eclipse弹出框里的`Rebase interactively`
3. 右键项目 Rebase > continue. 如果报错 Rebase > skip commit and continue
4. 如果发现冲突(Conflict), 解决完冲突内容后，添加到git索引中 (Add Index). 继续步骤3
5. Rebase完成后，在eclipse中选择 Push Branch 'xxx(开发分支名)'，记得勾选`Force overwrite branch ...`

#### 开发分支多个debug提交合并为一个提交
1. 确认当前的workspace是自己的开发分支, 并拉取最新的代码,并且保证本地所有修改都已经commit
2. 查看工程的history，确认要合并提交的开始位置后，右键`Reset Soft`
3. 提交(commit)当前工程所有修改，添加好合并后的注释。但不要选择Push到远程库
4. 在eclipse中选择 Push Branch 'xxx(开发分支名)'，记得勾选`Force overwrite branch ...`

#### 主干代码合并开发分支
1. 切换到主干分支(master), 并确认是最新的代码
2. 在主干分支(master) Merge 开发分支内容
3. 如果发生冲突，请重复上面的步骤(开发分支rebase合并主干代码)

## 本机PC进行web测试
1. 切换当前目录到`weizhu_server`工程，执行`mvn install`将maven模块安装到本地库中
2. 在eclipse中找到weizhu-all-server, 执行`src/test/java/com.weizhu.server.all.test.Main.java`java类
3. 切换当前目录到`weizhu_server/webapp/xxx`，执行`mvn jetty:run`
4. 在浏览器中打开`http://127.0.0.1:8080`，即可测试。
5. 管理后台`weizhu-admin-webapp`的测试账号为`root@weizhu.com`/`123abcABC`
6. 在`weizhu-mobile-webapp`中，因为需要身份验证，可以访问`http://127.0.0.1:8080/test/test_login.html`创建测试身份，方便测试接口
