<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>

<a href="../api/scene/update_scene_order.json?scene_id_order_str=1,2,3">更新场景顺序</a><br/>
<br/>
<a href="../api/scene/create_scene.json?scene_name=sceneName&image_name=imageName&scene_desc=sceneDesc&parent_scene_id=3">创建场景</a><br/>
<a href="../api/scene/update_scene.json?scene_id=4&scene_name=sceneName2&image_name=imageName2&scene_desc=sceneDesc2">更新场景</a><br/>
<a href="../api/scene/delete_scene.json?scene_id=4">删除场景</a><br/>
<a href="../api/scene/display_scene.json?scene_id=4">显示场景</a><br/>
<a href="../api/scene/disable_scene.json?scene_id=4">作废场景</a><br/>
<a href="../api/scene/get_scene.json">获取场景</a><br/>
<a href="../api/scene/update_scene_item_order.json?scene_id=2&item_id_order_str=1,2,3">更新场景中条目的显示顺序</a><br/>
<br/>
<a href="../api/scene/create_scene_item.json?createItemParameter={%22createItemParameter%22:[{%22scene_id%22:2&#44;%22discover_item_id%22:6}]}">创建场景条目</a><br/>
<a href="../api/scene/delete_scene_item.json?item_id=6">删除场景条目</a><br/>
<a href="../api/scene/disable_scene_item.json?item_id=6">作废场景条目</a><br/>
<a href="../api/scene/display_scene_item.json?item_id=6">显示场景条目</a><br/>
<a href="../api/scene/migrate_scene_item.json?item_id=6&scene_id=2">迁移场景条目</a><br/>
<a href="../api/scene/get_scene_item.json?scene_id=2&length=100">获取场景条目</a><br/>

<br/><br/><br/><br/>
<!-- 工具盖帽神器 -->
<a href="../api/scene/tool/recommender/get_recommender_home.json">获取盖帽神器home</a><br/>
<br/>
<a href="../api/scene/tool/recommender/create_recommender_category.json?category_name=categoryName&image_name=imageName&category_desc=categoryDesc&parent_category_id=3">创建分类</a><br/>
<a href="../api/scene/tool/recommender/update_recommender_category.json?category_name=categoryName1&image_name=imageName1&category_desc=categoryDesc1&parent_category_id=3">更新分类</a><br/>
<a href="../api/scene/tool/recommender/update_recommender_category_state.json?category_id=5&state=DISABLE">作废分类</a><br/>
<a href="../api/scene/tool/recommender/migrate_recommender_competitor_product.json?category_id=3&competitor_product_id=1">迁移分类</a><br/>
<br/>
<a href="../api/scene/tool/recommender/get_recommender_competitor_product.json?length=100">获取竞品</a><br/>
<a href="../api/scene/tool/recommender/create_recommender_competitor_product.json?competitor_product_name=competitorName&image_name=imageName&category_id=5">创建竞品</a><br/>
<a href="../api/scene/tool/recommender/update_recommender_competitor_product.json?competitor_product_id=11&competitor_product_name=competitorName1&image_name=imageName1&category_id=5">更改竞品</a><br/>
<a href="../api/scene/tool/recommender/update_recommender_competitor_product_state.json?competitor_product_id=1&state=DISABLE">作废竞品</a><br/>
<br/>
<a href="../api/scene/tool/recommender/get_recommender_recommend_product.json?length=100">获取推荐产品</a><br/>
<a href="../api/scene/tool/recommender/create_recommender_recommend_product.json?recommend_product_name=recommendName&image_name=imageName">创建推荐产品</a><br/>
<a href="../api/scene/tool/recommender/update_recommender_recommend_product.json?recommend_product_id=11&recommend_product_name=recommendName1&image_name=imageName1">更改推荐产品</a><br/>
<a href="../api/scene/tool/recommender/update_recommender_recommend_product_state.json?recommend_product_id=1&state=DISABLE">作废推荐产品</a><br/>
<a href="../api/scene/tool/recommender/add_recommend_prod_to_competitor_prod.json?recommend_product_id=1&competitor_product_id=1">给竞品增加推荐产品的引用</a><br/>
<a href="../api/scene/tool/recommender/delete_recommend_prod_from_competitor_prod.json?recommend_product_id=1&competitor_product_id=1">删除竞品对推荐产品的引用</a><br/>
<br/>
<a href="../api/scene/tool/recommender/get_recommender_recommend_product_price_web_url.json?recommend_product_id=1">获取价格web链接</a><br/>
<a href="../api/scene/tool/recommender/create_recommender_recommend_product_price_web_url.json">创建价格web链接</a><br/>
<a href="../api/scene/tool/recommender/update_recommender_recommend_product_price_web_url.json">修改价格web链接</a><br/>
<a href="../api/scene/tool/recommender/delete_recommender_recommend_product_price_web_url.json">删除价格web链接</a><br/>

</body>
</html>