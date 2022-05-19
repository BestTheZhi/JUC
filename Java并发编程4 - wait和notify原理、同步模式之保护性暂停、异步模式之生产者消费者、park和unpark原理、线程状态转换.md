## 一、 wait和notify

### 1、小故事

![20201220084049915](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201220084049915.png)

![20201220084213239](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201220084213239.png)



### 2、wait、notify介绍 (必须要获取到锁对象, 才能调用这些方法)

![20201220084652893](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201220084652893.png)



当 线程0 获得到了锁, 成为Monitor的Owner, 但是此时它发现自己想要执行synchroized代码块的条件不满足; 此时它就调用`obj.wait()`方法, 进入到Monitor中的WaitSet集合, 此时线程0的状态就变为`WAITING`。

处于BLOCKED和WAITING状态的线程都为阻塞状态，CPU都不会分给他们时间片。但是有所区别：

- BLOCKED状态的线程是在竞争锁对象时，发现Monitor的Owner已经是别的线程了，此时就会**进入EntryList中，并处于BLOCKED状态**
- WAITING状态的线程是获得了对象的锁，但是自身的原因无法执行synchroized的临界区资源需要进入阻塞状态时，锁对象调用了wait方法而**进入了WaitSet中，处于WAITING状态**

处于BLOCKED状态的线程会在锁被释放的时候被唤醒

处于WAITING状态的线程只有被锁对象调用了notify方法(obj.notify/obj.notifyAll)，才会被唤醒。然后它会进入到EntryList, 重新竞争锁。



### 3、API介绍

下面的三个方法都是Object中的方法; 通过锁对象来调用

- `wait()`: 让获得对象锁的线程到waitSet中一直等待
- `wait(long n)` : 当该等待线程没有被notify, 等待时间到了之后, 也会自动唤醒
- `notify()`: 让获得对象锁的线程, 使用锁对象调用notify去waitSet的等待线程中挑一个唤醒
- `notifyAll() `: 让获得对象锁的线程, 使用锁对象调用notifyAll去唤醒waitSet中所有的等待线程

它们都是线程之间进行协作的手段, 都属于Object对象的方法, 必须获得此对象的锁, 才能调用这些方法。

**只有当线程 获得到锁，才能调用锁对象的wait()、notify()**，不然就会抛出`IllegalMonitorStateException`



演示wait和notify方法:

```java
@Slf4j
public class WaitNotifyTest {
    static final Object obj = new Object();

    public static void main(String[] args) throws Exception {

        new Thread(() -> {
            synchronized (obj) {
                log.debug("执行...");
                try {
                    // 只有获得锁对象之后, 才能调用wait/notify
                    obj.wait(); // 此时t1线程进入WaitSet等待
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.debug("其它代码...");
            }
        }, "t1").start();

        new Thread(() -> {
            synchronized (obj) {
                log.debug("执行...");
                try {
                    obj.wait(); // 此时t2线程进入WaitSet等待
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.debug("其它代码...");
            }
        }, "t2").start();

        // 让主线程等两秒在执行,为了`唤醒`,不睡的话,那两个线程还没进入waitSet,主线程就开始唤醒了
        Thread.sleep(1000);
        log.debug("唤醒waitSet中的线程!");
        // 只有获得锁对象之后, 才能调用wait/notify
        synchronized (obj) {
            // obj.notify(); // 唤醒waitset中的一个线程
             obj.notifyAll(); // 唤醒waitset中的全部等待线程
        }
    }
}

13:01:36.176  [t1] - 执行...
13:01:36.178  [t2] - 执行...
13:01:37.175  [main] - 唤醒waitSet中的线程!
13:01:37.175  [t2] - 其它代码...
13:01:37.175  [t1] - 其它代码...
```



### 4、Sleep(long n) 和 Wait(long n)的区别

不同点:

- Sleep是Thread类的静态方法，Wait是Object的方法，Object又是所有类的父类，所以所有类都有Wait方法。
- Sleep在阻塞的时候不会释放锁，而Wait在阻塞的时候会释放锁。
- Sleep方法不需要与synchronized一起使用，而Wait方法需要与synchronized一起使用。

