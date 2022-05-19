package top.THEZHI.pack9;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.BlockingQueue;


/**
 * @author ZHI LIU
 * @date 2022-05-11
 *
 * -Xms1m -Xmx1m
 *
 */
@Slf4j(topic = ":")
public class Test3 {

    static Runnable runnable = new Runnable() {
        @SneakyThrows
        @Override
        public void run() {
            Thread.sleep(2000000);
        }
    };

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        Future<Boolean> f = pool.submit(() -> {
            log.debug("task1");
            int i = 1 / 0;
            return true;
        });
        log.debug("result:{}", f.get());
    }

    //Exception in thread "main" java.lang.OutOfMemoryError:
    // unable to create new native thread
    private static void fun1(){

        int i = Integer.MIN_VALUE;
        while (true){

            new Thread(runnable).start();
            i++;
            System.out.println(i);

        }
    }

    //-Xms1m -Xmx1m
    //Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
    private static void fun2(){
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        while (true){
            queue.add(new Runnable() {
                @Override
                public void run() {

                }
            });
        }

    }





}
