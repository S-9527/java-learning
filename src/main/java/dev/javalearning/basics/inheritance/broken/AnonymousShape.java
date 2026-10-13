package dev.javalearning.basics.inheritance.broken;

import dev.javalearning.basics.inheritance.Shape;

public class AnonymousShape {
    public static void main(String[] args) {
        Shape s = new Shape("圆") {};   // ← 匿名类，没实现 area()
    }
}
