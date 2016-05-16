
## 账号管理
---
#### 创建用户
* PATH: /api/user/create_user.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
raw_id | string | 员工id | 
user_name | string | 用户名称 | 
gender | string | 用户性别 | 'MALE' 男 'FEMALE' 女
mobile_no | string | 用户手机号 | 可多值
phone_no | string | 用户座机号 | 可多值 
email | string | 电子邮箱 | 
level_id | number | 职级id | 
team_id | number | 部门id | 
position_id | number | 职位id | 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 删除成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_RAW_ID_INVALID* 员工Id错误<br/> *FAIL_NAME_INVALID* 员工姓名错误<br/> *FAIL_MOBILE_NO_INVALID* 员工手机号错误<br/> *FAIL_LEVEL_INVALID* 员工直接错误<br/>*FAIL_POSITION_INVALID* 职位错误<br/>
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 
user_id | number | 创建后用户id |

---
#### 更新用户
* PATH: /api/user/update_user.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
user_id | number | 用户id | 
user_name | string | 用户名称 | 
gender | string | 用户性别 | 'MALE' 男 'FEMALE' 女
mobile_no | string | 用户手机号 | 可多值
phone_no | string | 用户座机号 | 可多值
email | string | 邮箱 | 
is_expert | boolean | 是否是专家 | 
level_id | number | 职级id | 
team_id | number | 部门id | 
position_id | number | 职位id | 
state | string | 用户状态 | *NORMAL* 正常状态<br/> *DISABLE* 停用<br/>

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 删除成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_NAME_INVALID* 员工姓名错误<br/> *FAIL_MOBILE_NO_INVALID* 员工手机号错误<br/> *FAIL_LEVEL_INVALID* 员工职级错误<br/> *FAIL_POSITION_INVALID* 职位错误<br/> *FAIL_USER_NOT_EXIST* 用户不存在<br/>
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 删除用户
* PATH: /api/user/delete_user.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
user_id | number | 用户id | 可多值

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 删除成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 获取用户列表
* PATH: /api/user/get_user_list.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
draw | number | 校验参数,无意义, 参见datatables | 
start | number | 获取列表起始位置 | 
length | number | 获取列表⻓长度 | 取值必须⼩小于等于50 
is_expert | boolean | 是否为专家 | 可为空
team_id | number | 部门id | 可为空
position_id | number | 职位id | 可为空 
keyword | string | 过滤名称关键词 | 可为空  
mobile_no | string | 过滤手机号关键词 | 可为空 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
draw | number | 校验参数,无意义, 参见datatables | 
recordsTotal | number | 总记录数 | 
recordsFiltered | number |  关键词过滤后所剩总记录数 | 
data | User array | 用户信息 | 可为空，具体字段参见User JSON Object

* User JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
user_id | number | 用户id | 
raw_id | string | 原始人员id(员工id) | 
user_name | string | 用户姓名 长度为8字,非空 | 
gender | string | 性别 | 'MALE' 男 'FEMALE' 女, 可为空 
mobile_no | string array | 手机号 | 
phone_no | string array | 座机号 | 
email | string | 电子邮箱 | 可为空  
is_expert | bool | 是否为专家 | 可为空  
level_id | number | 职级id | 可为空   
level | Level obj | 职级对象 | 可为空 
state | string | 用户状态 | 'NORMAL' 正常 'DISABLE' 停用 
team_id | number | 部门id | 可为空 
team | Team obj array | 部门路径，包含从根部门到当前部门信息的对象数组 | 可为空 
position_id | number | 职位id | 可为空 
position | Position obj | 职位对象 | 可为空
state | string | 用户状态 | *NORMAL* 正常状态<br/> *DISABLE* 停用<br/>

* Team JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
team_id | number | 部门id | 
team_name | string | 部门名称 | 
parent_team_id | number | 父部门id | 可为空 

* Position JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
position_id | number | 职位id | 
position_name | string | 职位名称 | 
position_desc | string | 职位描述 | 

* Level JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
level_id | number | 职级id | 
level_name | string | 职级名称 | 


---
#### 根据id获取用户
* PATH: /api/user/get_user_by_id.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
user_id | number | 用户id | 可多值

* Response User array JSON Object, 具体字段参见User JSON Object

---
#### 设置专家
* PATH: /api/user/set_expert.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
user_id | number | 用户id | 
is_expert | bool | 是否为专家 |  

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 设置成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_USER_NOT_EXIST* 用户不存在<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 设置用户状态
* PATH: /api/user/set_state.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
user_id | number | 用户id | 
state | string | 用户状态 | *NORMAL* 正常状态<br/> *DISABLE* 停用<br/>

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 设置成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_USER_NOT_EXIST* 用户不存在<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 创建职位
* PATH: /api/user/create_position.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
position_name | string | 职位名称 | 
position_desc | string | 职位描述 |  

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 设置成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_NAME_INVALID* 职位名称<br/> *FAIL_DESC_INVALID* 职位描述错误<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 
position_id | number | 添加成功后的职位id | 

