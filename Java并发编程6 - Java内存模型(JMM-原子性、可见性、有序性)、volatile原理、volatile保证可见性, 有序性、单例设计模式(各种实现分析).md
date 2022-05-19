多线程的三大特性 (原子性、可见性、有序性)

之前讲的synchronized底层Monitor主要关注的是访问共享变量时，保证临界区代码的 原子性 。下面进一步深入学习共享变量在多线程间的【可见性】问题与多条指令执行时的【有序性】问题



## 一、 Java 内存模型

JMM 即 `Java Memory Model` ，它从Java层面定义了 主存、工作内存 抽象概念，底层对应着CPU 寄存器、缓存、硬件内存、CPU 指令优化等。JMM 体现在以下几个方面

- **`原子性`** - 保证指令不会受 **线程上下文切换的影响**
- **`可见性`** - 保证指令不会受 **cpu 缓存的影响 `(JIT对热点代码的缓存优化)`**
- **`有序性`** - 保证指令不会受 **cpu 指令并行优化的影响**



### 1、 可见性 

#### 1.1、退不出的循环

先来看一个现象，main线程对run变量的修改对于t线程不可见，导致了 t 线程无法停止

>一开始一直不结束, 是因为无限循环, run都是true, JIT及时编译器, 会对t1线程所执行的run变量,进行缓存, 缓存到本地工作内存. 不去访问主存中的run. 这样可以提高性能; 也可以说是JVM打到一定阈值之后, while(true)变成了一个热点代码, 所以一直访问的都是缓存到本地工作内存(局部)中的run. 当主线程修改主存中的run变量的时候,t1线程一直访问的是自己缓存的, 所以不认为run已经改为false了. 所以一直运行. 我们为主存(成员变量)进行volatile修饰, 增加变量的可见性, 当主线程修改run为false, t1线程对run的值可见. 这样就可以退出循环。

```java
@Slf4j()
public class Test1 {

    static boolean run = true;
//    static volatile boolean run = true;
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            while (run) {
                // 如果打印一句话
                // 此时就可以结束, 因为println方法中, 使用到了synchronized
                // 内存屏障，会清空工作内存，去主存中读
                // System.out.println("123");
            }
        });

        t1.start();
        Thread.sleep(1000);
        run = false;
        System.out.println(run);
    }
}
```



为什么会出现对run变量的不可见性呢呢？分析一下：

初始状态， t线程刚开始从主内存(成员变量), 因为主线程sleep(1)秒, 这时候t1线程循环了好多次run的值, 超过了一定的阈值, JIT就会将主存中的run值读取到工作内存 (相当于缓存了一份, 不会去主存中读run的值了)，减少对主存中run的访问以提高效率。

![87c7](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/87c7.png)

1 秒之后，main线程修改了run的值, 并同步至主存。而 t线程是从自己工作内存中的高速缓存中读取这个变量的值，结果永远是旧值



![20210202183308792](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20210202183308792.png)



#### 1.2、解决方法

1.使用volatile（表示易变关键字的意思），它可以用来修饰成员变量和静态成员变量，**它可以避免线程从自己的工作缓存中查找变量的值，必须到主存中获取它的值，线程操作 volatile 变量都是直接操作主存**

2.使用synchronized关键字也有相同的效果, 在Java内存模型中，synchronized规定，**线程在加锁时， 先清空工作内存 → 在主内存中拷贝最新变量的副本到工作内存 → 执行完代码 → 将更改后的共享变量的值刷新到主内存中 → 释放互斥锁。**



#### 1.3、可见性 vs 原子性

前面例子体现的实际就是可见性，它保证的是在多个线程之间一个线程对 volatile 变量的修改对另一个线程可见， 而不能保证原子性。volatile用在一个写线程，多个读线程的情况, 比较合适。

比较一下之前我们讲线程安全时举的例子：两个线程一个 i++ 一个 i-- ，只能保证看到最新值(可见性)，不能解决指令交错(原子性)

```java
// 假设i的初始值为0
getstatic i // 线程2-获取静态变量i的值 线程内i=0
getstatic i // 线程1-获取静态变量i的值 线程内i=0
iconst_1 // 线程1-准备常量1
iadd // 线程1-自增 线程内i=1
putstatic i // 线程1-将修改后的值存入静态变量i 静态变量i=1
iconst_1 // 线程2-准备常量1
isub // 线程2-自减 线程内i=-1
putstatic i // 线程2-将修改后的值存入静态变量i 静态变量i=-1 
```

