package dev.javalearning.basics.inheritance;

/** 第 05 课的类：矩形。{@code Shape} 的另一个具体子类。 */
public class Rectangle extends Shape {

    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        super("矩形");
        this.width = width;
        this.height = height;
    }

    @Override
    public double area() {
        return width * height;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    @Override
    public String toString() {
        return name + "(" + width + " × " + height + ")";
    }
}
