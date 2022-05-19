package top.THEZHI.atomic;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

/**
 * @author ZHI LIU
 * @date 2022-05-06
 */
public class AtomicIntegerTest {

    public static void main(String[] args) {

        AtomicInteger i = new AtomicInteger(5);

        i.updateAndGet(new IntUnaryOperator() {
            @Override
            public int applyAsInt(int x) {
                return x * 5;
            }
        });

        i.updateAndGet(x -> x * 5);


        System.out.println(i.get());

    }

}
