package top.THEZHI.pack8;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

/**
 * @author ZHI LIU
 * @date 2022-05-13
 */

@Slf4j
public class Test1 {

    public static void main(String[] args) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    log.debug("{}", dtf.parse("1951-04-21"));
                } catch (Exception e) {
                    log.error("{}", e);
                }
            }).start();
        }

    }

}
