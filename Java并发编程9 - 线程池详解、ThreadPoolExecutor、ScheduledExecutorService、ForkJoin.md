# 一、 共享模型之工具

## 一、线程池

>池化技术有很多, 比如线程池、数据库连接池、HTTP连接池等等都是对这个思想的应用。池化技术的思想主要是为了减少每次获取资源的消耗，提高对资源的利用率。

线程池提供了一种 限制和管理资源（包括执行一个任务）。 每个线程池还维护一些基本统计信息，例如已完成任务的数量。

使用线程池的好处：

- 降低资源消耗。通过重复利用已创建的线程降低线程创建和销毁造成的消耗。(创建的线程,实际最后要和操作系统的线程做映射,很消耗资源)
- 提高响应速度。当任务到达时，任务可以不需要等到线程创建就能立即执行。
- 提高线程的可管理性。线程是稀缺资源，如果无限制的创建，不仅会消耗系统资源，还会降低系统的稳定性，使用线程池可以进行统一的分配，调优和监控。



### 1.1、 自定义一个简单的线程池

![6cdb](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/6cdb.png) 

- 阻塞队列中维护了由主线程（或者其他线程）所产生的的任务
- 主线程类似于生产者，产生任务并放入阻塞队列中
- 线程池类似于消费者，得到阻塞队列中已有的任务并执行



自定义线程池的实现步骤 :

- 步骤1：自定义拒绝策略接口
- 步骤2：自定义任务阻塞队列
- 步骤3：自定义线程池
- 步骤4：测试

```java
@Slf4j(topic = "TestPool")
public class TestPool {
    public static void main(String[] args) {
        ThreadPool threadPool = new ThreadPool(1, 1000, TimeUnit.MILLISECONDS, 1, new RejectPolicy<Runnable>() {
            @Override
            public void reject(BlockingQueue<Runnable> queue, Runnable task) {
                // 拒绝策略
                // 1、死等
                // queue.put(task);

                // 2、带超时等待
//                queue.offer(task, 2000, TimeUnit.MILLISECONDS);

                // 3、让调用者放弃任务执行
                // log.debug("放弃-{}", task);

                // 4、让调用者抛弃异常
                // throw new RuntimeException("任务执行失败" + task);

                // 5、让调用者自己执行任务
                 task.run();
            }
        });
        // 创建5个任务
        for (int i = 0; i <= 4; i++) {
            int j = i;
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    log.debug("{}", j);
                }
            });
        }
    }
}

@FunctionalInterface
interface RejectPolicy<T> {
    void reject(BlockingQueue<T> queue, T task);
}


/**
 * 线程池
 */
@Slf4j(topic = "TestPool")
class ThreadPool {

    // 阻塞任务队列
    private BlockingQueue<Runnable> taskQueue;

    // 线程集合
    private HashSet<Worker> workers = new HashSet<>();

    // 核心线程数
    private int coreSize;

    // 获取任务的超时时间
    private long timeout;

    private TimeUnit timeUnit;

    // 拒绝策略
    private RejectPolicy<Runnable> rejectPolicy;

    public ThreadPool(int coreSize, long timeout, TimeUnit timeUnit, int queueCapacity, RejectPolicy<Runnable> rejectPolicy) {
        this.coreSize = coreSize;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.taskQueue = new BlockingQueue<>(queueCapacity);
        this.rejectPolicy = rejectPolicy;
    }

    // 执行任务
    public void execute(Runnable task) {
        synchronized (workers) {
            // 当任务没有超过线程数, 说明当前worker线程可以消费这些任务, 不用将任务加入到阻塞队列中
            if (workers.size() < coreSize) {
                Worker worker = new Worker(task);
                log.debug("新增 worker {}, {}", worker, task);
                workers.add(worker);
                worker.start();
            } else {
                // taskQueue.put(task); // 这一种死等
                // 拒绝策略
                // 1、死等
                // 2、带超时等待
                // 3、让调用者放弃任务执行
                // 4、让调用者抛弃异常
                // 5、让调用者自己执行任务
                taskQueue.tryPut(rejectPolicy, task);

            }
        }
    }

    class Worker extends Thread {
        private Runnable task;

        public Worker(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            // 执行任务
            // 1): 当task不为空, 执行任务
            // 2): 当task执行完毕, 从阻塞队列中获取任务并执行
            //while (task != null || (task = taskQueue.take()) != null) {
            while (task != null || (task = taskQueue.poll(timeout, timeUnit)) != null) {
                try {
                    log.debug("正在执行...{}", task);
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    task = null;
                }
            }

            // 将线程集合中的线程移除
            synchronized (workers) {
                log.debug("worker被移除 {}", this);
                workers.remove(this);
            }
        }
    }

}

/**
 * 用于存放任务的阻塞队列
 *
 * @param <T> Runnable, 任务抽象为Runnable
 */
@Slf4j(topic = "TestPool")
class BlockingQueue<T> {
    // 1、任务队列
    private Deque<T> queue = new ArrayDeque<>();

    // 2、锁
    private ReentrantLock lock = new ReentrantLock();

    // 3、生产者的条件变量 (当阻塞队列塞满任务的时候, 没有空间, 此时进入条件变量中等待)
    private Condition fullWaitSet = lock.newCondition();

    // 4、消费者的条件变量 (当没有任务可以消费的时候, 进入条件变量中等待)
    private Condition emptyWaitSet = lock.newCondition();

    // 5、阻塞队列的容量
    private int capacity;

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }


    // 从阻塞队列中获取任务, 如果没有任务, 会等待指定的时间
    public T poll(long timeout, TimeUnit unit) {
        lock.lock();
        try {
            // 将timeout统一转换为纳秒
            long nanos = unit.toNanos(timeout);
            while (queue.isEmpty()) {
                try {
                    // 表示超时, 无需等待, 直接返回null
                    if (nanos <= 0) {
                        return null;
                    }
                    // 返回值的时间(剩余时间) = 等待时间 - 经过时间 所以不存在虚假唤醒(时间还没等够就被唤醒,然后又从新等待相同时间)
                    nanos = emptyWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T t = queue.removeFirst();
            fullWaitSet.signal(); // 唤醒生产者进行生产, 此时阻塞队列没有满
            return t;
        } finally {
            lock.unlock();
        }
    }

    // 从阻塞队列中获取任务, 如果没有任务,会一直等待
    public T take() {
        lock.lock();
        try {
            // 阻塞队列是否为空
            while (queue.isEmpty()) {
                // 进入消费者的条件变量中等待,此时没有任务供消费
                try {
                    emptyWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 阻塞队列不为空, 获取队列头部任务
            T t = queue.removeFirst();
            fullWaitSet.signal(); // 唤醒生产者进行生产, 此时阻塞队列没有满
            return t;
        } finally {
            lock.unlock();
        }
    }

    // 往阻塞队列中添加任务  满 即阻塞等待
    public void put(T task) {
        lock.lock();
        try {
            // 阻塞队列是否满了
            while (queue.size() == capacity) {
                try {
                    log.debug("等待进入阻塞队列...");
                    fullWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            queue.addLast(task);
            log.debug("加入任务阻塞队列 {}", task);
            emptyWaitSet.signal(); // 此时阻塞队列中有任务了, 唤醒消费者进行消费任务
        } finally {
            lock.unlock();
        }
    }

    // 往阻塞队列中添加任务(带超时)
    public boolean offer(T task, long timeout, TimeUnit timeUnit) {
        lock.lock();
        try {
            long nanos = timeUnit.toNanos(timeout);
            while (queue.size() == capacity) {
                try {
                    if (nanos <= 0) {
                        return false;
                    }
                    log.debug("等待进入阻塞队列 {}...", task);
                    nanos = fullWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.debug("加入任务阻塞队列 {}", task);
            queue.addLast(task);
            emptyWaitSet.signal(); // 此时阻塞队列中有任务了, 唤醒消费者进行消费任务
            return true;
        } finally {
            lock.unlock();
        }
    }

    // 获取队列大小
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    public void tryPut(RejectPolicy<T> rejectPolicy, T task) {
        lock.lock();
        try {
            // 判断队列是否满
            if (queue.size() == capacity) {
                rejectPolicy.reject(this, task);
            } else {
                // 有空闲
                log.debug("加入任务队列 {}", task);
                queue.addLast(task);
                emptyWaitSet.signal();
            }
        } finally {
            lock.unlock();
        }
    }
}
```



