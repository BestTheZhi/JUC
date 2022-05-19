## 一、多把锁

小故事:
一间大屋子有两个功能：睡觉、学习，互不相干。
现在小南要学习，小女要睡觉，但如果只用一间屋子（一个对象锁）的话，那么并发度很低
小南获得锁之后, 学完习之后, 小女才能进来睡觉。

解决方法是准备多个房间（多个对象锁）

```java
@Slf4j
public class BigRoomTest {
    public static void main(String[] args) {
        BigRoom bigRoom = new BigRoom();
        new Thread(() -> bigRoom.sleep(), "小南").start();
        new Thread(() -> bigRoom.study(), "小女").start();
    }
}

@Slf4j(topic = "BigRoom")
class BigRoom {
    public void sleep() {
        synchronized (this) {
            log.debug("sleeping 2 小时");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void study() {
        synchronized (this) {
            log.debug("study 1 小时");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```

相当于串行执行, 因为锁对象是整个屋子, 所以并发性很低



改进让小南, 小女获取不同的锁即可:

```java
@Slf4j(topic = "BigRoomTest")
public class BigRoomTest {
    private static final BigRoom sleepRoom = new BigRoom();
    private static final BigRoom studyRoom = new BigRoom();

    public static void main(String[] args) {
    	// 不同对象调用
        new Thread(() -> sleepRoom.sleep(), "小南").start();
        new Thread(() -> studyRoom.study(), "小女").start();
    }
}
```



将锁的粒度细分

- 好处，是可以`增强并发度`
- 坏处，如果一个线程需要同时获得多把锁，就`容易发生死锁`



## 二、 活跃性

因为某种原因，使得代码一直无法执行完毕，这样的现象叫做 **活跃性**

活跃性相关的一系列问题都可以用 **`ReentrantLock`** 进行解决。



### 1、死锁

有这样的情况：一个线程需要 同时获取多把锁，这时就容易发生死锁

如：线程1获取A对象锁, 线程2获取B对象锁; 此时线程1又想获取B对象锁, 线程2又想获取A对象锁; 它们都等着对象释放锁, 此时就称为死锁

```java
public static void main(String[] args) {
	final Object A = new Object();
	final Object B = new Object();
	
	new Thread(()->{
		synchronized (A) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			synchronized (B) {

			}
		}
	}).start();

	new Thread(()->{
		synchronized (B) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			synchronized (A) {

			}
		}
	}).start();
}
```



#### 1.2、发生死锁的必要条件

- 互斥条件
  - 在一段时间内，一种资源只能被一个进程所使用
- 请求和保持条件
  - 进程已经拥有了至少一种资源，同时又去申请其他资源。因为其他资源被别的进程所使用，该进程进入阻塞状态，并且不释放自己已有的资源
- 不可抢占条件
  - 进程对已获得的资源在未使用完成前不能被强占，只能在进程使用完后自己释放
- 循环等待条件
  - 发生死锁时，必然存在一个进程——资源的循环链。



#### 1.3、定位死锁的方法

方式一、JPS + JStack 进程ID

- jps先找到JVM进程
- jstack 进程ID
  - 在Java控制台中的`Terminal`中输入 **`jps`** 指令可以查看`正在运行中的进程ID`，使用 **`jstack 进程ID`** 可以查看进程状态。

![20201223123554788](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201223123554788.png)



方式二、 jconsole检测死锁

![d641](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/d641.png)

![efe3](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/efe3.png)



#### 1.4、死锁举例 - 哲学家就餐问题

![20201223123802724](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201223123802724.png) 

有五位哲学家，围坐在圆桌旁。

- 他们只做两件事，思考和吃饭，思考一会吃口饭，吃完饭后接着思考。
- 吃饭时要用两根筷子吃，桌上共有 5 根筷子，每位哲学家左右手边各有一根筷子。
- 如果筷子被身边的人拿着，自己就得等待

当每个哲学家即线程持有一根筷子时，他们都在等待另一个线程释放锁，因此造成了死锁。

