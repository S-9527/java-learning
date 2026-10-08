package dev.javaquest.basics.inheritance;

/** 圆：把父类留的 {@code area()} 补上。 */
public final class Circle extends Shape {

    private final double radius;

    public Circle(double radius) {
        super("圆");     // 必须是构造器的第一句
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    /** 重写接口的默认方法——这不是必需的，但这里想让输出更好看。 */
    @Override
    public String label() {
        return "圆(" + radius + ")";
    }

    /**
     * 和父类的 {@code kind()} 同名，但不是重写。
     *
     * <p>static 方法属于类，编译期就定死了调哪个。
     */
    public static String kind() {
        return "circle";
    }

    /** 父类的 {@code name} 是 protected，这里能直接读，不用调 getter。 */
    @Override
    public String toString() {
        return name + "(半径 " + radius + ")";
    }
}
