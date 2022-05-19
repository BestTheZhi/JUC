package top.THEZHI.pack6;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ZHI LIU
 * @date 2022-05-07
 *
 */
@Slf4j(topic = "Test1")
public class Test1 {
    static boolean run = true;
    final static Object obj = new Object();
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            // 1s内,一直都在无限循环获取锁. 1s后主线程抢到锁,修改为false, 此时t1线程抢到锁对象,while循环也退出
            while (run) {
                //只有循环超过一定阈值，才"不可见"
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                synchronized (obj) {

                }
            }
        });

        t1.start();
        Thread.sleep(1000);
        // 当主线程获取到锁的时候, 就修改为false了
//        synchronized (obj) {
            run = false;
            System.out.println("false");
//        }
    }
}

//public class Test1 {
//
////    static boolean run = true;
//    static volatile boolean run = true;
//    public static void main(String[] args) throws InterruptedException {
//        Thread t1 = new Thread(() -> {
//            while (run) {
//                // 如果打印一句话
//                // 此时就可以结束, 因为println方法中, 使用到了synchronized
//                // 内存屏障，清空工作内存，去主存中读
//                 System.out.println("123");
//            }
//        });
//
//        t1.start();
//        Thread.sleep(1000);
//        run = false;
//        System.out.println(run);
//    }
//}