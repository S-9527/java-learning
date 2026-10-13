package dev.javalearning.basics.inheritance;

/**
 * 第 05 课用来证明「新增子类不用改老代码」的类。
 *
 * <p>它是这一课<b>最后</b>加的——加完之后 {@code InheritanceDemo.totalArea}
 * 一行都不用改就能算它。这就是多态的价值。
 */
public class Triangle extends Shape {

    private final double a;
    private final double b;
    private final double c;

    public Triangle(double a, double b, double c) {
        super("三角形");
        this.a = a;
        this.b = b;
        this.c = c;
    }

    /** 海伦公式。 */
    @Override
    public double area() {
        double s = (a + b + c) / 2;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }
}
