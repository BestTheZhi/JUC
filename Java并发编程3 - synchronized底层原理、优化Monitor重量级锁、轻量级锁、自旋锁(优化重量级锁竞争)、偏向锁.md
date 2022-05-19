## 一、 锁优化

### 1、 Java 对象头

对象头包含两部分：运行时元数据（Mark Word）和类型指针 (Klass Word)

1.运行时元数据

- `哈希值（HashCode）`，可以看作是堆中对象的地址
- `GC分代年龄（年龄计数器）` (用于新生代from/to区晋升老年代的标准, 阈值为15)
- 锁状态标志 (用于JDK1.6对synchronized的优化 -> 轻量级锁)
- 线程持有的锁
- 偏向线程ID (用于JDK1.6对synchronized的优化 -> 偏向锁)
- 偏向时间戳

2.类型指针

- 指向`类元数据InstanceKlass`，确定该对象所属的类型。指向的其实是方法区中存放的类元信息

说明：如果对象是数组，还需要记录数组的长度



Makr Word(存储对象自身的运行时数据，如哈希码，GC分代年龄等，在32位和64位的Java虚拟机中分别会占用32或64个比特)

以 32 位虚拟机为例,普通对象的对象头结构如下，其中的`Klass Word`为`类型指针`，指向`方法区`对应的`Class对象`；

![d1d6](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/d1d6.png)



数组对象

![e254](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/e254.png)



对象在不同状态时，**其中 Mark Word 结构为: `无锁(001)、偏向锁(101)、轻量级锁(00)、重量级锁(10)`**

![4162](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/4162.png)



所以一个对象的结构如下：

![580b](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/580b.png) 



### 2、 Monitor 原理 (Synchronized底层实现-重量级锁)

>**多线程同时访问临界区: 使用重量级锁**
>
>- JDK6对Synchronized的优先状态：`偏向锁–>轻量级锁–>重量级锁`

`Monitor`被翻译为`监视器`或者`管程`

每个Java对象都可以关联一个(操作系统的)Monitor，如果使用synchronized给对象上锁（重量级），该`对象头的MarkWord`中就被设置为指向Monitor对象的指针 

>下图原理解释:
>
>当Thread2访问到synchronized(obj)中的共享资源的时候
>
>- 首先会将synchronized中的`锁对象`中对象头的MarkWord去尝试指向操作系统的Monitor对象. 让锁对象中的MarkWord和Monitor对象相关联. 如果关联成功, 将obj对象头中的MarkWord的对象状态从01改为10。
>- 因为Monitor没有和其他的obj的MarkWord相关联, 所以Thread2就成为了该Monitor的Owner(所有者)。
>- 又来了个`Thread1`执行synchronized(obj)代码, 它首先会看看能不能执行该临界区的代码; 它会检查obj是否关联了Montior, 此时已经有关联了, 它就会去看看该Montior有没有所有者(Owner), 发现有所有者了(Thread2); Thread1 也会和该Monitor关联, 该线程就会进入到它的`EntryList(阻塞队列)`;
>- 当 Thread2 执行完临界区代码后, Monitor的Owner(所有者)就空出来了. 此时就会通知Monitor中的EntryList阻塞队列中的线程, 这些线程通过竞争, 成为新的所有者

![20201219192811839](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201219192811839.png)

--

![6eba](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/6eba.png)

- 刚开始时`Monitor`中的Owner为null
- 当Thread-2 执行`synchronized(obj){}`代码时就会将Monitor的所有者Owner 设置为 Thread-2，上锁成功，Monitor中同一时刻只能有一个Owner
- 当Thread-2 占据锁时，如果线程Thread-3，Thread-4也来执行synchronized(obj){}代码，就会进入EntryList中变成`BLOCKED状态`
- Thread-2 执行完同步代码块的内容，然后唤醒 EntryList 中等待的线程来竞争锁，竞争时是非公平的 (仍然是抢占式)
- **图中 WaitSet 中的Thread-0，Thread-1 是之前获得过锁，但条件不满足进入 WAITING 状态的线程**，后面讲wait-notify 时会分析



