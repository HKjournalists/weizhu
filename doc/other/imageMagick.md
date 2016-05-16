# imageMagick安装与应用
## 1.安装
* mac : 安装homebrew的情况下，执行：brew install imagemagick
* windows : http://www.imagemagick.org/script/binary-releases.php

## 2.应用

### 2.1 命令行
#### 常用命令
* 图片格式转换：convert rose.jpg rose.png
* 图片大小调整：convert rose.jpg -resize 50% rose.png
* 图片尺寸调整：convert dragon.gif    -resize 64x64  resize_dragon.gif （默认按比例调整）

    convert dragon.gif    -resize 64x64\!  exact_dragon.gif （强制按64x64进行转换）
    
    convert dragon.gif    -resize 64x64\>  shrink_dragon.gif （小图片不变）
* 获取图片信息：identify image.png 

	identify -format "%m\n%w\n%h\n%Q" image.png （只获取格式，宽，高，特性）
	
### 2.2 java调用
#### 2.2.1 Im4java (调用命令行接口)

* 官网：http://im4java.sourceforge.net/
* 应用实例：http://blog.csdn.net/cofesun/article/details/8131102 ；

	 http://www.programcreek.com/java-api-examples/index.php?api=org.im4java.core.IdentifyCmd
	 
	 http://blog.csdn.net/newborn2012/article/details/24964577
	 
* 简要使用步骤:

    第一步：确保命令行可用
 
    第二步：在pom.xml文件中添加依赖

```
		<dependency>
			<groupId>org.im4java</groupId>
			<artifactId>im4java</artifactId>
			<version>1.4.0</version>
		</dependency>
```

    第三步：编写调用代码

#### 2.2.2 JMagick (JNI调用)

* 官网：http://www.jmagick.org/
* 安装与应用：http://www.blogjava.net/void241/archive/2011/07/10/354032.html

#### PS : 使用－font命令时可能会保存，因为应用中确实字体包，需要下载
