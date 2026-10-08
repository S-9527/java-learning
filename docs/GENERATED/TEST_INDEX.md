<!--
本文件由 `./gradlew generateDocIndex` 自动生成，请勿手改。
改了测试方法名或 @DisplayName 之后跑一遍该 task，然后提交这个文件。
-->

# 测试清单

共 104 条断言（78 条启用、26 条是练习答案），分布在 5 个包。

## dev.javaquest.basics.classes

| # | 测试类 | 方法 | 断言 | 状态 |
|---|---|---|---|---|
| 1 | `ClassExercisesTest` | `exerciseDefaultEqualsIsIdentity` | 练习 1：两个内容相同的 Point，用 equals 比较结果是什么？ | 练习答案 |
| 2 | `ClassExercisesTest` | `exerciseDefaultFieldValues` | 练习 2：没写构造器的类，字段拿到什么默认值？ | 练习答案 |
| 3 | `ClassExercisesTest` | `exerciseStaticCounterIsShared` | 练习 3：连续造两个 Counter，它们的 id 分别是多少？ | 练习答案 |
| 4 | `ClassExercisesTest` | `exerciseNullArrayFieldThrows` | 练习 4：对一个没初始化的数组字段取 length 会怎样？ | 练习答案 |
| 5 | `ClassExercisesTest` | `exerciseSelfAssignmentDoesNotAssignField` | 练习 5：构造器里写 value = value 而不是 this.value = value，字段会拿到值吗？ | 练习答案 |
| 6 | `ClassTest.Constructing` | `thisIsRequiredToAssignFields` | 构造器里的 x = x 是在给参数赋值，不是给字段赋值 | |
| 7 | `ClassTest.Constructing` | `missingConstructorMeansDefaultValues` | 一个类没写构造器时，字段是默认值而不是报错 | |
| 8 | `ClassTest.StateAndBehaviour` | `fieldsHoldState` | 字段存状态，方法读出来 | |
| 9 | `ClassTest.StateAndBehaviour` | `methodUsesBothObjects` | 方法可以同时用到自己的字段和别人的字段 | |
| 10 | `ClassTest.StateAndBehaviour` | `immutableFieldsMeanNewObjects` | 字段是 final 时，方法返回一个改过的新对象 | |
| 11 | `ClassTest.StaticMembers` | `staticIsSharedAndInstanceIsNot` | static 字段被所有实例共享，实例字段各归各 | |
| 12 | `ClassTest.StaticMembers` | `instanceFieldsAreIndependent` | 实例字段不共享：各自 increment 各自涨 | |
| 13 | `ClassTest.StaticMembers` | `staticMethodsNeedNoInstance` | static 方法通过类名调用，不需要先造对象 | |
| 14 | `ClassTest.ThisKeyword` | `thisRefersToTheReceiver` | 方法里的 this 指向调用它的那个对象 | |
| 15 | `ClassTest.TypeAndInstance` | `oneClassManyInstances` | 同一个类可以造出很多个对象，它们互相独立 | |
| 16 | `ClassTest.TypeAndInstance` | `identityEqualityByDefault` | 没有重写 equals 时，比较的是身份而不是内容 | |
| 17 | `ClassTest.TypeAndInstance` | `toStringHasADefaultFormat` | toString 的默认格式是类名加哈希值 | |
| 18 | `ClassTest.Visibility` | `privateFieldsAreReachedThroughMethods` | private 字段只能通过方法访问 | |
| 19 | `ClassTest.Visibility` | `finalFieldsLockDownTheObjectsShape` | 字段用 final 时，对象的对外表现也就固定了 | |

## dev.javaquest.basics.controlflow

