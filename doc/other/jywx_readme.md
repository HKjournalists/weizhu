# 军用微信代码交付说明

1. weizhu_server 军用微信(微助)后台代码
2. weizhu_config 军用微信(微助)后台部署环境配置
3. weizhu_software 军用微信(微助)后台部署所需软件。构建／部署文档中涉及的软件都可以在这里找到
4. weizhu_android 军用微信(微助)android客户端代码
5. build_src.pdf 军用微信(微助)后台代码构建文档
6. deploy_single.pdf 军用微信(微助)后台单机部署文档
7. introduction.pdf 军用微信(微助)后台设计简介

# 代码交付说明

1. weizhu_server / weizhu_config / weizhu_android 三个项目都包含git信息。可使用git工具追溯每次交付的更改记录
2. 军用微信(微助)交付的所有技术文档均为markdown编写后使用Mou软件导出。可在代码库中找到对应的markdown原始文件，以便于版本管理。
3. 强烈建议搭建自己的gitlab服务。便于管理交付的代码和文档，并且markdown编写的文档可直接在gitlab中在线浏览，十分方便。

# 部署说明

1. 推荐PC使用virtualbox快速部署后台
2. 在PC中使用android虚拟机安装weizhu_software中的安装包`app-jy_weixin_local_test-release.apk`
3. 使用手机号 18600000000,验证码 666666 登录使用