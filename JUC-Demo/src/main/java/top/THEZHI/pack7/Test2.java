package top.THEZHI.pack7;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;

/**
 * @author ZHI LIU
 * @date 2022-05-08
 */
public class Test2 {

    static int x = 3;

    public static void main(String[] args) {
        AtomicInteger i = new AtomicInteger(1);

        i.accumulateAndGet(x, new IntBinaryOperator() {
            @Override
            public int applyAsInt(int p, int x) {
                return p + x;
            }
        });

        i.accumulateAndGet(x, (p, x) -> p + x);


        i.accumulateAndGet(x, Integer::sum);

        System.out.println(i.get());
    }

}