| # | 测试类 | 方法 | 断言 | 状态 |
|---|---|---|---|---|
| 20 | `ControlFlowExercisesTest` | `exerciseSwitchFallThroughResult` | 练习 1：一个 case 值为 4 的传统 switch，漏写 break 会算出什么？ | 练习答案 |
| 21 | `ControlFlowExercisesTest` | `exerciseWhileRunsZeroTimes` | 练习 2：while 循环在条件一开始就为 false 时，循环体跑几次？ | 练习答案 |
| 22 | `ControlFlowExercisesTest` | `exerciseLabeledBreakVisits` | 练习 3：两层循环，内层在 j==2 时 break 外层，一共访问多少次？ | 练习答案 |
| 23 | `ControlFlowExercisesTest` | `exerciseEnhancedForCannotWriteBack` | 练习 4：在增强 for 里把每个元素乘 10，原列表会变吗？ | 练习答案 |
| 24 | `ControlFlowExercisesTest` | `exerciseWhyRemoveWhileIteratingFails` | 练习 5：为什么在增强 for 里删元素会抛 ConcurrentModificationException？ | 练习答案 |
| 25 | `ControlFlowExercisesTest` | `exerciseRemoveOverloadPicksIndex` | 练习 6：[1,2,3,4].remove(2) 删掉的是哪个元素？ | 练习答案 |
| 26 | `ControlFlowTest.Choosing` | `andSkipsTheRightSide` | && 左边为 false 时，右边不执行 | |
| 27 | `ControlFlowTest.Choosing` | `orSkipsTheRightSide` | \|\| 左边为 true 时，右边不执行 | |
| 28 | `ControlFlowTest.Choosing` | `bitwiseOrAlwaysEvaluatesBothSides` | 非短路的 \| 会让两边都执行，所以副作用会发生两次 | |
| 29 | `ControlFlowTest.Choosing` | `classicSwitchFallsThroughWhenBreakIsMissing` | 传统 switch 漏写 break 会一路往下走 | |
| 30 | `ControlFlowTest.Choosing` | `switchExpressionOnIntNeedsDefault` | switch 表达式处理 int 时必须写 default | |
| 31 | `ControlFlowTest.Choosing` | `switchExpressionOnEnumIsExhaustive` | switch 表达式处理枚举时不需要 default | |
| 32 | `ControlFlowTest.Choosing` | `ternaryPromotesToTheWiderType` | 三目运算符两个分支类型不同时会提升到更宽的那个 | |
| 33 | `ControlFlowTest.Jumping` | `continueSkipsOnlyTheCurrentIteration` | continue 只跳过本次，循环继续 | |
| 34 | `ControlFlowTest.Jumping` | `plainBreakExitsOnlyTheInnermostLoop` | break 只能跳出最内层那一层 | |
| 35 | `ControlFlowTest.Jumping` | `labeledBreakExitsTheOuterLoop` | 标签能让 break 一次跳出多层 | |
| 36 | `ControlFlowTest.Repeating` | `forLoopHasThreeParts` | for 的三段式：初始化、条件、更新 | |
| 37 | `ControlFlowTest.Repeating` | `whileAndDoWhileDifferOnWhenTheyCheck` | while 和 do-while 的区别只在「什么时候判断」 | |
| 38 | `ControlFlowTest.Repeating` | `enhancedForCannotWriteBack` | 增强 for 里的变量是副本，改它不会改回集合 | |
| 39 | `ControlFlowTest.Repeating` | `removingWhileIteratingThrows` | 在增强 for 里删元素会抛 ConcurrentModificationException | |
| 40 | `ControlFlowTest.Repeating` | `removeHasTwoOverloadsAndIntPicksTheIndexOne` | remove(int) 按下标删，remove(Object) 按值删 | |
| 41 | `ControlFlowTest.Repeating` | `removeIfIsTheSafeWayToDelete` | 要删元素请用 removeIf | |

## dev.javaquest.basics.inheritance

