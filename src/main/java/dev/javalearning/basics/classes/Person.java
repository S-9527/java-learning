package dev.javalearning.basics.classes;

/**
 * 第 04 课的第一个类：{@code Person}。
 *
 * <p>它是这一课的"教材本体"——字段、构造器、getter、{@code this}、重写
 * {@code toString} 全在里面。{@link ClassesDemo} 会反复创建它。
 */
public class Person {

    /** 姓名。private：外面只能通过 {@link #name()} 读。 */
    private final String name;

    /** 年龄。 */
    private int age;

    /**
     * 构造器：造一个 Person 的唯一入口。
     *
     * <p>注意 {@code this.name = name}——左边是字段，右边是参数，
     * 两个都叫 name，用 this 区分。这正是 this 存在的理由。
     */
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    /** 读姓名。没有 setter，因为 name 是 final 的，造好就不能改。 */
    public String name() {
        return name;
    }

    /** 读年龄。 */
    public int age() {
        return age;
    }

    /** 过生日：年龄加一。 */
    public void birthday() {
        age++;
    }

    /**
     * 没重写时，打印出来是 {@code dev.javalearning.basics.classes.Person@1b6d3586}
     * 这种「类名@哈希值」，日志里根本没法看。重写之后才是人能读的东西。
     */
    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + "}";
    }
}
