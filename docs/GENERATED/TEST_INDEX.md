<!--
本文件由 `./gradlew generateDocIndex` 自动生成，请勿手改。
改了测试方法名或 @DisplayName 之后跑一遍该 task，然后提交这个文件。
-->

# 测试清单

共 58 条断言（42 条启用、16 条是练习答案），分布在 3 个包。

## dev.javaquest.basics.controlflow

| # | 测试类 | 方法 | 断言 | 状态 |
|---|---|---|---|---|
| 1 | `ControlFlowExercisesTest` | `exerciseSwitchFallThroughResult` | 练习 1：一个 case 值为 4 的传统 switch，漏写 break 会算出什么？ | 练习答案 |
| 2 | `ControlFlowExercisesTest` | `exerciseWhileRunsZeroTimes` | 练习 2：while 循环在条件一开始就为 false 时，循环体跑几次？ | 练习答案 |
| 3 | `ControlFlowExercisesTest` | `exerciseLabeledBreakVisits` | 练习 3：两层循环，内层在 j==2 时 break 外层，一共访问多少次？ | 练习答案 |
| 4 | `ControlFlowExercisesTest` | `exerciseEnhancedForCannotWriteBack` | 练习 4：在增强 for 里把每个元素乘 10，原列表会变吗？ | 练习答案 |
| 5 | `ControlFlowExercisesTest` | `exerciseWhyRemoveWhileIteratingFails` | 练习 5：为什么在增强 for 里删元素会抛 ConcurrentModificationException？ | 练习答案 |
| 6 | `ControlFlowExercisesTest` | `exerciseRemoveOverloadPicksIndex` | 练习 6：[1,2,3,4].remove(2) 删掉的是哪个元素？ | 练习答案 |
| 7 | `ControlFlowTest.Choosing` | `andSkipsTheRightSide` | && 左边为 false 时，右边不执行 | |
| 8 | `ControlFlowTest.Choosing` | `orSkipsTheRightSide` | \|\| 左边为 true 时，右边不执行 | |
| 9 | `ControlFlowTest.Choosing` | `bitwiseOrAlwaysEvaluatesBothSides` | 非短路的 \| 会让两边都执行，所以副作用会发生两次 | |
| 10 | `ControlFlowTest.Choosing` | `classicSwitchFallsThroughWhenBreakIsMissing` | 传统 switch 漏写 break 会一路往下走 | |
| 11 | `ControlFlowTest.Choosing` | `switchExpressionOnIntNeedsDefault` | switch 表达式处理 int 时必须写 default | |
| 12 | `ControlFlowTest.Choosing` | `switchExpressionOnEnumIsExhaustive` | switch 表达式处理枚举时不需要 default | |
| 13 | `ControlFlowTest.Choosing` | `ternaryPromotesToTheWiderType` | 三目运算符两个分支类型不同时会提升到更宽的那个 | |
| 14 | `ControlFlowTest.Jumping` | `continueSkipsOnlyTheCurrentIteration` | continue 只跳过本次，循环继续 | |
| 15 | `ControlFlowTest.Jumping` | `plainBreakExitsOnlyTheInnermostLoop` | break 只能跳出最内层那一层 | |
| 16 | `ControlFlowTest.Jumping` | `labeledBreakExitsTheOuterLoop` | 标签能让 break 一次跳出多层 | |
| 17 | `ControlFlowTest.Repeating` | `forLoopHasThreeParts` | for 的三段式：初始化、条件、更新 | |
| 18 | `ControlFlowTest.Repeating` | `whileAndDoWhileDifferOnWhenTheyCheck` | while 和 do-while 的区别只在「什么时候判断」 | |
| 19 | `ControlFlowTest.Repeating` | `enhancedForCannotWriteBack` | 增强 for 里的变量是副本，改它不会改回集合 | |
| 20 | `ControlFlowTest.Repeating` | `removingWhileIteratingThrows` | 在增强 for 里删元素会抛 ConcurrentModificationException | |
| 21 | `ControlFlowTest.Repeating` | `removeHasTwoOverloadsAndIntPicksTheIndexOne` | remove(int) 按下标删，remove(Object) 按值删 | |
| 22 | `ControlFlowTest.Repeating` | `removeIfIsTheSafeWayToDelete` | 要删元素请用 removeIf | |

## dev.javaquest.basics.methods