```java
/**
 * 使用synchronized加锁, 导致哲学家就餐问题, 
 * 死锁: 核心原因是因为synchronized的锁是不可打断的, 进入阻塞队列
 * 需要一直等待别的线程释放锁
 */
@Slf4j(topic = "PhilosopherEat")
public class PhilosopherEat {
    public static void main(String[] args) {
        Chopstick c1 = new Chopstick("1");
        Chopstick c2 = new Chopstick("2");
        Chopstick c3 = new Chopstick("3");
        Chopstick c4 = new Chopstick("4");
        Chopstick c5 = new Chopstick("5");
        new Philosopher("苏格拉底", c1, c2).start();
        new Philosopher("柏拉图", c2, c3).start();
        new Philosopher("亚里士多德", c3, c4).start();
        new Philosopher("赫拉克利特", c4, c5).start();
        new Philosopher("阿基米德", c5, c1).start();
    }
}

@Slf4j(topic = "Philosopher")
class Philosopher extends Thread {
    final Chopstick left;
    final Chopstick right;

    public Philosopher(String name, Chopstick left, Chopstick right) {
        super(name);
        this.left = left;
        this.right = right;
    }

    @Override
    public void run() {
        while (true) {
            // 尝试获取左手筷子
            synchronized (left) {
                // 尝试获取右手筷子
                synchronized (right) {
                    eat();
                }
            }
        }
    }

    private void eat() {
        log.debug("eating...");
        Sleeper.sleep(0.5);
    }
}

class Chopstick{
    String name;

    public Chopstick(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "筷子{" + name + '}';
    }
}
```

程序运行下去会发生死锁...



#### 1.5、避免死锁的方法

- 在线程使用锁对象时, 采用**固定加锁的顺序**, 可以使用Hash值的大小来确定加锁的先后
- 尽可能缩减加锁的范围, 等到操作共享变量的时候才加锁
- 使用可释放的定时锁 (一段时间申请不到锁的权限了, 直接释放掉)

![5c9b](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/5c9b.png)



```java
//修改加锁的顺序
...
new Philosopher("阿基米德",c1, c5).start();
...
```





### 2、活锁

活锁出现在两个线程 互相改变对方的结束条件，两个线程并不是阻塞，而是一直在运行，谁也无法结束。



#### 2.1、避免活锁的方法

在线程执行时，中途给予 不同的间隔时间, 让某个线程先结束即可。



#### 2.2、死锁与活锁的区别

死锁是因为线程互相持有对象想要的锁，并且都不释放，最后到时线程阻塞，停止运行的现象。
活锁是因为线程间修改了对方的结束条件，而导致代码一直在运行，却一直运行不完的现象。



### 3、饥饿

某些线程因为优先级太低，导致一直无法获得资源的现象。

在使用顺序加锁时，可能会出现饥饿现象



## 三、 ReentrantLock

ReentrantLock 的特点 (synchronized不具备的)

- 支持锁重入
  - 可重入锁是指同一个线程如果首次获得了这把锁，那么因为它是这把锁的拥有者，因此 有权利再次获取这把锁
- 可中断
  - lock.lockInterruptibly() : 可以被其他线程打断的中断锁
- 可以设置超时时间
  - lock.tryLock(时间) : 尝试获取锁对象, 如果超过了设置的时间, 还没有获取到锁, 此时就退出阻塞队列, 并释放掉自己拥有的锁
- 可以设置为公平锁
  - (先到先得) 默认是非公平, true为公平 new ReentrantLock(true)
- 支持多个条件变量( 有多个waitset)
  - (可避免虚假唤醒) - lock.newCondition()创建条件变量对象; 通过条件变量对象调用 await/signal方法, 等待/唤醒



基本语法：

```java
//获取ReentrantLock对象
private ReentrantLock lock = new ReentrantLock();
//加锁
lock.lock();
try {
	//需要执行的代码
}finally {
	//释放锁
	lock.unlock();
}
```



### ReentrantLock特点

#### 1、支持锁重入

- 可重入锁是指`同一个线程如果首次获得了这把锁`，那么因为它是这把`锁的拥有者`，因此 **有权利再次获取这把锁**
- 如果是不可重入锁，那么第二次获得锁时，自己也会被锁挡住



#### 2、可中断(针对于lockInterruptibly()方法获得的中断锁) 直接退出阻塞队列, 获取锁失败

>synchronized 和 reentrantlock.lock() 的锁, 是不可被打断的; 也就是说别的线程已经获得了锁, 其他的线程就需要一直等待下去. 不能中断
>
>可被中断的锁, 通过lock.lockInterruptibly()获取的锁对象, 可以通过调用阻塞线程的interrupt()方法

如果某个线程处于阻塞状态，可以调用其interrupt方法让其停止阻塞，获得锁失败

处于阻塞状态的线程，被打断了就不用阻塞了，会抛出异常:`InterruptedException`

可中断的锁, 在一定程度上可以被动的减少死锁的概率, 之所以被动, 是因为我们需要手动调用阻塞线程的interrupt方法;