| # | 测试类 | 方法 | 断言 | 状态 |
|---|---|---|---|---|
| 42 | `InheritanceExercisesTest` | `exerciseStaticMethodResolvesAtCompileTime` | 练习 1：把 Circle 对象声明成 Shape，再调 kind()，得到什么？ | 练习答案 |
| 43 | `InheritanceExercisesTest` | `exerciseWrongDowncastThrows` | 练习 2：把一个 Rectangle 强转成 Circle 会发生什么？ | 练习答案 |
| 44 | `InheritanceExercisesTest` | `exerciseEqualityIsStillIdentityByDefault` | 练习 3：两个 Circle(1) 用 equals 比较，结果是什么？ | 练习答案 |
| 45 | `InheritanceExercisesTest` | `exerciseBrokenHashCodeContractKeepsDuplicates` | 练习 4：只重写 equals 不重写 hashCode，两个相等的金额放进 HashSet 会剩几个？ | 练习答案 |
| 46 | `InheritanceExercisesTest` | `exerciseDefaultMethodCanBeOverridden` | 练习 5：Circle 和 Rectangle 的 label() 分别输出什么？ | 练习答案 |
| 47 | `InheritanceTest.AbstractAndInterface` | `abstractClassCannotBeInstantiated` | 抽象类不能直接造对象 | |
| 48 | `InheritanceTest.AbstractAndInterface` | `abstractClassHasConcreteParts` | 抽象类照样有构造器和具体方法 | |
| 49 | `InheritanceTest.AbstractAndInterface` | `interfaceMethodsArePublicAndAbstract` | 接口里的方法隐式是 public abstract | |
| 50 | `InheritanceTest.AbstractAndInterface` | `defaultMethodIsInherited` | default 方法不重写也能用 | |
| 51 | `InheritanceTest.AbstractAndInterface` | `overridingADefaultMethodIsOptional` | 重写 default 方法是可选的 | |
| 52 | `InheritanceTest.Extending` | `inheritsParentMethods` | 子类对象能直接用父类的具体方法 | |
| 53 | `InheritanceTest.Extending` | `superCallFillsParentFields` | super(...) 的参数决定了父类的字段 | |
| 54 | `InheritanceTest.Extending` | `protectedFieldIsVisibleToSubclasses` | protected 字段子类能直接读 | |
| 55 | `InheritanceTest.ObjectMethods` | `overriddenEqualsComparesContent` | 重写 equals 之后，内容相同就相等 | |
| 56 | `InheritanceTest.ObjectMethods` | `hashCodeFollowsEquals` | equals 相等的对象，hashCode 必须一样 | |
| 57 | `InheritanceTest.ObjectMethods` | `hashSetDeduplicatesWithTheContract` | 契约成立，HashSet 才能去重 | |
| 58 | `InheritanceTest.ObjectMethods` | `overriddenToStringIsReadable` | 重写 toString 让输出可读 | |
| 59 | `InheritanceTest.Overriding` | `subclassImplementationWins` | 子类重写之后，调用到的是子类的实现 | |
| 60 | `InheritanceTest.Overriding` | `staticMethodsAreHiddenNotOverridden` | 同名 static 方法是隐藏，不是重写 | |
| 61 | `InheritanceTest.Polymorphism` | `oneCallManyResults` | 同一个调用，不同的子类给出不同的结果 | |
| 62 | `InheritanceTest.Polymorphism` | `callerIgnoresConcreteTypes` | 调用方不需要知道具体类型 | |
| 63 | `InheritanceTest.Polymorphism` | `templateMethodWithDifferentFillings` | 父类的方法模板可以被不同子类填出不同文字 | |
| 64 | `InheritanceTest.TypeChecks` | `upcastIsImplicit` | 向上转型是隐式的，写子类给父类不用转换 | |
| 65 | `InheritanceTest.TypeChecks` | `instanceofLooksAtTheRealObject` | instanceof 看的是实际装的什么对象 | |
| 66 | `InheritanceTest.TypeChecks` | `patternMatchingCombinesCheckAndCast` | instanceof 加上模式匹配，判断和取值一步做完 | |
| 67 | `InheritanceTest.TypeChecks` | `downcastIsExplicit` | 向下转型要显式写出来 | |
| 68 | `InheritanceTest.TypeChecks` | `wrongDowncastThrows` | 转错的类型在运行期抛 ClassCastException | |

## dev.javaquest.basics.methods

| # | 测试类 | 方法 | 断言 | 状态 |
|---|---|---|---|---|
| 69 | `MethodExercisesTest` | `exerciseReassigningParameterDoesNotLeak` | 练习 1：在方法里把数组参数重新指向新数组，调用方看得见吗？ | 练习答案 |
| 70 | `MethodExercisesTest` | `exerciseMutatingArrayContentsIsVisible` | 练习 2：改数组的第一个元素，调用方看得见吗？ | 练习答案 |
| 71 | `MethodExercisesTest` | `exerciseVarargsIsAnArrayInside` | 练习 3：可变参数在方法内部是什么类型？ | 练习答案 |
| 72 | `MethodExercisesTest` | `exerciseVarargsOverloadAmbiguity` | 练习 4：f(int a) 和 f(int... a) 能同时存在吗？ | 练习答案 |
| 73 | `MethodsTest.Overloading` | `overloadsArePickedByTheArgumentList` | 同名方法可以有多个版本，靠参数个数或类型区分 | |
| 74 | `MethodsTest.Parameters` | `primitivesArePassedByValue` | 基本类型是值传递：方法内改参数，调用方看不到 | |
| 75 | `MethodsTest.Parameters` | `objectsShareTheSameTarget` | 对象是值传递地址：两个变量指向同一个对象，改一个另一个就变 | |
| 76 | `MethodsTest.Parameters` | `reassigningTheParameterDoesNotLeakOut` | 在方法内把参数重新指向别处，调用方看不到 | |
| 77 | `MethodsTest.Parameters` | `passingAMutableObjectHandsOverControl` | 传可变对象等于交出控制权，方法能改你的数据 | |
| 78 | `MethodsTest.ReturnValues` | `returningMethodsGiveYouTheValue` | 有返回值的方法，return 直接给出结果 | |
| 79 | `MethodsTest.ReturnValues` | `voidReturnOnlyExitsEarly` | void 方法里的 return 只是提前结束，不带值 | |
| 80 | `MethodsTest.Scope` | `localScopeEndsAtTheClosingBrace` | 局部变量的作用域到所属花括号结束为止 | |
| 81 | `MethodsTest.Varargs` | `varargsIsJustAnArray` | int... 就是 int[]，调用点自动打包 | |
| 82 | `MethodsTest.Varargs` | `passingAnArrayIsIdenticalToPassingScalars` | 传数组和传散数完全等价 | |