>注意 ：synchronized 语句块既可以保证代码块的原子性，也同时保证此线程中变量的可见性。但缺点是 synchronized 是属于重量级操作，性能相对更低。 如果在前面示例的死循环中加入 System.out.println() 会发现即使不加 volatile 修饰符，线程 t 也能正确看到 对 run 变量的修改了。



#### 1.4、模式之两阶段终止

>当我们在执行线程一时，想要终止线程二，这是就需要使用interrupt方法来优雅的停止线程二。这是我们之前的做法

使用volatile关键字来实现两阶段终止模式:

```java
@Slf4j
public class Test2 {
    public static void main(String[] args) throws InterruptedException {

        // 下面是两个线程操作共享变量stop
        Monitor monitor = new Monitor();
        monitor.start();

        Thread.sleep(3500);
        monitor.stop();
    }
}

@Slf4j
class Monitor {

    // private boolean stop = false; // 不会停止程序
    private volatile boolean stop = false; // 会停止程序

    /**
     * 启动监控器线程
     */
    public void start() {
        Thread monitor = new Thread(() -> {
            //开始不停的监控
            while (true) {
                if (stop) {
                    break;
                }
            }
        });
        monitor.start();
    }

    /**
     * 用于停止监控器线程
     */
    public void stop() {
        stop = true;
    }
}
```



#### 1.5、模式之 Balking(了解)

定义：Balking （犹豫）模式用在 一个线程发现另一个线程或本线程已经做了某一件相同的事，那么本线程就无需再做了，直接结束返回。有点类似于单例。

```java
@Slf4j
public class Test1 {
    public static void main(String[] args) throws InterruptedException {
        Monitor monitor = new Monitor();
        monitor.start();
        monitor.start();
        monitor.start();
        Thread.sleep(3.5);
        monitor.stop();
    }
}

@Slf4j
class Monitor {

    Thread monitor;
    //设置标记，用于判断是否被终止了
    private volatile boolean stop = false;
    //设置标记，用于判断是否已经启动过了
    private boolean starting = false;
    /**
     * 启动监控器线程
     */
    public void start() {
        //上锁，避免多线程运行时出现线程安全问题
        synchronized (this) {
            if (starting) {
                //已被启动，直接返回
                return;
            }
            //启动监视器，改变标记
            starting = true;
        }
        //设置线控器线程，用于监控线程状态
        monitor = new Thread(() -> {
            //开始不停的监控
            while (true) {
                if(stop) {
                    log.debug("处理后续儿事");
                    break;
                }
                log.debug("监控器运行中...");
                try {
                    //线程休眠
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.debug("被打断了...");
                }
            }
        });
        monitor.start();
    }

    /**
     * 	用于停止监控器线程
     */
    public void stop() {
        //打断线程
        stop = true;
        monitor.interrupt();
    }
}
```



### 2、 有序性

>是JIT即时编译器的优化, 可能会导致指令重排。为什么要优化? 因为CPU 支持多级指令流水线，例如支持同时执行 `取指令 - 指令译码 - 执行指令 - 内存访问 - 数据写回` 的处理器。效率快 

JVM会在不影响正确性的前提下，可以调整语句的执行顺序, 是一种优化

```
int i = 1;  //1
int j = 2;  //2
```

这两行代码的执行顺序，对最终的结果不会产生影响，在cpu高速运行下，这这两行代码的指令的执行顺序可能会发生改变。但在此，并不会对结果产生影响。

**在多线程环境下，指令重排序可能导致出现意料之外的结果**



#### 2.1、支持流水线的处理器

现代 CPU 支持多级指令流水线，例如支持同时执行 取指令 - 指令译码 - 执行指令 - 内存访问 - 数据写回 的处理器，就可以称之为五级指令流水线。这时 CPU 可以在一个时钟周期内，同时运行五条指令的不同阶段（相当于一 条执行时间长的复杂指令），IPC = 1，本质上，流水线技术并不能缩短单条指令的执行时间，但它变相地提高了指令地吞吐率。

![20201224232242688](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201224232242688.png)



