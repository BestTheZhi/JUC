package top.THEZHI.pack9;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author ZHI LIU
 * @date 2022-05-13
 */
public class Test5 {
    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

        executor.schedule(() -> System.out.println("任务1, 执行时间:" + new Date()),
                1000, TimeUnit.MILLISECONDS);

        executor.schedule(() -> System.out.println("任务2, 执行时间:" + new Date()),
                1000, TimeUnit.MILLISECONDS);
    }
}