相同点

- 阻塞状态都为TIMED_WAITING (限时等待)



### 5、wait/notify的正确使用

Step 1 : (逐步优化)

```java
@Slf4j
public class WaitNotifyTest {
    static final Object room = new Object();
    static boolean hasCigarette = false;
    static boolean hasTakeout = false;

    public static void main(String[] args) {
        //思考下面的解决方案好不好，为什么？
        new Thread(() -> {
            synchronized (room) {
                log.debug("有烟没？[{}]", hasCigarette);
                if (!hasCigarette) {
                    log.debug("没烟，先歇会！");
                    Sleeper.sleep(2);   // 会阻塞2s, 不会释放锁
                }
                log.debug("有烟没？[{}]", hasCigarette);
                if (hasCigarette) {
                    log.debug("可以开始干活了");
                }
            }
        }, "小南").start();

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                synchronized (room) {
                    log.debug("可以开始干活了");
                }
            }, "其它人").start();
        }

        Sleeper.sleep(1);
        new Thread(() -> {
            hasCigarette = true;
            log.debug("烟到了噢！");
        }, "送烟的").start();
    }
}
```

1.其他干活的线程，都要一直阻塞，效率低

2.小南线程必须睡足2s才能醒来，就算烟提前送达，也没有效果。



Step2:

```java
@Slf4j
public class WaitNotifyTest {
    static final Object room = new Object();
    static boolean hasCigarette = false;
    static boolean hasTakeout = false;

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (room) {
                log.debug("有烟没？[{}]", hasCigarette);
                if (!hasCigarette) {
                    log.debug("没烟，先歇会！");
                    try {
                        room.wait(); // 此时进入到waitset等待集合, 同时会释放锁
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("有烟没？[{}]", hasCigarette);
                if (hasCigarette) {
                    log.debug("可以开始干活了");
                }
            }
        }, "小南").start();

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                // 小南进入等待状态了, 其他线程就可以获得锁了
                synchronized (room) {
                    log.debug("可以开始干活了");
                }
            }, "其它人").start();
        }

        Sleeper.sleep(1);
        new Thread(() -> {
            synchronized (room) {
                hasCigarette = true;
                log.debug("烟到了噢！");
                room.notify();
            }
        }, "送烟的").start();
    }
}
```

如果此时除了小南在等待唤醒, 还有一个线程也在等待唤醒呢? 此时的`notify`方法会唤醒谁呢?

- 解决了其他干活线程阻塞的问题
- 但是如果有其多个线程调用了wait()呢？



Step3:

```java
@Slf4j
public class WaitNotifyTest {
    static final Object room = new Object();
    static boolean hasCigarette = false;
    static boolean hasTakeout = false;

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (room) {
                log.debug("有烟没？[{}]", hasCigarette);
                if (!hasCigarette) {
                    log.debug("没烟，先歇会！");
                    try {
                        room.wait(); // 此时进入到waitset等待集合, 同时会释放锁
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("有烟没？[{}]", hasCigarette);
                if (hasCigarette) {
                    log.debug("可以开始干活了");
                }
            }
        }, "小南").start();

        new Thread(() -> {
            synchronized (room) {
                log.debug("外卖送到没？[{}]", hasTakeout);
                if (!hasTakeout) {
                    log.debug("没外卖，先歇会！");
                    try {
                        room.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("外卖送到没？[{}]", hasTakeout);
                if (hasTakeout) {
                    log.debug("可以开始干活了");
                } else {
                    log.debug("没干成活...");
                }
            }
        }, "小女").start();

        Sleeper.sleep(1);
        new Thread(() -> {
            synchronized (room) {
                hasTakeout = true;
                log.debug("外卖到了噢！");
                room.notify();
            }
        }, "送外卖的").start();
    }
}
```

sout:

```java
11:10:39.516 WaitNotifyTest [小南] - 有烟没？[false]
11:10:39.521 WaitNotifyTest [小南] - 没烟，先歇会！
11:10:39.521 WaitNotifyTest [小女] - 外卖送到没？[false]
11:10:39.521 WaitNotifyTest [小女] - 没外卖，先歇会！
11:10:40.521 WaitNotifyTest [送外卖的] - 外卖到了噢！
11:10:40.521 WaitNotifyTest [小南] - 有烟没？[false]
```

