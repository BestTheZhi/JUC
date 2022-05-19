package top.THEZHI.pack7;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/**
 * @author ZHI LIU
 * @date 2022-05-08
 */
public class Test3 {

    public static void main(String[] args) {
        DecimalAccount.demo(new DecimalAccountCas(new BigDecimal("10000")));
    }
}

class DecimalAccountCas implements DecimalAccount {

    //原子引用，泛型类型为小数类型
    private final AtomicReference<BigDecimal> balance;

    public DecimalAccountCas(BigDecimal balance) {
        this.balance = new AtomicReference<>(balance);
    }

    @Override
    public BigDecimal getBalance() {
        return balance.get();
    }

    /*
    AtomicReference中的compareAndSet()方法，调用的是Unsafe.compareAndSwapObject(this, valueOffset, expect, update)
    线程一                           线程二
    expect = BigDecimal(10000)-①    expect = BigDecimal(10000)-①
    update = BigDecimal(9990)-②     update = BigDecimal(9990)-③
    ① ② ③ 分别为三个对象

    Unsafe.compareAndSwapObject(...)方法比较的是this的valueOffset位置的对象，是否同expect,从而判断是否修改成update

    线程一：CAS操作{发现balance.value的对象①==① ,那么可以修改成功，balance.value-->对象②}
    线程二：CAS操作{发现balance.value的对象②!=① ,那么修改失败，重试...}

     */
    @Override
    public void withdraw(BigDecimal amount) {
        while (true) {
            BigDecimal expect = balance.get();
            BigDecimal update = expect.subtract(amount);  //新对象
            if (balance.compareAndSet(expect, update)) {
                break;
            }
        }
    }
//    @Override
//    public void withdraw(BigDecimal amount) {
//        balance.getAndUpdate(expect -> expect.subtract(amount));
//
////        balance.getAndUpdate(new UnaryOperator<BigDecimal>() {
////            @Override
////            public BigDecimal apply(BigDecimal expect) {
////                return expect.subtract(amount);
////            }
////        });
//    }
}


interface DecimalAccount {
    // 获取余额
    BigDecimal getBalance();

    // 取款
    void withdraw(BigDecimal amount);

    /**
     * 方法内会启动 1000 个线程，每个线程做 -10 元 的操作
     * 如果初始余额为 10000 那么正确的结果应当是 0
     */
    static void demo(DecimalAccount account) {
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            ts.add(new Thread(() -> {
                account.withdraw(BigDecimal.TEN);
            }));
        }
        ts.forEach(Thread::start);
        ts.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.out.println(account.getBalance());
    }
}

