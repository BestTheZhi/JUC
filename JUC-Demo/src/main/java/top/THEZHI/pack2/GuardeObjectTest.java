package top.THEZHI.pack2;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ZHI LIU
 * @date 2022-05-03
 */
@Slf4j
public class GuardeObjectTest {
    public static void main(String[] args) {
        // 线程1等待线程2的下载结果
        GuardeObject guardeObject = new GuardeObject();
        new Thread(() -> {
            log.debug("begin");
            Object obj = guardeObject.get(2000);
            log.debug("结果是:{}", obj);
        }, "t1").start();

        new Thread(() -> {
            log.debug("begin");
            try {
                 Thread.sleep(1000); // 在等待时间内
//                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            guardeObject.complete(new Object());
        }, "t2").start();
    }
}

class GuardeObject {
    // 结果
    private Object response;

    // 获取结果
    // timeout表示等待多久. 这里假如是2s
    public Object get(long timeout) {
        synchronized (this) {
            // 假如开始时间为 15:00:00
            long begin = System.currentTimeMillis();
            // 经历的时间
            long passedTime = 0;
            while (response == null) {
                // 这一轮循环应该等待的时间
                long waitTime = timeout - passedTime;
                // 经历的时间超过了最大等待时间, 退出循环
                if (waitTime <= 0) {
                    break;
                }
                try {
                    //这里可能会出现虚假唤醒，所以等待时间不应该为timeout而是waitTime
                    this.wait(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 经历时间
                passedTime = System.currentTimeMillis() - begin; // 15:00:02
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
