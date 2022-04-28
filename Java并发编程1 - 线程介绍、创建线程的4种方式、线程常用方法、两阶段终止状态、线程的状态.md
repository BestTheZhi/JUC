## 一、线程与进程、并行并发、同步异步概念

### 1、进程与线程

**进程 (Process)**：是计算机中的程序关于某数据集合上的一次运行活动，是系统进行资源分配和调度的基本单位，是操作系统结构的基础。 在当代面向线程设计的计算机结构中，进程是线程的容器,程序是指令、数据及其组织形式的描述，进程是程序的实体。

**线程 (Thread)**: 进程可进一步细化为线程，是操作系统能够进行运算调度的最小单位。它被包含在进程之中，是进程中的实际运作单位。一条线程指的是进程中一个单一顺序的控制流，一个进程中可以并发多个线程，每条线程并行执行不同的任务。

简单来说，进程作为资源分配的基本单位，线程作为资源调度的基本单位。



### 2、 并行与并发

**并行:** 多核CPU**同时**执行多个任务。

**并发:** 单核CPU在一段时间内交替执行多个任务

>并发（concurrent）: 是同一时间应对（dealing with）多件事情的能力
>
>并行（parallel）: 是同一时间动手做（doing）多件事情的能力



### 3、同步和异步

以调用方的角度讲

- 如果需要等待结果返回才能继续运行的话就是同步
- 如果不需要等待就是异步



1 设计

- 多线程可以让方法执行变为异步的（即不要巴巴干等着）比如说读取磁盘文件时，假设读取操作花费了 5 秒钟，如果没有线程调度机制，这5秒cpu什么都做不了，其它代码都得暂停

2 结论

- 比如在项目中，视频文件需要转换格式等操作比较费时，这时开一个新线程处理视频转换，避免阻塞主线程
- tomcat 的异步 servlet 也是类似的目的，让用户线程处理耗时较长的操作，避免阻塞 tomcat 的工作线程
- UI 程序中，开线程进行其他操作，避免阻塞 UI 线程



## 二、线程的创建

### 1、通过继承Thread创建线程

```java
public class CreateThread {
	public static void main(String[] args) {
		Thread myThread = new MyThread();
        // 启动线程
		myThread.start();
	}
}

class MyThread extends Thread {
	@Override
	public void run() {
		System.out.println("my thread running...");
	}
}
```

使用继承方式的好处是，在run()方法内获取当前线程直接使用this就可以了，无须使用Thread.currentThread()方法；不好的地方是会有单继承的局限: 就是不能多继承 还有会使两个类的耦合性增加,如果父类有改动时会直接影响子类。另外任务与代码没有分离，当多个线程执行一样的任务时需要多份任务代码。



### 2、实现Runnable接口

```java
public class Test2 {
	public static void main(String[] args) {
		//创建线程任务
		Runnable r = new Runnable() {
			@Override
			public void run() {
				System.out.println("Runnable running");
			}
		};
		//将Runnable对象传给Thread
		Thread t = new Thread(r);
		//启动线程
		t.start();
	}
}
```

使用lambda表达式简化操作

```java
public class Test2 {
	public static void main(String[] args) {
		//创建线程任务
		Runnable r = () -> {
            //直接写方法体即可
			System.out.println("Runnable running");
			System.out.println("Hello Thread");
		};
		//将Runnable对象传给Thread
		Thread t = new Thread(r);
		//启动线程
		t.start();
	}
}
```

**小结**

- 继承Thread方式: 是把线程和任务合并在了一起
- 实现Runnable方式: 是把线程和任务分开了
- **用 Runnable 更容易与线程池等高级 API 配合 用 Runnable 让任务类脱离了 Thread 继承体系，更灵活**



另外，实现Runnable方式来开启一个线程，使用的是**代理模式**，Thread类是代理类

```java
public class Thread implements Runnable {
    
    /* What will be run. */
    private Runnable target;

    //constructor
    private Thread(...){
      ...
      this.target = target;
      ...
    }
    
    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }
    
}
```



### 3、使用FutureTask与Thread结合

