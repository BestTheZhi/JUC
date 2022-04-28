package top.THEZHI.utils;

import org.openjdk.jol.info.ClassLayout;

import java.awt.print.PrinterGraphics;

/**
 * @author ZHI LIU
 * @date 2022-04-27
 */

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
