## 一、 共享模型之无锁

Java中 synchronized 和 ReentrantLock 等 独占锁 就是 悲观锁 思想的实现。

在Java中java.util.concurrent.atomic包下面的原子变量类就是使用了乐观锁的一种实现方式 CAS 实现的。

管程即monitor是阻塞式的悲观锁实现并发控制，这章我们将通过非阻塞式的乐观锁的来实现并发控制。



### 1、 问题提出

有如下需求，保证account.withdraw取款方法的线程安全, 下面使用synchronized保证线程安全

```java
/**
 * 使用重量级锁synchronized来保证多线程访问共享资源发生的安全问题
 *
 */
@Slf4j
public class Test1 {

    public static void main(String[] args) {
        Account.demo(new AccountUnsafe(10000));
        Account.demo(new AccountCas(10000));
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


interface Account {
    // 获取余额
    Integer getBalance();

    // 取款
    void withdraw(Integer amount);

    /**
     * Java8之后接口新特性, 可以添加默认方法
     * 方法内会启动 1000 个线程，每个线程做 -10 元 的操作
     * 如果初始余额为 10000 那么正确的结果应当是 0
     */
    static void demo(Account account) {
        List<Thread> ts = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            ts.add(new Thread(() -> {
                account.withdraw(10);
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
        long end = System.nanoTime();
        System.out.println(account.getBalance()
                + " cost: " + (end - start) / 1000_000 + " ms");
    }
}
```



### 2、解决思路-无锁

上面的代码中使用synchronized加锁操作来保证线程安全，但是 synchronized加锁操作太耗费资源 (因为底层使用了操作系统mutex指令, 造成内核态和用户态的切换)，这里我们使用 无锁 来解决此问题。

```java
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

```



### 3、 CAS 与 volatile

使用原子操作来保证线程访问共享资源的安全性, cas+重试的机制来确保(乐观锁思想), 相对于悲观锁思想的synchronized,reentrantLock来说, cas的方式效率会更好!



#### 3.1、cas + 重试 的原理

```java
@Override
public void withdraw(Integer amount) {
    // 核心代码
    // 需要不断尝试，直到成功为止
    while (true){
        // 比如拿到了旧值 1000
        int prev = balance.get();
        // 在这个基础上 1000-10 = 990
        int next = prev - amount;
        /*
         compareAndSet 保证操作共享变量安全性的操作:
         ① 线程A首先获取balance.get(),拿到当前的balance值prev
         ② 根据这个prev值 - amount值 = 修改后的值next
         ③ 调用compareAndSet方法, 首先会判断当初拿到的prev值,是否和现在的
         	balance值相同;
         	3.1、如果相同,表示其他线程没有修改balance的值, 此时就可以将next值
         		设置给balance属性
         	3.2、如果不相同,表示其他线程也修改了balance值, 此时就设置next值失败, 
				然后一直重试, 重新获取balance.get()的值,计算出next值,
				并判断本次的prev和balnce的值是否相同...重复上面操作
		*/
        //compareAndSet()是原子的操作
        if (balance.compareAndSet(prev,next)){
            break;
        }
    }
}
```

其中的关键是 compareAndSet（比较并设置值），它的简称就是 CAS （也有 Compare And Swap 的说法），**它必须是原子操作**。

![cc0c](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/cc0c.png)

流程 :

当一个线程要去修改Account对象中的值时，先获取值prev（调用get方法），然后再将其设置为新的值next（调用cas方法）。在调用cas方法时，会将prev与Account中的余额进行比较。

- 如果两者相等，就说明该值还未被其他线程修改，此时便可以进行修改操作。
- 如果两者不相等，就不设置值，重新获取值prev（调用get方法），然后再将其设置为新的值next（调用cas方法），直到修改成功为止。

**注意 :**

其实 CAS 的底层是 lock cmpxchg 指令（X86 架构），在单核 CPU 和多核 CPU 下都能够保证【比较-交换】的 原子性。

在多核状态下，某个核执行到带 lock 的指令时，CPU 会让总线锁住，当这个核把此指令执行完毕，再开启总线。这个过程中不会被线程的调度机制所打断，保证了多个线程对内存操作的准确性，是原子的。

