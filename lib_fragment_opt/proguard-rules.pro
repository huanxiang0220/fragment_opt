# 公共 API - 只保留公开成员（方法/属性），私有内部实现（如 private 方法、字段）将被混淆
-keep class com.mystery.fragment_opt.core.FragmentOpt { 
    public *; 
}
-keep class com.mystery.fragment_opt.core.IOptStrategy { 
    public *; 
}
-keep class com.mystery.fragment_opt.core.FragmentOptHelper { 
    public *; 
}
-keep class com.mystery.fragment_opt.core.FragmentOptHelper$Companion {
    public *;
}
-keep class com.mystery.fragment_opt.config.FragmentOptConfig { 
    public *; 
}
-keep class com.mystery.fragment_opt.config.FragmentOptConfig$Companion {
    public *;
}
-keep class com.mystery.fragment_opt.adapter.OptFragmentStateAdapter { 
    public *; 
}
-keep class com.mystery.fragment_opt.adapter.OptLegacyFragmentStateAdapter { 
    public *; 
}

# Room 实体类 (必须保留字段名以保证数据库 Schema 稳定)
-keep class com.mystery.fragment_opt.db.FragmentStateEntity { *; }
# Room DAO 和 Database (可选，建议保留以避免潜在问题)
-keep class com.mystery.fragment_opt.db.FragmentStateDao { *; }
-keep class com.mystery.fragment_opt.db.OptDatabase { *; }

-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
