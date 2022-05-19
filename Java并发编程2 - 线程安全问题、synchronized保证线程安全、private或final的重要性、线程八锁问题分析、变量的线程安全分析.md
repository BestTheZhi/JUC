![20210202223437264](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20210202223437264.png) 



## 一、 线程安全问题

>[对线面试官-多线程基础](https://mp.weixin.qq.com/s/TPZ2NBFy6niBq7b6FOJp4Q)

### 1、 线程出现问题的根本原因分析

线程出现问题的根本原因是**因为线程上下文切换**，导致线程里的指令没有执行完就切换执行其它线程了，下面举一个例子

```java
public class Test {
	static int count = 0;
	public static void main(String[] args) throws InterruptedException {
	    Thread t1 = new Thread(()->{
	        for (int i = 1; i < 5000; i++){
	            count++;
	        }
	    });
	    Thread t2 =new Thread(()->{
	        for (int i = 1; i < 5000; i++){
	            count--;
	        }
	    });
	    t1.start();
	    t2.start();
	    t1.join(); // 主线程等待t1线程执行完
	    t2.join(); // 主线程等待t2线程执行完
	    
	    // main线程只有等待t1, t2线程都执行完之后, 才能打印count, 否则main线程不会等待t1,t2
	    // 直接就打印count的值为0
	    log.debug("count的值是{}",count);
	}
}

// 打印: 并不是我们期望的0值, 为什么呢? 看下文分析
09:42:42.921 guizy.ThreadLocalDemo [main] - count的值是511 
```



我们从字节码的层面进行分析：**因为在Java中对变量的 自增/自减 并不是原子操作**

![e0af6d100ae461307062b2631a5679e3](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/e0af6d100ae461307062b2631a5679e3.png) 

![e52fadc8e19d2c448b205ab01c79deb7](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/e52fadc8e19d2c448b205ab01c79deb7.png) 

```tex
getstatic i // 获取静态变量i的值
iconst_1 // 准备常量1
iadd // 自增
putstatic i // 将修改后的值存入静态变量i
    
getstatic i // 获取静态变量i的值
iconst_1 // 准备常量1
isub // 自减
putstatic i // 将修改后的值存入静态变量i
```

可以看到count++ 和 count-- 操作实际都是需要这个4个指令完成的，那么这里问题就来了！Java 的内存模型如下，完成静态变量的自增，自减需要在主存和工作内存中进行数据交换：

![9e7c4a40edd2941dcb71bc21f257f05a](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/9e7c4a40edd2941dcb71bc21f257f05a.png) 



如果代码是正常按顺序运行的，那么count的值不会计算错

![12121](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/12121.png) 

出现负数的情况：一个线程没有完成一次完整的自增/自减(多个指令) 的操作, 就被别的线程进行操作, 此时就会出现线程安全问题:

![55e6](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/55e6.png) 



出现正数的情况同理，主要就是因为线程的++/--操作不是一个原子操作, 在执行4条指令期间被其他线程抢夺cpu



### 2、 问题的进一步描述

#### 临界区

- 一个程序运行多线程本身是没有问题的
- 问题出现在多个线程共享资源(临界资源)的时候
  - 多个线程同时对共享资源进行读操作本身也没有问题 - 对读操作没问题
  - 问题出现在对对共享资源同时进行读写操作时就有问题了 - 同时读写操作有问题
- 先定义一个叫做临界区的概念：一段代码内如果存在对共享资源的多线程读写操作，那么称这段代码为临界区; 共享资源也成为临界资源

```java
static int counter = 0;
static void increment() 
// 临界区 
{   
   counter++; 
}

static void decrement() 
// 临界区 
{ 
   counter--; 
}
```



#### 竞态条件

多个线程在临界区执行，那么由于代码指令的执行不确定而导致的结果问题，称为竞态条件



### 3、 synchronized 解决方案

为了避免临界区中的竞态条件发生，由多种手段可以达到

- 阻塞式解决方案： synchronized , Lock (ReentrantLock)
- 非阻塞式解决方案： 原子变量 (CAS)

现在讨论使用synchronized来进行解决，即俗称的对象锁，它采用互斥的方式让同一时刻至多只有一个线程持有对象锁，其他线程如果想获取这个锁就会阻塞住，这样就能保证拥有锁的线程可以安全的执行临界区内的代码，不用担心线程上下文切换

>注意: 虽然Java 中互斥和同步都可以采用 synchronized 关键字来完成，但它们还是有区别的：
>
>- 互斥是保证临界区的竞态条件发生，同一时刻只能有一个线程执行临界区的代码
>- 同步是由于线程执行的先后，顺序不同但是需要一个线程等待其它线程运行到某个点。



#### 3.1 synchronized语法

```java
synchronized(对象) { // 线程1获得锁， 那么线程2的状态是(blocked)
 	临界区
}
```

上面的实例程序使用synchronized后如下，计算出的结果是正确！

```java
static int counter = 0;
static final Object room = new Object();
public static void main(String[] args) throws InterruptedException {
     Thread t1 = new Thread(() -> {
         for (int i = 0; i < 5000; i++) {
         	 // 对临界资源(共享资源的操作) 进行 加锁
             synchronized (room) {
             counter++;
        	}
 		}
 	}, "t1");
     Thread t2 = new Thread(() -> {
         for (int i = 0; i < 5000; i++) {
             synchronized (room) {
             counter--;
         }
     }
     }, "t2");
     t1.start();
     t2.start();
     t1.join();
     t2.join();
     log.debug("{}",counter);
}

09:56:24.210 guizy.ThreadLocalDemo [main] - count的值是0
```



#### 3.2 synchronized原理

synchronized实际上利用对象锁保证了临界区代码的原子性，临界区内的代码在外界看来是不可分割的，不会被线程切换所打断

小故事:

![20201219110609489](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201219110609489.png) 

![20201219110651392](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201219110651392.png) 



![9745](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/9745.png) 



总结:

当多个线程对临界资源进行写操作的时候, 此时会造成线程安全问题, 如果使用synchronized关键字, 对象锁一定要是多个线程共有的, 才能避免竞态条件的发生。



#### 3.3、synchronized 加在方法上

加在实例方法上, 锁对象就是对象实例

```java
public class Demo {
	//在方法上加上synchronized关键字
	public synchronized void test() {
	
	}
	//等价于
	public void test() {
		synchronized(this) {
		
		}
	}
}
```

加在静态方法上, 锁对象就是当前类的Class实例

```java
public class Demo {
	//在静态方法上加上synchronized关键字
	public synchronized static void test() {
	
	}
	//等价于
	public void test() {
		synchronized(Demo.class) {
		
		}
	}
}
```



## 二、'线程八锁'案例分析

其实就是考察synchronized 锁住的是哪个对象, 如果锁住的是同一对象, 就不会出现线程安全问题,对synchronized关键字熟悉的可以直接跳过。

1、锁住同一个对象都是this（e1对象），结果为：1,2或者2,1

```java
/**
 * Description: 不会出现安全问题, 打印结果顺序为: 1/2 或 2/1
 *
 * @author guizy
 * @date 2020/12/19 11:24
 */
@Slf4j(topic = "guizy.EightLockTest")
public class EightLockTest {
    // 锁对象就是this, 也就是e1
    public synchronized void a() {
        log.debug("1");
    }
//    public void a () {
//        synchronized (this) {
//            log.debug("1");
//        }
//    }

    // 锁对象也是this, e1
    public synchronized void b() {
        log.debug("2");
    }

    public static void main(String[] args) {
        EightLockTest e1 = new EightLockTest();
        new Thread(() -> e1.a()).start();
        new Thread(() -> e1.b()).start();
    }
}
```

2、锁住同一个对象都是this（e1对象），结果为：1s后1,2 || 2,1s后1

```java
/**
 * Description: 不会出现安全问题, 打印结果顺序为: 1s后1,2 || 2,1s后1
 *
 * @author guizy
 * @date 2020/12/19 11:24
 */
@Slf4j(topic = "guizy.EightLockTest")
public class EightLockTest {
    // 锁对象就是this, 也就是e1
    public synchronized void a(){
        Thread.sleep(1000);
        log.debug("1");
    }

    // 锁对象也是this, e1
    public synchronized void b() {
        log.debug("2");
    }

    public static void main(String[] args) {
        EightLockTest e1 = new EightLockTest();
        new Thread(() -> e1.a()).start();
        new Thread(() -> e1.b()).start();
    }
}
```

3、a，b锁住同一个对象都是this（e1对象），c没有上锁。结果为：3,1s后1,2 || 2,3,1s后1 || 3,2,1s后1

```java
/**
 * Description: 会出现安全问题, 因为前两个线程, 执行run方法时, 都对相同的对象加锁;
 *              而第三个线程,调用的方法c, 并没有加锁, 所以它可以同前两个线程并行执行;
 *  打印结果顺序为: 分析: 因为线程3和线程1,2肯定是并行执行的, 所以有以下情况
 *               3,1s后1,2 || 2,3,1s后1 || 3,2,1s后1
 *               至于 1,3,2的情况是不会发生的, 可以先调用到1,但需要sleep一秒.3肯定先执行了
 *
 * @author guizy
 * @date 2020/12/19 11:24
 */
@Slf4j(topic = "guizy.EightLockTest")
public class EightLockTest {
    // 锁对象就是this, 也就是e1
    public synchronized void a() throws InterruptedException {
        Thread.sleep(1000);
        log.debug("1");
    }

    // 锁对象也是this, e1
    public synchronized void b() {
        log.debug("2");
    }

    public void c() {
        log.debug("3");
    }

    public static void main(String[] args) {
        EightLockTest e1 = new EightLockTest();
        new Thread(() -> {
            try {
                e1.a();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> e1.b()).start();
        new Thread(() -> e1.c()).start();
    }
}
```

4、a锁住对象this（n1对象），b锁住对象this（n2对象），不互斥。结果为：2,1s后1

```java
/**
 * Description: 会出现安全问题, 线程1的锁对象为e1, 线程2的锁对象为e2. 所以他们会同一时刻执行1,2
 *
 * @author guizy
 * @date 2020/12/19 11:24
 */
@Slf4j(topic = "guizy.EightLockTest")
public class EightLockTest {
    // 锁对象是e1
    public synchronized void a() {
    	Thread.sleep(1000);
        log.debug("1");
    }

    // 锁对象是e2
    public synchronized void b() {
        log.debug("2");
    }

    public static void main(String[] args) {
        EightLockTest e1 = new EightLockTest();
        EightLockTest e2 = new EightLockTest();
        new Thread(() -> e1.a()).start();
        new Thread(() -> e2.b()).start();
    }
}
```

5、a锁住的是EightLockTest.class对象, b锁住的是this(e1),不会互斥; 结果: 2,1s后1

```java
/**
 * Description: 会发生安全问题, 因为a锁住的是EightLockTest.class对象, b锁住的是this(e1),不会互斥
 *              结果: 2,1s后1
 *
 * @author guizy
 * @date 2020/12/19 11:24
 */
@Slf4j(topic = "guizy.EightLockTest")
public class EightLockTest {
    // 锁对象是EightLockTest.class类对象
    public static synchronized void a() {
        Thread.sleep(1000);
        log.debug("1");
    }

    // 锁对象是e2
    public synchronized void b() {
        log.debug("2");
    }

    public static void main(String[] args) {
        EightLockTest e1 = new EightLockTest();
        new Thread(() -> e1.a()).start();
        new Thread(() -> e1.b()).start();
    }
}
```

6、a,b锁住的是EightLockTest.class对象, 会发生互斥; 结果为：2,1s后1 || 1s后1,2

```java
/**
 * Description: 不会发生安全问题, 因为a,b锁住的是EightLockTest.class对象, 会发生互斥
 *              结果: 2,1s后1 || 1s后1,2
 *
 * @author guizy
 * @date 2020/12/19 11:24
 */
@Slf4j(topic = "guizy.EightLockTest")
public class EightLockTest {
    // 锁对象是EightLockTest.class类对象
    public static synchronized void a() {
        Thread.sleep(1000);
        log.debug("1");
    }

    // 锁对象是EightLockTest.class类对象
    public static synchronized void b() {
        log.debug("2");
    }

    public static void main(String[] args) {
        EightLockTest e1 = new EightLockTest();
        new Thread(() -> e1.a()).start();
        new Thread(() -> e1.b()).start();
    }
}
```

7、a锁住的是EightLockTest.class对象, b锁住的是this(e1),不会互斥; 结果: 2,1s后1

```java
/**
 * Description: 会发生安全问题, 因为a锁住的是EightLockTest.class对象, b锁住的是this(e1),不会互斥
 *              结果: 2,1s后1
 *
 * @author guizy
 * @date 2020/12/19 11:24
 */
@Slf4j(topic = "guizy.EightLockTest")
public class EightLockTest {
    // 锁对象是EightLockTest.class类对象
    public static synchronized void a() {
        Thread.sleep(1000);
        log.debug("1");
    }

    // 锁对象是this,e2对象
    public synchronized void b() {
        log.debug("2");
    }

    public static void main(String[] args) {
        EightLockTest e1 = new EightLockTest();
        EightLockTest e2 = new EightLockTest();
        new Thread(() -> e1.a()).start();
        new Thread(() -> e2.b()).start();
    }
}
```

8、a,b锁住的是EightLockTest.class对象, 会发生互斥; 结果为：2,1s后1 || 1s后1,2

```java
/**
 * Description: 不会发生安全问题, 因为a,b锁住的是EightLockTest.class对象, 会发生互斥
 *              结果: 2,1s后1 || 1s后1,2
 *
 * @author guizy
 * @date 2020/12/19 11:24
 */
@Slf4j(topic = "guizy.EightLockTest")
public class EightLockTest {
    // 锁对象是EightLockTest.class类对象
    public static synchronized void a() {
        Thread.sleep(1000);
        log.debug("1");
    }

    // 锁对象是EightLockTest.class类对象
    public static synchronized void b() {
        log.debug("2");
    }

    public static void main(String[] args) {
        EightLockTest e1 = new EightLockTest();
        EightLockTest e2 = new EightLockTest();
        new Thread(() -> e1.a()).start();
        new Thread(() -> e2.b()).start();
    }
}
```



## 三、 变量的线程安全分析

### 1、 成员变量和静态变量的线程安全分析

- 如果变量没有在线程间共享，那么变量是安全的
- 如果变量在线程间共享
  - 如果只有读操作，则线程安全
  - 如果有读写操作，则这段代码是临界区，需要考虑线程安全



### 2、 局部变量线程安全分析

- 局部变量【局部变量被初始化为基本数据类型】是安全的
- 但局部变量引用的对象则未必 （要看该对象是否被共享且被执行了读写操作）
  - 如果该对象没有逃离方法的作用范围，它是线程安全的
  - 如果该对象逃离方法的作用范围，需要考虑线程安全



### 3、线程安全的情况

- 局部变量表是存在于栈帧中, 而虚拟机栈中又包括很多栈帧, 虚拟机栈是线程私有的;
- 局部变量【局部变量被初始化为基本数据类型】是安全的，示例如下

```java
public static void test1() {
     int i = 10;
     i++;
}
```

每个线程调用 test1() 方法时局部变量 i，会在每个线程的栈帧内存中被创建多份，因此不存在共享

![2020121913434871](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/2020121913434871.png)



### 4、线程不安全的情况

对成员变量的操作，需要考虑线程安全问题的，代码示例如下

>循环创建了100个线程, 在线程体里面都调用了method1方法, 在method1方法中又循环调用了100次method2,method3方法。方法2,3都使用到了成员变量arrayList, 此时的问题就是: 1个线程它会循环调用100次方法2和3, 一共有100个线程, 此时100个线程操作的共享资源就是arrayList成员变量 , 而且还进行了读写操作. 会造成线程不安全的问题

```java
public class Test15 {
    public static void main(String[] args) {
        UnsafeTest unsafeTest = new UnsafeTest();
        for (int i =0;i<100;i++){
            new Thread(()->{
                unsafeTest.method1();
            },"线程"+i).start();
        }
    }
}
class UnsafeTest{
    ArrayList<String> arrayList = new ArrayList<>();
    public void method1(){
        for (int i = 0; i < 100; i++) {
            method2();
            method3();
        }
    }
    private void method2() {
        arrayList.add("1");
    }
    private void method3() {
        arrayList.remove(0);
    }
}

Exception in thread "线程1" Exception in thread "线程2" java.lang.ArrayIndexOutOfBoundsException: -1
```



#### 4.1、不安全原因分析

无论哪个线程中的 method2 和 method3 引用的都是同一个对象中的 list 成员变量
一个 ArrayList ，在添加一个元素的时候，它可能会有两步来完成：

- 第一步: 在 arrayList[size]的位置存放此元素
- 第二步: size++

在多线程情况下，比如有两个线程，线程 A 先将元素存放在位置 0。但是此时 CPU 进行上下文切换 (线程A还没来得及size++)，线程 B 得到运行的机会。线程B也向此 ArrayList 添加元素，因为此时 Size 仍等于0, 然后两个线程再执行size++操作，都写回内存中，此时的size=1，执行了两次添加操作，而内存中的size值却为1，这样的话，再执行两次remove(0) 删除第一个位置的元素，就会导致数组越界。

ArrayList 源码：

```java
public Class Arraylist ...{
    
    public boolean add(E e) {
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        elementData[size++] = e;
        return true;
    }
    
    
    public E remove(int index) {
        rangeCheck(index);  //在此抛出异常

        modCount++;
        E oldValue = elementData(index);

        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
        elementData[--size] = null; // clear to let GC do its work

        return oldValue;
    }
    
    private void rangeCheck(int index) {
        if (index >= size)  0 >= 0
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
}
```



#### 4.2、解决方法

可以将list修改成局部变量，局部变量存放在栈帧中, 栈帧又存放在虚拟机栈中, 虚拟机栈是作为线程私有的;
因为method1方法, 将arrayList传给method2,method3方法, 此时他们三个方法共享这同一个arrayList, 此时不会被其他线程访问到, 所以不会出现线程安全问题, 因为这三个方法使用的同一个线程。
在外部, 创建了100个线程, 每个线程都会调用method1方法, 然后都会再从新创建一个新的arrayList对象, 这个新对象再传递给method2,method3方法.

```java
class UnsafeTest {
    public void method1() {
        ArrayList<String> arrayList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            method2(arrayList);
            method3(arrayList);
        }
    }

    private void method2(List<String> arrayList) {
        arrayList.add("1");
    }

    private void method3(List<String> arrayList) {
        arrayList.remove(0);
    }
}
```



#### 4.3、思考 private 或 final的重要性

提高线程的安全性 : 

方法访问修饰符带来的思考: 如果把method2和method3 的方法修改为public 会不会导致线程安全问题; 分情况:

- 情况1：有其它线程调用 method2 和 method3
  - 只修改为public修饰,此时不会出现线程安全的问题, 即使线程2调用method2/3方法, 给2/3方法传过来的list对象也是线程2调用method1方法时,传递给method2/3的list对象, 不可能是线程1调用method1方法传的对象。 具体原因看上面: 4.2解决方法。
- 情况2：在情况1 的基础上，为ThreadSafe 类添加子类，子类覆盖method2 或 method3方法，即如下所示： 从这个例子可以看出 private 或 final 提供【安全】的意义所在，请体会开闭原则中的【闭】

>如果改为public, 此时子类可以重写父类的方法, 在子类中开线程来操作list对象, 此时就会出现线程安全问题: 子类和父类共享了list对象
>
>如果改为private, 子类就不能重写父类的私有方法, 也就不会出现线程安全问题; 所以所private修饰符是可以避免线程安全问题.
>
>所以如果不想子类, 重写父类的方法的时候, 我们可以将父类中的方法设置为private, final修饰的方法, 此时子类就无法影响父类中的方法了!

```java
class ThreadSafe {
    public final void method1(int loopNumber) {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < loopNumber; i++) {
            method2(list);
            method3(list);
        }
    }
    private void method2(ArrayList<String> list) {
        list.add("1");
    }
    public void method3(ArrayList<String> list) {
        list.remove(0);
    }
}
class ThreadSafeSubClass extends ThreadSafe{
    @Override
    public void method3(ArrayList<String> list) {
        new Thread(() -> {
            list.remove(0);  //多个线程操作共享数据
        }).start();
    }
}
```



#### 4.4、 常见线程安全类

- String
- Integer
- StringBuffer
- Random
- Vector
- Hashtable
- java.util.concurrent 包下的类 JUC



这里说它们是线程安全的是指，多个线程调用它们同一个实例的某个方法时，是线程安全的 , 也可以理解为 它们的每个方法是原子的，它们的每个方法是原子的（方法都被加上了synchronized）
但注意它们多个方法的组合不是原子的，所以可能会出现线程安全问题

```java
Hashtable table = new Hashtable();
// 线程1，线程2
if( table.get("key") == null) {
 table.put("key", value);
}
```

这里只能是get方法内部是线程安全的, put方法内部是线程安全的. 组合起来使用还是会受到上下文切换的影响

![4555](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/4555.png)



**不可变类的线程安全**

String和Integer类都是不可变的类，因为其类内部状态是不可改变的，因此它们的方法都是线程安全的, 都被final修饰, 不能被继承.

肯定有些人他们知道String 有 replace，substring 等方法【可以】改变值啊，其实调用这些方法返回的已经是一个新创建的对象了！ (在字符串常量池中当修改了String的值,它不会再原有的基础上修改, 而是会重新开辟一个空间来存储)



#### 4.5、 示例分析-是否线程安全

示例一:

Servlet运行在Tomcat环境下并只有一个实例，因此会被Tomcat的多个线程共享使用，因此存在成员变量的共享问题。

```java
public class MyServlet extends HttpServlet {
	 // 是否安全？  否：HashMap不是线程安全的，HashTable是
	 Map<String,Object> map = new HashMap<>();
	 // 是否安全？  是:String 为不可变类，线程安全
	 String S1 = "...";
	 // 是否安全？ 是
	 final String S2 = "...";
	 // 是否安全？ 否：不是常见的线程安全类
	 Date D1 = new Date();
	 // 是否安全？  否：引用值D2不可变，但是日期里面的其它属性比如年月日可变。与字符串的最大区别是Date里面的属性可变。
	 final Date D2 = new Date();
 
	 public void doGet(HttpServletRequest request,HttpServletResponse response) {
	  // 使用上述变量
	 }
}
```



示例二:

分析线程是否安全，先对类的成员变量，类变量，局部变量进行考虑，如果变量会在各个线程之间共享，那么就得考虑线程安全问题了，如果变量A引用的是线程安全类的实例，并且只调用该线程安全类的一个方法，那么该变量A是线程安全的的。下面对实例一进行分析：此类不是线程安全的。**MyAspect切面类只有一个实例，成员变量start 会被多个线程同时进行读写操作**

Spring中的Bean都是单例的, 除非使用@Scope修改为多例。

```java
@Aspect
@Component 
public class MyAspect {
        // 是否安全？不安全, 因为MyAspect是单例的
        private long start = 0L;

        @Before("execution(* *(..))")
        public void before() {
            start = System.nanoTime();
        }

        @After("execution(* *(..))")
        public void after() {
            long end = System.nanoTime();
            System.out.println("cost time:" + (end-start));
        }
    }
```



示例三:

此例是典型的三层模型调用，MyServlet UserServiceImpl UserDaoImpl类都只有一个实例，UserDaoImpl类中没有成员变量，update方法里的变量引用的对象不是线程共享的，所以是线程安全的；UserServiceImpl类中只有一个线程安全的UserDaoImpl类的实例，那么UserServiceImpl类也是线程安全的，同理 MyServlet也是线程安全的

Servlet调用Service, Service调用Dao这三个方法使用的是同一个线程。

```java
public class MyServlet extends HttpServlet {
	 // 是否安全    是：UserService不可变，虽然有一个成员变量,
	 			// 但是是私有的, 没有地方修改它
	 private UserService userService = new UserServiceImpl();
	 
	 public void doGet(HttpServletRequest request, HttpServletResponse response) {
	 	userService.update(...);
	 }
}

public class UserServiceImpl implements UserService {
	 // 是否安全     是：Dao不可变, 其没有成员变量
	 private UserDao userDao = new UserDaoImpl();
	 
	 public void update() {
	 	userDao.update();
	 }
}

public class UserDaoImpl implements UserDao { 
	 // 是否安全   是：没有成员变量，无法修改其状态和属性
	 public void update() {
	 	String sql = "update user set password = ? where username = ?";
	 	// 是否安全   是：不同线程创建的conn各不相同，都在各自的栈内存中
	 	try (Connection conn = DriverManager.getConnection("","","")){
	 	// ...
	 	} catch (Exception e) {
	 	// ...
	 	}
	 }
}
```



示例四:

跟示例二大体相似，UserDaoImpl类中有成员变量，那么多个线程可以对成员变量conn 同时进行操作，故是不安全的

```java
public class MyServlet extends HttpServlet {
    // 是否安全
    private UserService userService = new UserServiceImpl();

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        userService.update(...);
    }
}

public class UserServiceImpl implements UserService {
    // 是否安全
    private UserDao userDao = new UserDaoImpl();
    public void update() {
       userDao.update();
    }
}

public class UserDaoImpl implements UserDao {
    // 是否安全: 不安全; 当多个线程,共享conn, 一个线程拿到conn,刚创建一个连接赋值给conn, 此时另一个线程进来了, 直接将conn.close
    //另一个线程恢复了, 拿到conn干事情, 此时conn都被关闭了, 出现了问题
    private Connection conn = null;
    public void update() throws SQLException {
        String sql = "update user set password = ? where username = ?";
        conn = DriverManager.getConnection("","","");
        // ...
        conn.close();
    }
}
```



示例五:

跟示例三大体相似，UserServiceImpl类的update方法中UserDao是作为局部变量存在的，所以每个线程访问的时候都会新建有一个UserDao对象，新建的对象是线程独有的，所以是线程安全的,但是开发中不建议这样写

```java
public class MyServlet extends HttpServlet {
    // 是否安全
    private UserService userService = new UserServiceImpl();
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        userService.update(...);
    }
}
public class UserServiceImpl implements UserService {
    public void update() {
        UserDao userDao = new UserDaoImpl();
        userDao.update();
    }
}
public class UserDaoImpl implements UserDao {
    // 是否安全
    private Connection = null;
    public void update() throws SQLException {
        String sql = "update user set password = ? where username = ?";
        conn = DriverManager.getConnection("","","");
        // ...
        conn.close();
    }
}
```



示例六:

```java
public abstract class Test {
    public void bar() {
        // 是否安全
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        foo(sdf);
    }
    public abstract foo(SimpleDateFormat sdf);
    public static void main(String[] args) {
        new Test().bar();
    }
}
```

其中foo 的行为是不确定的，可能导致不安全的发生，被称之为外星方法，因为foo方法可以被重写，导致线程不安全。 在String类中就考虑到了这一点，String类是final的，根本就不存在子类，更别说重写它的方法了。

```java
public void foo(SimpleDateFormat sdf) {
    String dateStr = "1999-10-11 00:00:00";
    for (int i = 0; i < 20; i++) {
        new Thread(() -> {
            try {
                sdf.parse(dateStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
```



#### 4.6 习题分析

卖票练习:

```java
@Slf4j(topic = "c.ExerciseSell")
public class ExerciseSell {
    public static void main(String[] args) throws InterruptedException {
        // 模拟多人买票
        TicketWindow window = new TicketWindow(1000);

        // 所有线程的集合（由于threadList在主线程中，不被共享，因此使用ArrayList不会出现线程安全问题）
        List<Thread> threadList = new ArrayList<>();
        // 卖出的票数统计(Vector为线程安全类)
        List<Integer> amountList = new Vector<>();
        for (int i = 0; i < 2000; i++) {
            Thread thread = new Thread(() -> {
                // 买票
                int amount = window.sell(random(5));
                // 统计买票数
                amountList.add(amount);
            });
            threadList.add(thread);
            thread.start();
        }

        for (Thread thread : threadList) {
            thread.join();
        }

        // 统计卖出的票数和剩余票数
        log.debug("余票：{}",window.getCount());
        log.debug("卖出的票数：{}", amountList.stream().mapToInt(i -> i).sum());
    }

    // Random 为线程安全
    static Random random = new Random();

    // 随机 1~5
    public static int random(int amount) {
        return random.nextInt(amount) + 1;
    }
}

// 售票窗口
class TicketWindow {
	// 票总数
    private int count;

    public TicketWindow(int count) {
        this.count = count;
    }

    // 获取余票数量
    public int getCount() {
        return count;
    }

    // 售票
    public synchronized int sell(int amount) {  //在此要加锁才线程安全
        if (this.count >= amount) {
            //在此sleep() 现象会更明显
            this.count -= amount;
            return amount;
        } else {
            return 0;
        }
    }
}
```



转账练习

```java
package cn.itcast.n4.exercise;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j(topic = "c.ExerciseTransfer")
public class ExerciseTransfer {
    public static void main(String[] args) throws InterruptedException {
        Account a = new Account(1000);
        Account b = new Account(1000);
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                a.transfer(b, randomAmount());
            }
        }, "t1");
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                b.transfer(a, randomAmount());
            }
        }, "t2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        // 查看转账2000次后的总金额
        log.debug("total:{}", (a.getMoney() + b.getMoney()));
    }

    // Random 为线程安全
    static Random random = new Random();

    // 随机 1~100
    public static int randomAmount() {
        return random.nextInt(100) + 1;
    }
}

// 账户
class Account {
    private int money;

    public Account(int money) {
        this.money = money;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    // 转账
    public void transfer(Account target, int amount) {
        synchronized(Account.class) {   //锁住Account类，因为涉及到A.money和B.money。
            if (this.money >= amount) {
                this.setMoney(this.getMoney() - amount);
                target.setMoney(target.getMoney() + amount);
            }
        }
    }
}

// 没问题, 最终的结果仍然是 2000元
```