问题: 当外卖送到了, 却唤醒了小南线程, 此时就出现了问题

- notify()只能随机唤醒一个waitset中的线程，此时如果有其他线程也在等待，那么就可能唤醒不了正确的线程，这就是【虚假唤醒】
- 解决方法：notifyAll()



Step4:

```java
new Thread(() -> {
 synchronized (room) {
	 hasTakeout = true;
	 log.debug("外卖到了噢！");
	 room.notifyAll();
 }
}, "送外卖的").start();
```

sout:

```java
11:14:53.670 WaitNotifyTest [小南] - 有烟没？[false]
11:14:53.676 WaitNotifyTest [小南] - 没烟，先歇会！
11:14:53.676 WaitNotifyTest [小女] - 外卖送到没？[false]
11:14:53.676 WaitNotifyTest [小女] - 没外卖，先歇会！
11:14:54.674 WaitNotifyTest [送外卖的] - 外卖到了噢！
11:14:54.674 WaitNotifyTest [小女] - 外卖送到没？[true]
11:14:54.674 WaitNotifyTest [小女] - 可以开始干活了
11:14:54.675 WaitNotifyTest [小南] - 有烟没？[false]
```

还是唤醒了小南, 小南还是回去看看送来的是外卖还是烟. 很麻烦, 怎么解决?

![20201220111655619](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201220111655619.png)



Step5:

使用while循环来解决:(该唤醒还是唤醒，只是不向后执行)

```java
@Slf4j
public class Main {
    static final Object room = new Object();
    static boolean hasCigarette = false;
    static boolean hasTakeout = false;

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (room) {
                log.debug("有烟没？[{}]", hasCigarette);
                while (!hasCigarette) {
                    log.debug("没烟，先歇会！");
                    try {
                        room.wait(); // 此时进入到waitset等待集合, 同时会释放锁
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("有烟没？[{}]", hasCigarette);
                if (hasCigarette) {
                    log.debug("可以开始干活了");
                }
            }
        }, "小南").start();

        new Thread(() -> {
            synchronized (room) {
                log.debug("外卖送到没？[{}]", hasTakeout);
                while (!hasTakeout) {
                    log.debug("没外卖，先歇会！");
                    try {
                        room.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.debug("外卖送到没？[{}]", hasTakeout);
                if (hasTakeout) {
                    log.debug("可以开始干活了");
                } else {
                    log.debug("没干成活...");
                }
            }
        }, "小女").start();

        Sleeper.sleep(1);
        new Thread(() -> {
            synchronized (room) {
                hasTakeout = true;
                log.debug("外卖到了噢！");
                room.notifyAll();
            }
        }, "送外卖的").start();
    }
}

```

因为改为while如果唤醒之后, 就在while循环中执行了, 不会跑到while外面去执行"有烟没…", 此时小南就不需要每次notify, 就去看是不是送来的烟, 如果是烟, while就为false了.



## 二、同步模式之保护性暂停 (join、Future的实现)

即Guarded Suspension，用在一个线程等待另一个线程的执行结果

- 有一个结果需要从一个线程传递到另一个线程，让他们关联同一个 GuardedObject
- 如果有结果不断从一个线程到另一个线程 那么可以使用消息队列（见生产者/消费者）
- JDK 中，join 的实现、Future 的实现，采用的就是此模式
- 因为要等待另一方的结果，因此归类到同步模式

![28c9](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/28c9.png)



一方等待另一方的执行结果举例 :

举例, 线程1等待线程2下载的结果,并获取该结果

