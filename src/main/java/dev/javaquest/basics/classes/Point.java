package dev.javaquest.basics.classes;

/**
 * 一个最普通的类：私有字段 + 构造器 + 方法。
 *
 * <p>字段标成 {@code private final}，外面只能通过方法访问，这就是封装。
 */
public final class Point {

    private final int x;
    private final int y;

    /**
     * 构造器。方法名和类名相同，<b>没有返回类型</b>（连 {@code void} 都不写）。
     *
     * <p>注意这里必须写 {@code this.x = x}：如果只写 {@code x = x}，
     * 两边的 {@code x} 解析到的都是形参，等于把参数赋给自己，字段的值没变过。
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /**
     * 到另一个点的距离平方。
     *
     * <p>用 {@code this.x} 而不是 {@code x}：这里 {@code x} 已经是本对象的字段，
     * 写成 {@code x} 也能编译通过，但两个 {@code x} 挨得太近，容易看错成同一个。
     */
    public int distanceSquaredTo(Point other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return dx * dx + dy * dy;
    }

    /**
     * 返回一个改了 x 的新对象，原对象不动。
     *
     * <p>因为字段是 {@code final} 的，方法没法原地修改自己，只能造一个新的。
     */
    public Point withX(int newX) {
        return new Point(newX, this.y);
    }
}