测试使用lock.lockInterruptibly()可以从阻塞队列中打断

```java
/**
 * ReentrantLock, 演示RenntrantLock中的可打断锁方法 lock.lockInterruptibly();
 */
@Slf4j(topic = "ReentrantTest")
public class ReentrantTest {

    private static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {

        Thread t1 = new Thread(() -> {
            log.debug("t1线程启动...");
            try {
                // lockInterruptibly()是一个可打断的锁, 如果有锁竞争在进入阻塞队列后,可以通过interrupt进行打断
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.debug("等锁的过程中被打断"); //没有获得锁就被打断跑出的异常
                return;
            }
            try {
                log.debug("t1线程获得了锁");
            } finally {
                lock.unlock();
            }
        }, "t1");

        // 主线程获得锁(此锁不可打断)
        lock.lock();
        log.debug("main线程获得了锁");
        // 启动t1线程
        t1.start();
        try {
            Sleeper.sleep(1);
            t1.interrupt();            //打断t1线程
            log.debug("执行打断");
        } finally {
            lock.unlock();
        }
    }
}
```



#### 3、锁超时 (lock.tryLock()) 直接退出阻塞队列, 获取锁失败

防止无限制等待, 减少死锁 

- 使用 lock.tryLock() 方法会返回获取锁是否成功。如果成功则返回true，反之则返回false。
- 并且tryLock方法可以设置指定等待时间，参数为：tryLock(long timeout, TimeUnit unit) , 其中timeout为最长等待时间，TimeUnit为时间单位

获取锁的过程中, 如果超过等待时间, **或者被打断**, 就直接从阻塞队列移除, 此时获取锁就失败了, 不会一直阻塞着 ! (可以用来实现死锁问题)



不设置等待时间, 快速失败:

```java
/**
 * ReentrantLock, 演示RenntrantLock中的tryLock(), 获取锁立即失败
 */
@Slf4j(topic = "ReentrantTest")
public class ReentrantTest {

    private static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            log.debug("尝试获得锁");
            // 此时肯定获取失败, 因为主线程已经获得了锁对象
            if (!lock.tryLock()) {
                log.debug("获取立刻失败，返回");
                return;
            }
            try {
                log.debug("获得到锁");
            } finally {
                lock.unlock();
            }
        }, "t1");

        lock.lock();
        log.debug("获得到锁");
        t1.start();
        // 主线程2s之后才释放锁
        Thread.sleep(2000);
        log.debug("释放了锁");
        lock.unlock();
    }
}

14:52:19.726 WaitNotifyTest [main] - 获得到锁
14:52:19.728 WaitNotifyTest [t1] - 尝试获得锁
14:52:19.728 WaitNotifyTest [t1] - 获取立刻失败，返回
14:52:21.728 WaitNotifyTest [main] - 释放了锁
```



设置等待时间, 超过等待时间还没有获得锁, 失败, 从阻塞队列移除该线程

```java
/**
 * ReentrantLock, 演示RenntrantLock中的tryLock(long mills), 超过锁设置的等待时间,就从阻塞队列移除
 */
@Slf4j(topic = "ReentrantTest")
public class ReentrantTest {

    private static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            log.debug("尝试获得锁");
            try {
                // 设置等待时间, 超过等待时间 / 被打断, 都会获取锁失败; 退出阻塞队列
                if (!lock.tryLock(1, TimeUnit.SECONDS)) {
                    log.debug("获取锁超时，返回");
                    return;
                }
            } catch (InterruptedException e) {
                log.debug("被打断了, 获取锁失败, 返回");
                e.printStackTrace();
                return;
            }
            try {
                log.debug("获得到锁");
            } finally {
                lock.unlock();
            }
        }, "t1");

        lock.lock();
        log.debug("获得到锁");
        t1.start();
//        t1.interrupt();
        // 主线程2s之后才释放锁
        Thread.sleep(2000);
        log.debug("main线程释放了锁");
        lock.unlock();
    }
}

// 超时的打印
14:55:56.647 WaitNotifyTest [main] - 获得到锁
14:55:56.651 WaitNotifyTest [t1] - 尝试获得锁
14:55:57.652 WaitNotifyTest [t1] - 获取锁超时，返回
14:55:58.652 WaitNotifyTest [main] - main线程释放了锁

// 中断的打印
14:56:41.258 WaitNotifyTest [main] - 获得到锁
14:56:41.260 WaitNotifyTest [main] - main线程释放了锁
14:56:41.261 WaitNotifyTest [t1] - 尝试获得锁
14:56:41.261 WaitNotifyTest [t1] - 被打断了, 获取锁失败, 返回
java.lang.InterruptedException
```