实现了一个简单的线程池：

- 阻塞队列BlockingQueue用于暂存来不及被线程执行的任务
  - 也可以说是平衡生产者和消费者执行速度上的差异
  - 里面的获取任务和放入任务用到了 生产者消费者模式
- 线程池中对线程Thread进行了再次的封装，封装为了Worker
  - 在调用 任务对象 (Runnable、Callable) 的run方法时，线程会去执行该任务，执行完毕后还会到阻塞队列中获取新任务来执行
- 线程池中执行任务的主要方法为execute方法
  - 执行时要判断正在执行的线程数是否大于了线程池容量



### 1.2、 ThreadPoolExecutor

#### 1.2.1、线程池的继承关系

![20210102231106611](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20210102231106611.png) 



#### 1.2.2、Executor 框架结构 : 主要由三大部分组成

任务类 (`Runnable` /`Callable`)

- 执行任务需要实现的 `Runnable 接口` 或 `Callable接口`。Runnable 接口或 Callable 接口 实现类都可以被 ThreadPoolExecutor 或 ScheduledThreadPoolExecutor 执行。

任务的执行 (`Executor`)

- 如上图所示，包括任务执行机制的核心接口 Executor ，以及继承自 Executor 接口的 ExecutorService 接口。ThreadPoolExecutor 和 ScheduledThreadPoolExecutor 这两个关键类实现了 ExecutorService 接口。
- 这里有很多底层的类关系，但是，实际上我们需要更多关注的是 ThreadPoolExecutor 这个类，它在我们实际使用线程池的过程中，使用频率非常高。

异步计算的结果 (`Future`)

- Future 接口 以及 Future接口的 实现类 FutureTask 类 都可以代表异步计算的结果。
- 当把 Runnable接口 或 Callable 接口 的实现类提交给 ThreadPoolExecutor 或 ScheduledThreadPoolExecutor 执行。（调用 submit() 方法时会返回一个 FutureTask对象）
  - Futrue和join方法类似, futrue的get方法需要等待线程执行完毕,才可以获取的线程的执行结果。也称之为`保护性暂停`



#### 1.2.3、使用示意

1.主线程首先要创建实现 Runnable 或者 Callable 接口的任务对象。

2.把创建完成的实现 Runnable/Callable接口的对象 直接交给 ExecutorService 执行:

- ExecutorService.execute（Runnable command））
- ExecutorService.submit（Runnable task）
- ExecutorService.submit（Callable <T> task）。

3.如果执行 ExecutorService.submit（…），ExecutorService 将返回一个实现Future接口的对象
（刚刚也提到过了执行 execute()方法和 submit()方法的区别，submit()会返回一个 FutureTask 对象）。

4.最后，主线程可以执行 FutureTask.get()方法来等待任务执行完成。主线程也可以执行 FutureTask.cancel（boolean mayInterruptIfRunning）来取消此任务的执行



### 1.3、 线程池状态

ThreadPoolExecutor 使用 int 的高 3 位来表示线程池状态，低 29 位表示线程数量

```java
// 线程池状态
// runState is stored in the high-order bits
// RUNNING 高3位为111
private static final int RUNNING    = -1 << COUNT_BITS;

// SHUTDOWN 高3位为000
private static final int SHUTDOWN   =  0 << COUNT_BITS;

// 高3位 001
private static final int STOP       =  1 << COUNT_BITS;

// 高3位 010
private static final int TIDYING    =  2 << COUNT_BITS;

// 高3位 011
private static final int TERMINATED =  3 << COUNT_BITS;
```

| 状态名称   | 高3位的值 | 描述                                          |
| ---------- | --------- | :-------------------------------------------- |
| RUNNING    | 111       | 接收新任务，同时处理任务队列中的任务          |
| SHUTDOWN   | 000       | 不接受新任务，但是处理任务队列中的任务        |
| STOP       | 001       | 中断正在执行的任务，同时抛弃阻塞队列中的任务  |
| TIDYING    | 010       | 任务执行完毕，活动线程为0时，即将进入终结阶段 |
| TERMINATED | 011       | 终结状态                                      |

