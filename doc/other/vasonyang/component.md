# 微助小组件需求

## 简单描述

1. 服务名：ComponentService
2. proto文件：component.proto admin_component.proto
3. 一期分为三个小组件： 打分，投票，评论
4. 打分功能参考 发现中的打分功能(目前做成五颗星的打分)
5. 投票功能参考 调研中的投票功能
6. 评论功能参考 社区中的评论功能

## 实现目标
1. 方便使用id接入。后台服务／web管理／手机端
2. 管理后台功能：查看综合信息，详细信息，可以简单检索。报表excel导出
3. 前台的功能：参见参考


# 编码问题集锦 

1. 编码顺序：
  * proto 文件编写
  * proto文件生成java类
  * 编写service接口
  * 编写sql 文件，并在数据库中运行sql语句创建表
  * 创建service目录结构，创建pom.xml文件，使用命令行将创建的目录生成eclipse工程。 mkdir –p src/main/java src/main/resources src/test/java src/test/resources vi pom.xml  mvn eclipse:eclipse 
  * 编写service接口实现类
  * 编写Module类 ，在类中编写service和serciceimpl的provide类
  * 编写DB类
  * 编写cache类
  * 编写service接口实现类


2. 注意问题:
  * proto文件中 结构体内字段类型和字段名字之间一个空格（以类型名字最长的类型为准）;字段名和 = 号之间一个空格（以最长的字段名为准），字段名后注释符号和分号之间一个空格，注释内容和注释符号之间一个空格，明显区分的两种字段定义之间空一行，service方法的response结构体中非主要返回实体的实体字段名前加 ref_ ，proto文件中service方法一般是按ID查询的方法在前，然后是list查询，然后是创建和更新的方法。相应的request和response 结构体的顺序要与方法顺序对应。
  * sql文件中字段名称和字段类型之间两个空格，用关键字作为字段名的要在关键字外加``符号（tab键上的按钮），编码使用utf8mb4，sql文件中的字段名和proto文件中的字段名一致。proto中的int32对应sql中的int，int64对应bigint，proto中的string可以对应sql中的txt，表中增加一个公司id的字段。但是proto中不用
  * 编写service 接口类的时候接口方法中 返回值类型用ListenableFuture<GetScoreByIdResponse>，其中GetScoreByIdResponse是方法本身的response，参数中除了本身的request外还要加上RequestHead 类型的request如：(RequestHead head,GetScoreByIdRequest request)其中GetScoreByIdRequest就是方法本身的request，在方法上要加上@ResponseType(GetScoreByIdResponse.class)的注解，其中的GetScoreByIdResponse就是方法本身的response，如果是实现添加或更新的方法还要加上@WriteMethod的注解。
  * module 类就是用来绑定和定义provide方法的。在configure方法中将service的实现类都定义为Singleton，使用Multibinder<String>将创建表和删除表的两个sql文件绑定到相应的变量上。另外在configure方法外定义service的provide方法。（这里不太明白这两个方法是怎么定义的，为什么要这么定义呢？）
  
3.
