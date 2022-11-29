import lombok.extern.slf4j.Slf4j;
import top.THEZHI.utils.Dog;
import top.THEZHI.utils.PrintMarkWord;
import top.THEZHI.utils.Sleeper;

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
            Sleeper.sleep(1);
            synchronized (d){
                log.debug(PrintMarkWord.print(d));
                Sleeper.sleep(2);
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
            Sleeper.sleep(2);
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


/*不同步下的某种测试结果*/
// [t2] DEBUG Test2 - 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000101
// [t1] DEBUG Test2 - 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000101
// [t2] DEBUG Test2 - 00000000 00000000 00000000 00000000 00000011 00100010 10110110 00011010
// [t2] DEBUG Test2 - 00000000 00000000 00000000 00000000 00000011 00100010 10110110 00011010
// [t1] DEBUG Test2 - 00000000 00000000 00000000 00000000 00000011 00100010 10110110 00011010
// [t1] DEBUG Test2 - 00000000 00000000 00000000 00000000 00000011 00100010 10110110 00011010
// [t3] DEBUG Test2 - 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001
// [t3] DEBUG Test2 - 00000000 00000000 00000000 00000000 00011111 10011100 11110101 11110000
// [t3] DEBUG Test2 - 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001
