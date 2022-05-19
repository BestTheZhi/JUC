package top.THEZHI.pack1;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.LockSupport;

/**
 * @author ZHI LIU
 * @date 2022-05-02
 *
 *
 * park()
 * parkUntil() 都会被打断,但是不会抛异常
 *
 */

@Slf4j
public class ParkTest {


    public static void main(String[] args) {


        Thread t1 = new Thread(()->{
            log.debug("park....");
            LockSupport.park();
            //不清除打断标记 就不会被阻塞住;
            LockSupport.park();
//            LockSupport.parkUntil(System.currentTimeMillis()+1000000);
        },"t1");

        t1.start();

        t1.interrupt();


        LockSupport.unpark(t1);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
