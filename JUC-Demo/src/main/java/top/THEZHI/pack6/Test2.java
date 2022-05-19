package top.THEZHI.pack6;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ZHI LIU
 * @date 2022-05-07
 */
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

//     private boolean stop = false; // 不会停止程序
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