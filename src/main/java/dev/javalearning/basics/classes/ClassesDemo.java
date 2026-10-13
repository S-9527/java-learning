package dev.javalearning.basics.classes;

/**
 * 第 04 课的示例代码：类与对象。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/basics/classes/ClassesDemo.java}
 * 每一节对应 {@code docs/04-类与对象.md} 里的一节，输出都是真跑出来的。
 */
public class ClassesDemo {

    /** 一个普通字段，用来证明「每个对象一份」。 */
    static int instanceCounter;

    /** 一个静态字段，用来证明「所有对象共享一份」。 */
    static int sharedCounter;

    private final String name;

    public ClassesDemo(String name) {
        this.name = name;
        instanceCounter++;
        sharedCounter++;
    }

    public String name() {
        return name;
    }

    public static void main(String[] args) {
        section("1. 类是说明书，对象是按说明书造出来的每一件");
        Person albert = new Person("Albert", 30);
        Person neil = new Person("Neil", 25);
        System.out.println("albert = " + albert);
        System.out.println("neil   = " + neil);
        System.out.println("同一个人吗 → " + albert.equals(neil));

        section("2. 每个对象有自己的一份字段");
        System.out.println("albert.age() = " + albert.age() + "，neil.age() = " + neil.age());
        albert.birthday();
        System.out.println("给 albert 过完生日：");
        System.out.println("albert.age() = " + albert.age() + "   ← 只有它变了");
        System.out.println("neil.age()   = " + neil.age() + "   ← 另一个对象没被影响");

        section("3. 字段是 private 的：外面只能通过方法碰");
        System.out.println("albert.name() = " + albert.name());
        // albert.name = "x";   // 编译不过：name 是 private 的

        section("4. static 字段：属于类，不属于任何对象");
        instanceCounter = 0;
        sharedCounter = 0;
        System.out.println("new 之前：instanceCounter = " + instanceCounter
                + "，sharedCounter = " + sharedCounter);
        ClassesDemo a = new ClassesDemo("a");
        ClassesDemo b = new ClassesDemo("b");
        ClassesDemo c = new ClassesDemo("c");
        System.out.println("new 了 3 个之后：");
        System.out.println("  通过对象 a 读 instanceCounter = " + a.instanceCounter
                + "（不推荐这么写）");
        System.out.println("  通过类名读 ClassesDemo.instanceCounter = " + ClassesDemo.instanceCounter);
        System.out.println("  三个对象看到的是同一个值吗 → "
                + (a.instanceCounter == b.instanceCounter && b.instanceCounter == c.instanceCounter));
        System.out.println("  但每个对象有自己的 name：" + a.name() + " / " + b.name() + " / " + c.name());

        section("5. 没写构造器时，编译器送你一个无参的");
        NoConstructor nc = new NoConstructor();
        System.out.println("nc.count = " + nc.count + "（int 字段的默认值 0）");
        System.out.println("nc.text  = " + nc.text + "（引用类型字段的默认值 null）");

        section("6. 构造器里可以不写 this，编译器会补");
        DefaultInit di = new DefaultInit();
        System.out.println("di.value = " + di.value);

        section("7. 重写 toString：让打印出来的是人话");
        System.out.println("重写之后     → " + albert);
        System.out.println("没重写的话是  → " + new NoToString("Albert", 31)
                + "  ← 类名加 @ 加十六进制，日志里根本认不出是谁");

        section("8. equals 不重写时比的是身份");
        Person albertAgain = new Person("Albert", 30);
        System.out.println("内容完全一样 → " + albertAgain);
        System.out.println("albert == albertAgain       → " + (albert == albertAgain));
        System.out.println("albert.equals(albertAgain)  → " + albert.equals(albertAgain));
        System.out.println("同一个对象自己比             → " + albert.equals(albert));
    }

    /** 和 Person 内容一样，但**不**重写 toString——用来对比默认输出长什么样。 */
    static class NoToString {
        private final String name;
        private final int age;

        NoToString(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /** 什么都不写，也有构造器——编译器送的那个。 */
    static class NoConstructor {
        int count;
        String text;
    }

    /** 构造器里给字段赋初始值，这是最常见的用法。 */
    static class DefaultInit {
        int value = 42;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