#### 2.2、'奇怪'的结果

观察如下代码片段：

```java
int num = 0;

boolean ready = false; 

// volatile 修饰的变量，可以禁用指令重排 volatile boolean ready = false; 可以防止变量之前的代码被重排序

// 线程1 执行此方法
public void actor1(I_Result r) {
	 if(ready) {
	 	r.r1 = num + num;
	 } 
	 else {
	 	r.r1 = 1;
	 }
}
// 线程2 执行此方法
public void actor2(I_Result r) {
	 num = 2;
	 ready = true;
}
```

I_Result 是一个对象，有一个属性 r1 用来保存结果，问可能的结果有几种？

- 情况1：线程1 先执行，这时 ready = false，所以进入 else 分支结果为 1
- 情况2：线程2 先执行 num = 2，但没来得及执行 ready = true，线程1 执行，还是进入 else 分支，结果为1
- 情况3：线程2 执行到 ready = true，线程1 执行，这回进入 if 分支，结果为4（因为 num 已经执行过了）

但是结果还有可能是 0 ，这种情况下是：线程2 执行 `ready = true`，切换到线程1，进入 if 分支，相加为 0，再切回线程2 执行 `num = 2`。

这种现象叫做指令重排，是JIT 编译器在运行时的一些优化，这个现象需要通过大量测试才能复现，可以使用jcstress工具进行测试。上面仅是从代码层面体现出了有序性问题，下面在讲到 double-checked locking (双重检查锁)案例时还会从java字节码的层面了解有序性的问题。

>实验现象可以查看：[黑马程序员全面深入学习Java并发编程，JUC并发编程全套教程](https://www.bilibili.com/video/BV16J411h7Rd?p=144&share_source=copy_web) P144

0 1 4 分别出现的结果次数:

![20201224234810568](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201224234810568.png) 



#### 2.3、重排序也需要遵守一定规则

指令重排序操作不会对存在数据依赖关系的操作进行重排序。比如：a=1;b=a; 这个指令序列，由于第二个操作依赖于第一个操作，所以在编译时和处理器运行时这两个操作不会被重排序。

重排序是为了优化性能，但是不管怎么重排序，单线程下程序的执行结果不能被改变。 比如：a=1;b=2;c=a+b这三个操作，第一步（a=1)和第二步(b=2)由于不存在数据依赖关系，所以可能会发生重排序，但是c=a+b这个操作是不会被重排序的，因为需要保证最终的结果一定是c=a+b=3。

指令重排序 在 单线程模式下是一定会保证最终结果的正确性， 但是在多线程环境下，就可能出现问题。

**解决方法：volatile 修饰的变量，可以禁用指令重排**



#### 2.4、volatile 原理

**volatile 的底层实现原理是内存屏障，Memory Barrier（Memory Fence)**

- 对 volatile 变量的写指令后会加入写屏障。(保证写屏障之前的写操作, 都能同步到主存中，并且不会将写屏障之前的代码排在写屏障之后)
- 对 volatile 变量的读指令前会加入读屏障。(保证读屏障之后的读操作, 都能读到主存的数据，并且不会将读屏障之后的代码排在读屏障之前)



根据如下代码理解：

```java
public class Test3 {

//    static volatile boolean run = true;
//    static boolean run2 = true;   //可以暂停

    static boolean run = true;
    static volatile boolean run2 = true;  //不可以暂停

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            while (run || run2) {

            }
        });

        t1.start();
        Thread.sleep(1000);
        run = false;
        run2 = false;
        System.out.println("修改...");
    }
}
```





#### 2.5、volatile是如何保证可见性

**写屏障（sfence）保证在该屏障之前的，对共享变量的改动，都同步到主存当中**

```java
public void actor2(I_Result r) {
     num = 2;
     ready = true; // ready是被volatile修饰的 ，赋值带写屏障
     // 写屏障.(在ready=true写指令之后加的, 
     //在该屏障之前对共享变量的改动, 都同步到主存中. 包括num)
}
```

**读屏障（lfence）保证在该屏障之后，对共享变量的读取，加载的是主存中最新数据**

```java
public void actor1(I_Result r) {
	 // 读屏障
	 //  ready是被volatile修饰的 ，读取值带读屏障
	 if(ready) {	// ready, 读取的就是主存中的新值
	 	r.r1 = num + num; // num, 读取的也是主存中的新值
	 } else {
	 	r.r1 = 1;
	 }
}
```

