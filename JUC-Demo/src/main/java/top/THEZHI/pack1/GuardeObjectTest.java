package top.THEZHI.pack1;

import lombok.extern.slf4j.Slf4j;


/**
 * @author ZHI LIU
 * @date 2022-05-03
 * 多线程同步模式 - 一个线程需要等待另一个线程的执行结果
 */
@Slf4j
public class GuardeObjectTest {
    public static void main(String[] args) {
        // 线程1等待线程2的下载结果
        GuardeObject guardeObject = new GuardeObject();
        new Thread(() -> {
            log.debug("等待结果");
            Object obj = guardeObject.get();
            log.debug("下载结果是：{}",obj.toString());
        }, "t1").start();

        new Thread(() -> {
            log.debug("执行下载");
            try {
                //
                Thread.sleep(2000);
                guardeObject.complete(new String("下载完成"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, "t2").start();
    }
}

class GuardeObject {
    // 结果
    private Object response;

    // 获取结果
    public Object get() {
        synchronized (this) {
            // 没有结果
            while (response == null) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return response;
        }
    }

    // 产生结果
    public void complete(Object response) {
        synchronized (this) {
            // 给结果变量赋值
            this.response = response;
            this.notifyAll();
        }
    }
}