#### 4、公平锁 new ReentrantLock(true)

ReentrantLock默认是非公平锁, 可以指定为公平锁。

在线程获取锁失败，进入阻塞队列时，先进入的会在锁被释放后先获得锁。这样的获取方式就是公平的。一般不设置ReentrantLock为公平的, 会降低并发度

Synchronized底层的Monitor锁就是不公平的, 和谁先进入阻塞队列是没有关系的。



4.1、什么是公平锁? 什么是非公平锁?

公平锁 (new ReentrantLock(true))

- 公平锁, 可以把竞争的线程放在一个先进先出的阻塞队列上
- 只要持有锁的线程执行完了, 唤醒阻塞队列中的下一个线程获取锁即可; 此时先进入阻塞队列的线程先获取到锁

非公平锁 (synchronized, new ReentrantLock())

- 非公平锁, 当阻塞队列中已经有等待的线程A了, 此时后到的线程B, 先去尝试看能否获得到锁对象. 如果获取成功, 此时就不需要进入阻塞队列了. 这样以来后来的线程B就先获得到锁了



#### 5、条件变量 (可避免虚假唤醒) - lock.newCondition()创建条件变量对象; 通过条件变量对象调用await/signal方法, 等待/唤醒

Synchronized 中也有条件变量，就是Monitor监视器中的 waitSet等待集合，当条件不满足时进入waitSet 等待

ReentrantLock 的条件变量比 synchronized 强大之处在于,它是 支持多个条件变量。

这就好比synchronized 是那些不满足条件的线程都在一间休息室等通知; (此时会造成虚假唤醒), 而 ReentrantLock 支持多间休息室，有专门等烟的休息室、专门等早餐的休息室、唤醒时也是按休息室来唤醒; (可以避免虚假唤醒)

使用要点：

- await 前需要 获得锁
- await 执行后，会释放锁，进入 conditionObject (条件变量)中等待
- await 的线程被唤醒（或打断、或超时）要重新竞争 lock 锁
- 竞争 lock 锁成功后，从 await 后继续执行
- signal 方法用来唤醒条件变量(等待室)汇总的某一个等待的线程
- signalAll方法, 唤醒条件变量(休息室)中的所有线程

```java
/**
 * ReentrantLock可以设置多个条件变量(多个休息室), 相对于synchronized底层monitor锁中waitSet
 *
 */
@Slf4j(topic = "ConditionVariable")
public class ConditionVariable {
    private static boolean hasCigarette = false;
    private static boolean hasTakeout = false;
    private static final ReentrantLock lock = new ReentrantLock();
    // 等待烟的休息室
    static Condition waitCigaretteSet = lock.newCondition();
    // 等外卖的休息室
    static Condition waitTakeoutSet = lock.newCondition();

    public static voi d main(String[] args) {

        new Thread(() -> {
            lock.lock();
            try {
                log.debug("有烟没？[{}]", hasCigarette);
                while (!hasCigarette) {
                    log.debug("没烟，先歇会！");
                    try {
                        // 此时小南进入到 等烟的休息室
                        waitCigaretteSet.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("烟来咯, 可以开始干活了");
            } finally {
                lock.unlock();
            }
        }, "小南").start();

        new Thread(() -> {
            lock.lock();
            try {
                log.debug("外卖送到没？[{}]", hasTakeout);
                while (!hasTakeout) {
                    log.debug("没外卖，先歇会！");
                    try {
                        // 此时小女进入到 等外卖的休息室
                        waitTakeoutSet.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("外卖来咯, 可以开始干活了");
            } finally {
                lock.unlock();
            }
        }, "小女").start();

        Sleeper.sleep(1);
        new Thread(() -> {
            lock.lock();
            try {
                log.debug("送外卖的来咯~");
                hasTakeout = true;
                // 唤醒等外卖的小女线程
                waitTakeoutSet.signal();
            } finally {
                lock.unlock();
            }
        }, "送外卖的").start();

        Sleeper.sleep(1);
        new Thread(() -> {
            lock.lock();
            try {
                log.debug("送烟的来咯~");
                hasCigarette = true;
                // 唤醒等烟的小南线程
                waitCigaretteSet.signal();
            } finally {
                lock.unlock();
            }
        }, "送烟的").start();
    }
}
```