![20210202190842397](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20210202190842397.png)



#### 3.2、volatile的作用

在上面代码中的AtomicInteger类，保存值的value属性使用了volatile 修饰。获取共享变量时，为了保证该变量的可见性，需要使用 volatile 修饰。

volatile可以用来修饰 成员变量和静态成员变量，他可以避免线程从自己的工作缓存中查找变量的值，必须到主存中获取它的值，线程操作 volatile 变量都是直接操作主存。即一个线程对 volatile 变量的修改，对另一个线程可见。





#### 3.3、为什么CAS+重试(无锁)效率高

使用CAS+重试---无锁情况下，即使重试失败，线程始终在高速运行，没有停歇，而 synchronized会让线程在没有获得锁的时候，发生上下文切换，进入阻塞。

打个比喻：线程就好像高速跑道上的赛车，高速运行时，速度超快，一旦发生上下文切换，就好比赛车要减速、熄火，等被唤醒又得重新打火、启动、加速… 恢复到高速运行，代价比较大

但无锁情况下，因为线程要保持运行，需要额外 CPU 的支持，CPU 在这里就好比高速跑道，没有额外的跑道，线程想高速运行也无从谈起，虽然不会进入阻塞，但由于没有分到时间片，仍然会进入可运行状态，还是会导致上下文切换。



#### 3.4、CAS 的特点 (乐观锁和悲观锁的特点)

结合 CAS 和 volatile 可以实现无锁并发，适用于线程数少、多核 CPU 的场景下。

CAS 是基于乐观锁的思想：最乐观的估计，不怕别的线程来修改共享变量，就算改了也没关系，我吃亏点再重试呗。

synchronized 是基于悲观锁的思想：最悲观的估计，得防着其它线程来修改共享变量，我上了锁你们都别想改，我改完了解开锁，你们才有机会。

CAS 体现的是无锁并发、无阻塞并发，请仔细体会这两句话的意思

- 因为没有使用 synchronized，所以线程不会陷入阻塞，这是效率提升的因素之一
- 但如果竞争激烈(写操作多)，可以想到重试必然频繁发生，反而效率会受影响



### 4、原子整数类 - AtomicInteger

`java.util.concurrent.atomic`并发包提供了一些并发工具类，这里把它分成五类：

使用原子的方式 (共享数据为基本数据类型原子类)

- AtomicInteger：整型原子类

- AtomicLong：长整型原子类

- AtomicBoolean ：布尔型原子类

上面三个类提供的方法几乎相同，所以我们将以 AtomicInteger为例子来介绍。

先讨论原子整数类，以 AtomicInteger 为例讨论它的api接口：通过观察源码可以发现:

AtomicInteger 内部都是通过cas的原理来实现的

```java
public static void main(String[] args) {
    AtomicInteger i = new AtomicInteger(0);
    
    // 获取并自增（i = 0, 结果 i = 1, 返回 0），类似于 i++
    System.out.println(i.getAndIncrement());
    
    // 自增并获取（i = 1, 结果 i = 2, 返回 2），类似于 ++i
    System.out.println(i.incrementAndGet());
    
    // 自减并获取（i = 2, 结果 i = 1, 返回 1），类似于 --i
    System.out.println(i.decrementAndGet());
    
    // 获取并自减（i = 1, 结果 i = 0, 返回 1），类似于 i--
    System.out.println(i.getAndDecrement());
    
    // 获取并加值（i = 0, 结果 i = 5, 返回 0）
    System.out.println(i.getAndAdd(5));
    
    // 加值并获取（i = 5, 结果 i = 0, 返回 0）
    System.out.println(i.addAndGet(-5));
    
    // 获取并更新（i = 0, p 为 i 的当前值, 结果 i = -2, 返回 0）
    System.out.println(i.getAndUpdate(p -> p - 2));
    
    // 更新并获取（i = -2, p 为 i 的当前值, 结果 i = 0, 返回 0）
    System.out.println(i.updateAndGet(p -> p + 2));
    
    System.out.println(i.getAndAccumulate(10, (p, x) -> p + x));

}
```



### 5、原子引用 (AtomicReference)

