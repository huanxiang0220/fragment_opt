# 保护公共 API 不被使用方混淆 - 只保留公开接口
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

# 保护 Room 实体类字段名，确保数据库 Schema 在使用方也能保持稳定
-keep class com.mystery.fragment_opt.db.FragmentStateEntity { *; }
-keep class com.mystery.fragment_opt.db.FragmentStateDao { *; }
-keep class com.mystery.fragment_opt.db.OptDatabase { *; }

-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes *Annotation*
