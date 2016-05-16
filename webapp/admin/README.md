# 微助管理后台webapp

## 项目说明

1. 项目使用`Guice Servlet`做servlet配置 [Guice Servlet文档](https://github.com/google/guice/wiki/Servlets)
2. `com.weizhu.webapp.admin.GuiceConfigListener`类用来初始化guice配置
3. `com.weizhu.webapp.admin.AdminServletModule`类用来配置servle和filter的访问路径
4. `com.weizhu.webapp.admin.AdminVerifySessionFilter`类用来校验管理员身份和权限
5. `com.weizhu.webapp.admin.PermissionConst`类用来放置权限相关的全局常量
6. 页面资源放置在`/src/main/webapp/`目录下
    * `/src/main/webapp/static`放置静态页面资源,例如`js/css/img/html`等。访问该目录下的文件不会校验登录身份和权限
    * 页面访问身份校验失败后，会redirect跳转到`login.html`
    * 页面访问权限校验失败后，会forward到`403.html`
    * 需要跳过身份/权限校验的页面访问，请到`com.weizhu.webapp.admin.AdminVerifySessionFilter`类中单独配置
7. api接口servlet类放置在`com.weizhu.webapp.admin.api`package下, 访问路径以`/api/`开头
    * 返回json数据的接口请以`.json`结尾，导出的下载文件以`.download`结尾
    * api接口身份校验失败后，会返回`401 UNAUTHORIZED`http状态码
    * api接口权限校验失败后，会返回`403 FORBIDDEN`http状态码
    * 需要跳过身份/权限校验的api接口访问，请到`com.weizhu.webapp.admin.AdminVerifySessionFilter`类中单独配置
8. 在页面中所有的访问路径请使用相对路径

## 管理员登录流程

```flow
st=>start: 开始
op_enter_page=>operation: 进入登录页面
op_login_admin=>operation: 输入管理员邮箱和密码登陆
cond_login_result=>condition: 是否登陆成功？
sub_retry_login=>subroutine: 重新登陆
cond_company_count=>condition: 可管理多个公司？
io_input_company=>inputoutput: 选择要管理的公司
op_get_admin_info=>operation: 获取该公司相关的角色/权限/配置
op_enter_home=>operation: 进入公司管理首页，根据权限展示左侧栏目树
e=>end: 结束

st->op_enter_page->op_login_admin->cond_login_result
cond_login_result(no, right)->sub_retry_login(right)->op_login_admin
cond_login_result(yes)->cond_company_count
cond_company_count(no)->op_get_admin_info
cond_company_count(yes, bottom)->io_input_company->op_get_admin_info->op_enter_home->e
```

## 身份&权限校验流程

```flow
```

## 新增页面接口说明

1. 确认是否需要登录后操作
2. 确认是否需要配置权限

## 管理员相关接口文档


## 参考

* PlantUML/Graphviz
* [flowchart.js 流程图](http://adrai.github.io/flowchart.js/)
* [SVG to PNG converter](http://runemadsen.com/svg-converter/)

## 运行该项目

1. 安装redis mysql [可以参考这里](README.md)
2. 在项目代码的目录下执行 mvn jetty:run
3. 默认访问地址: http://127.0.0.1:8080/