import lombok.extern.slf4j.Slf4j;
import top.THEZHI.utils.Dog;
import top.THEZHI.utils.PrintMarkWord;

/**
 * -XX:BiasedLockingStartupDelay=0
 */
@Slf4j
public class Test4 {

    public static void main(String[] args) {
        Dog d = new Dog();
        new Thread(()->{
            log.debug(PrintMarkWord.print(d));
            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }

            System.out.println(d.hashCode());

            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }
            log.debug(PrintMarkWord.print(d));

        },"t1").start();

    }

}
