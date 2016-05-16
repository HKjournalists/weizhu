#  ScheduelExecutorService 中4个scheduel接口的用法。
## `schedule(Callable<Integer> callable, long delay, TimeUnit unit)`和`schedule(Runnable command, long delay, TimeUnit unit)`的用法。
他们两个接口的使用方法基本一致，只有参数有区别：一个是线程，另一个是回调方法，回调会多出一个返回结果。

每个参数的含义:
* 1.新初始化个线程或者回调方法。
* 2.等待时间。
* 3.时间单位。

两个函数比较简单直接示例：

参数是线程的：
```
System.out.println("print start..., now : " + sdf.format(new Date(System.currentTimeMillis())));
executor.schedule(new Print(), 1000 * 2, TimeUnit.MILLISECONDS);
```
返回结果：
```
print start..., now : 2015-12-14 20:09:25
2015-12-14 20:09:27
```
参数是回调函数的：
```
System.out.println("print start..., now : " + sdf.format(new Date(System.currentTimeMillis())));
Callable<Integer> call = new PrintCall();
Future<Integer> future = executor.schedule(call, 1000 * 2, TimeUnit.MILLISECONDS);
System.out.println(future.get());
```
返回结果是：
``` 
print start..., now : 2015-12-14 20:11:52
return 
100
```

这个函数没什么说的执行一次之后挂起。

## `scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)`的用法。

参数：
* 1.待执行的线程。
* 2.等待多长时间后执行。
* 3.每隔多长时间执行一次。
* 4.时间单位。

示例：
```
System.out.println("print start... ,now : " + sdf.format(new Date(System.currentTimeMillis())));
executor.scheduleAtFixedRate(new Print(), 1000 * 2, 1000 * 2, TimeUnit.MILLISECONDS);
```
返回结果：
```
print start... ,now : 2015-12-14 20:17:05
2015-12-14 20:17:07
2015-12-14 20:17:09
```
从返回结果可以看到每隔两秒执行一次输出，但是如果执行的方法比间隔时间长了什么效果，示例：
```
System.out.println("print start... ,now : " + sdf.format(new Date(System.currentTimeMillis())));
executor.scheduleAtFixedRate(new PrintAfter5sec(), 1000 * 2, 1000, TimeUnit.MILLISECONDS);
```

线程每隔5秒执行一次，返回结果：
```
print start... ,now : 2015-12-14 20:21:46
2015-12-14 20:21:53
2015-12-14 20:21:58
2015-12-14 20:22:03
```
也是每隔5秒，此时定义的每隔2秒就没效果了，如果第一个实行失败了还会往下执行么,示例：
```
System.out.println("print start... , now : " + sdf.format(new Date(System.currentTimeMillis())));
executor.scheduleAtFixedRate(new PrintException(), 1000 * 2, 1000, TimeUnit.MILLISECONDS);
```
返回结果：
```
print start... , now : 2015-12-14 20:24:23
2015-12-14 20:24:25
```
我等了1分钟它没有在打印，此时的线程已经挂起了。

## `scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)`的用法。

参数：
* 1.待执行的线程。
* 2.等待多长时间后执行。
* 3.每隔多长时间执行一次。
* 4.时间单位。

这个函数意思是必须等够要等的时间才执行线程，示例：
```
System.out.println("print start... , now : " + sdf.format(new Date(System.currentTimeMillis())));
executor.scheduleWithFixedDelay(new PrintAfter5sec(), 1000 * 2, 1000, TimeUnit.MILLISECONDS);
```
返回结果：
```
print start... , now : 2015-12-14 20:28:13
2015-12-14 20:28:20
2015-12-14 20:28:26
2015-12-14 20:28:32
```
可以看出返回结果每隔6秒，就是线程的5秒加上每隔1秒的和，如果遇到execption也是挂起。

下面是我上面用到的线程类，和回调方法类
```
class Print implements Runnable {

	@Override
	public void run() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long now = System.currentTimeMillis();
		System.out.println(sdf.format(new Date(now)));
	}
	
}

class PrintCall implements Callable<Integer> {

	@Override
	public Integer call() throws Exception {
		System.out.println("return ");
		int a = 100;
		return a;
	}
	
}

class PrintAfter5sec implements Runnable {

	@Override
	public void run() {
		try {
			Thread.sleep(1000 * 5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long now = System.currentTimeMillis();
		System.out.println(sdf.format(new Date(now)));
		
	}
	
}

class PrintException implements Runnable {

	@Override
	public void run() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long now = System.currentTimeMillis();
		System.out.println(sdf.format(new Date(now)));
		throw new RuntimeException("occur exception");
	}
	
}
```