>它加锁就是依赖底层操作系统的 `mutex`相关指令实现, 所以会造成`用户态和内核态之间的切换`, 非常耗性能 !
>
>在JDK6的时候, 对synchronized进行了优化, 引入了`轻量级锁, 偏向锁`, 它们是在JVM的层面上进行加锁逻辑, 就没有了切换时性能的消耗~



### 3、synchronized原理

实例代码：

```java
static final Object lock = new Object();
static int counter = 0;
public static void main(String[] args) {
    synchronized (lock) { 
        counter++;
    }
}
```

反编译后的部分字节码  `java -p  *.class`  指令

![20201219201521709](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201219201521709.png)

**字节码 异常表中的信息就是，如果在锁期间发生了异常，也会释放锁。**

### 4、synchronized 原理进阶

小故事: 方便后面理解 `偏向锁`,`轻量级锁`

![20201219202939493](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201219202939493.png) 

![20201219203225659](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201219203225659.png) 

![202101191526347](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/202101191526347.png) 



### 5、轻量级锁 (用于优化Monitor这类的重量级锁）

>轻量级锁的设计初衷实在没有多线程竞争的前提下，减少传统的重量级锁使用操作系统互斥量产生的性能消耗。

轻量级锁的使用场景: 如果一个对象虽然有多个线程要对它进行加锁，但是加锁的时间是错开的（也就是没有人可以竞争的），那么可以使用轻量级锁来进行优化。

轻量级锁对使用者是透明的，即语法仍然是synchronized (jdk6对synchronized的优化)，假设有两个方法同步块，利用同一个对象加锁

eg: 线程A来操作临界区的资源, 给资源加锁,到执行完临界区代码,释放锁的过程, 没有线程来竞争, 此时就可以使用轻量级锁; 如果**这期间有线程来竞争的话, 就会升级为重量级锁**



#### 轻量级锁的工作过程

--参考自《深入理解Java虚拟机》

在代码即将进入同步块时，如果此同步对象没有被锁定(锁标志位为“01”状态)，虚拟机首先在当前线程的栈帧中创建一个名为锁记录(Lock Record)的空间,用于存储锁对象目前的Mark word的拷贝，此时的状态如下图：

![cc79](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/cc79.png)

(Object reference 在书中写做 owner)

然后，虚拟机将使用CAS操作尝试把对象的Mark Word更新为指向Lock Record的指针。如果更新动作成功了，即代表该线程拥有了这个对象的锁，并且对象的Mark Word的锁标记位转变为“00”，表示此对象处于轻量级锁定状态，此时线程堆栈与对象头的状态就如下图所示：

![6eaf](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/6eaf.png)



如果这个更新操作失败了，那就意味着至少存在一条线程与当前线程竞争获取该对象的锁。虚拟机首先会检查对象的Mark Word是否指向当前线程的栈帧：

- 如果是：说明当前线程已经拥有了这个对象的锁，那直接进入同步块中继续执行就可以了。
- 否则：**就说明这个锁对象已经被其他线程抢占。如果出现两条以上的线程争用同一个锁的情况，那轻量级锁就不再有效，必须膨胀为重量级锁**，锁标志的状态变为“10”，此时Mark Word中存储的就是指向重量级锁(互斥量)的指针，后面等待锁的线程也必须进入阻塞状态。



它的解锁过程也同样是通过CAS操作来进行的，如果对象的Mark Word仍然指向线程的锁记录，那就用CAS操作把对象当前的Mark Word和线程中复制的Mark Word替换回来。假如能够成功替换，那整个同步过程就顺利完成了，如果替换失败，则是说有其他线程尝试过获取该锁，就要在释放锁的同时，唤醒被挂起的线程。

轻量级锁能提升程序同步性能的依据是“对于绝大部分的锁，在整个同步周期内都是不存在竞争的”这一经验法则。如果没有竞争，轻量级锁便通过CAS操作，成功避免了使用互斥量的开销；但如果确实存在锁竞争，除了互斥量的本身开销外，还额外发生了CAS操作的开销。因此在有竞争的情况下，轻量级锁反而会比传统的重量级锁更慢。