![4d0d](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/4d0d.png)



#### 2.6、volatile是如何保证有序性

**写屏障会确保指令重排序时，不会将写屏障之前的代码排在写屏障之后**

```java
public void actor2(I_Result r) {
 num = 2;
 ready = true; //  ready是被volatile修饰的 ， 赋值带写屏障
 // 写屏障
}
```

**读屏障会确保指令重排序时，不会将读屏障之后的代码排在读屏障之前**

```java
public void actor1(I_Result r) {
	 // 读屏障
	 //  ready是被volatile修饰的 ，读取值带读屏障
	 if(ready) {
	 	r.r1 = num + num;
	 } else {
	 	r.r1 = 1;
	 }
}
```

![8c83](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/8c83.png)



volatile不能解决指令交错 (不能解决原子性)：

- 写屏障仅仅是保证之后的读能够读到最新的结果，但不能保证其它线程的读, 跑到它前面去
- 有序性的保证也只是保证了本线程内相关代码不被重排序,下图t2线程, 就先读取了i=0, 此时还是会出现指令交错的现象, 可以使用synchronized来解决原子性

![33e9](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/33e9.png)



#### 2.7、 double-checked locking (双重检查锁) 问题

以著名的double-checked locking(双重检查锁) 单例模式为例，这是volatile最常使用的地方。

```java
// 最开始的单例模式是这样的
public final class Singleton {
    private Singleton() { }
    private static Singleton INSTANCE = null;
    public static Singleton getInstance() {
	    /*
	      多线程同时调用getInstance(), 如果不加synchronized锁, 此时两个线程同时
	      判断INSTANCE为空, 此时都会new Singleton(), 此时就破坏单例了.所以要加锁,
	      防止多线程操作共享资源,造成的安全问题
	     */
	    synchronized(Singleton.class) {
	    	if (INSTANCE == null) { // t1
	    		INSTANCE = new Singleton();
	        }
	    }
        return INSTANCE;
    }
}


/*
	首先上面代码的效率是有问题的, 因为当我们创建了一个单例对象后, 又来一个线程获取到锁了,还是会加锁, 
	严重影响性能,再次判断INSTANCE==null吗, 此时肯定不为null, 然后就返回刚才创建的INSTANCE;
	这样导致了很多不必要的判断; 

	所以要双重检查, 在第一次线程调用getInstance(), 直接在synchronized外,判断instance对象是否存在了,
	如果不存在, 才会去获取锁,然后创建单例对象,并返回; 第二个线程调用getInstance(), 会进行
	if(instance==null)的判断, 如果已经有单例对象, 此时就不会再去同步块中获取锁了. 提高效率
*/
public final class Singleton {
    private Singleton() { }
    private static Singleton INSTANCE = null;
    public static Singleton getInstance() {
        if(INSTANCE == null) { // t2
            // 首次访问会同步，而之后的使用没有 synchronized
            synchronized(Singleton.class) {
                if (INSTANCE == null) { // t1
                    INSTANCE = new Singleton();
                }
            }
        }
        return INSTANCE;
    }
}
//但是上面的if(INSTANCE == null)判断代码没有在同步代码块synchronized中，
// 不能享有synchronized保证的原子性、可见性、以及有序性。所以可能会导致 指令重排
```

以上的实现特点是：

- 懒汉式单例
- 首次使用 getInstance() 才使用 synchronized 加锁，后续使用时无需加锁 (也就是上面的第二个单例)
- 有隐含的: 但很关键的一点：第一个 if 使用了 INSTANCE 变量，是在同步块之外, 这样会导致synchronized无法保证指令的有序性, 此时可能会导致指令重排问题



注意: 但在多线程环境下，上面的代码是有问题的，getInstance 方法对应的字节码为

