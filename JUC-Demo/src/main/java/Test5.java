import lombok.extern.slf4j.Slf4j;
import top.THEZHI.utils.Dog;
import top.THEZHI.utils.PrintMarkWord;


/**
 * -XX:BiasedLockingStartupDelay=0
 */
@Slf4j
public class Test5 {
    public static void main(String[] args) throws InterruptedException {
        Dog d = new Dog();

        Thread t1= new Thread(()->{
            log.debug(PrintMarkWord.print(d));
            System.out.println("获取锁对象");
            synchronized (d){
                try {
                    log.debug(PrintMarkWord.print(d));
                    System.out.println("调用wait()方法");
                    d.wait(1000);
                    log.debug(PrintMarkWord.print(d));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("释放锁");
            log.debug(PrintMarkWord.print(d));
        },"t1");
        t1.start();
        t1.join();

        new Thread(()->{
            System.out.println("等待t1结束后，线程t2再尝试获取");
            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }


        },"t2").start();

    }
}
