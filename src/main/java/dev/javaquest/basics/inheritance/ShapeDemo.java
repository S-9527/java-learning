package dev.javaquest.basics.inheritance;

import java.util.List;

/**
 * 第 05 章的调用方。
 *
 * <p>注意这些方法的参数类型都是 {@link Shape}——一个具体子类都没提到。
 * 加了新的图形子类，这里的代码一行都不用改。这就是多态。
 */
public final class ShapeDemo {

    private ShapeDemo() {
    }

    /** 把所有图形的面积加起来。 */
    public static double totalArea(List<Shape> shapes) {
        double sum = 0;
        for (Shape shape : shapes) {
            sum += shape.area();
        }
        return sum;
    }

    /** 只挑出面积超过门槛的那些。 */
    public static List<Shape> biggerThan(List<Shape> shapes, double limit) {
        return shapes.stream()
                .filter(shape -> shape.area() > limit)
                .toList();
    }

    /** 打印每个图形的说明文字。 */
    public static String describeAll(List<Shape> shapes) {
        StringBuilder sb = new StringBuilder();
        for (Shape shape : shapes) {
            if (!sb.isEmpty()) {
                sb.append("；");
            }
            sb.append(shape.describe());
        }
        return sb.toString();
    }
}