```java
0: getstatic #2 // Field INSTANCE:Lcn/itcast/n5/Singleton;
3: ifnonnull 37 // 判断是否为空
// ldc是获得类对象
6: ldc #3 // class cn/itcast/n5/Singleton
// 复制操作数栈栈顶的值放入栈顶, 将类对象的引用地址复制了一份
8: dup
// 操作数栈栈顶的值弹出，即将对象的引用地址存到局部变量表中
// 将类对象的引用地址存储了一份，是为了将来解锁用
9: astore_0
10: monitorenter
11: getstatic #2 // Field INSTANCE:Lcn/itcast/n5/Singleton;
14: ifnonnull 27
// 新建一个实例
17: new #3 // class cn/itcast/n5/Singleton
// 复制了一个实例的引用
20: dup
// 通过这个复制的引用调用它的构造方法
21: invokespecial #4 // Method "<init>":()V
// 最开始的这个引用用来进行赋值操作
24: putstatic #2 // Field INSTANCE:Lcn/itcast/n5/Singleton;
27: aload_0
28: monitorexit
29: goto 37
32: astore_1
33: aload_0
34: monitorexit
35: aload_1
36: athrow
37: getstatic #2 // Field INSTANCE:Lcn/itcast/n5/Singleton;
40: areturn
```

其中:

- 17 表示创建对象，将对象引用入栈 // new Singleton
- 20 表示复制一份对象引用 // 复制了引用地址, 解锁使用
- 21 表示利用一个对象引用，调用构造方法 // 根据复制的引用地址调用构造方法
- 24 表示利用一个对象引用，赋值给 static INSTANCE



通过上面的字节码发现, 这一步INSTANCE = new Singleton();操作不是一个原子操作, 它分为21, 24两个指令, 此时可能就会发生指令重排的问题，可能jvm 会优化为：**先执行 24(赋值)，再执行 21(构造方法)**。如果两个线程 t1，t2 按如下时间序列执行：

![4079](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/4079.png)

关键在于 0: getstatic 这行代码在 monitor 控制之外，它就像之前举例中不守规则的人，可以越过 monitor 读取 INSTANCE 变量的值；

这时 t1 还未完全将构造方法执行完毕，如果在构造方法中要执行很多初始化操作，那么 t2 拿到的是将是一个未初始化完毕的单例 ，提前使用此对象就会出现问题；对 INSTANCE 使用 volatile 修饰即可，可以禁用指令重排。

注意在 JDK 5 以上的版本的 volatile 才会真正有效



#### 2.8、double-checked locking 关于指令重排的解决

加volatile修饰：

```java
public final class Singleton {
    private Singleton() { }
    private static volatile Singleton INSTANCE = null;
    public static Singleton getInstance() {
        // 实例没创建，才会进入内部的 synchronized代码块
        if (INSTANCE == null) {
            synchronized (Singleton.class) { // t2
                // 也许有其它线程已经创建实例，所以再判断一次
                if (INSTANCE == null) { // t1
                    INSTANCE = new Singleton();
                }
            }
        }
        return INSTANCE;
    }
}
```

字节码上看不出来 volatile 指令的效果:

```java
// -------------------------------------> 加入对 INSTANCE 变量的读屏障
0: getstatic #2 // Field INSTANCE:Lcn/itcast/n5/Singleton;
3: ifnonnull 37
6: ldc #3 // class cn/itcast/n5/Singleton
8: dup
9: astore_0
10: monitorenter -----------------------> 保证原子性、可见性
11: getstatic #2 // Field INSTANCE:Lcn/itcast/n5/Singleton;
14: ifnonnull 27
17: new #3 // class cn/itcast/n5/Singleton
20: dup
21: invokespecial #4 // Method "<init>":()V
24: putstatic #2 // Field INSTANCE:Lcn/itcast/n5/Singleton;
// -------------------------------------> 加入对 INSTANCE 变量的写屏障
27: aload_0
28: monitorexit ------------------------> 保证原子性、可见性
29: goto 37
32: astore_1
33: aload_0
34: monitorexit
35: aload_1
36: athrow
37: getstatic #2 // Field INSTANCE:Lcn/itcast/n5/Singleton;
40: areturn
```

如上面的注释内容所示，读写 volatile 变量操作（即getstatic操作和putstatic操作）时会加入内存屏障（Memory Barrier（Memory Fence）），保证下面两点：

- 可见性
  - 写屏障（sfence）保证在该屏障之前的 t1 对共享变量的改动，都同步到主存当中
  - 读屏障（lfence）保证在该屏障之后 t2 对共享变量的读取，加载的是主存中最新数据