从数字上比较(第一位是符号位)，TERMINATED > TIDYING > STOP > SHUTDOWN > RUNNING

线程池状态和线程池中线程的数量 **由一个原子整型ctl来共同表示**

使用一个数来表示两个值的主要原因是：可以通过一次CAS同时更改两个属性的值

```java
// 原子整数，前3位保存了线程池的状态，剩余位保存的是线程数量
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));

// 并不是所有平台的int都是32位。
// 去掉前三位保存线程状态的位数，剩下的用于保存线程数量
// 高3位为0，剩余位数全为1
private static final int COUNT_BITS = Integer.SIZE - 3;  //32

// 2^COUNT_BITS次方，表示可以保存的最大线程数
// CAPACITY 的高3位为 0
//0001 1111 1111 1111 1111 1111 1111 1111
private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
```

获取线程池状态、线程数量以及合并两个值的操作

```java
// Packing and unpacking ctl
// 获取运行状态
// 该操作会让除高3位以外的数全部变为0
private static int runStateOf(int c)     { return c & ~CAPACITY; }

// 获取运行线程数
// 该操作会让高3位为0
private static int workerCountOf(int c)  { return c & CAPACITY; }
```

这些信息存储在一个原子变量 ctl 中，目的是将线程池状态与线程个数合二为一，这样就可以用一次 cas 原子操作进行赋值

```java
// c 为旧值， ctlOf 返回结果为新值
ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))));

// rs 为高 3 位代表线程池状态， wc 为低 29 位代表线程个数，ctl 是合并它们
private static int ctlOf(int rs, int wc) { return rs | wc; }
```



### 1.4、线程池的属性

```java
// 工作线程，内部封装了Thread
private final class Worker
        extends AbstractQueuedSynchronizer
        implements Runnable {
    ...
}

// 阻塞队列，用于存放来不及被核心线程执行的任务
private final BlockingQueue<Runnable> workQueue;

// 锁
private final ReentrantLock mainLock = new ReentrantLock();

//  用于存放核心线程的容器，只有当持有锁时才能够获取其中的元素（核心线程）
private final HashSet<Worker> workers = new HashSet<Worker>();

```



### 1.5、 构造方法及参数

ThreadPoolExecutor最全的构造方法,也是构造自定义线程池的方法

```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler)
```

参数解释 :

- corePoolSize：核心线程数
- maximumPoolSize：最大线程数
  - maximumPoolSize - corePoolSize = 救急线程数
  - 注意 : 救急线程在没有空闲的核心线程和任务队列满了的情况才使用救急线程
- keepAliveTime：救急线程空闲时的最大生存时间 (核心线程可以一直运行)
- unit：时间单位 (针对救急线程)
- workQueue：阻塞队列（存放任务）
  - 有界阻塞队列 ArrayBlockingQueue
  - 无界阻塞队列 LinkedBlockingQueue
  - 最多只有一个同步元素的 SynchronousQueue
  - 优先队列 PriorityBlockingQueue
- threadFactory：线程工厂（给线程取名字）
- handler：拒绝策略



工作方式:

![Inked20210202214622633_LI](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/Inked20210202214622633_LI.jpg)

当一个任务传给线程池以后，可能有以下几种可能:

- 将任务分配给一个核心线程来执行
- 核心线程都在执行任务，将任务放到阻塞队列workQueue中等待被执行
- 阻塞队列满了，使用救急线程来执行任务
  - 救急线程用完以后，超过生存时间（keepAliveTime）后会被释放
- 任务总数大于了 最大线程数（maximumPoolSize）与阻塞队列容量的最大值（workQueue.capacity），使用拒接策略



#### 拒绝策略

如果线程到达 maximumPoolSize 仍然有新任务这时会执行拒绝策略。拒绝策略 jdk 提供了 4 种实现

![40c5](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/40c5.png)



1.AbortPolicy 中止策略：丢弃任务并抛出RejectedExecutionException异常。这是默认策略

```tex
这是线程池默认的拒绝策略，在任务不能再提交的时候，抛出异常，及时反馈程序运行状态。如果是比较关键的业务，推荐使用此拒绝策略，这样子在系统不能承载更大的并发量的时候，能够及时的通过异常发现。
`功能`：当触发拒绝策略时，直接抛出拒绝执行的异常，中止策略的意思也就是打断当前执行流程.
`使用场景`：这个就没有特殊的场景了，但是有一点要正确处理抛出的异常。ThreadPoolExecutor中默认的策略就是AbortPolicy，ExecutorService接口的系列ThreadPoolExecutor因为都没有显示的设置拒绝策略，所以默认的都是这个。但是请注意，ExecutorService中的线程池实例队列都是无界的，也就是说把内存撑爆了都不会触发拒绝策略。当自己自定义线程池实例时，使用这个策略一定要处理好触发策略时抛的异常，因为他会打断当前的执行流程。
```

2.DiscardPolicy 丢弃策略：丢弃任务，但是不抛出异常。如果线程队列已满，则后续提交的任务都会被丢弃，且是静默丢弃。

```tex
使用此策略，可能会使我们无法发现系统的异常状态。建议是一些无关紧要的业务采用此策略。例如，本人的博客网站统计阅读量就是采用的这种拒绝策略。
`功能`：直接静悄悄的丢弃这个任务，不触发任何动作。
`使用场景`：如果你提交的任务无关紧要，你就可以使用它 。因为它就是个空实现，会悄无声息的吞噬你的的任务。所以这个策略基本上不用了。
```

3.DiscardOldestPolicy 弃老策略：丢弃队列最前面的任务，然后重新提交被拒绝的任务。

