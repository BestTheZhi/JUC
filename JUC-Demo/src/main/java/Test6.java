import lombok.extern.slf4j.Slf4j;
import top.THEZHI.utils.Dog;
import top.THEZHI.utils.Person;
import top.THEZHI.utils.PrintMarkWord;

import java.util.Vector;

/**
 * -XX:BiasedLockingStartupDelay=0
 */
@Slf4j
public class Test6 {
    public static void main(String[] args) throws InterruptedException {
        Vector<Dog> list = new Vector<>();
        Thread t1 = new Thread(()->{
            for (int i = 0; i < 30; i++) {
                Dog d = new Dog();
                list.add(d);
                synchronized (d){
                    log.debug(i+"\t"+ PrintMarkWord.print(d));
                }
            }
        },"t1");
        t1.start();
        t1.join();


        Thread t2 = new Thread(()->{
            log.debug("-------------------->");
            for (int i = 0; i < 30; i++) {
                Dog d = list.get(i);
                log.debug(i+"\t"+PrintMarkWord.print(d));
                synchronized (d){
                    log.debug(i+"\t"+PrintMarkWord.print(d));
                }
                log.debug(i+"\t"+PrintMarkWord.print(d));
            }
        },"t2");
        t2.start();
        t2.join();

        Person p = new Person();
        Thread t3 =  new Thread(()->{
            log.debug("-------------------->");
            log.debug(PrintMarkWord.print(p));
            synchronized (p){
                log.debug(PrintMarkWord.print(p));
            }
        },"t3");
        t3.start();
        t3.join();

        Thread t4 = new Thread(()->{
            synchronized (p){
                log.debug(PrintMarkWord.print(p));
            }
            log.debug(PrintMarkWord.print(p));
        },"t4");
        t4.start();
    }
}