### 通过lock.tryLock()来解决, 哲学家就餐问题

```java
/**
 * 使用了ReentrantLock锁, 该类中有一个tryLock()方法, 在指定时间内获取不到锁对象, 就从阻塞队列移除,不用一直等待。
 *              当获取了左手边的筷子之后, 尝试获取右手边的筷子, 如果该筷子被其他哲学家占用, 获取失败, 此时就先把自己左手边的筷子,
 *              给释放掉. 这样就避免了死锁问题
 */
@Slf4j(topic = "guizy.PhilosopherEat")
public class PhilosopherEat {
    public static void main(String[] args) {
        Chopstick c1 = new Chopstick("1");
        Chopstick c2 = new Chopstick("2");
        Chopstick c3 = new Chopstick("3");
        Chopstick c4 = new Chopstick("4");
        Chopstick c5 = new Chopstick("5");
        new Philosopher("苏格拉底", c1, c2).start();
        new Philosopher("柏拉图", c2, c3).start();
        new Philosopher("亚里士多德", c3, c4).start();
        new Philosopher("赫拉克利特", c4, c5).start();
        new Philosopher("阿基米德", c5, c1).start();
    }
}

@Slf4j(topic = "guizy.Philosopher")
class Philosopher extends Thread {
    final Chopstick left;
    final Chopstick right;

    public Philosopher(String name, Chopstick left, Chopstick right) {
        super(name);
        this.left = left;
        this.right = right;
    }

    @Override
    public void run() {
        while (true) {
            // 获得了左手边筷子 (针对五个哲学家, 它们刚开始肯定都可获得左筷子)
            if (left.tryLock()) {
                try {
                	// 此时发现它的right筷子被占用了, 使用tryLock(), 
                	// 尝试获取失败, 此时它就会将自己左筷子也释放掉
                    // 临界区代码
                    if (right.tryLock()) { //尝试获取右手边筷子, 如果获取失败, 则会释放左边的筷子
                        try {
                            eat();
                        } finally {
                            right.unlock();
                        }
                    }
                } finally {
                    left.unlock();
                }
            }
        }
    }

    private void eat() {
        log.debug("eating...");
        Sleeper.sleep(0.5);
    }
}

// 继承ReentrantLock, 让筷子类称为锁
class Chopstick extends ReentrantLock {
    String name;

    public Chopstick(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "筷子{" + name + '}';
    }
}
```



## 四、同步模式之顺序控制

假如有两个线程, 线程A打印1, 线程B打印2，要求: 程序先打印2, 再打印1



### 1、Wait/Notify版本实现

```java
/**
 * 使用wait/notify来实现顺序打印 2, 1
 *
 */
@Slf4j(topic = "guizy.SyncPrintWaitTest")
public class SyncPrintWaitTest {

    public static final Object lock = new Object();
    // t2线程释放执行过
    public static boolean t2Runned = false;

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (lock) {
                while (!t2Runned) {
                    try {
                    	// 进入等待(waitset), 会释放锁
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("1");
            }
        }, "t1");

        Thread t2 = new Thread(() -> {
            synchronized (lock) {
                log.debug("2");
                t2Runned = true;
                lock.notify();
            }
        }, "t2");

        t1.start();
        t2.start();
    }
}
```



### 2、使用ReentrantLock的await/signal

```java
/**
 * 使用ReentrantLock的await/sinal 来实现顺序打印 2, 1
 *
 */
@Slf4j(topic = "guizy.SyncPrintWaitTest")
public class SyncPrintWaitTest {

    public static final ReentrantLock lock = new ReentrantLock();
    public static Condition condition = lock.newCondition();
    // t2线程释放执行过
    public static boolean t2Runned = false;

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            lock.lock();
            try {
                // 临界区
                while (!t2Runned) {
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("1");
            } finally {
                lock.unlock();
            }
        }, "t1");

        Thread t2 = new Thread(() -> {
            lock.lock();
            try {
                log.debug("2");
                t2Runned = true;
                condition.signal();
            } finally {
                lock.unlock();
            }
        }, "t2");

        t1.start();
        t2.start();
    }
}
```



### 3、使用LockSupport中的park/unpart

```java
/**
 * Description: 使用LockSupport中的park,unpark来实现, 顺序打印 2, 1
 *
 * @author guizy1
 * @date 2020/12/23 16:04
 */
@Slf4j(topic = "guizy.SyncPrintWaitTest")
public class SyncPrintWaitTest {
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            LockSupport.park();
            log.debug("1");
        }, "t1");
        t1.start();

        new Thread(() -> {
            log.debug("2");
            LockSupport.unpark(t1);
        }, "t2").start();
    }
}
```





