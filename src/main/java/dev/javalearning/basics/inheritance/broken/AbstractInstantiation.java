package dev.javalearning.basics.inheritance.broken;

import dev.javalearning.basics.inheritance.Shape;

public class AbstractInstantiation {
    public static void main(String[] args) {
        Shape s = new Shape("圆");   // ← Shape 是抽象的
    }
}