---



-- 视频中老师是这样描述的：

每次指向到`synchronized代码块`时(对象无锁状态下)，都会在`栈帧中`创建`锁记录（Lock Record）对象`，**`每个线程都会包括一个锁记录的结构`**，锁记录内部可以储存`对象的MarkWord`和`锁对象引用reference`:

![cc79](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/cc79.png)

让锁记录中的Object reference指向锁对象地址，并且尝试用CAS(compare and sweep)将栈帧中的锁记录的(lock record 地址 00)替换Object对象的Mark Word，将Mark Word 的值(01)存入锁记录(lock record地址)中 ------相互替换

![7660](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/7660.png)

>视频中老师讲的和《深入理解Java虚拟机》中描述的有些许出入，但是线程栈帧Lock Record和对象Mark Word最终的状态是一样的。

如果CAS替换成功, 获得了轻量级锁，那么对象的对象头储存的就是锁记录的地址和状态00，如下所示：

- 线程中锁记录, 记录了锁对象的锁状态标志; 锁对象的对象头中存储了锁记录的地址和状态, 标志哪个线程获得了锁
- 此时栈帧中就存储了对象的对象头中的锁状态标志,年龄计数器,哈希值等; 对象的对象头中就存储了栈帧中锁记录的地址和状态00, 这样的话对象就知道了是哪个线程拥有此锁。

![6eaf](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/6eaf.png)



如果CAS替换失败，有两种情况 : ① 锁膨胀 ② 重入锁失败

- 1、如果是其它线程已经持有了该Object的轻量级锁，那么表示有竞争，将进入 锁膨胀阶段(此时对象Object对象头中已经存储了别的线程的锁记录地址 00,指向了其他线程)
- 2、如果是自己的线程已经执行了synchronized进行加锁，那么再添加一条 Lock Record 作为重入锁的计数 – 线程多次加锁, 锁重入
  - 在下面代码中,临界区中又调用了method2, method2中又进行了一次synchronized加锁操作, 此时就会在虚拟机栈中再开辟一个method2方法对应的栈帧(栈顶), 该栈帧中又会存在一个独立的Lock Record, 此时它发现对象的对象头中指向的就是自己线程中栈帧的锁记录; 加锁也就失败了. 这种现象就叫做锁重入; 线程中有多少个锁记录, 就能表明该线程对这个对象加了几次锁 (锁重入计数)

![1a3b](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/1a3b.png)

```java
static final Object obj = new Object();
public static void method1() {
     synchronized( obj ) {
         // 同步块 A
         method2();
     }
}
public static void method2() {
     synchronized( obj ) {
         // 同步块 B
     }
}
```



轻量级锁解锁流程 :

当线程退出synchronized代码块的时候，如果获取的是取值为 null 的锁记录 ，表示有锁重入，这时重置锁记录，表示重入计数减一

当线程退出synchronized代码块的时候，如果获取的锁记录取值不为 null，那么使用CAS将Mark Word的值恢复给对象, 将直接替换的内容还原。

- 成功则解锁成功 (轻量级锁解锁成功)
- 失败，表示有竞争, 则说明轻量级锁进行了锁膨胀或已经升级为重量级锁，进入重量级锁解锁流程 (Monitor流程)

 

### 6、锁膨胀

如果在尝试`加轻量级锁`的过程中，`CAS替换操作无法成功`，这时**有一种情况就是其它线程已经为这个对象加上了轻量级锁**，这时就要进行`锁膨胀(有竞争)`，将轻量级锁变成重量级锁。

当 Thread-1 进行轻量级加锁时，Thread-0 已经对该对象加了轻量级锁, 此时发生`锁膨胀`

![ea82](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/ea82.png)



这时Thread-1加轻量级锁失败，进入锁膨胀流程
因为Thread-1线程加轻量级锁失败, 轻量级锁没有阻塞队列的概念, 所以此时就要为对象申请Monitor锁(重量级锁)，让Object指向重量级锁地址 10，然后自己进入Monitor 的EntryList 变成BLOCKED状态

