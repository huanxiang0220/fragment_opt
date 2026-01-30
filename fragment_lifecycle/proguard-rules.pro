-keep class com.mystery.fragment.lifecycle.FragmentLifeOwner {
    public *;
}
-keep class com.mystery.fragment.lifecycle.FragmentLifecycle {
    public *;
}
-keep class com.mystery.fragment.lifecycle.FragmentLifecycleRegistry {
    public *;
}
-keep class com.mystery.fragment.lifecycle.IFragmentLifecycleCallbacks {
    public *;
}

-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }

# 忽略 JDK 17 字符串拼接工厂类的缺失警告，D8 会处理脱糖
-dontwarn java.lang.invoke.StringConcatFactory