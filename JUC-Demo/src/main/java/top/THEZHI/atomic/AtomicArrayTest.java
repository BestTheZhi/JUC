package top.THEZHI.atomic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author ZHI LIU
 * @date 2022-05-07
 */
public class AtomicArrayTest {
    public static void main(String[] args) {
        demo(
                () -> new int[10],
                array -> array.length,
                (array, index) -> array[index]++,
                array -> System.out.println(Arrays.toString(array))
        );

//        demo(new Supplier<int[]>() {
//                 @Override
//                 public int[] get() {
//                     return new int[10];
//                 }
//             },
//                new Function<int[], Integer>() {
//                    @Override
//                    public Integer apply(int[] ints) {
//                        return ints.length;
//                    }
//                },
//                new BiConsumer<int[], Integer>() {
//                    @Override
//                    public void accept(int[] ints, Integer integer) {
//                        ints[integer]++;
//                    }
//                },
//                new Consumer<int[]>() {
//                    @Override
//                    public void accept(int[] ints) {
//                        System.out.println(Arrays.toString(ints));
//                    }
//                }
//        );

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
