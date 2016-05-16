<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>

<a href="../api/discover/update_discover_banner_order.json?banner_order_str=1,2,3">更新banner顺序</a><br/>
<a href="../api/discover/update_discover_module_order.json?module_order_str=1,2,3">更新模块顺序</a><br/>
<br/>
<a href="../api/discover/create_discover_banner.json?banner_name=weizhu&image_name=weizhu.jpg&item_id=1">创建banner</a><br/>
<a href="../api/discover/update_discover_banner.json?banner_id=2&banner_name=weizhu&image_name=weizhu.jpg&item_id=1">更改banner</a><br/>
<a href="../api/discover/delete_discover_banner.json?banner_id=3">删除banner</a><br/>
<a href="../api/discover/disable_discover_banner.json?banner_id=3">作废banner</a><br/>
<a href="../api/discover/display_discover_banner.json?banner_id=3">显示banner</a><br/>
<a href="../api/discover/get_discover_banner.json">获取banner</a><br/>
<br/>
<a href="../api/discover/create_discover_module.json?module_name=kaoshi&image_name=kaoshi.jpg&item_id=2">创建模块</a><br/>
<a href="../api/discover/update_discover_module.json?module_id=2&module_name=kaoshi&image_name=kaoshi.jpg&item_id=2">更改模块</a><br/>
<a href="../api/discover/delete_discover_module.json?module_id=3">删除模块</a><br/>
<a href="../api/discover/disable_discover_module.json?module_id=3">作废模块</a><br/>
<a href="../api/discover/display_discover_module.json?module_id=3">显示模块</a><br/>
<a href="../api/discover/get_discover_module.json">获取模块</a><br/>
<br/>
<a href="../api/discover/create_discover_module_category.json?module_id=1&category_name=safe">创建分类</a><br/>
<a href="../api/discover/update_discover_module_category.json?module_id=1&category_id=3&category_name=safe">更改分类</a><br/>
<a href="../api/discover/delete_discover_module_category.json?category_id=3">删除分类</a><br/>
<a href="../api/discover/disable_discover_module_category.json?category_id=3">作废分类</a><br/>
<a href="../api/discover/display_discover_module_category.json?category_id=3">显示分类</a><br/>
<a href="../api/discover/update_discover_module_category_order.json?module_id=1&category_order_str=3,2,1">更新分类顺序</a><br/>
<a href="../api/discover/migrate_discover_module_category.json?module_id=2&category_id=3">迁移分类</a><br/>
<br/>

<a href="../api/discover/create_discover_item.json?category_id=2&item_name=itemName&item_desc=itemDesc&image_name=imageName">创建条目</a><br/>
<a href="../api/discover/update_discover_item.json?item_id=2&category_id=2&item_name=itemName&item_desc=itemDesc&image_name=imageName">更改条目</a><br/>
<a href="../api/discover/delete_discover_item.json?item_id=3">删除条目</a><br/>
<a href="../api/discover/disable_discover_item.json?item_id=3">作废条目</a><br/>
<a href="../api/discover/display_discover_item.json?item_id=3">显示条目</a><br/>
<a href="../api/discover/migrate_discover_item.json?category_id=2&item_id=3">迁移条目</a><br/>
<a href="../api/discover/get_discover_item.json?category_id=2&length=100">获取条目</a><br/>
<a href="../api/discover/delete_discover_item_from_category.json?category_id=2&item_id=3">从分类删除条目</a><br/>
<a href="../api/discover/export_discover_item.json">导出条目</a><br/>
<a href="../api/discover/get_discover_item_by_ids.json?item_id=1">根据条目id获取条目</a><br/>
<br/>
<a href="../api/discover/get_discover_comment.json?item_id=1&length=100">获取评论</a><br/>

上传问题列表文件程序应用示例
<form action="../api/discover/import_discover_item.json" method="post" enctype="multipart/form-data">
<%-- 类型enctype用multipart/form-data，这样可以把文件中的数据作为流式数据上传，不管是什么文件类型，均可上传。--%>
请选择要上传的文件<input type="file" name="import_discover_item_file" size="50">
<input type="submit" value="提交">
</body>
</html>