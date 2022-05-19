## 一、 不可变类设计 

如果一个对象在不能够修改其内部状态（属性），那么它就是线程安全的，因为不存在并发修改; 

类可以使用 final 修饰保证了该类中的方法不能被覆盖，防止子类无意间破坏不可变性。



### 1、日期转换的问题

问题提出：
下面的代码在运行时，由于 SimpleDateFormat 不是线程安全的

```java
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
for (int i = 0; i < 10; i++) {
	 new Thread(() -> {
	 try {
	 	log.debug("{}", sdf.parse("1951-04-21"));
	 } catch (Exception e) {
	 	log.error("{}", e);
	 }
	 }).start();
}
```

有很大几率出现 `java.lang.NumberFormatException:multiple points`或者出现不正确的日期解析结果



可以使用JD8中的 不可变日期格式化类：

>如果一个对象在不能够修改其内部状态（属性），那么它就是线程安全的，因为不存在并发修改啊！这样的对象在Java 中有很多，例如在 Java 8 后，提供了一个新的日期格式化类：

```java
DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
for (int i = 0; i < 10; i++) {
    new Thread(() -> {
        TemporalAccessor date = dtf.parse("2020-12-29");
        log.debug("{}", date);
    }).start();
}
```



### 2、final 的使用

Integer、Double、String、DateTimeFormatter以及基本类型包装类, 都是使用final来修饰的,以String类 为例，说明一下不可变类设计的要素

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
    /** The value is used for character storage. */
    private final char value[];	// 在JDK9 使用了byte[] 数组
    /** Cache the hash code for the string */
    private int hash; // Default to 0
    // ...
}
```

发现该类、类中大部分属性都是 final 的，属性用 final 修饰保证了该属性是只读的，不能修改，类用 final 修饰保证了该类中的方法不能被覆盖，防止子类无意间破坏不可变性。



#### 2.1、保护性拷贝

使用字符串时，也有一些跟修改相关的方法啊，比如substring、replace 等，那么下面就看一看这些方法是 如何实现的，就以 substring 为例：

```java
public String substring(int beginIndex, int endIndex) {
    ...
    // 上面是一些校验，下面才是真正的创建新的String对象
    return ((beginIndex == 0) && (endIndex == value.length)) ? this
            : new String(value, beginIndex, subLen);
}
```

发现其方法最后是调用String 的构造方法创建了一个新字符串，再进入这个构造看看，是否对 final char[] value 做出了修改：结果发现也没有，构造新字符串对象时，会生成新的 char[] value，对内容进行复制。

这种通过创建副本对象来避免共享的手段称之为 `保护性拷贝（defensive copy）`

```java
public String(char value[], int offset, int count) {
    ...
    // 上面是一些安全性的校验，下面是给String对象的value赋值，新创建了一个数组来保存String对象的值
    this.value = Arrays.copyOfRange(value, offset, offset+count);
}
```



#### 2.2、final原理

```java
public class TestFinal {
	 final int a = 20; 
}
```

字节码:

```java
0: aload_0
1: invokespecial #1 // Method java/lang/Object."<init>":()V
4: aload_0
5: bipush 20
7: putfield #2 // Field a:I
 <-- 写屏障
10: retu
```

 final 变量的赋值也会通过 putfield 指令来完成，同样在这条指令之后也会加入写屏障，保证在其它线程读到它的值时不会出现为 0 的情况。



### 3、享元设计模式

简介定义英文名称：Flyweight pattern, 重用数量有限的同一类对象。

享元模式的体现:

1.在JDK中Boolean，Byte，Short，Integer，Long，Character等包装类提供了valueOf方法，例如 Long 的valueOf会缓存-128~127之间的 Long 对象，在这个范围之间会重用对象，大于这个范围，才会新建 Long 对象。

```java
public static Long valueOf(long l) {
    final int offset = 128;
    if (l >= -128 && l <= 127) { // will cache
        return LongCache.cache[(int)l + offset];
    }
    return new Long(l);
}
```

>注意：
>
>- Byte, Short, Long 缓存的范围都是-128-127
>- Character 缓存的范围是 0-127
>- Boolean 缓存了 TRUE 和 FALSE
>- Integer的默认范围是 -128~127，最小值不能变，但最大值可以通过调整虚拟机参数 "-Djava.lang.Integer.IntegerCache.high "来改变

2.String常量池

3.BigDecimal , BigInteger



Integer 示例：

![12321](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/12321.png)



### 4、实现一个简单的连接池

例如：一个线上商城应用，QPS 达到数千，如果每次都重新创建和关闭数据库连接，性能会受到极大影响。 这时预先创建好一批连接，放入连接池。一次请求到达后，从连接池获取连接，使用完毕后再还回连接池，这样既节约了连接的创建和关闭时间，也实现了连接的重用，不至于让庞大的连接数压垮数据库。

```java
public class Test2 {
    public static void main(String[] args) {
        /*使用连接池*/
        Pool pool = new Pool(2);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                Connection conn = pool.borrow();
                try {
                    Thread.sleep(new Random().nextInt(1000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                pool.free(conn);
            }).start();
        }
    }
}

@Slf4j(topic = "Pool")
class Pool {
    // 1. 连接池大小
    private final int poolSize;

    // 2. 连接对象数组
    private Connection[] connections;

    // 3. 连接状态数组: 0 表示空闲, 1 表示繁忙
    private AtomicIntegerArray states;

    // 4. 构造方法初始化
    public Pool(int poolSize) {
        this.poolSize = poolSize;
        this.connections = new Connection[poolSize];
        this.states = new AtomicIntegerArray(new int[poolSize]);//使用AtomicIntegerArray保证states的线程安全
        for (int i = 0; i < poolSize; i++) {
            connections[i] = new MockConnection("连接" + (i + 1));
        }
    }

    // 5. 借连接
    public Connection borrow() {
        while (true) {
            for (int i = 0; i < poolSize; i++) {
                // 获取空闲连接
                if (states.get(i) == 0) {
                    if (states.compareAndSet(i, 0, 1)) {//使用compareAndSet保证线程安全
                        log.debug("borrow {}", connections[i]);
                        return connections[i];
                    }
                }
            }
            // 如果没有空闲连接，当前线程进入等待, 如果不写这个synchronized,其他线程不会进行等待,
            // 一直在上面while(true), 空转, 消耗cpu资源
            synchronized (this) {
                try {
                    log.debug("wait...");
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 6. 归还连接
    public void free(Connection conn) {
        for (int i = 0; i < poolSize; i++) {
            if (connections[i] == conn) {
                states.set(i, 0);
                synchronized (this) {
                    log.debug("free {}", conn);
                    this.notifyAll();
                }
                break;
            }
        }
    }
}

class MockConnection implements Connection {

    private String name;

    public MockConnection(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MockConnection{" +
                "name='" + name + '\'' +
                '}';
    }


    // Connection 实现方法略
}
```



![Inked20201229220417401_LI](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/juc/Inked20201229220417401_LI.jpg)









