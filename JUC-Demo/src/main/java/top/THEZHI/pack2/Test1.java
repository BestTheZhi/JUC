package top.THEZHI.pack2;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ZHI LIU
 * @date 2022-05-03
 */

@Slf4j
public class Test1 {

    public static void main(String[] args) {
        Thread t1 = new Thread(()->{
            try {
                Thread.sleep(2000000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"t1");
        t1.start();


        Thread t3 = new Thread(()->{
            try {
                t1.join();
            } catch (InterruptedException e) {
                log.debug(e.getMessage());
            }
        },"t3");
        t3.start();

        Thread t2 = new Thread(()->{
            t3.interrupt();
        });
        t2.start();
    }

}