| # | 测试类 | 方法 | 断言 | 状态 |
|---|---|---|---|---|
| 23 | `MethodExercisesTest` | `exerciseReassigningParameterDoesNotLeak` | 练习 1：在方法里把数组参数重新指向新数组，调用方看得见吗？ | 练习答案 |
| 24 | `MethodExercisesTest` | `exerciseMutatingArrayContentsIsVisible` | 练习 2：改数组的第一个元素，调用方看得见吗？ | 练习答案 |
| 25 | `MethodExercisesTest` | `exerciseVarargsIsAnArrayInside` | 练习 3：可变参数在方法内部是什么类型？ | 练习答案 |
| 26 | `MethodExercisesTest` | `exerciseVarargsOverloadAmbiguity` | 练习 4：f(int a) 和 f(int... a) 能同时存在吗？ | 练习答案 |
| 27 | `MethodsTest.Overloading` | `overloadsArePickedByTheArgumentList` | 同名方法可以有多个版本，靠参数个数或类型区分 | |
| 28 | `MethodsTest.Parameters` | `primitivesArePassedByValue` | 基本类型是值传递：方法内改参数，调用方看不到 | |
| 29 | `MethodsTest.Parameters` | `objectsShareTheSameTarget` | 对象是值传递地址：两个变量指向同一个对象，改一个另一个就变 | |
| 30 | `MethodsTest.Parameters` | `reassigningTheParameterDoesNotLeakOut` | 在方法内把参数重新指向别处，调用方看不到 | |
| 31 | `MethodsTest.Parameters` | `passingAMutableObjectHandsOverControl` | 传可变对象等于交出控制权，方法能改你的数据 | |
| 32 | `MethodsTest.ReturnValues` | `returningMethodsGiveYouTheValue` | 有返回值的方法，return 直接给出结果 | |
| 33 | `MethodsTest.ReturnValues` | `voidReturnOnlyExitsEarly` | void 方法里的 return 只是提前结束，不带值 | |
| 34 | `MethodsTest.Scope` | `localScopeEndsAtTheClosingBrace` | 局部变量的作用域到所属花括号结束为止 | |
| 35 | `MethodsTest.Varargs` | `varargsIsJustAnArray` | int... 就是 int[]，调用点自动打包 | |
| 36 | `MethodsTest.Varargs` | `passingAnArrayIsIdenticalToPassingScalars` | 传数组和传散数完全等价 | |

## dev.javaquest.basics.variables

| # | 测试类 | 方法 | 断言 | 状态 |
|---|---|---|---|---|
| 37 | `VariableExercisesTest` | `exerciseModuloOfNegativeNumbers` | 练习 1：-7 % 2 等于几？ | 练习答案 |
| 38 | `VariableExercisesTest` | `exerciseEvenCheckThatWorksForNegatives` | 练习 2：写一个判断负数是否偶数、不会出错的表达式 | 练习答案 |
| 39 | `VariableExercisesTest` | `exerciseComparingFloatingPoint` | 练习 3：怎么正确断言 0.1 + 0.2 等于 0.3？ | 练习答案 |
| 40 | `VariableExercisesTest` | `exerciseComparingTwoBuiltStrings` | 练习 4：两个 new 出来的 "java" 怎么判断内容相等？ | 练习答案 |
| 41 | `VariableExercisesTest` | `exerciseAvoidingOverflowByWideningFirst` | 练习 5：怎么算出 Integer.MAX_VALUE + 1 的真实值而不是 -2147483648？ | 练习答案 |
| 42 | `VariableExercisesTest` | `exerciseCharacterSubtractionOrderMatters` | 练习 6：'A' - 'a' 等于几？'a' - 'A' 呢？ | 练习答案 |
| 43 | `VariableTest` | `integerDivisionDropsTheRemainder` | 整数除法直接丢掉余数，不做四舍五入 | |
| 44 | `VariableTest` | `negativeDivisionTruncatesTowardZero` | 负数除法是向零截断，不是向下取整 | |
| 45 | `VariableTest` | `moduloSignFollowsTheDividend` | 取模结果的符号跟被除数走，不跟除数走 | |
| 46 | `VariableTest` | `integerOverflowWrapsAroundSilently` | int 溢出会静默回绕成最小值，不报错 | |
| 47 | `VariableTest` | `byteAdditionPromotesToInt` | 两个 byte 相加自动提升成 int，不会挤回 byte | |
| 48 | `VariableTest` | `floatingPointIsNotExact` | 浮点数天生不精确，0.1 + 0.2 不等于 0.3 | |
| 49 | `VariableTest` | `stringLiteralsShareOneObject` | 两个完全一样的字符串字面量共享同一个对象 | |
| 50 | `VariableTest` | `freshlyBuiltStringIsNotTheSameObject` | 手工 new 出来的字符串内容和字面量相同，但不是同一个对象 | |
| 51 | `VariableTest` | `concatenationLeavesLeftOperandUntouched` | String 不可变：拼接不会修改左边的对象 | |
| 52 | `VariableTest` | `finalLocksTheReferenceNotTheObject` | final 锁的是引用，不是对象内容 | |
| 53 | `VariableTest` | `charIsAnUnsignedNumber` | char 背后就是一个 16 位无符号整数 | |
| 54 | `VariableTest` | `boxedIntegerIdentityIsCachedUpTo127` | 装箱后的 Integer 在 -128 到 127 之间会复用同一个对象 | |
| 55 | `VariableTest` | `boxedIntegerIdentityIsNotCachedAbove127` | 超出缓存范围的装箱对象每次都是新的，== 会失败 | |
| 56 | `VariableTest` | `negativeEvenNumberModuloIsZero` | 负偶数取模是 0，这就是奇偶判断写错的根源 | |
| 57 | `VariableTest` | `overflowCanBeDetectedOrAvoided` | 溢出可以主动检测：Math.addExact 抛异常，转换成 long 则不溢出 | |
| 58 | `VariableTest` | `twoBuiltStringsAreContentEqualButNotIdentical` | 两个 new 出来的 String 内容相等，但 == 为 false | |