原子引用的作用: **保证引用类型的共享变量是线程安全的**。

原子引用类型：

- AtomicReference：引用类型原子类
- AtomicMarkableReference：原子更新带有标记的引用类型。该类将 boolean 标记与引用关联起来。
- AtomicStampedReference：原子更新带有版本号的引用类型。该类将整数值与引用关联起来，可用于解决原子的更新数据和数据的版本号



示例 : 使用原子引用实现BigDecimal存取款的线程安全

```java
public class Test1 {

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

    @Override
    public void withdraw(BigDecimal amount) {
        while (true) {
            BigDecimal prev = balance.get();
            BigDecimal next = prev.subtract(amount);
            if (balance.compareAndSet(prev, next)) {
                break;
            }
        }
    }
    
//    @Override
//    public void withdraw(BigDecimal amount) {
//        balance.getAndUpdate(expect -> expect.subtract(amount));
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

```



#### 理解

```java
/*
AtomicReference中的compareAndSet(V expect, V update)方法，调用的是Unsafe.compareAndSwapObject(this, valueOffset, expect, update)
线程一                           线程二
expect = BigDecimal(10000)-①    expect = BigDecimal(10000)-①
update = BigDecimal(9990)-②     update = BigDecimal(9990)-③
① ② ③ 分别为三个对象

Unsafe.compareAndSwapObject(...)方法比较的是this对象的valueOffset位置的引用对象，是否同expect,从而判断是否修改成update

线程一：CAS操作{发现balance.value的对象①==① ,那么可以修改成功，balance.value-->对象②}
线程二：CAS操作{发现balance.value的对象②!=① ,那么修改失败，重试...}

 */
@Override
public void withdraw(BigDecimal amount) {
    while (true) {
        BigDecimal prev = balance.get();
        BigDecimal next = prev.subtract(amount);
        if (balance.compareAndSet(prev, next)) {
            break;
        }
    }
}
```



#### 理解之加深

**其中关键的CAS操作调用的是`UnSafe`类中`compareAndSwapObject()`方法，比较的是引用的对象是否改变，准确的来说比较的是对象的地址，对象中内容的改变对此操作是没有任何影响的！**



示例1：

```java
//identityHashCode()可以代表对象的内存地址
@Slf4j
public class BigDecimalTest {

    private static AtomicReference<BigDecimal> b = new AtomicReference<>(new BigDecimal("10000"));


    public static void main(String[] args) throws InterruptedException {
        BigDecimal prev = b.get();
        log.debug("identityHashCode：{}",System.identityHashCode(b.get()));
        log.debug("开始的值:{}",prev.intValue());
        log.debug("开始操作...");
        //执行操作
        operation();
        Thread.sleep(500);

        boolean b = BigDecimalTest.b.compareAndSet(prev, prev.subtract(new BigDecimal("1000")));
        log.debug("修改成功: {}",b);

    }


    private static void operation() throws InterruptedException{
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
            //虽然BigDecimal.value没有变化，但是已经是两个对象了
            b.compareAndSet(prev,prev.subtract(new BigDecimal("-1000")));
            log.debug("identityHashCode：{}",System.identityHashCode(b.get()));
        }," t2 ").start();
    }


}
```

![WM-Screenshots-20220508162954](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/WM-Screenshots-20220508162954.png)



示例2：

```java
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


    private static void operation() throws InterruptedException{
        new Thread(()->{
            log.debug("10 -> 0");
            BigDecimal prev = b.get();
            b.compareAndSet(prev,prev.subtract(BigDecimal.TEN));
            log.debug("identityHashCode：{}",System.identityHashCode(b.get()));
        }," t1 ").start();
        
        Thread.sleep(500);
        
        new Thread(()->{
            log.debug("0 -> 10");
            BigDecimal prev = b.get();
            b.compareAndSet(prev,prev.add(BigDecimal.TEN));
            log.debug("identityHashCode：{}",System.identityHashCode(b.get()));
        }," t2 ").start();
    }

}
```

![WM-Screenshots-20220508204537](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/WM-Screenshots-20220508204537.png)



总结：(表述可能有问题，按照案例理解下就行)

