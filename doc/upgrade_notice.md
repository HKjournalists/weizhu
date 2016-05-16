# 微助后台升级发布注意事项

## protobuf文件
1. proto原有字段不能更改。最主要为类型和tag数字。例如: `required int32 abc = 1;` 中，`int32`和`1`一定不能更改，否则会导致数据不兼容。
2. proto新增字段必须为`optional`/`repeated`
3. enum类型的字段必须为`optional`，并且带有默认值. 例如：`optional State state = 1 [ default = NORMAL];`.否则会导致enum字段扩展enum枚举值时数据不兼容。

## DB表结构
1. 原有表结构字段不可修改类型。同类类型可以适当扩容，例如 `VARCHAR(191)` -> `TEXT`, `INT` -> `BIGINT`
2. 表新增字段必须为`DEFAULT NULL`，对应的java代码做好对`NULL`值的判断
3. 提前编写表结构更改脚本

## 服务器配置
1. 新增配置需要事先加入到`server.conf`文件中

## 发布前准备
1. 根据上一次发布的git hash确定本次发布的更改内容
2. 确认protobuf是否有修改，是否满足平滑升级条件
3. 确认db表结构是否有修改。如有修改，编写db升级脚本并测试
4. 确认配置是否有新增。如有新增配置，提前加入到`weizhu_config`项目中对应的`server.conf`文件中
5. 根据修改内容制作本次发布升级的Todo list，并在测试环境上演习验证

