<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>

<a href="../api/community/get_community.json">获取社区信息</a><br/>
<a href="../api/community/set_community.json?community_name=微助社区">保存社区信息</a><br/>
<a href="../api/community/update_board_order.json?board_id_order_str=2,1,3">更改板块顺序</a><br/>


<a href="../api/community/get_board.json">获取版块列表</a><br/>
<a href="../api/community/create_board.json?board_name=测试版块&board_icon=测试icon&board_desc=测试描述">新增版块</a><br/>
<a href="../api/community/update_board.json?board_id=4&board_name=测试版块update&board_icon=测试iconupdate&board_desc=测试描述update">更新版块</a><br/>
<a href="../api/community/delete_board.json?board_id=3&is_force_delete=true">删除版块</a><br/>



<a href="../api/community/get_post.json?length=100">获取帖子列表</a><br/>
<a href="../api/community/migrate_post.json?board_id=3&post_id=110">迁移帖子</a><br/>
<a href="../api/community/delete_post.json?post_id=110">删除帖子</a><br/>
<a href="../api/community/export_post.json">导出帖子</a><br/>
<a href="../api/community/recommend_post.json?post_id=101&is_recommend=true">推荐帖子</a><br/>
<a href="../api/community/set_sticky_post.json?post_id=100&is_sticky=true">置顶帖子</a><br/>

<a href="../api/community/get_comment.json?post_id=100&length=100">获取评论列表</a><br/>
<a href="../api/community/delete_comment.json?post_id=100&comment_id=10">删除评论</a><br/>

</body>
</html>