## dev.javaquest.basics.variables

| # | 测试类 | 方法 | 断言 | 状态 |
|---|---|---|---|---|
| 83 | `VariableExercisesTest` | `exerciseModuloOfNegativeNumbers` | 练习 1：-7 % 2 等于几？ | 练习答案 |
| 84 | `VariableExercisesTest` | `exerciseEvenCheckThatWorksForNegatives` | 练习 2：写一个判断负数是否偶数、不会出错的表达式 | 练习答案 |
| 85 | `VariableExercisesTest` | `exerciseComparingFloatingPoint` | 练习 3：怎么正确断言 0.1 + 0.2 等于 0.3？ | 练习答案 |
| 86 | `VariableExercisesTest` | `exerciseComparingTwoBuiltStrings` | 练习 4：两个 new 出来的 "java" 怎么判断内容相等？ | 练习答案 |
| 87 | `VariableExercisesTest` | `exerciseAvoidingOverflowByWideningFirst` | 练习 5：怎么算出 Integer.MAX_VALUE + 1 的真实值而不是 -2147483648？ | 练习答案 |
| 88 | `VariableExercisesTest` | `exerciseCharacterSubtractionOrderMatters` | 练习 6：'A' - 'a' 等于几？'a' - 'A' 呢？ | 练习答案 |
| 89 | `VariableTest` | `integerDivisionDropsTheRemainder` | 整数除法直接丢掉余数，不做四舍五入 | |
| 90 | `VariableTest` | `negativeDivisionTruncatesTowardZero` | 负数除法是向零截断，不是向下取整 | |
| 91 | `VariableTest` | `moduloSignFollowsTheDividend` | 取模结果的符号跟被除数走，不跟除数走 | |
| 92 | `VariableTest` | `integerOverflowWrapsAroundSilently` | int 溢出会静默回绕成最小值，不报错 | |
| 93 | `VariableTest` | `byteAdditionPromotesToInt` | 两个 byte 相加自动提升成 int，不会挤回 byte | |
| 94 | `VariableTest` | `floatingPointIsNotExact` | 浮点数天生不精确，0.1 + 0.2 不等于 0.3 | |
| 95 | `VariableTest` | `stringLiteralsShareOneObject` | 两个完全一样的字符串字面量共享同一个对象 | |
| 96 | `VariableTest` | `freshlyBuiltStringIsNotTheSameObject` | 手工 new 出来的字符串内容和字面量相同，但不是同一个对象 | |
| 97 | `VariableTest` | `concatenationLeavesLeftOperandUntouched` | String 不可变：拼接不会修改左边的对象 | |
| 98 | `VariableTest` | `finalLocksTheReferenceNotTheObject` | final 锁的是引用，不是对象内容 | |
| 99 | `VariableTest` | `charIsAnUnsignedNumber` | char 背后就是一个 16 位无符号整数 | |
| 100 | `VariableTest` | `boxedIntegerIdentityIsCachedUpTo127` | 装箱后的 Integer 在 -128 到 127 之间会复用同一个对象 | |
| 101 | `VariableTest` | `boxedIntegerIdentityIsNotCachedAbove127` | 超出缓存范围的装箱对象每次都是新的，== 会失败 | |
| 102 | `VariableTest` | `negativeEvenNumberModuloIsZero` | 负偶数取模是 0，这就是奇偶判断写错的根源 | |
| 103 | `VariableTest` | `overflowCanBeDetectedOrAvoided` | 溢出可以主动检测：Math.addExact 抛异常，转换成 long 则不溢出 | |
| 104 | `VariableTest` | `twoBuiltStringsAreContentEqualButNotIdentical` | 两个 new 出来的 String 内容相等，但 == 为 false | |

