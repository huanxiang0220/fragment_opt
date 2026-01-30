package com.mystery.fragment_opt.config

/**
 * 优化库配置类
 */
data class FragmentOptConfig(
    /**
     * 最大内存缓存 Fragment 数量
     * 超过此数量的 Fragment 状态将被持久化到 Room，并从内存中清除 ViewModel
     */
    val maxMemoryCacheCount: Int = 5,

    /**
     * 是否开启调试日志
     */
    val debug: Boolean = false,

    /**
     * 默认自动刷新阈值 (毫秒)
     * 当 Fragment 在后台时间超过此阈值时，onResume 会触发 onFragmentLongTimeBackground
     * 默认 2 分钟
     */
    val defaultAutoRefreshDuration: Long = 2 * 60 * 1000
) {
    companion object {
        @Volatile
        private var instance: FragmentOptConfig? = null

        fun get(): FragmentOptConfig {
            return instance ?: synchronized(this) {
                instance ?: FragmentOptConfig().also { instance = it }
            }
        }

        fun init(config: FragmentOptConfig) {
            instance = config
        }
    }
}
