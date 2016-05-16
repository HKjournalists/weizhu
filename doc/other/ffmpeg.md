# ffmpeg 使用

查看视频格式
```
ffprobe -v quiet -print_format json -show_format 5614989011.mp4 
```

视频截图
```
ffmpeg -ss 0 -i 5614989011.mp4 -vframes 1 -f image2 -y 5614989011.jpg
```
-ss       截图开始时间  
-i        输入文件  
-vframes  要截的帧数  
-f        输出格式  
-y        覆盖输出文件  

视频转换
```
ffmpeg -i out.ogv -vcodec mpeg4 out.mp4
```

java 代码
```java
Process processDuration = new ProcessBuilder("ffmpeg", "-i", absolutePath).redirectErrorStream(true).start();
StringBuilder strBuild = new StringBuilder();
try (BufferedReader processOutputReader = new BufferedReader(new InputStreamReader(processDuration.getInputStream(), Charset.defaultCharset()));) {
    String line;
    while ((line = processOutputReader.readLine()) != null) {
        strBuild.append(line + System.lineSeparator());
    }
    processDuration.waitFor();
}
String outputJson = strBuild.toString().trim();
```