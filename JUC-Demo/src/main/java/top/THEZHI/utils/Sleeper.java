package top.THEZHI.utils;

/**
 * @author ZHI LIU
 * @date 2022-09-23
 */
public class Sleeper {

    public static void sleep(int s){
        try {
            Thread.sleep(1000 * s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
