package dev.javaquest.basics.inheritance;

/** 矩形：和圆是兄弟，两个都继承自 {@link Shape}。 */
public final class Rectangle extends Shape {

    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        super("矩形");
        this.width = width;
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    @Override
    public double area() {
        return width * height;
    }
}