只要compareAndSet()操作中，预期对象发生了改变，那么CAS操作就不会成功，也说明了引用的对象中的值  是否与 之前引用的对象的值相同与否 对CAS操作不会有任何影响。还有，如果中间对`private volatile V value`进行多次修改，只要最后的引用的对象没有发生变化，CAS操作就能成功。



#### 理解之加深Ⅱ

示例1：

```java
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
```

![WM-Screenshots-20220508211247](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/WM-Screenshots-20220508211247.png)



示例2：

```java
//操作的是堆中的String对象 两个new String("A")的地址不相同
@Slf4j
public class BigDecimalTest4 {

    static AtomicReference<String> ref = new AtomicReference<>(new String("A"));

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
            log.debug("change A->B :{}" ,ref.compareAndSet(ref.get(), new String("B")));
            log.debug("identityHashCode：{}",System.identityHashCode(ref.get()));
        }," t1 ").start();
        Thread.sleep(500);
        new Thread(() -> {
            // 同上, 修改为A
            log.debug("change B->A :{}" ,ref.compareAndSet(ref.get(), new String("A")));
            log.debug("identityHashCode：{}",System.identityHashCode(ref.get()));
        }," t2 ").start();
    }
}
```

![WM-Screenshots-20220508211316](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/WM-Screenshots-20220508211316.png)



主线程仅能判断出 **CAS操作时共享变量的值** 与 **最初ref.get()值 **是否相同，不能感知到这种从 A 改为 B 又改回 A 的情况，如果主线程希望：只要有其它线程【动过】共享变量，那么自己的 cas 就算失败，这时，仅比较值是不够的，需要再加一个版本号。使用`AtomicStampedReference`来解决。



#### 1、AtomicStampedReference(版本号)

```java
public class Test4 {
    //指定版本号
    static AtomicStampedReference<String> ref = new AtomicStampedReference<>("A", 0);

    public static void main(String[] args) {
        new Thread(() -> {
            String pre = ref.getReference();
            //获得版本号
            int stamp = ref.getStamp(); // 此时的版本号还是第一次获取的
            System.out.println("change");
            try {
                other();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //把ref中的A改为C,并比对版本号，如果版本号相同，就执行替换，并让版本号+1
            System.out.println("change A->C stamp:" + stamp+ " " + ref.compareAndSet(pre, "C", stamp, stamp + 1));
        }).start();
    }

    static void other() throws InterruptedException {
        new Thread(() -> {
            int stamp = ref.getStamp();
            System.out.println("change A->B stamp:" + stamp + " " + ref.compareAndSet(ref.getReference(), "B", stamp, stamp + 1));
        }).start();
        Thread.sleep(500);
        new Thread(() -> {
            int stamp = ref.getStamp();
            System.out.println("change B->A stamp:" + stamp + " " + ref.compareAndSet(ref.getReference(), "A", stamp, stamp + 1));
        }).start();
    }
}
```

![WM-Screenshots-20220508212701](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/WM-Screenshots-20220508212701.png) 

会同时判断 引用 和 版本号 是否发生了变化。



#### 2、AtomicMarkableReference (标记cas的共享变量是否被修改过)

AtomicStampedReference 可以给原子引用加上版本号，追踪原子引用整个的变化过程，如：A -> B -> A ->C，通过AtomicStampedReference，我们可以知道，引用变量中途被更改了几次。

但是有时候，并不关心引用变量更改了几次，只是单纯的关心是否更改过，所以就有了AtomicMarkableReference



![84bc](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/84bc.png) 

```java
public class Test5 {
    public static void main(String[] args) throws InterruptedException {
        GarbageBag bag = new GarbageBag("装满了垃圾");

        // 参数2 mark 可以看作一个标记，表示垃圾袋满了
        AtomicMarkableReference<GarbageBag> ref = new AtomicMarkableReference<>(bag, true);
        log.debug("主线程 start...");

        GarbageBag prev = ref.getReference();
        log.debug(prev.toString());

        new Thread(() -> {
            log.debug("打扫卫生的线程 start...");
            bag.setDesc("空垃圾袋");
            while (!ref.compareAndSet(bag, bag, true, false)) {
            }
            log.debug(bag.toString());
        }).start();

        Thread.sleep(1000);
        log.debug("主线程想换一只新垃圾袋？");

        boolean success = ref.compareAndSet(prev, new GarbageBag("空垃圾袋"), true, false);
        log.debug("换了么？" + success);
        log.debug(ref.getReference().toString());
    }
}

class GarbageBag {
    String desc;

    public GarbageBag(String desc) {
        this.desc = desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return super.toString() + " " + desc;
    }
}
```

