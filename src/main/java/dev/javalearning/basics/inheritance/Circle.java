package dev.javalearning.basics.inheritance;

/** 第 05 课的类：圆。{@code Shape} 的一个具体子类。 */
public class Circle extends Shape {

    private final double radius;

    /** 第一句必须是 super(...)：父类构造器先跑。 */
    public Circle(double radius) {
        super("圆");
        this.radius = radius;
    }

    /** 补上父类规定必须会的那件事。 */
    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    public double radius() {
        return radius;
    }

    /** 圆自己才有的方法——父类没有。 */
    public double diameter() {
        return 2 * radius;
    }

    /** 同名 static 方法：这是<b>隐藏</b>父类方法，不是重写。 */
    public static String kind() {
        return "circle";
    }

    @Override
    public String toString() {
        return name + "(半径 " + radius + ")";
    }
}
