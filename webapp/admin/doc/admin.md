## 账号管理
---
#### 获取权限数据
* PATH: /api/get_permission_group.json
* Request: 空
* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
permission_group | PermissionGroup array | 权限分组Json Object | 

* PermissionGroup JSON Object:

PermissionGroup字段名 | 类型 | 说明 | 备注
-------------------- | --- | --- | ---- 
group_name | string | 权限分组名称 | 
permission | Permission array | 权限Json Object | 

* Permission JSON Object:

Permission字段名 | 类型 | 说明 | 备注
--------------- | --- | --- | ---- 
permission_id | number | 权限id | 
permission_name | string | 权限名称 | 

---
#### 管理员登陆
* PATH: /api/admin_login.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
admin_email | string | 管理员登陆邮箱 | 
admin_password | string | 管理员登陆密码 | 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 登陆结果 | *SUCC* 登陆成功<br/> *FAIL_EMAIL_INVALID* 登陆邮箱格式错误<br/> *FAIL_PASSWORD_INVALID* 登陆密码格式错误<br/> *FAIL_EMAIL_OR_PASSWORD_INVALID* 登陆邮箱或密码不正确<br/> *FAIL_ADMIN_DISABLE* 管理员被冻结<br/> *FAIL_ADMIN_FORCE_RESET_PASSWORD* 管理员需要重置密码后才能登陆
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 
session_key | string | 登陆会话key,用来标识用户登陆身份 | 
admin | Admin | 管理员信息 | 
first_login | bool | 是否是初次登陆 | 

* Admin JSON Object:

Admin 字段名 | 类型 | 说明 | 备注 
----------- | --- | --- | ---
admin_id | number | 管理员id | 
admin_name | string | 管理员名 | 
admin_email | string | 管理员电子邮箱 | 
is_enable | bool | 管理员启用状态 | 
force_reset_password | bool | 是否强制更新密码 | 
create_time | number | 管理员创建时间 | unix时间戳,单位:秒. 内置账号无此字段 
create_admin_id | number | 创建该管理员的管理员id | 内置账号无此字段
permission_id | number array | 管理员权限id | 

---
#### 管理员登出
* PATH: /api/admin_logout.json
* Request: 空
* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 登出结果 | *SUCC* 登出成功

---
#### 管理员重置密码
* PATH: /api/admin_reset_password.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
admin_email | string | 管理员登陆邮箱 | 
old_password | string | 管理员旧登陆密码 | 
new_password | string | 管理员新登陆密码 | 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 重置结果 | *SUCC* 重置成功<br/> *FAIL_EMAIL_INVALID* 登陆邮箱格式错误<br/> *FAIL_OLD_PASSWORD_INVALID* 旧登陆密码格式错误<br/>  *FAIL_NEW_PASSWORD_INVALID* 新登陆密码格式错误<br/> *FAIL_ADMIN_NOT_EXIST* 管理员不存在<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 管理员忘记密码
* PATH: /api/admin_forgot_password.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
admin_email | string | 管理员登陆邮箱 | 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 重置邮件发送成功<br/> *FAIL_EMAIL_INVALID* 管理员邮箱错误<br/> *FAIL_ADMIN_NOT_EXIST* 管理员不存在<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 管理员忘记密码重设
* PATH: /api/admin_forgot_password_reset.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
admin_email | string | 管理员登陆邮箱 | 
forgot_token | number | 重设验证码 | 
new_password | string | 管理员新登陆密码 | 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 重设密码成功<br/> *FAIL_EMAIL_INVALID* 管理员邮箱错误<br/> *FAIL_FORGOT_TOKEN_EXPIRE* 重设验证码过期或不正确<br/> *FAIL_NEW_PASSWORD_INVALID* 重设新密码不正确 <br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 创建管理员
* PATH: /api/create_admin.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
admin_name | string | 管理员名称 |
admin_email | string | 管理员登陆邮箱 | 
admin_password | string | 管理员登陆密码 |
is_enable | bool | 管理员是否启用 |
permission_id | number | 管理员权限id | 可多值

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 创建成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_NAME_INVALID* 管理员名称错误<br/> *FAIL_EMAIL_INVALID* 管理员登陆邮箱格式错误<br/> *FAIL_PASSWORD_INVALID* 管理员登陆密码格式不正确<br/> *FAIL_PERMISSION_EMPTY* 管理员权限为空<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 
admin_id | number | 创建成功的管理员id | 

---
#### 更新管理员
* PATH: /api/update_admin.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
admin_id | number | 管理员id |
admin_name | string | 管理员名称 |
admin_email | string | 管理员登陆邮箱 | 
is_enable | bool | 管理员是否启用 |
force_reset_password | bool | 是否强制更新密码 | 
permission_id | number | 管理员权限id | 可多值 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 更新成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_NAME_INVALID* 管理员名称错误<br/> *FAIL_EMAIL_INVALID* 管理员登陆邮箱格式错误<br/> *FAIL_ADMIN_NOT_EXIST* 管理员不存在<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 删除管理员
* PATH: /api/delete_admin.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
admin_id | number | 管理员id | 可多值

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 删除成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> *FAIL_DELETE_SELF* 管理员不能删除自己<br/>
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 批量更改管理员冻结状态
* PATH: /api/change_admin_enable.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
admin_id | number | 管理员id | 可多值
is_enable | bool | 管理员是否启用 | true 启用，false 冻结

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
result | string | 请求结果 | *SUCC* 更改成功<br/> *FAIL_PERMISSION_DENIED* 无此操作权限<br/> 
fail_text | string | 失败文本信息,可直接展示给用户.调用成功时,无此字段 | 

---
#### 根据id获取管理员信息
* PATH: /api/get_admin_by_id.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
admin_id | number | 管理员id | 可多值 

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
admin | Admin array | 管理员信息 | 可为空，具体字段参见Admin JSON Object

---
#### 获取管理员列表
* PATH: /api/get_admin_list.json
* Request: 

请求参数名 | 类型 | 说明 | 备注
-------- | --- | --- | ---- 
draw | number | 校验参数,无意义, 参见datatables |
start | number | 获取列表起始位置 | 
length | number | 获取列表⻓长度 | 取值必须⼩小于等于50 
name_keyword | string | 过滤名称关键词 | 可为空  

* Response JSON Object:

JSON字段名 | 类型 | 说明 | 备注
--------- | --- | --- | ---- 
draw | number | 校验参数,无意义, 参见datatables | 
recordsTotal | number | 总记录数 | 
recordsFiltered | number |  关键词过滤后所剩总记录数 | 
data | Admin array | 管理员信息 | 可为空，具体字段参见Admin JSON Object