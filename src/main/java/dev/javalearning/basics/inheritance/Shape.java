package dev.javalearning.basics.inheritance;

/**
 * 第 05 课的第一个类：图形。
 *
 * <p>它是<b>抽象类</b>——不能直接 new。它负责两件事：
 * 保存所有图形共有的 {@code name}，以及规定「所有图形都必须会算面积」。
 * 至于怎么算，交给子类。
 */
public abstract class Shape implements Describable {

    /** 所有图形共有的字段。protected：子类可以直接读。 */
    protected final String name;

    protected Shape(String name) {
        this.name = name;
    }

    /** 具体方法：父类说了算。子类直接继承，不用重写。 */
    public String describe() {
        return name + " 的面积是 " + String.format("%.2f", area());
    }

    /** 抽象方法：父类只规定「必须会」，不规定「怎么算」。 */
    public abstract double area();

    /** 实现 Describable 要求的方法。加了 @Override，编译器会盯着。 */
    @Override
    public String name() {
        return name;
    }

    /** 一个 static 方法，用来演示「隐藏」而不是「重写」。 */
    public static String kind() {
        return "shape";
    }
}
