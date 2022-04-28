package top.THEZHI.pack1;

/**
 * @author ZHI LIU
 * @date 2022-04-27
 */
public class ClassOrder {
    static final Object lock = new Object();
    static int counter = 0;
    public static void main(String[] args) {
        synchronized (lock) {
            counter++;
        }
    }

}