```java
/**
 * 多线程同步模式 - 一个线程需要等待另一个线程的执行结果
 */
@Slf4j
public class GuardeObjectTest {
    public static void main(String[] args) {
        // 线程1等待线程2的下载结果
        GuardeObject guardeObject = new GuardeObject();
        new Thread(() -> {
            log.debug("等待结果");
            Object obj = guardeObject.get();
            log.debug("下载结果是：{}",obj.toString());
        }, "t1").start();

        new Thread(() -> {
            log.debug("执行下载");
            try {
                //
                Thread.sleep(2000);
                guardeObject.complete(new String("下载完成"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, "t2").start();
    }
}

class GuardeObject {
    // 结果
    private Object response;

    // 获取结果
    public Object get() {
        synchronized (this) {
            // 没有结果
            while (response == null) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return response;
        }
    }

    // 产生结果
    public void complete(Object response) {
        synchronized (this) {
            // 给结果变量赋值
            this.response = response;
            this.notifyAll();
        }
    }
}
```



线程t1 等待 线程t2的结果, 可以设置超时时间, 如果超过时间还没返回结果,此时就不等了.退出while循环:

```java
@Slf4j
public class GuardeObjectTest {
    public static void main(String[] args) {
        // 线程1等待线程2的下载结果
        GuardeObject guardeObject = new GuardeObject();
        new Thread(() -> {
            log.debug("begin");
            Object obj = guardeObject.get(2000);
            log.debug("结果是:{}", obj);
        }, "t1").start();

        new Thread(() -> {
            log.debug("begin");
            try {
                 Thread.sleep(1000); // 在等待时间内
//                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            guardeObject.complete(new Object());
        }, "t2").start();
    }
}

class GuardeObject {
    // 结果
    private Object response;

    // 获取结果
    // timeout表示等待多久. 这里假如是2s
    public Object get(long timeout) {
        synchronized (this) {
            // 假如开始时间为 15:00:00
            long begin = System.currentTimeMillis();
            // 经历的时间
            long passedTime = 0;
            while (response == null) {
                // 这一轮循环应该等待的时间
                long waitTime = timeout - passedTime;
                // 经历的时间超过了最大等待时间, 退出循环
                if (waitTime <= 0) {
                    break;
                }
                try {
                    //这里可能会出现虚假唤醒，所以等待时间不应该为timeout而是waitTime
                    this.wait(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 经历时间
                passedTime = System.currentTimeMillis() - begin; // 15:00:02
            }
            return response;
        }
    }

    // 产生结果
    public void complete(Object response) {
        synchronized (this) {
            // 给结果变量赋值
            this.response = response;
            this.notifyAll();
        }
    }
}
```

sout:

```java
// 在等待时间内的情况
16:20:41.627 GuardeObjectTest [t1] - begin
16:20:41.627 GuardeObjectTest [t2] - begin
16:20:42.633 GuardeObjectTest [t1] - 结果是:java.lang.Object@1e1d0168

// 超时的情况
16:21:24.663 GuardeObjectTest [t2] - begin
16:21:24.663 GuardeObjectTest [t1] - begin
16:21:26.667 GuardeObjectTest [t1] - 结果是:null
```



关于超时的增强，在join(long millis) 的源码中得到了体现：

```java
/*
 * 假设：在main线程中，调用t1.join()
 */
public final synchronized void join(long millis)  //锁的是对象t1
throws InterruptedException {
    long base = System.currentTimeMillis();
    long now = 0;

    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (millis == 0) {
        //防止main线程被打断
        while (isAlive()) {
            //只要t1线程还存活，main线程，在t1的WaitSet中等待
            wait(0);  //异常从方法上抛出
        }
    } else {
    // join一个指定的时间
        while (isAlive()) {
            long delay = millis - now;
            if (delay <= 0) {
                break;
            }
            wait(delay);   //异常从方法上抛出
            now = System.currentTimeMillis() - base;
        }
    }
}
```



多任务版GuardedObject图中 Futures 就好比居民楼一层的信箱（每个信箱有房间编号），左侧的 t0，t2，t4 就好比等待邮件的居民，右侧的 t1，t3，t5 就好比邮递员如果需要在多个类之间使用 GuardedObject 对象，作为参数传递不是很方便，因此设计一个用来解耦的中间类。

不仅能够解耦【结果等待者】和【结果生产者】，还能够同时支持多个任务的管理。和生产者消费者模式的区别就是：这个产生结果的线程和使用结果的线程是一一对应的关系，但是生产者消费者模式并不是。

rpc框架的调用中就使用到了这种模式

