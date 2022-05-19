package top.THEZHI.pack1;

/**
 * @author ZHI LIU
 * @date 2022-05-06
 */
public class PrivateTest {



}

class Person{

    private int i;

    private Person() {

    }

    public Person(int i){
        this.i = i;
    }
}

class Student extends Person{

    public Student(int i) {
        super(i);
    }
}
