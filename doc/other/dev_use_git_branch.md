# 使用git分支开发新功能

### 在EclipseIDE中

1. 创建开发分支并切换
    * 创建本地分支
        ```
        右键工程 -> Team -> switch to -> new branch
        ```
    * 推送到远程代码库
        ```
        右键工程 -> Team -> push branch 'XXX'
        ```
2. 在开发分支开发新功能并提交代码
3. 功能开发完毕后，开发分支所有更改作为一次提交合入主干
    * 显示历史提交记录
        ```
        右键工程 -> Team -> Show In History
        ```
    * 将代码soft reset到主干分支提交点上
        ```
        在History栏里右键包含master标签的commit -> Reset -> Soft
        ```
    * 切换到主干分支
        ```
        右键工程 -> Team -> switch to -> master
        ```
    * 提交代码并推送到远程代码库
4. 处理开发分支合入主干的冲突
    * 查看冲突内容
        ```
        右键工程 -> Team -> merge tool -> 查看冲突内容
        ```
    * 逐个处理冲突文件
        ```
        更改原来的文件 -> 右键 -> Team -> Add to Index
        ```
    * 提交代码并推送到远程代码库
5. 将开发分支重置为主干最新代码
    * 切换到开发分支
        ```
        右键工程 -> Team -> switch to -> `XXX`
        ```
    * 重置分支为主干最新代码
        ```
        右键工程 -> Team -> Reset -> 选定master,勾选Hard
        ```
    * 强制提交分支内容
        ```
        右键工程 -> Team -> push branch 'XXX' -> 勾选 Force overwrite branch in remote if its exists and has diverged -> Finish
        ```