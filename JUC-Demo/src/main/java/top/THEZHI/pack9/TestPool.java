package top.THEZHI.pack9;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author ZHI LIU
 * @date 2022-05-11
 *
 * 自定义一个简单的线程池
 */




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
