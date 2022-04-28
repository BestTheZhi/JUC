import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.ClassLayout;
import top.THEZHI.utils.Dog;
import top.THEZHI.utils.PrintMarkWord;

/**
 * -XX:BiasedLockingStartupDelay=0
 */
@Slf4j()
public class Test1 {

    public static void main(String[] args) {
        Dog d = new Dog();
        new Thread(()->{
            System.out.println("synchronized 前 ----- ");
            log.debug(PrintMarkWord.print(d));
            synchronized (d){
                System.out.println("synchronized 中 ----- ");
                log.debug(PrintMarkWord.print(d));
//                System.out.println("对象头中信息 ----- ");
//                System.out.println(ClassLayout.parseInstance(d).toPrintable());
            }
            System.out.println("synchronized 后 ----- ");
            log.debug(PrintMarkWord.print(d));
        },"t1").start();
    }

}
