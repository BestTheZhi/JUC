package top.THEZHI.atomic;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author ZHI LIU
 * @date 2022-05-07
 */

//一直操作的是字符串常量池中的字符串
@Slf4j
public class BigDecimalTest3 {

    static AtomicReference<String> ref = new AtomicReference<>("A");

    public static void main(String[] args) throws InterruptedException {

        String pre = ref.get();
        log.debug("identityHashCode：{}",System.identityHashCode(pre));
        log.debug("开始操作...");
        other();
        Thread.sleep(1000);

        //把ref中的A改为C
        log.debug("change A->C :{}" ,ref.compareAndSet(pre, "C"));

    }

    static void other() throws InterruptedException {
        new Thread(() -> {
            // 此时ref.get()为A,此时共享变量ref也是A,没有被改过, 此时CAS
            // 可以修改成功, B
            log.debug("change A->B :{}" ,ref.compareAndSet(ref.get(), "B"));
            log.debug("identityHashCode：{}",System.identityHashCode(ref.get()));
        }," t1 ").start();
        Thread.sleep(500);
        new Thread(() -> {
            // 同上, 修改为A
            log.debug("change B->A :{}" ,ref.compareAndSet(ref.get(), "A"));
            log.debug("identityHashCode：{}",System.identityHashCode(ref.get()));
        }," t2 ").start();
    }
}
