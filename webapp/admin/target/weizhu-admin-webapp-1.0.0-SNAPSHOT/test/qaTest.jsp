<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>

<a href="../api/qa/get_category.json">获取分类列表</a><br/>
<a href="../api/qa/add_category.json?category_name=其他">添加分类</a><br/>
<a href="../api/qa/delete_category.json?category_id=9">删除分类</a><br/>
<a href="../api/qa/update_category.json?category_id=4&category_name=其他">更新分类</a><br/>


<a href="../api/qa/get_question.json?length=100">获取问题列表</a><br/>
<a href="../api/qa/add_question.json?question_content=接口测试问题&category_id=1">新增问题</a><br/>
<a href="../api/qa/delete_question.json?question_id=21">删除问题</a><br/>
<a href="../api/qa/export_question.download">导出问题列表</a><br/>
<a href="../api/qa/get_question.json?length=100&keyword=1">搜索问题</a><br/>


<a href="../api/qa/get_answer.json?length=100&question_id=1">获取回答列表</a><br/>
<a href="../api/qa/add_answer.json?question_id=1&answer_content=接口测试回答">新增回答</a><br/>
<a href="../api/qa/delete_answer.json?answer_id=40">删除回答</a><br/>



上传问题列表文件程序应用示例
<form action="../api/qa/import_question.json?category_id=1" method="post" enctype="multipart/form-data">
<%-- 类型enctype用multipart/form-data，这样可以把文件中的数据作为流式数据上传，不管是什么文件类型，均可上传。--%>
请选择要上传的文件<input type="file" name="import_qa_question_file" size="50">
<input type="submit" value="提交">
</form>
</body>
</html>