**使用FutureTask可以用泛型指定线程的返回值类型（Runnable的run方法没有返回值）**

```java
public class Test3 {
	public static void main(String[] args) throws ExecutionException, InterruptedException {
        //需要传入一个Callable对象
		FutureTask<Integer> task = new FutureTask<Integer>(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				System.out.println("线程执行!");
				Thread.sleep(1000);
				return 100;
			}
		});

		Thread r1 = new Thread(task, "t2");
		r1.start();
		//获取线程中方法执行后的返回结果
		System.out.println(task.get());
	}
}
```



### 4、使用线程池来创建线程

```java
/**
 * 创建线程的方式四：使用线程池
 *
 * 好处：
 * 1.提高响应速度（减少了创建新线程的时间）
 * 2.降低资源消耗（重复利用线程池中线程，不需要每次都创建）
 * 3.便于线程管理
 *      corePoolSize：核心池的大小
 *      maximumPoolSize：最大线程数
 *      keepAliveTime：线程没有任务时最多保持多长时间后会终止
 *
 *
 * 面试题：创建多线程有几种方式？四种！
 */

class NumberThread implements Runnable{

    @Override
    public void run() {
        for(int i = 0;i <= 100;i++){
            if(i % 2 == 0){
                System.out.println(Thread.currentThread().getName() + ": " + i);
            }
        }
    }
}

class NumberThread1 implements Runnable{

    @Override
    public void run() {
        for(int i = 0;i <= 100;i++){
            if(i % 2 != 0){
                System.out.println(Thread.currentThread().getName() + ": " + i);
            }
        }
    }
}

public class ThreadPool {

    public static void main(String[] args) {
        //1. 提供指定线程数量的线程池
        ExecutorService service = Executors.newFixedThreadPool(10);
        ThreadPoolExecutor service1 = (ThreadPoolExecutor) service;
        //设置线程池的属性
//        System.out.println(service.getClass());
//        service1.setCorePoolSize(15);
//        service1.setKeepAliveTime();


        //2.执行指定的线程的操作。需要提供实现Runnable接口或Callable接口实现类的对象
        service.execute(new NumberThread());//适合适用于Runnable
        service.execute(new NumberThread1());//适合适用于Runnable

//        service.submit(Callable callable);//适合使用于Callable
        //3.关闭连接池
        service.shutdown();
    }
}

```



5、查看进程和线程的方法

