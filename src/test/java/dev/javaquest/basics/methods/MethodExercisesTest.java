package dev.javaquest.basics.methods;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 第 03 章的练习题。
 *
 * <p>用法：把方法上的 {@code @Disabled} 删掉，重新运行，就能看到答案跑起来。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("03 练习题：方法")
class MethodExercisesTest {

    @Test
    @Order(1)
    @Disabled("答案：看不见，还是 [1,2,3]。Java 只有值传递。传数组进去只是把地址的值复制了一份，" +
            "方法里改的是复印件的指向。原数组还指向老对象，没人动过它。")
    @DisplayName("练习 1：在方法里把数组参数重新指向新数组，调用方看得见吗？")
    void exerciseReassigningParameterDoesNotLeak() {
        int[] data = {1, 2, 3};

        reassignToNewArray(data);

        assertThat(data).containsExactly(1, 2, 3);
    }

    @Test
    @Order(2)
    @Disabled("答案：看得见，是 100。传数组进去改的是同一个对象的内容，所以调用方立刻就看见了。" +
            "这和方法内重新指向别处是两回事，对比练习 1 一起看。")
    @DisplayName("练习 2：改数组的第一个元素，调用方看得见吗？")
    void exerciseMutatingArrayContentsIsVisible() {
        int[] data = {1, 2, 3};

        mutateFirstElement(data);

        assertThat(data[0]).isEqualTo(100);
    }

    @Test
    @Order(3)
    @Disabled("答案：就是 int[]。所以能正常 for 循环遍历，也能整个传给另一个可变参数方法。")
    @DisplayName("练习 3：可变参数在方法内部是什么类型？")
    void exerciseVarargsIsAnArrayInside() {
        assertThat(describeTheType(1, 2, 3)).isEqualTo(6);
    }

    @Test
    @Order(4)
    @Disabled("答案：编译报错，歧义。f(int) 和 f(int...) 能共存，但 f(1) 两个都匹配得上，编译器不知道调哪个。" +
            "所以别写这种重载，改名或者用真正不同的参数个数。")
    @DisplayName("练习 4：f(int a) 和 f(int... a) 能同时存在吗？")
    void exerciseVarargsOverloadAmbiguity() {
        assertThat(true).as("这个练习的正确答案是「不能同时调用」，改代码会编译失败").isTrue();
    }

    /** 把参数重新指向一个新数组。 */
    private static void reassignToNewArray(int[] x) {
        x = new int[]{100};
    }

    /** 通过参数指向的对象改内容。 */
    private static void mutateFirstElement(int[] x) {
        x[0] = 100;
    }

    /** 可变参数在方法内部就是一个普通数组。 */
    private static int describeTheType(int... numbers) {
        int sum = 0;
        for (int n : numbers) {
            sum += n;
        }
        return sum;
    }
}