```tex
此拒绝策略，是一种喜新厌旧的拒绝策略。是否要采用此种拒绝策略，还得根据实际业务是否允许丢弃老任务来认真衡量。
`功能`：如果线程池未关闭，就弹出队列头部的元素，然后尝试执行
`使用场景`：这个策略还是会丢弃任务，丢弃时也是毫无声息，但是特点是丢弃的是老的未执行的任务，而且是待执行优先级较高的任务。基于这个特性，想到的场景就是，发布消息和修改消息，当消息发布出去后，还未执行，此时更新的消息又来了，这个时候未执行的消息的版本比现在提交的消息版本要低就可以被丢弃了。因为队列中还有可能存在消息版本更低的消息会排队执行，所以在真正处理消息的时候一定要做好消息的版本比较。
```

4.CallerRunsPolicy 调用者运行策略：由调用线程处理该任务。

```tex
`功能`：当触发拒绝策略时，只要线程池没有关闭，就由提交任务的当前线程处理。
`使用场景`：一般在不允许失败的、对性能要求不高、并发量较小的场景下使用，因为线程池一般情况下不会关闭，也就是提交的任务一定会被运行，但是由于是调用者线程自己执行的，当多次提交任务时，就会阻塞后续任务执行，性能和效率自然就慢了。
```



>1.线程池中刚开始没有线程，当一个任务提交给线程池后，线程池会创建一个新线程来执行任务。
>2.当线程数达到 corePoolSize 并没有线程空闲，这时再加入任务，新加的任务会被加入workQueue 队列排队，直到有空闲的线程。
>3.如果队列选择了有界队列，那么任务超过了队列大小时，会创建 maximumPoolSize - corePoolSize 数目的线程来救急。
>4.如果线程到达 maximumPoolSize 仍然有新任务这时会执行拒绝策略。拒绝策略 jdk 提供了 下面的前4 种实现，其它著名框架也提供了实现
>
>- ThreadPoolExecutor.AbortPolicy让调用者抛出 RejectedExecutionException 异常，这是默认策略
>- ThreadPoolExecutor.CallerRunsPolicy 让调用者运行任务
>- ThreadPoolExecutor.DiscardPolicy 放弃本次任务
>- ThreadPoolExecutor.DiscardOldestPolicy 放弃队列中最早的任务，本任务取而代之
>- Dubbo 的实现，在抛出 RejectedExecutionException 异常之前会记录日志，并 dump 线程栈信息，方便定位问题
>- Netty 的实现，是创建一个新线程来执行任务
>- ActiveMQ 的实现，带超时等待（60s）尝试放入队列，类似我们之前自定义的拒绝策略
>- PinPoint 的实现，它使用了一个拒绝策略链，会逐一尝试策略链中每种拒绝策略
>
>5.当高峰过去后，超过corePoolSize 的救急线程如果一段时间没有任务做，需要结束节省资源，这个时间由keepAliveTime 和 unit 来控制。



示例：

```java
public class Test2 {

    static AtomicInteger threadId = new AtomicInteger(0);

    public static void main(String[] args) {
        // 手动创建线程池
        // 创建有界阻塞队列, 用来存放任务对象
        ArrayBlockingQueue<Runnable> runnable = new ArrayBlockingQueue<>(10);
        // 创建线程工厂: 主要给线程起名字
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "thezhi_thread" + threadId.getAndIncrement());
            }
        };

        // 手动创建线程池
        // 拒绝策略采用默认策略
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 7, 10, TimeUnit.SECONDS, runnable, threadFactory);

        // 执行20个任务
        for (int i = 0; i < 20; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println(Thread.currentThread());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }


}
```

三个任务被拒绝，阻塞队列中的任务能执行

根据这个构造方法，JDK Executors 类中提供了众多工厂方法来创建各种用途的线程池



### 1.6、Executors工具类

#### newFixedThreadPool

这个是Executors类提供的工厂方法来创建线程池！Executors 是Executor 框架的工具类！

```java
public class TestFixedThreadPool {
   public static void main(String[] args) {
      // 自定义线程工厂
      ThreadFactory factory = new ThreadFactory() {
         AtomicInteger atomicInteger = new AtomicInteger(1);

         @Override
         public Thread newThread(Runnable r) {
            return new Thread(r, "myThread_" + atomicInteger.getAndIncrement());
         }
      };

      // 创建核心线程数量为2的线程池
      // 通过 ThreadFactory可以给线程添加名字

      ExecutorService executorService = Executors.newFixedThreadPool(2, factory);

      // 任务
      Runnable runnable = new Runnable() {
         @Override
         public void run() {
            System.out.println(Thread.currentThread().getName());
            System.out.println("this is fixedThreadPool");
         }
      };
      
      executorService.execute(runnable);
   }
}
```

固定大小的线程池可以传入两个参数

- 核心线程数：nThreads
- 线程工厂：threadFactory

内部调用的构造方法

```java
public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
    return new ThreadPoolExecutor(nThreads, nThreads,
                                  0L, TimeUnit.MILLISECONDS,
                                  new LinkedBlockingQueue<Runnable>(),
                                  threadFactory);
}
```

**特点**

1. 核心线程数 == 最大线程数（没有救急线程被创建），因此也无需超时时间
2. 阻塞队列是无界的，可以放任意数量的任务
3. 适用于任务量已知，相对耗时的任务



#### newCachedThreadPool

```java
ExecutorService executorService = Executors.newCachedThreadPool();
```

内部构造方法

```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                  60L, TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>());
}
```

特点

- 没有核心线程，最大线程数为Integer.MAX_VALUE，所有创建的线程都是救急线程 (可以无限创建)，空闲时生存时间为60秒
- 阻塞队列使用的是SynchronousQueue
  - SynchronousQueue是一种特殊的队列
    - 没有容量，没有线程来取是放不进去的
    - 只有当线程取任务时，才会将任务放入该阻塞队列中
- 整个线程池表现为线程数会根据任务量不断增长，没有上限，当任务执行完毕，空闲 1分钟后释放线程。 适合任务数比较密集，但每个任务执行时间较短的情况



#### newSingleThreadExecutor

```java
public static ExecutorService newSingleThreadExecutor() {
    return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
                                0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>()));
}
```

使用场景：

1.希望多个任务排队执行。线程数固定为 1，任务数多于 1 时，会放入无界队列排队。 任务执行完毕，这唯一的线程也不会被释放。

2.区别：