其实就是AtomicStampedReference的简化



AtomicStampedReference和AtomicMarkableReference两者的区别：

- AtomicStampedReference 需要我们传入 整型变量 作为版本号，来判定是否被更改过
- AtomicMarkableReference需要我们传入布尔变量 作为标记，来判断是否被更改过



### 6、原子数组 (AtomicIntegerArray)

保证数组内的元素的线程安全

使用原子的方式更新数组里的某个元素

- AtomicIntegerArray：整形数组原子类
- AtomicLongArray：长整形数组原子类
- AtomicReferenceArray：引用类型数组原子类

上面三个类提供的方法几乎相同，所以我们这里以 AtomicIntegerArray 为例子来介绍。实例代码



普通数组内元素, 多线程访问造成安全问题

```java
public class AtomicArrayTest {
    public static void main(String[] args) {
        demo(
                () -> new int[10],
                array -> array.length,
                (array, index) -> array[index]++,
                array -> System.out.println(Arrays.toString(array))
        );

        demo(
                ()-> new AtomicIntegerArray(10),
                AtomicIntegerArray::length,
                AtomicIntegerArray::getAndIncrement,
                System.out::println
        );
    }

    /**
     * 参数1，提供数组、可以是线程不安全数组或线程安全数组
     * 参数2，获取数组长度的方法
     * 参数3，自增方法，回传 array, index
     * 参数4，打印数组的方法
     */
    // supplier 提供者 无中生有 ()->结果
    // function 函数 一个参数一个结果 (参数)->结果 , BiFunction (参数1,参数2)->结果
    // consumer 消费者 一个参数没结果 (参数)->void, BiConsumer (参数1,参数2)->void
    private static <T> void demo(Supplier<T> arraySupplier, Function<T, Integer> lengthFun,
                                 BiConsumer<T, Integer> putConsumer, Consumer<T> printConsumer) {
        List<Thread> ts = new ArrayList<>();
        T array = arraySupplier.get();
        int length = lengthFun.apply(array);

        for (int i = 0; i < length; i++) {
            // 创建10个线程, 每个线程对数组作 10000 次操作
            ts.add(new Thread(() -> {
                for (int j = 0; j < 10000; j++) {
                    putConsumer.accept(array, j % length);
                }
            }));
        }

        ts.forEach(Thread::start); // 启动所有线程
        ts.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }); // 等所有线程结束

        printConsumer.accept(array);
    }
}

```

```sout
[6520, 6497, 6481, 6463, 6458, 6454, 6465, 6446, 6484, 6463]
[10000, 10000, 10000, 10000, 10000, 10000, 10000, 10000, 10000, 10000]
```



### 7、字段更新器

保证多线程访问同一个对象的成员变量时, 成员变量的线程安全性。

- AtomicReferenceFieldUpdater —引用类型的属性
- AtomicIntegerFieldUpdater —整形的属性
- AtomicLongFieldUpdater —长整形的属性



注意：利用字段更新器，可以针对对象的某个域（Field）进行原子操作，只能配合 volatile 修饰的字段使用，否则会出现异常:

```
AtomicReferenceFieldUpdaterImpl() {
	...
	if (!Modifier.isVolatile(modifiers))
                throw new IllegalArgumentException("Must be volatile type");
	...
}
```

```java
Exception in thread "main" java.lang.IllegalArgumentException: Must be volatile type
```

示例：

