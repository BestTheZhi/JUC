import lombok.extern.slf4j.Slf4j;
import top.THEZHI.utils.Dog;
import top.THEZHI.utils.PrintMarkWord;

/**
 * @author ZHI LIU
 * @date 2022-04-27
 */
@Slf4j
public class Test2 {

    public static void main(String[] args) throws InterruptedException {
        Dog d = new Dog();

        Thread t1 = new Thread(()->{
            log.debug(PrintMarkWord.print(d));
            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }
            log.debug(PrintMarkWord.print(d));

//            synchronized (String.class){
//                String.class.notify();
//            }
        },"t1");
        t1.start();


        Thread t2 = new Thread(()->{
//            synchronized (String.class){
//                try {
//                    String.class.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }

            log.debug(PrintMarkWord.print(d));
            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }
            log.debug(PrintMarkWord.print(d));
        },"t2");
        t2.start();


        System.out.println();
        Thread.sleep(10000);
        new Thread(()->{
            log.debug(PrintMarkWord.print(d));
            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }
            log.debug(PrintMarkWord.print(d));
        },"t3").start();


    }

}
