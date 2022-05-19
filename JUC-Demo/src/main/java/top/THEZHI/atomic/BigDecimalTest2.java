package top.THEZHI.atomic;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author ZHI LIU
 * @date 2022-05-07
 */

@Slf4j
public class BigDecimalTest2 {

    private static AtomicReference<BigDecimal> b = new AtomicReference<>(BigDecimal.TEN);


    public static void main(String[] args) throws InterruptedException {
        BigDecimal prev = b.get();
        log.debug("identityHashCode：{}",System.identityHashCode(b.get()));
        log.debug("开始的值:{}",prev.intValue());
        log.debug("开始操作...");
        operation();
        Thread.sleep(500);

        boolean f = BigDecimalTest2.b.compareAndSet(prev, prev.subtract(new BigDecimal("1000")));
        log.debug("修改成功: {}",f);

    }


    private static void operation(){
        new Thread(()->{
            log.debug("10 -> 0");
            BigDecimal prev = b.get();
            b.compareAndSet(prev,prev.subtract(BigDecimal.TEN));
            log.debug("identityHashCode：{}",System.identityHashCode(b.get()));
        }," t1 ").start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(()->{
            log.debug("0 -> 10");
            BigDecimal prev = b.get();
            b.compareAndSet(prev,prev.add(BigDecimal.TEN));
            log.debug("identityHashCode：{}",System.identityHashCode(b.get()));
        }," t2 ").start();
    }

}
