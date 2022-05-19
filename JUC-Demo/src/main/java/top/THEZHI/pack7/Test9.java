package top.THEZHI.pack7;

import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author ZHI LIU
 * @date 2022-05-09
 */
public class Test9 {

    public static void main(String[] args) {
        Account.demo(new MyAtomicInteger(10000));
    }

}

//这里实现接口方便测试
class MyAtomicInteger implements Account{
    private volatile int value;
    private static final long offset;
    private static final Unsafe UNSAFE;

    static {
        try {
            Class unsafeClass = Unsafe.class;
            Constructor constructor = unsafeClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            UNSAFE= (Unsafe) constructor.newInstance();

            offset = UNSAFE.objectFieldOffset(MyAtomicInteger.class.getDeclaredField("value"));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    public MyAtomicInteger(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void decrement(int count){
        int prev,next;
        do {
            prev = this.value;
            next = prev - count;
        }while (!compareAndSet(prev,next));
    }

    private boolean compareAndSet(int expect , int update){
        return UNSAFE.compareAndSwapInt(this,offset,expect,update);
    }


    @Override
    public Integer getBalance() {
        return getValue();
    }

    @Override
    public void withdraw(Integer amount) {
        decrement(amount);
    }
}
