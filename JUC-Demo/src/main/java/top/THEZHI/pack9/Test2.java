package top.THEZHI.pack9;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ZHI LIU
 * @date 2022-05-11
 */
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
