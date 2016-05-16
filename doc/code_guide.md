# 微助后台编码指引

## 代码结构


## proto 协议相关约定

1. Message结构体命名采用驼峰命名。和Java类的命名一致
2. 字段命名采用"小写字母数字和下划线`_`"命名
3. Service和方法采用驼峰命名。参照官方建议的风格，方法命名首字母要大写
4. 方法命名采用 动词＋名词的命名方式，例如：`CreateUser`,`UpdateUser`,`DeleteUser`
5. 方法要有对应的 XXXRequest/XXXResponse Message结构体
6. 方法参数／返回值为空数据的话，可以使用 `weizhu.EmptyRequest`, `weizhu.EmptyResponse`
7. `CreateXXX`,`UpdateXXX`方法建议填入具体的数据，而不是使用对应的结构体
8. 写操作方法要有result字段，标记操作结果
9. **新扩展的字段必须为没有使用过的tag号，必须为optional/repeated**，否则协议会有兼容问题，导致老版本的客户端/服务端无法正常运行
10. Service要有对应的Java接口类，服务名方法名一致。方法名首字母要小写，这一点java代码和proto协议不一样。

## db 相关约定

1. db相关文件放置在对应服务的resource目录下。`db_create_table.sql`, `db_drop_table.sql`
2. 表命名需要加相应服务的前缀。例如发现服务中的表, 需要命名为 `weizhu_discover_xxx`
3. 字段命名采用"小写字母数字和下划线`_`"命名。和proto中相应的字段保持一致
4. 时间类型采用`INT`类型存储。保存时间的UNIX时间戳timestamp值，精确到秒即可。这样做可以减少不必要的校验过程和时区问题
5. 对于可为NULL值的字段，java代码读取时，一顶要判断是否为null。boolean/int/long 等java原生类型，可使用 wasNull()方法判断

## redis cache相关约定

1. redis key命名采用"小写字母数字和下划线`_`"命名
2. 注意**数据不存在** 这种状态的缓存

## Java 代码相关约定

1. 方法中可为Null的参数需要添加Annotatin `@Nullable`
2. 方法返回的List/Map/Set容器类型不能为Null。对于空的容器，可以使用Collections.emptyXXX() 对象返回
3. 方法返回的List/Map/Set容器类型为不可变类型，不能再继续放入/删除/删除其中的元素
4. List/Map/Set容器类型中不能放入Null元素
5. 根据key从Map中获取元素后，一定要判断是否为Null
6. Map的Key类型必须为id字段，不能直接使用protobuf结构体做key
7. 服务中的XXXDB 为db操作类，尽量只为操作db的方法，不要放置业务逻辑。这样可以增强封装性和代码复用
8. 服务中的XXXCache 为cache操作类，尽量只为操作cache的方法，不要放置业务逻辑。这样可以增强封装性和代码复用
9. 服务中的XXXImpl 为业务实现类，主要放置业务逻辑的具体实现。
10. 服务中的XXXModule 为业务依赖定义

## Config 相关约定

1. 配置项命名需要添加服务对应的前缀。例如发现服务中的配置，需要命名为 `discover_xxx=value`
2. 配置项命名采用"小写字母数字和下划线`_`"命名

## 新建Service流程

1. 为新的Service命名，建议使用一到两个英文单词。例如 Demo服务， DemoService
2. 在 weizhu_server/common/proto/src/main/proto/ 目录下建立 demo.proto 并编写协议，要有DemoService定义。
3. 在 weizhu_server/common/proto 工程里创建 com.weizhu.proto.DemoService 接口，参照其他服务接口编写。保证和demo.proto中定义的服务一致
4. 建立相应的service工程目录 weizhu_server/service/demo, 并创建pom.xml 和源代码目录 `src/main/java`,`src/main/resource`,`src/test/java`,`src/test/resource`
5. 将demo加入到 weizhu_server/service/pom.xml 中
6. 在weizhu_server根目录执行 `mvn eclipse:eclipse` 重新创建eclipse项目
7. 将 weizhu_server/service/demo 加入到eclipse工程中，开始编码。java代码和其他资源请放到 `com.weizhu.service.demo` 包下
8. 编写完业务逻辑代码后，创建单元测试
9. 将该服务加入到对应的 server pom.xml中，并在server的module中配置该服务的依赖关系

## 常用编码模式

1. 根据id批量获取数据
    * 创建对应的DB读取方法
    * 创建对应的Cache读取方法
    * 完成整个读取逻辑： 先从cache中获取，如果没有数据，再从db中获取
    * 注意：对**没有数据**这个状态的缓存
2. 管理后台获取数据列表
3. 客户端获取数据列表
4. 创建数据
5. 修改数据
6. 删除数据
7. 控制多个数据间的顺序