![20201219214748700](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201219214748700.png)



当Thread-0 线程执行完synchronized同步块时，使用CAS将Mark Word的值恢复给对象头, 肯定恢复失败(因为对象的对象头中存储的是重量级锁的地址,状态变为10了之前的是00)。那么会进入重量级锁的解锁过程，即按照Monitor的地址找到Monitor对象，将Owner设置为null，唤醒EntryList中的Thread-1线程。



### 7、自旋锁与自适应自旋

**互斥同步对性能最大的影响是阻塞的实现，挂起线程和恢复线程的操作都需要转入内核态中完成，这些操作给Java虚拟机的并发性能带来了很大的压力**。同时，虚拟机的开发团队也注意到在许多应用上，**共享数据的锁定状态只会持续很短的一段时间，为了这段时间去挂起和恢复线程并不值得**。现在绝大多数的个人电脑和服务器都是多路（核）处理器系统，如果物理机器有一个以上的处理器或者处理器核心，能让两个或以上的线程同时并行执行，我们就可以让后面请求锁的那个线程“稍等一会”，但不放弃处理器的执行时间，看看持有锁的线程是否很快就会释放锁。**为了让线程等待，我们只须让线程执行一个忙循环（自旋），这项技术就是所谓的自旋锁**

自旋锁在JDK 1.4.2中就已经引入，只不过默认是关闭的，可以使用-XX：+UseSpinning参数来开启，在JDK 6中就已经改为默认开启了。自旋等待不能代替阻塞，且先不说对处理器数量的要求，自旋等待本身虽然避免了线程切换的开销，但它是要占用处理器时间的，所以如果锁被占用的时间很短，自旋等待的效果就会非常好，反之如果锁被占用的时间很长，那么自旋的线程只会白白消耗处理器资源，而不会做任何有价值的工作，这就会带来性能的浪费。因此自旋等待的时间必须有一定的限度，如果自旋超过了限定的次数仍然没有成功获得锁，就应当使用传统的方式去挂起线程。自旋次数的默认值是十次，用户也可以使用参数-XX：PreBlockSpin来自行更改

不过无论是默认值还是用户指定的自旋次数，对整个Java虚拟机中所有的锁来说都是相同的。在JDK 6中对自旋锁的优化，引入了自适应的自旋。自适应意味着自旋的时间不再是固定的了，而是由前一次在同一个锁上的自旋时间及锁的拥有者的状态来决定的。如果在同一个锁对象上，自旋等待刚刚成功获得过锁，并且持有锁的线程正在运行中，那么虚拟机就会认为这次自旋也很有可能再次成功，进而允许自旋等待持续相对更长的时间，比如持续100次忙循环。另一方面，如果对于某个锁，自旋很少成功获得过锁，那在以后要获取这个锁时将有可能直接省略掉自旋过程，以避免浪费处理器资源。有了自适应自旋，随着程序运行时间的增长及性能监控信息的不断完善，虚拟机对程序锁的状况预测就会越来越精准，虚拟机就会变得越来越“聪明”了



1.自旋重试成功的情况：

![9c4c](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/9c4c.png)

2.`自旋重试失败的情况`，**自旋了一定次数还是没有等到 持锁的线程释放锁**, 线程2就会加入Monitor的阻塞队列(EntryList)

![aef0](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/aef0.png)



### 8、偏向锁 (用于优化轻量级锁重入)

它的目的是消除数据在无竞争情况下的同步原语，进一步提高程序的运行性能。如果说轻量级锁是在**无竞争情况下**使用CAS操作，去消除同步使用的互斥量，那么偏向锁就是在无竞争的情况下把整个同步都消除掉，连CAS操作都不用去做了。

这个锁会偏向于第一个获得它的线程，如果该锁一直没有被其他的线程获取，则持有偏向锁的线程将永远不需要再进行该同步。