![d03c](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/d03c.png)

```java
/**
 * Description: 同步模式保护性暂停模式 (多任务版)
 */
@Slf4j(topic = "GuardedObjectTest")
public class GuardedObjectTest {
    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            new People().start();
        }
        Sleeper.sleep(1);
        for (Integer id : Mailboxes.getIds()) {
            new Postman(id, "内容" + id).start();
        }
    }
}

@Slf4j(topic = "People")
class People extends Thread {
    @Override
    public void run() {
        // 收信
        GuardedObject guardedObject = Mailboxes.createGuardedObject();
        log.debug("开始收信 id:{}", guardedObject.getId());
        Object mail = guardedObject.get(5000);
        log.debug("收到信 id:{}, 内容:{}", guardedObject.getId(), mail);
    }
}

@Slf4j(topic = "Postman")
// 邮寄员类
class Postman extends Thread {
    private int id;
    private String mail;

    public Postman(int id, String mail) {
        this.id = id;
        this.mail = mail;
    }

    @Override
    public void run() {
        GuardedObject guardedObject = Mailboxes.getGuardedObject(id);
        log.debug("送信 id:{}, 内容:{}", id, mail);
        guardedObject.complete(mail);
    }
}

// 信箱类
class Mailboxes {
    private static Map<Integer, GuardedObject> boxes = new Hashtable<>();

    private static int id = 1;

    // 产生唯一 id
    private static synchronized int generateId() {
        return id++;
    }

    public static GuardedObject getGuardedObject(int id) {
        return boxes.remove(id);
    }

    public static GuardedObject createGuardedObject() {
        GuardedObject go = new GuardedObject(generateId());
        boxes.put(go.getId(), go);
        return go;
    }

    public static Set<Integer> getIds() {
        return boxes.keySet();
    }
}

// 用来传递信息的作用, 当多个类使用GuardedObject,就很不方便,此时需要一个设计一个解耦的中间类
class GuardedObject {
    // 标记GuardedObject
    private int id;
    // 结果
    private Object response;

    public int getId() {
        return id;
    }

    public GuardedObject(int id) {
        this.id = id;
    }

    // 获取结果
    // timeout表示等待多久. 这里假如是2s
    public Object get(long timeout) {
        synchronized (this) {
            // 假如开始时间为 15:00:00
            long begin = System.currentTimeMillis();
            // 经历的时间
            long passedTime = 0;
            while (response == null) {
                // 这一轮循环应该等待的时间
                long waitTime = timeout - passedTime;
                // 经历的时间超过了最大等待时间, 退出循环
                if (waitTime <= 0) {
                    break;
                }
                try {
                    this.wait(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 经历时间
                passedTime = System.currentTimeMillis() - begin; // 15:00:02
            }
            return response;
        }
    }

    // 产生结果
    public void complete(Object response) {
        synchronized (this) {
            // 给结果变量赋值
            this.response = response;
            this.notifyAll();
        }
    }
}

```



## 三、异步模式之生产者/消费者

与前面的保护性暂停中的 GuardedObject 不同，不需要产生结果和消费结果的线程一一对应 (一个生产一个消费)

消费队列 可以用来平衡生产和消费的线程资源

生产者仅负责产生结果数据，不关心数据该如何处理，而消费者专心处理结果数据

消息队列是有容量限制的，满时不会再加入数据，空时不会再消耗数据

JDK 中各种 阻塞队列，采用的就是这种模式



异步模式中, 生产者产生消息之后消息没有被立刻消费
同步模式中, 消息在产生之后被立刻消费了。

![b27b](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/b27b.png)



我们下面写的小例子是线程间通信的消息队列，要注意区别,像RabbitMQ等消息框架是进程间通信的。