- 和自己创建单线程执行任务的区别：自己创建一个单线程串行执行任务，如果任务执行失败而终止那么没有任何补救措施，而newSingleThreadExecutor线程池还会新建一个线程，保证池的正常工作
- Executors.newSingleThreadExecutor() 线程个数始终为1，不能修改
  FinalizableDelegatedExecutorService 应用的是**装饰器模式**，只对外暴露了 ExecutorService 接口，因此不能调用 ThreadPoolExecutor 中特有的方法
- 和Executors.newFixedThreadPool(1) 初始时为1时的区别：Executors.newFixedThreadPool(1) 初始时为1，以后还可以修改，对外暴露的是 ThreadPoolExecutor 对象，可以强转后调用 setCorePoolSize 等方法进行修改



#### Executors 返回线程池对象的弊端如下 

注意: Executors 返回线程池对象的弊端如下：

- FixedThreadPool 和 SingleThreadExecutor ： 允许请求的队列长度为 Integer.MAX_VALUE (无界阻塞队列),可能堆积大量的请求，从而导致 OOM : `Exception in thread "main" java.lang.OutOfMemoryError: Java heap space`
- CachedThreadPool 和 ScheduledThreadPool ： 允许创建的线程数量为 Integer.MAX_VALUE ，可能会创建大量线程，从而导致 OOM : `Exception in thread "main" java.lang.OutOfMemoryError:unable to create new native thread`

建议使用`ThreadPoolExecutor`来创建线程

避免上面的措施 : 使用有界队列，控制线程创建数量。

除了避免 OOM 的原因之外，**不推荐使用 Executors**提供的两种快捷的线程池的原因还有：

- 实际使用中需要根据自己机器的性能、业务场景来手动配置线程池的参数比如核心线程数、使用的任务队列、饱和策略等等。
- 我们应该显示地给我们的线程池命名，这样有助于我们定位问题。



### 1.7、 执行/提交任务 execute/submit

```java
// 执行任务
void execute(Runnable command);

// 提交任务 task，用返回值 Future 获得任务执行结果，Future的原理就是利用我们之前讲到的保护性暂停模式来接受返回结果的，主线程可以执行 FutureTask.get()方法来等待任务执行完成
<T> Future<T> submit(Callable<T> task);

// 提交 tasks 中所有任务
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException;
 
// 提交 tasks 中所有任务，带超时时间
<T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,long timeout, TimeUnit unit) throws InterruptedException;

// 提交 tasks 中所有任务，哪个任务先成功执行完毕，返回此任务执行结果，其它任务取消
<T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException;

// 提交 tasks 中所有任务，哪个任务先成功执行完毕，返回此任务执行结果，其它任务取消，带超时时间
<T> T invokeAny(Collection<? extends Callable<T>> tasks,long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

```



#### execute()方法

```java
execute(Runnable command)
```

传入一个Runnable对象，执行其中的run方法

源码解析: 

```java
public void execute(Runnable command) {
    if (command == null)
        throw new NullPointerException();

    // 获取ctl
    int c = ctl.get();
    
    // 判断当前启用的线程数是否小于核心线程数
    if (workerCountOf(c) < corePoolSize) {
        // 为该任务分配线程
        if (addWorker(command, true))
            // 分配成功就返回
            return;
        
        // 分配失败再次获取ctl
        c = ctl.get();
    }
    
    // 分配和信息线程失败以后
    // 如果池状态为RUNNING并且插入到任务队列成功
    if (isRunning(c) && workQueue.offer(command)) {
        
        // 双重检测，可能在添加后线程池状态变为了非RUNNING
        int recheck = ctl.get();
        
        // 如果池状态为非RUNNING，则不会执行新来的任务
        // 将该任务从阻塞队列中移除
        if (! isRunning(recheck) && remove(command))
            // 调用拒绝策略，拒绝该任务的执行
            reject(command);
        
        // 如果没有正在运行的线程
        else if (workerCountOf(recheck) == 0)
            // 就创建新线程来执行该任务
            addWorker(null, false);
    }
    
    // 如果添加失败了（任务队列已满），就调用拒绝策略
    else if (!addWorker(command, false))
        reject(command);
}
```

其中调用了 **addWoker()** 方法，再看看看这个方法:

```java
private boolean addWorker(Runnable firstTask, boolean core) {
    retry:
    for (;;) {
        int c = ctl.get();
        int rs = runStateOf(c);

        // Check if queue empty only if necessary.
        // 如果池状态为非RUNNING状态、线程池为SHUTDOWN且该任务为空 或者阻塞队列中已经有任务
        if (rs >= SHUTDOWN &&
            ! (rs == SHUTDOWN &&
               firstTask == null &&
               ! workQueue.isEmpty()))
            // 创建新线程失败
            return false;

        for (;;) {
            // 获得当前工作线程数
            int wc = workerCountOf(c);

            // 参数中 core 为true
            // CAPACITY 为 1 << COUNT_BITS-1，一般不会超过
            // 如果工作线程数大于了核心线程数，则创建失败
            if (wc >= CAPACITY ||
                wc >= (core ? corePoolSize : maximumPoolSize))
                return false;
            // 通过CAS操作改变c的值
            if (compareAndIncrementWorkerCount(c))
                // 更改成功就跳出多重循环，且不再运行循环
                break retry;
            // 更改失败，重新获取ctl的值
            c = ctl.get();  // Re-read ctl
            if (runStateOf(c) != rs)
                // 跳出多重循环，且重新进入循环
                continue retry;
            // else CAS failed due to workerCount change; retry inner loop
        }
    }

    // 用于标记work中的任务是否成功执行
    boolean workerStarted = false;
    // 用于标记worker是否成功加入了线程池中
    boolean workerAdded = false;
    Worker w = null;
    try {
        // 创建新线程来执行任务
        w = new Worker(firstTask);
        final Thread t = w.thread;
        if (t != null) {
            final ReentrantLock mainLock = this.mainLock;
            // 加锁
            mainLock.lock();
            try {
                // Recheck while holding lock.
                // Back out on ThreadFactory failure or if
                // shut down before lock acquired.
                // 加锁的同时再次检测
                // 避免在释放锁之前调用了shut down
                int rs = runStateOf(ctl.get());

                if (rs < SHUTDOWN ||
                    (rs == SHUTDOWN && firstTask == null)) {
                    if (t.isAlive()) // precheck that t is startable
                        throw new IllegalThreadStateException();
                    // 将线程添加到线程池中
                    workers.add(w);
                    int s = workers.size();
                    if (s > largestPoolSize)
                        largestPoolSize = s;
                    // 添加成功标志位变为true
                    workerAdded = true;
                }
            } finally {
                mainLock.unlock();
            }
            // 如果worker成功加入了线程池，就执行其中的任务
            if (workerAdded) {
                t.start();
                // 启动成功
                workerStarted = true;
            }
        }
    } finally {
        // 如果执行失败
        if (! workerStarted)
            // 调用添加失败的函数
            addWorkerFailed(w);
    }
    return workerStarted;
}
```



