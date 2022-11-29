package top.THEZHI.pack1;

import top.THEZHI.utils.Sleeper;

/**
 * @author ZHI LIU
 * @date 2022-09-25
 */
public class JoinTest {

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(()->{
            Sleeper.sleep(10);

        });
        t1.start();

        Thread t2 = new Thread(()->{

            try {
                t1.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("t2");
        });
        t2.start();

        Sleeper.sleep(1);
        t2.interrupt();

    }

}