在`轻量级的锁`中，我们可以发现，如果同一个线程对同一个对象进行`重入锁`时，**也需要执行CAS替换操作，这是有点耗时。**

那么java6开始引入了`偏向锁`，将进入临界区的线程的ID, 直接设置给锁对象的Mark word, 下次该线程又获取锁, 发现线程ID是自己, 就不需要CAS操作了。

- 升级为轻量级锁的情况 (会进行偏向锁撤销) : 获取偏向锁的时候, 发现线程ID不是自己的, 此时通过CAS替换操作, 操作成功了, 此时该线程就获得了锁对象。( 此时是交替访问临界区, 撤销偏向锁, 升级为轻量级锁)
- 升级为重量级锁的情况 (会进行偏向锁撤销) : 获取偏向锁的时候, 发现线程ID不是自己的, 此时通过CAS替换操作, 操作失败了, 此时说明发生了锁竞争。( 此时是多线程访问临界区, 撤销偏向锁, 升级为重量级锁)

偏向锁可以提高带有同步但无竞争的程序性能，但它同样是一个带有效益权衡性质的优化，也就是说它并非总是对程序运行有利。如果程序中大多数的锁都总是被多个不同的线程访问，那么偏向模式就是多余的(会撤销偏向锁),可以使用参数`-XX:UseBiasedLocking`来禁止锁优化反而可以提升性能。



![20210202174407252](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20210202174407252.png) 

![20210202174448323](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20210202174448323.png) 



---



#### 8.1 偏向锁状态

 64 位虚拟机 ,普通对象的Mark Word结构如下:

![bbc0](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/bbc0.png)

- Normal：一般状态，没有加任何锁，前面62位保存的是对象的信息，最后2位为状态（01），倒数第三位表示是否使用偏向锁（未使用：0）

- Biased：偏向状态，使用偏向锁，前面54位保存的当前线程的ID，最后2位为状态（01），倒数第三位表示是否使用偏向锁（使用：1）
- Lightweight：使用轻量级锁，前62位保存的是锁记录的指针，最后2位为状态（00）
- Heavyweight：使用重量级锁，前62位保存的是Monitor的地址指针，最后2位为状态(10)

如果开启了偏向锁（默认开启），在创建对象时，对象的Mark Word后三位应该是101,但是偏向锁默认是**有延迟**的，不会再程序一启动就生效，而是会在程序运行一段时间（几秒之后），才会对创建的对象设置为偏向状态,如果没有开启偏向锁，对象的Mark Word后三位应该是001。



#### 8.2 对象的创建后的Mark word

如果开启了偏向锁（默认是开启的），那么对象刚创建之后，Mark Word 最后三位的值101，并且这是它的ThreadId，epoch，age(年龄计数器)都是0，在加锁的时候进行设置这些的值.

偏向锁默认是延迟的，不会在程序启动的时候立刻生效，如果想避免延迟，可以添加虚拟机参数来禁用延迟：`-XX:BiasedLockingStartupDelay=0`来禁用延迟

注意 : 处于偏向锁的对象解锁后，线程id仍存储于对象头中



---

这里会使用到一个工具类，其`ClassLayout.parseInstance(obj).toPrintable()`方法可以打印对象的对象头信息,其依赖如下:

```xml
<dependency>
    <groupId>org.openjdk.jol</groupId>
    <artifactId>jol-core</artifactId>
    <version>0.10</version>
</dependency>
```

但是我们所需要关注的就只是**对象头中Mark Word**,在这里，我根据`ClassLayout.parseInstance(obj).toPrintable()`方法打印某个类的对象,在其结果上做进一步处理，解析出Mark Word信息。具体的实现如下：