```java
/**
 * Description: 异步模式之生产者/消费者
 */
@Slf4j(topic = "ProductConsumerTest")
public class ProductConsumerTest {
    public static void main(String[] args) {
        MessageQueue queue = new MessageQueue(2);

        for (int i = 0; i < 3; i++) {
            int id = i;
            new Thread(() -> {
                queue.put(new Message(id, "值" + id));
            }, "生产者" + i).start();
        }

        new Thread(() -> {
            while (true) {
                Sleeper.sleep(1);
                Message message = queue.take();
            }
        }, "消费者").start();
    }

}

// 消息队列类,在线程之间通信
@Slf4j(topic = "MessageQueue")
class MessageQueue {
    // 消息的队列集合
    private LinkedList<Message> list = new LinkedList<>();
    // 队列容量
    private int capcity;

    public MessageQueue(int capcity) {
        this.capcity = capcity;
    }

    // 获取消息
    public Message take() {
        // 检查队列是否为空
        synchronized (list) {
            while (list.isEmpty()) {
                try {
                    log.debug("队列为空, 消费者线程等待");
                    list.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 从队列头部获取消息并返回
            Message message = list.removeFirst();
            log.debug("已消费消息 {}", message);
            list.notifyAll();
            return message;
        }
    }

    // 存入消息
    public void put(Message message) {
        synchronized (list) {
            // 检查对象是否已满
            while (list.size() == capcity) {
                try {
                    log.debug("队列已满, 生产者线程等待");
                    list.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 将消息加入队列尾部
            list.addLast(message);
            log.debug("已生产消息 {}", message);
            list.notifyAll();
        }
    }
}

final class Message {
    private int id;
    private Object value;

    public Message(int id, Object value) {
        this.id = id;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", value=" + value +
                '}';
    }
}
```

sout

```java
18:52:53.440 MessageQueue [生产者1] - 已生产消息 Message{id=1, value=值1}
18:52:53.443 MessageQueue [生产者0] - 已生产消息 Message{id=0, value=值0}
18:52:53.444 MessageQueue [生产者2] - 队列已满, 生产者线程等待
18:52:54.439 MessageQueue [消费者] - 已消费消息 Message{id=1, value=值1}
18:52:54.439 MessageQueue [生产者2] - 已生产消息 Message{id=2, value=值2}
18:52:55.439 MessageQueue [消费者] - 已消费消息 Message{id=0, value=值0}
18:52:56.440 MessageQueue [消费者] - 已消费消息 Message{id=2, value=值2}
18:52:57.441 MessageQueue [消费者] - 队列为空, 消费者线程等待
```



## 四、 park & unpack

### 1、基本使用

- park/unpark都是LockSupport类中的的方法
- 先调用unpark后,再调用park, 此时park不会暂停线程

```java
// 暂停当前线程
LockSupport.park();
// 恢复某个线程的运行
LockSupport.unpark(thread);
```



### 2、 park、 unpark 原理

每个线程都有自己的一个 Parker 对象，由三部分组成 _counter， _cond和 _mutex

- 打个比喻线程就像一个旅人，Parker 就像他随身携带的背包，条件变量 _ cond就好比背包中的帐篷。_counter 就好比背包中的备用干粮（0 为耗尽，1 为充足）
- 调用 park 就是要看需不需要停下来歇息
  - 如果备用干粮耗尽，那么钻进帐篷歇息
  - 如果备用干粮充足，那么不需停留，继续前进
- 调用 unpark，就好比令干粮充足
- 如果这时线程还在帐篷，就唤醒让他继续前进
- 如果这时线程还在运行，那么下次他调用 park 时，仅是消耗掉备用干粮，不需停留继续前进
- 因为背包空间有限，多次调用 unpark 仅会补充一份备用干粮



#### 先调用park再调用upark的过程

先调用park的情况

- 当前线程调用 Unsafe.park() 方法
- 检查 _counter, 本情况为0, 这时, 获得_mutex 互斥锁(mutex对象有个等待队列 _cond)
- 线程进入 _cond 条件变量阻塞
- 设置_counter = 0 (没干粮了)

![0d5e](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/0d5e.png) 

调用unpark

- 调用Unsafe.unpark(Thread_0)方法，设置_counter 为 1
- 唤醒 _cond 条件变量中的 Thread_0
- Thread_0 恢复运行
- 设置 _counter 为 0

![bd6](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/bd6.png) 



#### 先调用unpark再调用park的过程

