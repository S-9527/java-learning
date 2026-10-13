package dev.javalearning.reflection;

/**
 * {@link User} 的父类，存在的唯一目的是演示
 * {@code getFields()} / {@code getMethods()} 会把<b>父类的 public 成员</b>也捞进来，
 * 而 {@code getDeclaredFields()} / {@code getDeclaredMethods()} 不会。
 */
public class BaseEntity {

    /** 父类的 public 字段：{@code User.class.getFields()} 会把它一起返回。 */
    public long id;

    /** 父类的 public 方法：{@code User.class.getMethods()} 会把它一起返回。 */
    public String whoAmI() {
        return "BaseEntity#" + id;
    }

    /** 父类的 protected 方法：两个方法列表都不会返回它。 */
    protected String onlyForSubclass() {
        return "只有子类能直接调";
    }
}
