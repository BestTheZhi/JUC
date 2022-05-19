package top.THEZHI.pack2;

import java.awt.*;
import java.util.concurrent.locks.LockSupport;

/**
 * @author ZHI LIU
 * @date 2022-05-03
 */
public class ParkTest {
    private static Object obj =  new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(()->{
            LockSupport.park();
            while (true){

            }
        });

        t1.start();

        t1.interrupt();



    }

}
