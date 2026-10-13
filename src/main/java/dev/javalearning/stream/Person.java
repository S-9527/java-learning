package dev.javalearning.stream;

import java.util.Objects;

/**
 * 第 10 课的类：人。姓名 + 年龄 + 国家。
 *
 * <p>它是这一课全程的数据源。之所以要重写 {@code equals} / {@code hashCode}，
 * 是因为 {@code distinct()} 和 {@code Collectors.toSet()} 都靠这两个方法去重——
 * 不重写的话，两个内容一样的 {@code Person} 会被当成两个不同的人。
 *
 * <p>注意它<b>没有</b>实现 {@code Comparable}：一个人没有"天然的大小"，
 * 硬排一个顺序只会让调用方困惑。要排序就显式给一个 {@code Comparator}。
 */
public class Person {

    private final String name;
    private final int age;
    private final String country;

    public Person(String name, int age, String country) {
        this.name = name;
        this.age = age;
        this.country = country;
    }

    public String name() {
        return name;
    }

    public int age() {
        return age;
    }

    public String country() {
        return country;
    }

    /** 姓名、年龄、国家全都一样，就算同一个人。distinct() 就靠它。 */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Person that)) {
            return false;
        }
        return age == that.age
                && name.equals(that.name)
                && country.equals(that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age, country);
    }

    /** 紧凑写法，免得打印一整个列表时刷屏。 */
    @Override
    public String toString() {
        return name + "(" + age + "," + country + ")";
    }
}
