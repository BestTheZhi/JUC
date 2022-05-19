package top.THEZHI.pack7;

import sun.misc.Unsafe;

import java.lang.reflect.Constructor;

/**
 * @author ZHI LIU
 * @date 2022-05-09
 */
public class Test8 {
    public static void main(String[] args) throws Exception {
        // 通过反射获得Unsafe对象
        Class unsafeClass = Unsafe.class;
        // 获得构造函数，Unsafe的构造函数为私有的
        Constructor constructor = unsafeClass.getDeclaredConstructor();
        // 设置为允许访问私有内容
        constructor.setAccessible(true);
        // 创建Unsafe对象
        Unsafe unsafe = (Unsafe) constructor.newInstance();

        // 创建Person对象
        Person person = new Person();
        // 获得其属性 name 的偏移量
        long nameOffset = unsafe.objectFieldOffset(Person.class.getDeclaredField("name"));
        long ageOffset = unsafe.objectFieldOffset(Person.class.getDeclaredField("age"));

        // 通过unsafe的CAS操作改变值
        unsafe.compareAndSwapObject(person, nameOffset, null, "guizy");
        unsafe.compareAndSwapInt(person, ageOffset, 0, 22);
        System.out.println(person);
    }
}

class Person {
    // 配合CAS操作，必须用volatile修饰
    volatile String name;
    volatile int age;

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}