```java
public class PrintMarkWord {
    //根据toPrintable()的结果，一步步解析字符串，直到输出8bit的Mark Word
    public static String print(Object obj){
        String[] strs = ClassLayout.parseInstance(obj).toPrintable().split("                           ");
        String[] strss = strs[2].split("\\(|\\)");
        String[] strss1 = strs[3].split("\\(|\\)");

        StringBuilder sb = new StringBuilder();

        String[] strsss1 = strss1[1].split(" ");
        for(int i=3 ; i>=0 ;i--) {
            sb.append(strsss1[i]);
            sb.append(" ");
        }

        String[] strsss = strss[1].split(" ");
        for(int i=3 ; i>=0 ;i--) {
            sb.append(strsss[i]);
            sb.append(" ");
        }
        return sb.toString();
    }
}
```



---



**测试**：一个对象创建后，加锁前后 对象头中Mark Word信息，并测试上述工具类正确性：

```java
/**
 * -XX:BiasedLockingStartupDelay=0 关闭偏向锁延迟
 */
@Slf4j()
public class Test1 {

    public static void main(String[] args) {
        Dog d = new Dog();
        new Thread(()->{
            System.out.println("synchronized 前 ----- ");
            log.debug(PrintMarkWord.print(d));
            synchronized (d){
                System.out.println("synchronized 中 ----- ");
                log.debug(PrintMarkWord.print(d));
                System.out.println("对象头中信息 ----- ");
                System.out.println(ClassLayout.parseInstance(d).toPrintable());
            }
            System.out.println("synchronized 后 ----- ");
            log.debug(PrintMarkWord.print(d));
        },"t1").start();
    }

}

Class Dog{
    
}
```

![WM-Screenshots-20220427223857](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/WM-Screenshots-20220427223857.png)

可以看到，初始状态是可偏向的，一个线程获取偏向锁，锁对象的Mark Word中会记录线程ID，处于偏向锁的对象解锁后，线程ID仍存储于对象头中；其次比较两个输出结果，可以看到解析无误。



**测试禁用偏向锁**：如果没有开启偏向锁，那么对象创建后最后三位的值为001，为无锁状态，这时候它的hashcode，age都为0，hashcode是第一次用到hashcode时才赋值的。在上面测试代码运行时在添加 VM 参数`-XX:-UseBiasedLocking`禁用偏向锁（禁用偏向锁则优先使用轻量级锁），退出synchronized状态变回 `001`

- 禁止偏向锁, 虚拟机参数`-XX:-UseBiasedLocking`; 优先使用轻量级锁
- 输出结果: 最开始状态为001，然后加轻量级锁变成00，最后恢复成001

![20201219231656738](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/20201219231656738.png)



#### 8.3 撤销偏向锁-HashCode()

当对象进入偏向状态的时候，Mark Word大部分的空间(23个比特）都用于存储持有锁的线程ID了,这部分空间占用了原有存储对象哈希码的位置,那原来对象的哈希码怎么办呢?

在Java语言里面一个对象如果计算过哈希码，就应该一直保持该值不变，否则很多依赖对象哈希码的API都可能存在出错风险。而作为绝大多数对象哈希码来源的Object:.hashCode()方法，返回的是对象的一致性哈希码（Identity Hash Code)，这个值是能强制保证不变的,它通过在对象头中存储计算结果来保证第一次计算之后,再次调用该方法取到的哈希码值永远不会再发生改变。**因此,当一个对象已经计算过一致性哈希码后,它就再也无法进入偏向锁状态了 ; 而当一个对象当前正处于偏向锁状态，又收到需要计算其一致性哈希码请求时,它的偏向状态会被立即撤销,并且锁会膨胀为重量级锁。**在重量级锁的实现中,对象头指向了重量级锁的位置,代表重量级锁的ObjectMonitor类里有字段可以记录非加锁状态(标志位为“01”)下的 Mark Word,其中自然可以存储原来的哈希码。

---

测试：

```java
/**
 * -XX:BiasedLockingStartupDelay=0
 */
@Slf4j
public class Test4 {
    public static void main(String[] args) {
        Dog d = new Dog();
        new Thread(()->{
            log.debug(PrintMarkWord.print(d));
            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }

            System.out.println(d.hashCode()); //计算其一致性哈希码

            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }
            log.debug(PrintMarkWord.print(d));

        },"t1").start();     
    }
}
```

