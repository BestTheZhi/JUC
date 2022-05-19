package top.THEZHI.atomic;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author ZHI LIU
 * @date 2022-05-07
 */

//identityHashCode()可以代表对象的内存地址
@Slf4j
public class BigDecimalTest {

    private static AtomicReference<BigDecimal> b = new AtomicReference<>(new BigDecimal("10000"));


    public static void main(String[] args) throws InterruptedException {
        BigDecimal prev = b.get();
        log.debug("identityHashCode：{}",System.identityHashCode(b.get()));
        log.debug("开始的值:{}",prev.intValue());
        log.debug("开始操作...");
        operation();
        Thread.sleep(500);

        boolean b = BigDecimalTest.b.compareAndSet(prev, prev.subtract(new BigDecimal("1000")));
        log.debug("修改成功: {}",b);

    }


    private static void operation() throws InterruptedException {
        new Thread(()->{
            log.debug("10000 -> 9000");
            BigDecimal prev = b.get();
            b.compareAndSet(prev,prev.subtract(new BigDecimal("1000")));
            log.debug("identityHashCode：{}",System.identityHashCode(b.get()));
        }," t1 ").start();

        Thread.sleep(500);

        new Thread(()->{
            log.debug("9000 -> 10000");
            BigDecimal prev = b.get();
            b.compareAndSet(prev,prev.subtract(new BigDecimal("-1000")));
            log.debug("identityHashCode：{}",System.identityHashCode(b.get()));
        }," t2 ").start();
    }


}
