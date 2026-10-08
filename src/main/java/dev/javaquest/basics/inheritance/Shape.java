package dev.javaquest.basics.inheritance;

/**
 * 抽象父类：抽出「所有图形都有」的部分。
 *
 * <p>{@code name} 标成 {@code protected}——子类能直接读，
 * 其他包的人不行。这是第 04 章那张可见性表的实际用法。
 */
public abstract class Shape implements Describable {

    protected final String name;

    protected Shape(String name) {
        this.name = name;
    }

    /** 实现接口的抽象方法。 */
    @Override
    public String name() {
        return name;
    }

    /**
     * 抽象方法：子类必须各给各的算法。
     *
     * <p>这里只有声明，没有方法体（也没有 {@code abstract} 关键字修饰的方法本身，
     * 抽象性来自类上的 {@code abstract}）。
     */
    public abstract double area();

    /** 具体方法：父类说了算，子类不用重复写。 */
    public String describe() {
        return name + " 的面积是 " + round(area());
    }

    /**
     * static 方法：属于 Shape，不属于任何对象。
     *
     * <p>子类可以写同名方法来「隐藏」它，但那不是重写——具体见第 05 章。
     */
    public static String kind() {
        return "shape";
    }

    /** 把面积凑到两位小数，纯粹为了输出好看。 */
    static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
