db\_create\_table.sql
===================

1. 建表需加上 AUTO_INCREMENT
2. 表名规范： 

    weizhu_exam_join_user -> weizhu_exam_exam_join_user
    weizhu_exam_join_team -> weizhu_exam_exam_join_team

3. weizhu_exam_user_answer 主键缺失

ExamDB 
======

1. getOpenExamList 中，获取列表翻页顺序不对， 未加上 join\_user  join\_team 字段
2. getClosedExamList问题 同getOpenExamList
3. getQuestionByExamID 需指定 获取到的question list顺序 
4. getExamByID 更改为批量获取


service 实现原则：
===============

1. 确定实体
2. 实体中不变易变分离（将实体中全局一样的字段 和 每个用户展示不同的字段区分开，为缓存做好准备）
3. 确定实体db操作
   getInfoIdList, getInfoById (整体读取info中的信息)
   insert/update/delete 操作 (最好是批量操作)
   其他快捷操作 
4. 确定实体缓存操作
     list 缓存
     get/set/del
5. service impl 中 db操作次数尽量少且尽量一块操作完