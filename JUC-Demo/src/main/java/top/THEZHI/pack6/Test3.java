package top.THEZHI.pack6;

/**
 * @author ZHI LIU
 * @date 2022-05-07
 */
public class Test3 {

//    static volatile boolean run = true;
//    static boolean run2 = true;   //可以暂停

    static boolean run = true;
    static volatile boolean run2 = true;  //不可以暂停



    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            while (run || run2) {

            }
        });

        t1.start();
        Thread.sleep(1000);
        run = false;
        run2 = false;
        System.out.println("修改...");
    }

}