![WM-Screenshots-20220428114014](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/WM-Screenshots-20220428114014.png)

这里始终只有一个线程获取锁，初始为偏向锁，调用对象的一致性哈希码之后，再尝试去获取锁，发现锁对象已经升级成轻量级锁，解锁后，变成无锁状态。

>对于轻量级锁，获取锁的线程栈帧中有锁记录（Lock Record）空间，用于存储Mark Word的拷贝，官方称之为Displaced Mark Word，该拷贝中可以包含identity hash code，所以轻量级锁可以和identity hash code共存；对于重量级锁，ObjectMonitor类里有字段可以记录非加锁状态下的Mark Word，其中也可以存储identity hash code的值，所以重量级锁也可以和identity hash code共存。



#### 8.4 撤销偏向锁 - 调用 wait/notify()

因为只有重量级锁才支持这两个方法。

测试：

```java
/**
 * -XX:BiasedLockingStartupDelay=0
 */
@Slf4j
public class Test5 {
    public static void main(String[] args) throws InterruptedException {
        Dog d = new Dog();

        Thread t1= new Thread(()->{
            log.debug(PrintMarkWord.print(d));
            System.out.println("获取锁对象");
            synchronized (d){
                try {
                    log.debug(PrintMarkWord.print(d));
                    System.out.println("调用wait()方法");
                    d.wait(10);
                    log.debug(PrintMarkWord.print(d));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("释放锁");
            log.debug(PrintMarkWord.print(d));
        },"t1");
        t1.start();
        t1.join();

        new Thread(()->{
            System.out.println("等待t1结束后，线程t2再尝试获取");
            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }
        },"t2").start();

    }
}
```

![WM-Screenshots-20220428120522](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/WM-Screenshots-20220428120522.png)

可以观察到，初始状态为可偏向的，t1获取锁之后，偏向t1，调用wait()之后，锁对象升级成重量级锁，释放锁之后，之后再有线程去获取锁，都是走重量级锁流程。



#### 8.5 撤销偏向锁-多线程访问

1.如果一个偏向锁对象已经偏向了一个线程t1(线程t1此时已经释放了锁)，此时另一个线程t2尝试来获取锁，(交替获取，没有发生锁的竞争)，此时锁对象会升级成轻量级锁。

2.如果当t2线程尝试获取锁时，t1线程还没有释放锁，此时发生了锁竞争，锁对象会升级成重量级锁。



**偏向锁撤销, 升级轻量级锁：**

使用`wait` 和 `notify` 来辅助实现，以便两个线程先后执行

```java
/**
 * -XX:BiasedLockingStartupDelay=0
 */
@Slf4j
public class Test3 {

    public static void main(String[] args) {
        Dog d = new Dog();

        new Thread(()->{
            log.debug(PrintMarkWord.print(d));
            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }
            log.debug(PrintMarkWord.print(d));

            synchronized (String.class){
                String.class.notify();
            }
        },"t1").start();

        System.out.println();
        new Thread(()->{
            synchronized (String.class){
                try {
                    String.class.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            log.debug(PrintMarkWord.print(d));
            synchronized (d){
                log.debug(PrintMarkWord.print(d));
            }
            log.debug(PrintMarkWord.print(d));
        },"t2").start();
    }

}
```

结果:

![WM-Screenshots-20220428110741](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/WM-Screenshots-20220428110741.png)

输出结果，最开始使用的是偏向锁，但是第二个线程尝试获取对象锁时(前提是: 线程一已经释放掉锁了,也就是执行完synchroized代码块)，发现本来对象偏向的是线程一，那么偏向锁就会失效，加的就是轻量级锁



#### 8.6 批量重偏向

如果对象被多个线程访问，但是没有竞争 (上面撤销偏向锁就是这种情况: 一个线程执行完, 另一个线程再来执行, 没有竞争), **这时偏向T1的对象仍有机会重新偏向T2**,也就是说，不会把已偏向的锁升级成轻量级锁，而是会重新偏向另一个线程。发生的条件是撤销偏向锁阈值超过20次。

