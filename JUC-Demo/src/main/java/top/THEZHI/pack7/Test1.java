package top.THEZHI.pack7;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ZHI LIU
 * @date 2022-05-08
 */
@Slf4j
public class Test1 {

    public static void main(String[] args) {
        Account.demo(new AccountUnsafe(10000));
//        Account.demo(new AccountCas(10000));
    }
}

class AccountCas implements Account {
    //使用原子整数: 底层使用CAS+重试的机制
    private AtomicInteger balance;

    public AccountCas(int balance) {
        this.balance = new AtomicInteger(balance);
    }

    @Override
    public Integer getBalance() {
        //得到原子整数的值
        return balance.get();
    }

    @Override
    public void withdraw(Integer amount) {
        while(true) {
            //获得修改前的值
            int prev = balance.get();
            //获得修改后的值
            int next = prev - amount;
            //比较并设置值
			/*
				此时的prev为共享变量的值, 如果prev被别的线程改了.也就是说: 自己读到的共享变量的值 和 共享变量最新值 不匹配,
				就继续where(true),如果匹配上了, 将next值设置给共享变量.

				AtomicInteger中value属性, 被volatile修饰, 就是为了确保线程之间共享变量的可见性.
			*/
            if(balance.compareAndSet(prev, next)) {
                break;
            }
        }
    }
}


class AccountUnsafe implements Account {
    private Integer balance;

    public AccountUnsafe(Integer balance) {
        this.balance = balance;
    }

    @Override
    public Integer getBalance() {
        synchronized (this) {
            return balance;
        }
    }

    @Override
    public void withdraw(Integer amount) {
        // 通过这里加锁就可以实现线程安全，不加就会导致线程安全问题
        synchronized (this) {
            balance -= amount;
        }
    }
}

