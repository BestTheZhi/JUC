package top.THEZHI.pack7;

import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * @author ZHI LIU
 * @date 2022-05-08
 */
public class Test4 {
    //指定版本号
    static AtomicStampedReference<String> ref = new AtomicStampedReference<>("A", 0);

    public static void main(String[] args) {
        new Thread(() -> {
            String pre = ref.getReference();
            //获得版本号
            int stamp = ref.getStamp(); // 此时的版本号还是第一次获取的
            System.out.println("change");
            try {
                other();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //把ref中的A改为C,并比对版本号，如果版本号相同，就执行替换，并让版本号+1
            System.out.println("change A->C stamp:" + stamp+ " " + ref.compareAndSet(pre, "C", stamp, stamp + 1));
        }).start();
    }

    static void other() throws InterruptedException {
        new Thread(() -> {
            int stamp = ref.getStamp();
            System.out.println("change A->B stamp:" + stamp + " " + ref.compareAndSet(ref.getReference(), "B", stamp, stamp + 1));
        }).start();
        Thread.sleep(500);
        new Thread(() -> {
            int stamp = ref.getStamp();
            System.out.println("change B->A stamp:" + stamp + " " + ref.compareAndSet(ref.getReference(), "A", stamp, stamp + 1));
        }).start();
    }
}
