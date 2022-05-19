package top.THEZHI.pack1;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ZHI LIU
 * @date 2022-05-01
 */

@Slf4j
public class WatiTest1 {
    public static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {

        Thread t1 = new Thread(()->{
            synchronized (lock){

                try {
                    lock.wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        },"t1");
        t1.start();

        Thread.sleep(200);
        System.out.println(t1.getState());

    }
}
