package top.THEZHI.pack7;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @author ZHI LIU
 * @date 2022-05-09
 */
@Slf4j
public class Test6 {
    public static void main(String[] args) {
        Student stu = new Student();
        // 获得原子更新器
        // 泛型
        // 参数1 持有属性的类 参数2 被更新的属性的类
        // newUpdater中的参数：第三个为属性的名称
        AtomicReferenceFieldUpdater<Student,String> updater = AtomicReferenceFieldUpdater.newUpdater(Student.class, String.class, "name");
        // 期望的为null, 如果name属性没有被别的线程更改过, 默认就为null, 此时匹配, 就可以设置name为张三
        System.out.println(updater.compareAndSet(stu, null, "张三"));
        System.out.println(updater.compareAndSet(stu, stu.name, "王五"));
        System.out.println(stu);
    }
}

class Student {
    volatile String name;

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                '}';
    }
}