---
#### 更新职位
* PATH: /api/user/update_position.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
position_id | number | 职位id |  
position_name | string | 职位名称 | 
position_desc | string | 职位描述 |  

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 设置成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_NAME_INVALID* 职位名称<br/> *FAIL_DESC_INVALID* 职位描述错误<br/> *FAIL_POSITION_NOT_EXIST* 职位不存在<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 获取职位
* PATH: /api/user/get_position.json
* Request: 空

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
position | Position array | 职位object 数组 | 

* Position JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
position_id | number | 职位id | 
position_name | string | 职位名称 | 
position_desc | string | 职位描述 | 

---
#### 创建职级
* PATH: /api/user/create_level.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
level_name | string | 职级名称 |  

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 设置成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_NAME_INVALID* 职级名称<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 
level_id | number | 添加成功后的职级id | 

---
#### 更新职级
* PATH: /api/user/update_level.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
level_id | number | 职位id |  
level_name | string | 职级名称 | 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 设置成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_NAME_INVALID* 职级名称<br/> *FAIL_LEVEL_NOT_EXIST* 职级不存在<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 删除职级
* PATH: /api/user/delete_level.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
level_id | number array | 职级位id | 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 设置成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/>
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 获取职级
* PATH: /api/user/get_level.json
* Request: 空

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
level | Level array | 职级object 数组 | 

* Level JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
level_id | number | 职级id | 
level_name | string | 职级名称 | 

---

#### 创建部门
* PATH: /api/user/create_team.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
team_name | string | 部门名称 | 
parent_team_id | number | 父部门id | 为空表示创建根部门 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 设置成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_NAME_INVALID* 部门名称错误<br/> *FAIL_PARAENT_INVALID* 父部门错误 <br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 
team_id | number | 添加成功后的职级id | 

---
#### 更新部门
* PATH: /api/user/update_team.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
team_id | number | 职位id |  
team_name | string | 职级名称 |

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 设置成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_NAME_INVALID* 部门名称<br/> *FAIL_TEAM_NOT_EXIST* 部门不存在<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 删除部门
* PATH: /api/user/delete_team.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
team_id | number array | 部门id | 
recursive | bool | 是否递归循环删除子部门 | 默认为false 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 设置成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_HAS_SUB_TEAM* 部门下还有子部门删除失败<br/>
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 获取子部门
* PATH: /api/user/get_team.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
team_id | number | 部门id | 当前部门id，为空表示获取根一级部门 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
team | Team array | 子部门object 数组 | 

* Team JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
team_id | number | 部门id | 
team_name | string | 部门名称 | 
parent_team_id | number | 父部门id | 可为空 
has_sub_team | bool | 是否有子部门 | 

---
#### 批量导入用户
* PATH: /api/user/import_user.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
import_user_file | file | 导入用户文件 | 必须为post multipart/form-data 请求，文件必须为 xls xlsx 格式

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 导入成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_USER_INVALID* 导入用户失败<br/>
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 获取导入错误日志
* PATH: /api/user/get_import_fail_log.download
* Request: 空
* Response: 需要下载的导入错误日志文件

---
#### 批量导出用户
* PATH: /api/user/export_user.download
* Request: 空
* Response: 需要下载的导出excel文件

---
#### 获取用户登录和在线会话信息
* PATH: /api/user/get_user_login_session.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
user_id | number | 用户id | 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
login | Login | 登录短信验证码信息 | 可能为空  
session | Session array | 在线会话信息 | 数组，可多值 

* Login JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
code | number | 验证码 | 
create_time | number | 验证码发送时间 | 
mobile_no | string | 手机号 | 可为空 
is_expired | bool | 是否验证码已过期 | 

* Session JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
session_id | string | 会话id | 不要显示给用户 
login_time | number | 登录时间 | 
active_time | number | 最近活动时间 | 
weizhu_platform | string | 微助app平台 | 
weizhu_version_name | string | 微助app版本名 | 
weizhu_stage | string | 微助app版本阶段 | 
weizhu_build_time | number | 微助app 构建时间 | 
device_info | string | 设备相关信息 | 

---
#### 删除用户在线会话信息(踢用户下线)
* PATH: /api/user/delete_user_session.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
user_id | number | 用户id | 
session_id | string | 会话id | 可多值，用','分割

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 删除成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---