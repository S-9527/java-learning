package dev.javaquest.basics.inheritance;

/**
 * 一份「必须做到」的清单。
 *
 * <p>接口里的抽象方法没有方法体，实现者必须提供。{@code default} 方法有方法体，
 * 实现者可以选择不重写。
 */
public interface Describable {

    /** 抽象方法：只说要提供什么，不说怎么做。 */
    String name();

    /**
     * 默认方法：给了现成的做法。
     *
     * <p>实现者如果觉得不合适，可以重写它。
     */
    default String label() {
        return "<" + name() + ">";
    }
}