- 有序性
  - 写屏障 会确保指令重排序时，不会将写屏障之前的代码排在写屏障之后
  - 读屏障 会确保指令重排序时，不会将读屏障之后的代码排在读屏障之前

**这里`利用写屏障`解决了指令 `21-调用构造方法` 和 `24-给INSTANCE赋值` 的指令重排序问题，保证获取的实例已经初始化完成。**



小结 :

synchronized 能保证 `原子性`, 当前线程中变量的`可见性`，但是不能保证其代码块内的`有序性`，(如果一个变量完全被synchronized接管，如单例模式第一个例子中，INSTANCE变量的读写完全在synchronized代码块中，不能有多个线程对此进行读写，那么也就不用考虑此变量的有序性问题了)



#### 2.9、happens-before (对共享变量的写操作,对其它线程的读操作可见)

>happens-before 规定了对共享变量的写操作,对其它线程的读操作可见，它是可见性与有序性的一套规则总结。抛开以下 happens-before 规则，JMM 并不能保证一个线程对共享变量的写，对于其它线程对该共享变量的读可见。

下面说的变量都是指 **成员变量或静态成员变量**



1.线程解锁 m 之前对变量的写，对于接下来对 m 加锁的其它线程对该变量的 读可见

```java
	static int x;
	
	static Object m = new Object();
	
	new Thread(()->{
	    synchronized(m) {
	        x = 10;
	    }
	},"t1").start();
	
	new Thread(()->{
	    synchronized(m) {
	        System.out.println(x);
	    }
	},"t2").start();

// 10
```



2.线程对 volatile 变量的写，对接下来其它线程对该变量的读可见

```java
  volatile static int x;
  
  new Thread(()->{
   	x = 10;
  },"t1").start();
  
  new Thread(()->{
   	System.out.println(x);
  },"t2").start();
```



3.线程 start() 前对变量的写，对该线程开始后对该变量的读可见

```java
  static int x;
  x = 10;
  
  new Thread(()->{
   	System.out.println(x);
  },"t2").start();
```



4.线程结束前 对变量的写，对其它线程得知它结束后的读可见（比如其它线程调用 t1.isAlive() 或 t1.join()等待它结束）

```java
  static int x;
  
  Thread t1 = new Thread(()->{
   	x = 10;
  },"t1");
  t1.start();
  
  t1.join();
  System.out.println(x);
```



5.线程 t1 打断 t2（interrupt）前对变量的写，对于其他线程得知 t2 被打断后, 对变量的读可见（通过 t2.interrupted 或 t2.isInterrupted）

```java
  static int x;
  public static void main(String[] args) {
      Thread t2 = new Thread(()->{
          while(true) {
              if(Thread.currentThread().isInterrupted()) {
                  System.out.println(x); // 10, 打断了, 读取的也是打断前修改的值
                  break;
              }
          }
      },"t2");
      t2.start(); 
      
      new Thread(()->{
          sleep(1);
          x = 10;
          t2.interrupt();
      },"t1").start();
      
      while(!t2.isInterrupted()) {
          Thread.yield();
      }
      System.out.println(x);	// 10
  }
```





### 3、线程安全单例模式

单例模式有很多实现方法，饿汉、懒汉、静态内部类、枚举类，试分析每种实现下获取单例对象（即调用 getInstance）时的线程安全，并思考注释中的问题

- 饿汉式：类加载就会导致该单实例对象被创建
- 懒汉式：类加载不会导致该单实例对象被创建，而是首次使用该对象时才会创建

#### 3.1、饿汉式

```java
// 问题1：为什么加 final，防止子类继承后更改
// 问题2：如果实现了序列化接口, 还要做什么来防止反序列化破坏单例，如果进行反序列化的时候会生成新的对象，这样跟单例模式生成的对象是不同的。要解决直接加上readResolve()方法就行了，如下所示
public final class Singleton implements Serializable {
    // 问题3：为什么设置为私有? 放弃其它类中使用new生成新的实例，是否能防止反射创建新的实例?不能。
    private Singleton() {}
    // 问题4：这样初始化是否能保证单例对象创建时的线程安全?能，这是类变量，是jvm在类加载阶段就进行了初始化，jvm保证了此操作的线程安全性
    private static final Singleton INSTANCE = new Singleton();
    // 问题5：为什么提供静态方法而不是直接将 INSTANCE 设置为 public, 说出你知道的理由。
    //1.提供更好的封装性；2.提供范型的支持
    public static Singleton getInstance() {
        return INSTANCE;
    }
    public Object readResolve() {
        return INSTANCE;
    }
}

```

