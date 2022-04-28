package top.THEZHI.pack1;

import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.ClassLayout;
import top.THEZHI.utils.PrintMarkWord;

/**
 * @author ZHI LIU
 * @date 2022-04-27
 */

@Slf4j
public class MarkWord {
    public static void main(String[] args) {
        Dog d = new Dog();
        log.debug(PrintMarkWord.print(d));


//        Dog d = new Dog();
//        System.out.println(ClassLayout.parseInstance(d).toPrintable());
//        System.out.println(ClassLayout.parseInstance(d).toPrintable().split("\\\\n").length);
//        String[] strs = ClassLayout.parseInstance(d).toPrintable().split("                           ");
//
//        System.out.println("----------------------------------------------");
//        System.out.println(strs[2]);
//        System.out.println(strs[3]);
//
//        System.out.println("----------------------------------------------");
//
//        String[] strss = strs[2].split("\\(|\\)");
//        System.out.println(strss[1]);
//        String[] strss1 = strs[3].split("\\(|\\)");
//        System.out.println(strss1[1]);
//
//        System.out.println("----------------------------------------------");



    }
}

class Dog{

}
