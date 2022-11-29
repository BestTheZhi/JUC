package test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author ZHI LIU
 * @date 2022-09-22
 */
public class Test1 {
    public static void main(String[] args) {
        Set set = new HashSet();
        set.add(null);
        set.add(null);
        set.add(null);
        System.out.println(set.size());
    }
}