#### submit()方法

```java
Future<T> submit(Callable<T> task)
```

传入一个Callable对象，用Future来捕获返回值



使用:

```java
// 通过submit执行Callable中的call方法
// 通过Future来捕获返回值
Future<String> future = threadPool.submit(new Callable<String>() {
   @Override
   public String call() throws Exception {
      return "hello submit";
   }
});

// 查看捕获的返回值
System.out.println(future.get());
```



### 1.8、 关闭线程池 

#### shutdown()

- 将线程池的状态改为 SHUTDOWN
- 不再接受新任务，但是会将阻塞队列中的任务执行完

```java
/**
* 将线程池的状态改为 SHUTDOWN
* 不再接受新任务，但是会将阻塞队列中的任务执行完
*/
public void shutdown() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        
        // 修改线程池状态为 SHUTDOWN
        advanceRunState(SHUTDOWN);
        
  		// 中断空闲线程（没有执行任务的线程）
        // Idle：空闲的
        interruptIdleWorkers();
        onShutdown(); // hook for ScheduledThreadPoolExecutor
    } finally {
        mainLock.unlock();
    }
    // 尝试终结，不一定成功
    // 
    tryTerminate();
}

final void tryTerminate() {
    for (;;) {
        int c = ctl.get();
        // 终结失败的条件
        // 线程池状态为RUNNING
        // 线程池状态为 RUNNING SHUTDOWN STOP （状态值大于TIDYING）
        // 线程池状态为SHUTDOWN，但阻塞队列中还有任务等待执行
        if (isRunning(c) ||
            runStateAtLeast(c, TIDYING) ||
            (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
            return;
        
        // 如果活跃线程数不为0
        if (workerCountOf(c) != 0) { // Eligible to terminate
            // 中断空闲线程
            interruptIdleWorkers(ONLY_ONE);
            return;
        }

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 处于可以终结的状态
            // 通过CAS将线程池状态改为TIDYING
            if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                try {
                    terminated();
                } finally {
                    // 通过CAS将线程池状态改为TERMINATED
                    ctl.set(ctlOf(TERMINATED, 0));
                    termination.signalAll();
                }
                return;
            }
        } finally {
            mainLock.unlock();
        }
        // else retry on failed CAS
    }
}
```



#### shutdownNow()

- **将线程池的状态改为 STOP**
- **不再接受新任务，也不会在执行阻塞队列中的任务**
- 会将阻塞队列中未执行的任务返回给调用者
- 并用 interrupt 的方式中断正在执行的任务

```java
/**
* 将线程池的状态改为 STOP
* 不再接受新任务，也不会在执行阻塞队列中的任务
* 会将阻塞队列中未执行的任务返回给调用者
* 并用 interrupt 的方式中断正在执行的任务
*/
public List<Runnable> shutdownNow() {
    List<Runnable> tasks;
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        
        // 修改状态为STOP，不执行任何任务
        advanceRunState(STOP);
        
        // 中断所有线程
        interruptWorkers();
        
        // 将未执行的任务从队列中移除，然后返回给调用者
        tasks = drainQueue();
    } finally {
        mainLock.unlock();
    }
    // 尝试终结，一定会成功，因为阻塞队列为空了
    tryTerminate();
    return tasks;
}
```



其他方法:

```java
// 不在 RUNNING 状态的线程池，此方法就返回 true
boolean isShutdown();

// 线程池状态是否是 TERMINATED
boolean isTerminated();

// 调用 shutdown 后，由于调用使线程结束线程的方法是异步的并不会等待所有任务运行结束就返回，因此如果它想在线程池 TERMINATED 后做些其它事情，可以利用此方法等待
boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
```



## 二、 异步模式之工作线程

定义：

让有限的工作线程（Worker Thread）来轮流异步处理无限多的任务。也可以将其归类为分工模式，它的典型实现就是线程池，也体现了经典设计模式中的享元模式。

例如，海底捞的服务员（线程），轮流处理每位客人的点餐（任务），如果为每位客人都配一名专属的服务员，那么成本就太高了（对比另一种多线程设计模式：Thread-Per-Message）

注意: 不同任务类型应该使用不同的线程池，这样能够避免饥饿，并能提升效率

例如，如果一个餐馆的工人既要招呼客人（任务类型A），又要到后厨做菜（任务类型B）显然效率不咋地，分成服务员（线程池A）与厨师（线程池B）更为合理，当然你能想到更细致的分工



饥饿：

固定大小线程池会有饥饿现象：

1. 两个工人是同一个线程池中的两个线程
2. 他们要做的事情是：为客人点餐和到后厨做菜，这是两个阶段的工作
   1. 客人点餐：必须先点完餐，等菜做好，上菜，在此期间处理点餐的工人必须等待
   2. 后厨做菜：做就是了
3. 比如工人A 处理了点餐任务，接下来它要等着 工人B 把菜做好，然后上菜，他俩也配合的蛮好; 但现在同时来了两个客人，这个时候工人A 和工人B 都去处理点餐了，这时没人做饭了，饥饿

解决方法可以增加线程池的大小，不过不是根本解决方案，还是前面提到的，不同的任务类型，采用不同的线程池