```java
@Slf4j
public class Test6 {
    public static void main(String[] args) {
        Student stu = new Student();
        // 获得原子更新器
        // 泛型
        // 参数1 持有属性的类 参数2 被更新的属性的类
        // newUpdater中的参数：第三个为属性的名称
        AtomicReferenceFieldUpdater<Student,String> updater = AtomicReferenceFieldUpdater.newUpdater(Student.class, String.class, "name");
        // 期望的为null, 如果name属性没有被别的线程更改过, 默认就为null, 此时匹配, 就可以设置name为张三
        System.out.println(updater.compareAndSet(stu, null, "张三"));
        System.out.println(updater.compareAndSet(stu, stu.name, "王五"));
        System.out.println(stu);
    }
}

class Student {
    volatile String name;

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                '}';
    }
}
```



### 8、原子累加器 (LongAddr)

- LongAddr
- LongAccumulator
- DoubleAddr
- DoubleAccumulator



累加器性能比较 AtomicLong, LongAddr:

```java
@Slf4j(topic = "guizy.Test")
public class Test {
    public static void main(String[] args) {
        System.out.println("----AtomicLong----");
        for (int i = 0; i < 5; i++) {
            demo(() -> new AtomicLong(), adder -> adder.getAndIncrement());
        }

        System.out.println("----LongAdder----");
        for (int i = 0; i < 5; i++) {
            demo(() -> new LongAdder(), adder -> adder.increment());
        }
    }

    private static <T> void demo(Supplier<T> adderSupplier, Consumer<T> action) {
        T adder = adderSupplier.get();
        long start = System.nanoTime();
        List<Thread> ts = new ArrayList<>();
        // 4 个线程，每人累加 50 万
        for (int i = 0; i < 40; i++) {
            ts.add(new Thread(() -> {
                for (int j = 0; j < 500000; j++) {
                    action.accept(adder);
                }
            }));
        }
        ts.forEach(t -> t.start());
        ts.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long end = System.nanoTime();
        System.out.println(adder + " cost:" + (end - start) / 1000_000);
    }
}
```

```sout
----AtomicLong----
2000000 cost:45
2000000 cost:36
2000000 cost:27
2000000 cost:37
2000000 cost:33
----LongAdder----
2000000 cost:17
2000000 cost:4
2000000 cost:4
2000000 cost:4
2000000 cost:4
```

LongAddr

- 性能提升的原因很简单，就是在有竞争时，设置多个累加单元(但不会超过cpu的核心数)，Therad-0 累加 Cell[0]，而 Thread-1 累加Cell[1]… 最后将结果汇总。这样它们在累加时操作的不同的 Cell 变量，因此减少了 CAS 重试失败，从而提高性能。

AtomicLong

- 之前的AtomicLong等都是在一个共享资源变量上进行竞争, while(true)循环进行CAS重试, 性能没有LongAdder高



LongAdder原理，有实力再去读源码...



### 9、Unsafe类

Unsafe 对象提供了非常底层的，操作内存、线程的方法，Unsafe 对象不能直接调用，只能通过反射获得；可以发现AtomicInteger以及其他的原子类, 底层都使用的是Unsafe类



示例：使用底层的`Unsafe类`实现原子操作 (开发中不建议直接调用此类)

```java
public class Test8 {
    public static void main(String[] args) throws Exception {
        // 通过反射获得Unsafe对象
        Class unsafeClass = Unsafe.class;
        // 获得构造函数，Unsafe的构造函数为私有的
        Constructor constructor = unsafeClass.getDeclaredConstructor();
        // 设置为允许访问私有内容
        constructor.setAccessible(true);
        // 创建Unsafe对象
        Unsafe unsafe = (Unsafe) constructor.newInstance();

        // 创建Person对象
        Person person = new Person();
        // 获得其属性 name 的偏移量
        long nameOffset = unsafe.objectFieldOffset(Person.class.getDeclaredField("name"));
        long ageOffset = unsafe.objectFieldOffset(Person.class.getDeclaredField("age"));

        // 通过unsafe的CAS操作改变值
        unsafe.compareAndSwapObject(person, nameOffset, null, "guizy");
        unsafe.compareAndSwapInt(person, ageOffset, 0, 22);
        System.out.println(person);
    }
}

class Person {
    // 配合CAS操作，必须用volatile修饰
    volatile String name;
    volatile int age;

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
```



示例2：实现原子正数类,并测试

```java
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
```

```sout
0 cost: 89 ms
```