>问题2 : 首先通过反序列化操作, 也是可以创建一个对象的, 破坏了单例, 可以使用readResolve方法并返回instance对象, 当反序列化的时候就会调用自己写的readResolve方法
>
>问题4 : 因为单例对象是static的, 静态成员变量的初始化操作是在类加载阶段中的`初始化`步骤完成的，虚拟机必须保证一个类的<clinit>()方法在多线程下被同步加锁



#### 3.2、饿汉式-枚举

因为枚举的变量, 底层是通过public static final来修饰的, 类加载就创建了,所以是饿汉式

```java
 // 问题1：枚举单例是如何限制实例个数的：创建枚举类的时候就已经定义好了，每个枚举常量其实就是枚举类的一个静态成员变量
// 问题2：枚举单例在创建时是否有并发问题：没有，这是静态成员变量
// 问题3：枚举单例能否被反射破坏单例：不能
// 问题4：枚举单例能否被反序列化破坏单例：枚举类默认实现了序列化接口，枚举类已经考虑到此问题，无需担心破坏单例
// 问题5：枚举单例属于懒汉式还是饿汉式：饿汉式,类加载的时候就加载了
// 问题6：枚举单例如果希望加入一些单例创建时的初始化逻辑该如何做：加构造方法就行了
enum Singleton {
 INSTANCE;
}
```

>问题3 : 反射无法破坏枚举单例, 主要通过反射, newInstance的时候, 会在该方法中作判断, 如果检查是枚举类型, 就会抛出异常。
>
>- if ((this.clazz.getModifiers() & 16384) != 0)
>  throw new IllegalArgumentException(“Cannot reflectively create enum objects”);



3.3、懒汉式

```java
public final class Singleton {
    private Singleton() { }
    private static Singleton INSTANCE = null;
    // 分析这里的线程安全, 并说明有什么缺点：synchronized加载静态方法上，可以保证线程安全。缺点就是锁的范围过大.
    public static synchronized Singleton getInstance() {
        if( INSTANCE != null ){
            return INSTANCE;
        }
        INSTANCE = new Singleton();
        return INSTANCE;
    }
}
```



3.4、DCL 懒汉式

```java
public final class Singleton {
    private Singleton() { }
    // 问题1：解释为什么要加 volatile ?为了防止指令重排序问题
    private static volatile Singleton INSTANCE = null;

    // 问题2：对比实现3, 说出这样做的意义：提高了效率
    public static Singleton getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (Singleton.class) {
            // 问题3：为什么还要在这里加为空判断, 之前不是判断过了吗？这是为了第一次判断时的并发问题。
            if (INSTANCE != null) { // t2
                return INSTANCE;
            }
            INSTANCE = new Singleton();
            return INSTANCE;
        }
    }
}
```



3.5、懒汉式-静态内部类

```java
public final class Singleton {
    private Singleton() { }
    // 问题1：属于懒汉式还是饿汉式：懒汉式，这是一个静态内部类。类加载本身就是懒惰的，在没有调用getInstance方法时是没有执行LazyHolder内部类的类加载操作的。
    private static class LazyHolder {
        static final Singleton INSTANCE = new Singleton();
    }
    // 问题2：在创建时是否有并发问题，这是线程安全的，类加载时，jvm保证类加载操作的线程安全
    public static Singleton getInstance() {
        return LazyHolder.INSTANCE;
    }
}
```



## 二、本章小结

本章重点讲解了 JMM 中的

- 可见性 - 由 JVM 缓存优化引起; (JIT即时编译器, 通过对热点代码的优化)
- 有序性 - 由 JVM 指令重排序优化引起; (提高指令的执行效率, 类似流水线系统)
- happens-before 规则; (happens-before 规定了对共享变量的写操作对其它线程的读操作可见，它是可见性与有序性的一套规则总结。)

原理方面

- volatile (读写屏障)

模式方面

- 两阶段终止模式的 volatile 改进
- 同步模式之 balking (犹豫模式)