- 调用 Unsafe.unpark(Thread_0)方法，设置 _counter 为 1
- 当前线程调用 Unsafe.park() 方法
- 检查 _counter，本情况为 1，这时线程 无需阻塞，继续运行
- 设置 _counter 为 0

![4413](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/4413.png) 



## 五、 线程状态转换 

![20210129171228140](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20210129171228140.png)



![20201221214359753](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201221214359753.png)



假设有线程 Thread t

- 1、NEW <–> RUNNABLE
  - t.start()方法时, NEW --> RUNNABLE
- 2、RUNNABLE <–> WAITING
  - 线程用synchronized(obj)获取了对象锁后
  - 调用 obj.wait()方法时，t 线程进入waitSet中, 从RUNNABLE --> WAITING
  - 调用 obj.notify()，obj.notifyAll()，t.interrupt() 时, 唤醒的线程都到entrySet阻塞队列成为BLOCKED状态, 在阻塞队列,和其他线程再进行 竞争锁
    - 竞争锁成功，t 线程从 WAITING --> RUNNABLE
    - 竞争锁失败，t 线程从 WAITING --> BLOCKED
- 3、RUNNABLE <–> WAITING
  - 当前线程调用 t.join() 方法时，当前线程从 RUNNABLE --> WAITING ,注意是当前线程在t线程对象在waitSet上等待
  - t 线程运行结束，或调用了当前线程的 interrupt() 时，当前线程从 WAITING --> RUNNABLE
- 4、RUNNABLE <–> WAITING
  - 当前线程调用 LockSupport.park() 方法会让当前线程从RUNNABLE --> WAITING
  - 调用 LockSupport.unpark(目标线程) 或**调用了线程 的 interrupt()** ，会让目标线程从 WAITING --> RUNNABLE
- 5、RUNNABLE <–> TIMED_WAITING (带超时时间的wait)
  - t 线程用synchronized(obj) 获取了对象锁后
  - 调用 obj.wait(long n) 方法时，t 线程从 RUNNABLE --> TIMED_WAITING
  - t 线程等待时间超过了 n 毫秒，或调用 obj.notify() ， obj.notifyAll() ， t.interrupt() 时; 唤醒的线程都到entrySet阻塞队列成为BLOCKED状态, 在阻塞队列,和其他线程再进行 竞争锁
    - 竞争锁成功，t 线程从 TIMED_WAITING --> RUNNABLE
    - 竞争锁失败，t 线程从 TIMED_WAITING --> BLOCKED
- 6、RUNNABLE <–> TIMED_WAITING
  - 当前线程调用 t.join(long n) 方法时，当前线程从 RUNNABLE --> TIMED_WAITING 注意是当前线程在t 线程对象的waitSet等待
  - 当前线程等待时间超过了 n 毫秒，或t 线程运行结束，或调用了当前线程的 interrupt() 时，当前线程从 TIMED_WAITING --> RUNNABLE
- 7、RUNNABLE <–> TIMED_WAITING
  - 当前线程调用 Thread.sleep(long n) ，当前线程从 RUNNABLE --> TIMED_WAITING
  - 当前线程等待时间超过了 n 毫秒或调用了线程的 interrupt() ，当前线程从 TIMED_WAITING --> RUNNABLE
- 8、RUNNABLE <–> TIMED_WAITING
  - 当前线程调用 LockSupport.parkNanos(long nanos) 或 LockSupport.parkUntil(long millis) 时，当前线程从 RUNNABLE --> TIMED_WAITING
  - 调用LockSupport.unpark(目标线程) 或**调用了线程 的 interrupt()** ，或是等待超时，会让目标线程从 TIMED_WAITING--> RUNNABLE
- 9、RUNNABLE <–> BLOCKED
  - t 线程用 synchronized(obj) 获取了对象锁时如果竞争失败，从 RUNNABLE –> BLOCKED, 持 obj 锁线程的同步代码块执行完毕，会唤醒该对象上所有 BLOCKED 的线程重新竞争，如果其中 t 线程竞争 成功，从 BLOCKED –> RUNNABLE ，其它失败的线程仍然 BLOCKED
- 10、 RUNNABLE <–> TERMINATED
  - 当前线程所有代码运行完毕，进入 TERMINATED