测试：

```java

/**
 * -XX:BiasedLockingStartupDelay=0
 */
@Slf4j
public class Test6 {
    public static void main(String[] args) throws InterruptedException {
        Vector<Dog> list = new Vector<>();
        Thread t1 = new Thread(()->{
            for (int i = 0; i < 30; i++) {
                Dog d = new Dog();
                list.add(d);
                synchronized (d){
                    log.debug(i+"\t"+ PrintMarkWord.print(d));
                }
            }
        },"t1");
        t1.start();
        t1.join();


        Thread t2 = new Thread(()->{
            log.debug("-------------------->");
            for (int i = 0; i < 30; i++) {
                Dog d = list.get(i);
                log.debug(i+"\t"+PrintMarkWord.print(d));
                synchronized (d){
                    log.debug(i+"\t"+PrintMarkWord.print(d));
                }
                log.debug(i+"\t"+PrintMarkWord.print(d));
            }
        },"t2");
        t2.start();
    }
}
```

![WM-Screenshots-20220428144553](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/WM-Screenshots-20220428144553.png)

可以看到，19次输出之后，发生了重新偏向。



#### 8.7 批量撤销偏向锁

当 撤销偏向锁的阈值超过40以后 ，就会将**整个类的对象**都改为**不可偏向**的，新建此类的对象也是不可偏向的。



### 9、同步省略 (锁消除)

线程同步的代价是相当高的，同步的后果是降低并发性和性能。

在动态编译同步块的时候，JIT编译器可以借助**逃逸分析**来判断同步块所使用的锁对象是否只能够被一个线程访问而没有被发布到其他线程。

如果没有，那么JIT编译器在编译这个同步块的时候就会取消对这部分代码的同步。这样就能大大提高并发性和性能。这个取消同步的过程就叫同步省略，也叫锁消除。

也许读者会有疑问,变量是否逃逸，对于虚拟机来说是需要使用复杂的过程间分析才能确定的，但是程序员自己应该是很清楚的,怎么会在明知道不存在数据争用的情况下还要求同步呢?这个问题的答案是:有许多同步措施并不是程序员自己加入的，同步的代码在Java程序中出现的频繁程度也许超过了大部分人的想象。我们来看看如下例子,这段非常简单的代码仅仅是输出三个字符串相加的结果，无论是源代码字面上，还是程序语义上都没有进行同步。

```java
public String concatString(String s1,String s2,String s3){
    return s1 + s2 + s3;
}
```

我们也知道、由于String是一个不可变的类，对字符串的连接操作总是通过生成新的String对象来进行的、因此Javac编译器会对String连接做自动优化。在JDK5之前,字符串加法会转化为StringBuffer对象的连续append()操作，在JDK 5及以后的版本中，会转化为StringBuilder对象的连续 append()操作。即代码可能会变成如下所示的样子。

```java
public String concatString(String s1,String s2,String s3){
    StringBuffer sb = new StringBuffer();
    sb.append(s1);
    sb.append(s2);
    sb.append(s3);
    return sb.toString();
}
```

现在大家还认为这段代码没有涉及同步吗?每个 StringBuffer.append()方法中都有一个同步块，锁就是sb对象。虚拟机观察变量sb,经过逃逸分析后会发现它的动态作用域被限制在 concatString(方法内部。也就是sb的所有引用都永远不会逃逸到concatString()方法之外，其他线程无法访问到它，所以这里虽然有锁，但是可以被安全的消除掉。再解释执行时这里仍然会加锁，但在经过服务端编译器的即时编译之后，这段代码就会忽略所有的同步措施而直接执行。



**说明：**

由于Java底层的原理在网上的解释"万紫千红"，并且程序的运行结果可能会与JDK版本等各种因素有关，我也翻阅了很多资料想尽可能地 把原理给搞清。上面的陈述不一定十分的精准，如果自己觉得那里有问题的话还是要自己去找权威的资料解决。锁优化的知识在《深入理解Java虚拟机》中有描述，也可以翻阅《Java并发编程的艺术》。

