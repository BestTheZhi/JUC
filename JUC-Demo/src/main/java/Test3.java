import com.sun.tracing.dtrace.StabilityLevel;
import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.ClassLayout;
import top.THEZHI.utils.Dog;
import top.THEZHI.utils.PrintMarkWord;

/**
 * -XX:BiasedLockingStartupDelay=0
 */
@Slf4j
public class Test3 {

    public static void main(String[] args) throws InterruptedException {
        Dog d = new Dog();

        new Thread(()->{
            log.debug(PrintMarkWord.print(d));
            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }
            log.debug(PrintMarkWord.print(d));

            synchronized (String.class){
                String.class.notify();
            }
        },"t1").start();

        System.out.println();
        new Thread(()->{
            synchronized (String.class){
                try {
                    String.class.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            log.debug(PrintMarkWord.print(d));
            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }
            log.debug(PrintMarkWord.print(d));
        },"t2").start();


    }

}
