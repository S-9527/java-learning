package dev.javalearning.basics.inheritance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 第 05 课的示例代码：继承与多态。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/basics/inheritance/InheritanceDemo.java}
 * 每一节对应 {@code docs/05-继承与多态.md} 里的一节，输出都是真跑出来的。
 */
public class InheritanceDemo {

    public static void main(String[] args) {
        section("1. 向上转型：写子类，用父类接着");
        Shape circle = new Circle(1);
        Shape rectangle = new Rectangle(2, 3);
        System.out.println("circle.area()    = " + circle.area() + "   ← 调到了 Circle 的");
        System.out.println("rectangle.area() = " + rectangle.area() + "   ← 调到了 Rectangle 的");
        System.out.println("两行写法一模一样，结果不同");

        section("2. 多态：调用方不需要知道具体类型");
        List<Shape> shapes = new ArrayList<>(List.of(new Circle(1), new Rectangle(2, 3)));
        System.out.println("总面积 = " + totalArea(shapes));
        System.out.println("totalArea 里只有 Shape，没有 Circle 也没有 Rectangle");

        section("3. 父类的具体方法，子类直接继承");
        System.out.println(new Circle(1).describe());
        System.out.println(new Rectangle(2, 3).describe());

        section("4. 同名 static 方法是隐藏，不是重写");
        Circle c = new Circle(1);
        Shape asShape = c;
        System.out.println("c.kind()        = " + c.kind() + "   ← Circle 那个");
        System.out.println("asShape.kind()  = " + asShape.kind() + "   ← Shape 那个！");
        System.out.println("但 asShape 里装的确实是个 Circle：" + (asShape == c));
        System.out.println("换成实例方法就不一样：asShape.area() = " + asShape.area());

        section("5. instanceof 和模式匹配");
        for (Shape s : List.of(new Circle(2), new Rectangle(5, 1))) {
            if (s instanceof Circle ci) {
                System.out.println(s + " 是圆，直径 = " + ci.diameter());
            } else if (s instanceof Rectangle r) {
                System.out.println(s + " 是矩形，宽 = " + r.width());
            }
        }

        section("6. 向下转型：转错了要到运行期才知道");
        try {
            Shape rectangle2 = new Rectangle(2, 3);
            Circle wrong = (Circle) rectangle2;
            System.out.println("居然转过来了：" + wrong);
        } catch (ClassCastException e) {
            // 完整 message 末尾带一个每次运行都变的类加载器哈希，只打印稳定部分
            System.out.println("ClassCastException: class Rectangle cannot be cast to class Circle");
            System.out.println("（完整 message 还会带 module / loader 信息，每次都不同）");
        }

        section("7. 抽象类不能直接造对象");
        // new Shape("圆") {} 这种匿名类也不行——它必须补上抽象方法才能编译。
        // 真实错误见 docs/05-继承与多态.md，那里贴的是 javac 的原话。
        System.out.println("Shape 是抽象的，new Shape(\"圆\") 直接编译不过");
        System.out.println("子类补上 area() 之后就能造了："
                + new Circle(1) + " / " + new Rectangle(2, 3));

        section("8. equals 重写了，内容一样就算相等");
        Money a = new Money(100);
        Money b = new Money(100);
        System.out.println("a == b      → " + (a == b));
        System.out.println("a.equals(b) → " + a.equals(b));
        System.out.println("hashCode 相同吗 → " + (a.hashCode() == b.hashCode()));
        System.out.println("Set 里放 3 个相同的 → " + correctWalletSize() + " 个");
    }

    /** 变量类型是 Shape，方法里只有 Shape——这就是多态的实用价值。 */
    public static double totalArea(List<Shape> shapes) {
        double sum = 0;
        for (Shape shape : shapes) {
            sum += shape.area();
        }
        return sum;
    }

    static int correctWalletSize() {
        Set<Money> wallet = new HashSet<>();
        wallet.add(new Money(100));
        wallet.add(new Money(100));
        wallet.add(new Money(100));
        return wallet.size();
    }


    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