### 交替输出

需求:线程1 输出 a 5次, 线程2 输出 b 5次, 线程3 输出 c 5次。现在要求输出 abcabcabcabcabcabc

### 1、wait/notify版本

```java
/**
 * 使用wait/notify来实现三个线程交替打印abcabcabcabcabc
 *
 */
@Slf4j(topic = "TestWaitNotify")
public class TestWaitNotify {
    public static void main(String[] args) {
        WaitNotify waitNotify = new WaitNotify(1, 5);

        new Thread(() -> {
            waitNotify.print("a", 1, 2);

        }, "a线程").start();

        new Thread(() -> {
            waitNotify.print("b", 2, 3);

        }, "b线程").start();

        new Thread(() -> {
            waitNotify.print("c", 3, 1);

        }, "c线程").start();
    }
}

@Slf4j(topic = "WaitNotify")
@Data
@NoArgsConstructor
@AllArgsConstructor
class WaitNotify {

    private int flag;
    
    // 循环次数
    private int loopNumber;

    /*
        输出内容    运行标记    下一个标记
        a           1          2
        b           2          3
        c           3          1
     */
    public void print(String str, int waitFlag, int nextFlag) {
        for (int i = 0; i < loopNumber; i++) {
            synchronized (this) {
                while (waitFlag != this.flag) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.print(str);
                this.flag = nextFlag;
                this.notifyAll();
            }
        }
    }
}
```



### 2、await/signal版本

```java
/**
 * 使用await/signal来实现三个线程交替打印abcabcabcabcabc
 *
 */
@Slf4j(topic = "TestWaitNotify")
public class TestAwaitSignal {
    public static void main(String[] args) throws InterruptedException {
        AwaitSignal awaitSignal = new AwaitSignal(5);
        Condition a_condition = awaitSignal.newCondition();
        Condition b_condition = awaitSignal.newCondition();
        Condition c_condition = awaitSignal.newCondition();

        new Thread(() -> {
            awaitSignal.print("a", a_condition, b_condition);
        }, "a").start();

        new Thread(() -> {
            awaitSignal.print("b", b_condition, c_condition);
        }, "b").start();

        new Thread(() -> {
            awaitSignal.print("c", c_condition, a_condition);
        }, "c").start();

        Thread.sleep(1000);
        System.out.println("==========开始=========");
        awaitSignal.lock();
        try {
            a_condition.signal();  //首先唤醒a线程
        } finally {
            awaitSignal.unlock();
        }
    }
}

class AwaitSignal extends ReentrantLock {
    private final int loopNumber;

    public AwaitSignal(int loopNumber) {
        this.loopNumber = loopNumber;
    }

    public void print(String str, Condition condition, Condition next) {
        for (int i = 0; i < loopNumber; i++) {
            lock();
            try {
                try {
                    condition.await();
                    //System.out.print("i:==="+i);
                    System.out.print(str);
                    next.signal();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                unlock();
            }
        }
    }
}
```



### 3、LockSupport的park/unpark实现

```java
/**
 * 使用park/unpark来实现三个线程交替打印abcabcabcabcabc
 */
@Slf4j(topic = "TestWaitNotify")
public class TestParkUnpark {
    static Thread a;
    static Thread b;
    static Thread c;

    public static void main(String[] args) {
        ParkUnpark parkUnpark = new ParkUnpark(5);

        a = new Thread(() -> {
            parkUnpark.print("a", b);
        }, "a");

        b = new Thread(() -> {
            parkUnpark.print("b", c);
        }, "b");

        c = new Thread(() -> {
            parkUnpark.print("c", a);
        }, "c");

        a.start();
        b.start();
        c.start();

        LockSupport.unpark(a);

    }
}

class ParkUnpark {
    private final int loopNumber;

    public ParkUnpark(int loopNumber) {
        this.loopNumber = loopNumber;
    }

    public void print(String str, Thread nextThread) {
        for (int i = 0; i < loopNumber; i++) {
            LockSupport.park();
            System.out.print(str);
            LockSupport.unpark(nextThread);
        }
    }
}
```





## 五、本章小结

![20201223172500153](../../../../../Code/%E7%AC%94%E8%AE%B0/Images/JUC/20201223172500153.png)

![20201223172527523](../../../../../Code/%E7%AC%94%E8%AE%B0/Images/JUC/20201223172527523.png)



