```java
@Slf4j(topic = ":")
public class Test4 {

    static final List<String> MENU = Arrays.asList("地三鲜", "宫保鸡丁", "辣子鸡丁", "烤鸡翅");
    static Random RANDOM = new Random();
    static String cooking() {
        return MENU.get(RANDOM.nextInt(MENU.size()));
    }
    public static void main(String[] args) {
        fun1();
    }

    //没有更多的线程，运行不了
    private static void fun1() {
        ExecutorService Pool = Executors.newFixedThreadPool(2);

        Pool.execute(() -> {
            log.debug("处理点餐...");
            Future<String> f = Pool.submit(() -> {
                log.debug("做菜");
                return cooking();
            });
            try {
                log.debug("上菜: {}", f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        Pool.execute(() -> {
            log.debug("处理点餐...");
            Future<String> f = Pool.submit(() -> {
                log.debug("做菜");
                return cooking();
            });
            try {
                log.debug("上菜: {}", f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    //分工线程池
    private static void fun2() {
        ExecutorService waiterPool = Executors.newFixedThreadPool(1);
        ExecutorService cookPool = Executors.newFixedThreadPool(1);

        waiterPool.execute(() -> {
            log.debug("处理点餐...");
            Future<String> f = cookPool.submit(() -> {
                log.debug("做菜");
                return cooking();
            });
            try {
                log.debug("上菜: {}", f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        waiterPool.execute(() -> {
            log.debug("处理点餐...");
            Future<String> f = cookPool.submit(() -> {
                log.debug("做菜");
                return cooking();
            });
            try {
                log.debug("上菜: {}", f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }
}
```



线程池中线程设置多少为好?

过小会导致程序不能充分地利用系统资源、容易导致饥饿，过大会导致更多的线程上下文切换，占用更多内存

1. CPU 密集型运算：
   通常采用 cpu 核数 + 1 能够实现最优的 CPU 利用率，+1 是保证当线程由于页缺失故障（操作系统）或其它原因导致暂停时，额外的这个线程就能顶上去，保证 CPU 时钟周期不被浪费
2. I/O 密集型运算：
   CPU 不总是处于繁忙状态，例如，当你执行业务计算时，这时候会使用 CPU 资源，但当你执行 I/O 操作时、远程RPC 调用时，包括进行数据库操作时，这时候 CPU 就闲下来了，你可以利用多线程提高它的利用率。
   1. 经验公式如下：线程数 = 核数 * 期望 CPU 利用率 * 总时间(CPU计算时间+等待时间) / CPU 计算时间
      例如 4 核 CPU 计算时间是 50% ，其它等待时间是 50%，期望 cpu 被 100% 利用，套用公式
      4 * 100% * 100% / 50% = 8
      例如 4 核 CPU 计算时间是 10% ，其它等待时间是 90%，期望 cpu 被 100% 利用，套用公式
      4 * 100% * 100% / 10% = 40



### 2.1、任务调度线程池 

ScheduledExecutorService

在『任务调度线程池』功能加入之前，可以使用java.util.Timer来实现定时功能，Timer 的优点在于简单易用，但由于所有任务都是由同一个线程来调度，因此所有任务都是串行执行的，同一时间只能有一个任务在执行，前一个任务的延迟或异常都将会影响到之后的任务。

```java
@Slf4j
public class TestTimer {
    public static void main(String[] args) {
        Timer timer = new Timer();
        TimerTask task1 = new TimerTask() {
            @Override
            public void run() {
                log.debug("task 1");
                Sleeper.sleep(2);
            }
        };

        TimerTask task2 = new TimerTask() {
            @Override
            public void run() {
                log.debug("task 2");
            }
        };
		// 使用timer添加两个任务, 希望他们都在1s后执行
		// 由于timer内只有一个线程来执行队列中的任务, 所以task2必须等待task1执行完成才能执行
        timer.schedule(task1, 1000);
        timer.schedule(task2, 1000);
    }
}

08:21:17.548  [Timer-0] - task 1
08:21:19.553  [Timer-0] - task 2
    
//可以使用两个Timer
```



使用 ScheduledExecutorService

- ScheduledExecutorService 中schedule方法的使用

```java
public class Test5 {
    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

        executor.schedule(() -> System.out.println("任务1, 执行时间:" + new Date()), 1000, TimeUnit.MILLISECONDS);

        executor.schedule(() -> System.out.println("任务2, 执行时间:" + new Date()), 1000, TimeUnit.MILLISECONDS);
    }
}

任务1, 执行时间:Sun Jan 03 08:53:54 CST 2021
任务2, 执行时间:Sun Jan 03 08:53:54 CST 2021
```



- ScheduledExecutorService 中 scheduleAtFixedRate方法的使用

```java
public class TestTimer {
    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        log.debug("start....");
        // 延迟1s后, 按1s的速率打印running
        executor.scheduleAtFixedRate(() -> log.debug("running"), 1, 1, TimeUnit.SECONDS);
    }
}

08:51:59.930  [main] - start....
08:52:01.050  [pool-1-thread-1] - running
08:52:02.049  [pool-1-thread-1] - running
08:52:03.045  [pool-1-thread-1] - running


public class TestTimer {
    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        log.debug("start....");
        // 延迟1s后, 按1s的速率打印running
        executor.scheduleAtFixedRate(() -> {
            log.debug("running");
            Thread.sleep(2);
        }, 1, 1, TimeUnit.SECONDS);
    }
}

// 睡眠时间 > 速率, 按睡眠时间打印
08:54:58.567  [main] - start....
08:54:59.675  [pool-1-thread-1] - running
08:55:01.684  [pool-1-thread-1] - running
08:55:03.685  [pool-1-thread-1] - running
```



- ScheduledExecutorService 中scheduleWithFixedDelay方法的使用

```java
public class Test {
    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        log.debug("start....");
        // 两次执行 延迟间隔1s
        executor.scheduleWithFixedDelay(() -> {
            log.debug("running");
            Thread.sleep(2);
        }, 1, 1, TimeUnit.SECONDS);
    }
}

08:56:22.581  [main] - start....
08:56:23.674  [pool-1-thread-1] - running
08:56:26.679  [pool-1-thread-1] - running
08:56:29.680  [pool-1-thread-1] - running
08:56:32.689  [pool-1-thread-1] - running
```