![WM-Screenshots-20220426161152](https://cdn.jsdelivr.net/gh/bestthezhi/images@master/juc/WM-Screenshots-20220426161152.png)



## 三、线程运行原理

### 1、虚拟机栈与栈帧

虚拟机栈描述的是Java方法执行的内存模型：每个方法被执行的时候都会同时创建一个栈帧(stack frame)用于存储局部变量表、操作数栈、动态链接、方法出口等信息，是属于线程的私有的。当Java中使用多线程时，每个线程都会有它自己的虚拟机栈，程序计数器，本地方法栈。线程运行时，每个线程只能有一个活动栈帧(在栈顶)，对应着当前正在执行的那个方法。



### 2、线程上下文切换（Thread Context Switch)

因为以下一些原因导致 cpu 不再执行当前的线程，转而执行另一个线程

- 线程的 cpu 时间片用完(每个线程轮流执行，看前面并行的概念)
- 垃圾回收
- 有更高优先级的线程需要运行
- 线程自己调用了 sleep、yield、wait、join、park、synchronized、lock 等方法

当Thread Context Switch发生时，需要由操作系统保存当前线程的状态，并恢复另一个线程的状态，Java 中对应的概念就是程序计数器（Program Counter Register），它的作用是记住下一条 jvm 指令的执行地址，是线程私有的。

- 线程的状态包括程序计数器、虚拟机栈中每个栈帧的信息，如局部变量、操作数栈、返回地址等
- Context Switch 频繁发生会影响性能



### 3、Thread的常见方法

![20201218104837363](https://cdn.jsdelivr.net/gh/bestthezhi/images@master/juc/20201218104837363.png)![20201218104857415](https://cdn.jsdelivr.net/gh/bestthezhi/images@master/juc/20201218104857415.png)



#### 3.1、调用start 与 run方法的区别

1、调用start()方法

```java
public static void main(String[] args) {
    Thread thread = new Thread(){
      @Override
      public void run(){
          log.debug("我是一个新建的线程正在运行中");
          FileReader.read(fileName);
      }
    };
    thread.setName("新建线程");
    thread.start();
    log.debug("主线程");
}
```

输出：程序在t1 线程运行， run()方法里面内容的调用是异步的代码

```sout
11:59:40.711 [main] DEBUG com.concurrent.test.Test4 - 主线程
11:59:40.711 [新建线程] DEBUG com.concurrent.test.Test4 - 我是一个新建的线程正在运行中
11:59:40.732 [新建线程] DEBUG com.concurrent.test.FileReader - read [test] start ...
11:59:40.735 [新建线程] DEBUG com.concurrent.test.FileReader - read [test] end ... cost: 3 ms
```



2、调用run()方法

将上面代码的thread.start();改为 thread.run();输出结果如下：程序仍在 main 线程运行， run()方法里面内容的调用还是同步的

```sout
12:03:46.711 [main] DEBUG com.concurrent.test.Test4 - 我是一个新建的线程正在运行中
12:03:46.727 [main] DEBUG com.concurrent.test.FileReader - read [test] start ...
12:03:46.729 [main] DEBUG com.concurrent.test.FileReader - read [test] end ... cost: 2 ms
12:03:46.730 [main] DEBUG com.concurrent.test.Test4 - 主线程
```



小结

- 直接调用 run() 是在主线程中执行了 run()，没有启动新的线程
- 使用 start() 是启动新的线程，通过新的线程间接执行 run()方法中的代码



#### 3.2、 sleep 与 yield

sleep方法

- 调用 sleep() 会让当前线程从 Running(运行状态) 进入 Timed Waiting 状态（阻塞）
- 其它线程可以使用interrupt 方法打断正在睡眠的线程，那么被打断的线程这时就会抛出 InterruptedException异常【注意：这里打断的是正在休眠的线程，而不是其它状态的线程】
- 睡眠结束后的线程未必会立刻得到执行 (需要分配到cpu时间片)
- 建议用 TimeUnit 的 sleep() 代替 Thread 的 sleep()来获得更好的可读性

yield方法

- 释放当前线程的执行权，调用 yield 会让当前线程从Running 进入 Runnable 就绪状态，然后调度执行其它线程
- 具体的执行依赖于操作系统的任务调度器(有可能还是选择执行此线程)



#### 3.3、线程优先级

线程优先级会提示（hint）调度器优先调度该线程，但它仅仅是一个提示，调度器可以忽略它, 如果 cpu 比较忙，那么优先级高的线程会获得更多的时间片，但 cpu 闲时，优先级几乎没作用

```java
thread1.setPriority(Thread.MAX_PRIORITY); //设置为优先级最高
```



#### 3.4、 join方法

在主线程中调用 `t1.join`，则主线程会等待t1线程执行完之后再继续执行

```java
private static void test1() throws InterruptedException {
    log.debug("开始");
    Thread t1 = new Thread(() -> {
        log.debug("开始");
        sleep(1);
        log.debug("结束");
        r = 10;
    },"t1");
    t1.start();
    // t1.join(); 
    // 这里如果不加t1.join(), 此时主线程不会等待t1线程给r赋值, 主线程直接就输出r=0结束了
    // 如果加上t1.join(), 此时主线程会等待到t1线程执行完才会继续执行.(同步), 此时r=10;
    log.debug("结果为:{}", r);
    log.debug("结束");
}
```



#### 3.5、interrupt 方法详解

该方法用于打断 sleep，wait，join的线程, 在阻塞期间cpu不会分配给时间片

- 如果一个线程在在运行中被打断，打断标记会被置为true
- 如果是打断因sleep wait join方法而被阻塞的线程，会抛出`InterruptedException`异常，然后会将打断标记置为false

sleep，wait，join的线程，这几个方法都会让线程进入阻塞状态，以 sleep 为例

```java
public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            System.out.println("sleep...");
            try {
                Thread.sleep(5000); // wait, join
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t1.start();
        Thread.sleep(1000);
        System.out.println("iterrupt..");
        t1.interrupt();
        System.out.println(t1.isInterrupted()); // 如果是打断sleep,wait,join的线程, 即使打断了, 标记也为false
    }
}
```

```sout
sleep...
iterrupt..
打断标记为:false
java.lang.InterruptedException: sleep interrupted
	at java.lang.Thread.sleep(Native Method)
	at com.guizy.ThreadPrintDemo.lambda$main$0(ThreadPrintDemo.java:14)
	at java.lang.Thread.run(Thread.java:748)
```



打断正常运行的线程,线程并不会暂停，只是调用方法`Thread.currentThread().isInterrupted();`的返回值为true，可以判断其值来手动停止线程：

```java
public static void main(String[] args) throws InterruptedException {
    Thread t1 = new Thread(() -> {
        while(true) {
            boolean interrupted = Thread.currentThread().isInterrupted();
            if(interrupted) {
                System.out.println("被打断了, 退出循环");
                break;
            }
        }
    }, "t1");
    t1.start();
    Thread.sleep(1000);
    System.out.println("interrupt");
    t1.interrupt();
    System.out.println("打断标记为: "+t1.isInterrupted());
}
```

```sout
interrupt
被打断了, 退出循环
打断标记为: true
```



#### 3.6、 终止模式之两阶段终止模式

当我们在执行线程一时，想要终止线程二，这是就需要使用interrupt方法来优雅的停止线程二。

Two Phase Termination，就是考虑在一个线程T1中如何优雅地终止另一个线程T2？这里的优雅指的是给T2线程一个处理其他事情的机会（如释放锁）。

![20201218122110944](https://cdn.jsdelivr.net/gh/bestthezhi/images@master/juc/20201218122110944.png)



线程的isInterrupted()方法可以取得线程的打断标记

- 如果线程在睡眠sleep期间被打断，打断标记是不会变的，为false，但是sleep期间被打断会抛出异常，我们据此可以手动设置打断标记为true；
- 如果是在程序正常运行期间被打断的，那么打断标记就被自动设置为true。处理好这两种情况那我们就可以放心地来料理后事啦！

```java
public class Test7 {
	public static void main(String[] args) throws InterruptedException {
		Monitor monitor = new Monitor();
		monitor.start();
		Thread.sleep(3500);
		monitor.stop();
	}
}

class Monitor {

	Thread monitor;

	/**
	 * 启动监控器线程
	 */
	public void start() {
		//设置线控器线程，用于监控线程状态
		monitor = new Thread() {
			@Override
			public void run() {
				//开始不停的监控
				while (true) {
                    //判断当前线程是否被打断了
					if(Thread.currentThread().isInterrupted()) {
						System.out.println("处理后续任务");
                        //终止线程执行
						break;
					}
					System.out.println("监控器运行中...");
					try {
						//线程休眠
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						//如果是在休眠的时候被打断，不会将打断标记设置为true，这时要重新设置打断标记
						Thread.currentThread().interrupt();
					}
				}
			}
		};
		monitor.start();
	}

	/**
	 * 	用于停止监控器线程
	 */
	public void stop() {
		//打断线程
		monitor.interrupt();
	}
}

```



#### 3.7、sleep，yiled，wait，join 对比

>补充：
>
>- sleep，join，yield，interrupted是Thread类中的方法
>- wait/notify是object中的方法
>- sleep 不释放锁、释放cpu
>- join 释放锁、抢占cpu
>- yiled 不释放锁、释放cpu
>- wait 释放锁、释放cpu

![20201218122903649](https://cdn.jsdelivr.net/gh/bestthezhi/images@master/juc/20201218122903649.png)



#### 3.8、 守护线程

当Java进程中有多个线程在执行时，只有当所有非守护线程都执行完毕后，Java进程才会结束。但当非守护线程全部执行完毕后，守护线程无论是否执行完毕，也会一同结束。

- 垃圾回收器线程就是一种守护线程
- Tomcat 中的 Acceptor 和 Poller 线程都是守护线程，所以 Tomcat 接收到 shutdown 命令后，不会等



## 四、 线程状态

### 1、操作系统的层面上五种状态

![fe34](https://cdn.jsdelivr.net/gh/bestthezhi/images@master/juc/fe34.png)

1. **初始状态**，仅仅是在语言层面上创建了线程对象，即Thead thread = new Thead();，还未与操作系统线程关联
2. **可运行状态**，也称就绪状态，指该线程已经被创建，与操作系统相关联，等待cpu给它分配时间片就可运行
3. **运行状态**，指线程获取了CPU时间片，正在运行，当CPU时间片用完，线程会转换至【可运行状态】，等待 CPU再次分配时间片，会导致我们前面讲到的上下文切换
4. **阻塞状态**，如果调用了阻塞API，如BIO读写文件，那么线程实际上不会用到CPU，不会分配CPU时间片，会导致上下文切换，进入【阻塞状态】，等待BIO操作完毕，会由操作系统唤醒阻塞的线程，转换至【可运行状态】，【阻塞状态】与【可运行状态】的区别是，只要操作系统一直不唤醒线程，调度器就一直不会考虑调度它们，CPU就一直不会分配时间片
5. **终止状态**，表示线程已经执行完毕，生命周期已经结束，不会再转换为其它状态



### 2、Java API 层面上六种状态

根据Thread.State 枚举，分为六种状态

![20210129171228140](https://cdn.jsdelivr.net/gh/bestthezhi/images@master/juc/20210129171228140.png)



- **NEW (新建状态)** ：线程刚被创建，但是还没有调用 start() 方法
- **RUNNABLE (运行状态)**： 当调用了 start() 方法之后，注意，Java API 层面的RUNNABLE 状态涵盖了操作系统层面的 【就绪状态】、【运行中状态】和【阻塞状态】（由于 BIO 导致的线程阻塞，在 Java 里无法区分，仍然认为 是可运行）
- **BLOCKED (阻塞状态)** ， **WAITING (等待状态)** ， **TIMED_WAITING(定时等待状态)**： 都是 Java API 层面对【阻塞状态】的细分，如sleep就位TIMED_WAITING， join为WAITING状态。后面会在状态转换一节详述。
- **TERMINATED (结束状态)** ：当线程代码运行结束



线程状态枚举类 - Thread.State

```java
public enum State {
        NEW, //新建
		
        RUNNABLE, //准备就绪

        BLOCKED, //阻塞

        WAITING, // “ 不见不散 ”

        TIMED_WAITING, // “ 过时不候 ”

        TERMINATED; //终结
    }
```



六种线程状态举例：

```java
@Slf4j(topic = "c.TestState")
public class TestState {
    public static void main(String[] args) throws IOException {
        Thread t1 = new Thread("t1") {	// new 状态
            @Override
            public void run() {
                log.debug("running...");
            }
        };

        Thread t2 = new Thread("t2") {
            @Override
            public void run() {
                while(true) { // runnable 状态

                }
            }
        };
        t2.start();

        Thread t3 = new Thread("t3") {
            @Override
            public void run() {
                log.debug("running...");  // terminated 状态
            }
        };
        t3.start();

        Thread t4 = new Thread("t4") {
            @Override
            public void run() {
                synchronized (TestState.class) {
                    try {
                        Thread.sleep(1000000); // timed_waiting 显示阻塞状态
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t4.start();

        Thread t5 = new Thread("t5") {
            @Override
            public void run() {
                try {
                    t2.join(); // waiting 状态
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t5.start();

        Thread t6 = new Thread("t6") {
            @Override
            public void run() {
                synchronized (TestState.class) { // blocked 状态
                    try {
                        Thread.sleep(1000000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t6.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.debug("t1 state {}", t1.getState());
        log.debug("t2 state {}", t2.getState());
        log.debug("t3 state {}", t3.getState());
        log.debug("t4 state {}", t4.getState());
        log.debug("t5 state {}", t5.getState());
        log.debug("t6 state {}", t6.getState());
    }
}
```