整个线程池表现为：线程数固定，任务数多于线程数时，会放入无界队列排队。任务执行完毕，这些线程也不会被释放。用来执行延迟或反复执行的任务。



示例 : 如何让每周四 18:00:00 定时执行任务？

```java
public class TestSchedule {

    // 如何让每周四 18:00:00 定时执行任务？
    public static void main(String[] args) {
        //  获取当前时间
        LocalDateTime now = LocalDateTime.now();
        System.out.println(now);
        // 获取周四时间
        LocalDateTime time = now.withHour(18).withMinute(0).withSecond(0).withNano(0).with(DayOfWeek.THURSDAY);
        // 如果 当前时间 > 本周周四，必须找到下周周四
        if(now.compareTo(time) > 0) {
            time = time.plusWeeks(1);
        }
        System.out.println(time);
        // initailDelay 代表当前时间和周四的时间差
        // period 一周的间隔时间
        long initailDelay = Duration.between(now, time).toMillis();
        long period = 1000 * 60 * 60 * 24 * 7;
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
        pool.scheduleAtFixedRate(() -> {
            System.out.println("running...");
        }, initailDelay, period, TimeUnit.MILLISECONDS);
    }
}
```



### 2.2、 正确处理执行任务异常

可以发现，如果线程池中的线程执行任务时，如果任务抛出了异常，默认是中断执行该任务而不是抛出异常或者打印异常信息。

方法1：主动捉异常

```java
ExecutorService pool = Executors.newFixedThreadPool(1);
pool.submit(() -> {
	 try {
         log.debug("task1");
         int i = 1 / 0;
	 } catch (Exception e) {
         log.error("error:", e);
	 }
});
```

方法2：使用 Future，**错误信息都被封装进submit方法的返回方法中！**

```java
ExecutorService pool = Executors.newFixedThreadPool(1);
Future<Boolean> f = pool.submit(() -> {
	 log.debug("task1");
	 int i = 1 / 0;
	 return true;
});
log.debug("result:{}", f.get());
```





## 三、 Tomcat 线程池

![0f03](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/0f03.png)

1. LimitLatch 用来限流，可以控制最大连接个数，类似 J.U.C 中的 Semaphore 后面再讲
2. Acceptor 只负责【接收新的 socket 连接】
3. Poller 只负责监听 socket channel 是否有【可读的 I/O 事件】
4. 一旦可读，封装一个任务对象（socketProcessor），提交给 Executor 线程池处理
5. Executor 线程池中的工作线程最终负责【处理请求】

> 体现了不同的线程池做不同的工作

Tomcat 线程池扩展了 ThreadPoolExecutor，行为稍有不同

如果总线程数达到 maximumPoolSize，这时不会立刻抛 RejectedExecutionException 异常，而是再次尝试将任务放入队列，如果还失败，才抛出 RejectedExecutionException 异常

源码 tomcat-7.0.42:

```java
public void execute(Runnable command, long timeout, TimeUnit unit) {
    submittedCount.incrementAndGet();
    try {
        super.execute(command);
    } catch (RejectedExecutionException rx) {
        if (super.getQueue() instanceof TaskQueue) {
            final TaskQueue queue = (TaskQueue)super.getQueue();
            try {
                // 使任务从新进入阻塞队列
                if (!queue.force(command, timeout, unit)) {
                    submittedCount.decrementAndGet();
                    throw new RejectedExecutionException("Queue capacity is full.");
                }
            } catch (InterruptedException x) {
                submittedCount.decrementAndGet();
                Thread.interrupted();
                throw new RejectedExecutionException(x);
            }
        } else {
            submittedCount.decrementAndGet();
            throw rx;
        }
    }
}
```

TaskQueue.java

```java
public boolean force(Runnable o, long timeout, TimeUnit unit) throws InterruptedException {
    if ( parent.isShutdown() )
        throw new RejectedExecutionException(
                "Executor not running, can't force a command into the queue"
        );
    return super.offer(o,timeout,unit); //forces the item onto the queue, to be used if the task is rejected
}
```



## 四、 Fork/Join

[JDK8辅助学习(六)：Fork/Join 框架](https://blog.csdn.net/lzb348110175/article/details/103977640)

概念：

1. Fork/Join 是 JDK 1.7 加入的新的线程池实现，它体现的是一种分治思想，适用于能够进行任务拆分的 cpu 密集型运算
2. 所谓的任务拆分，是将一个大任务拆分为算法上相同的小任务，直至不能拆分可以直接求解。跟递归相关的一些计算，如归并排序、斐波那契数列、都可以用分治思想进行求解
3. Fork/Join 在分治的基础上加入了多线程，可以把每个任务的分解和合并交给不同的线程来完成，进一步提升了运算效率
4. Fork/Join 默认会创建与 cpu 核心数大小相同的线程池



使用:

提交给 Fork/Join 线程池的任务需要继承 RecursiveTask（有返回值）或 RecursiveAction（没有返回值）

```java
@Slf4j(topic = "TestForkJoin2")
public class TestForkJoin {

    public static void main(String[] args) {
        ForkJoinPool pool = new ForkJoinPool(4);
        System.out.println(pool.invoke(new MyTask(5)));

        // new MyTask(5)  5+ new MyTask(4)  4 + new MyTask(3)  3 + new MyTask(2)  2 + new MyTask(1)
    }
}

// 1~n 之间整数的和
@Slf4j(topic = "MyTask")
class MyTask extends RecursiveTask<Integer> {

    private int n;

    public MyTask(int n) {
        this.n = n;
    }

    @Override
    public String toString() {
        return "{" + n + '}';
    }

    @Override
    protected Integer compute() {
        // 如果 n 已经为 1，可以求得结果了
        if (n == 1) {
            log.debug("join() {}", n);
            return n;
        }

        // 将任务进行拆分(fork)
        MyTask t1 = new MyTask(n - 1);
        t1.fork();  //让一个线程去执行此任务
        log.debug("fork() {} + {}", n, t1);

        // 合并(join)结果
        int result = n + t1.join();   //获取任务结果
        log.debug("join() {} + {} = {}", n, t1, result);
        return result;
    }
}
```







