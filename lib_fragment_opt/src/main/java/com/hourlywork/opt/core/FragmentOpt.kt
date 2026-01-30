package com.hourlywork.opt.core

import android.content.Context
import com.hourlywork.opt.config.FragmentOptConfig

object FragmentOpt {
    
    /**
     * 初始化库
     * 建议在 Application onCreate 中调用
     */
    fun init(context: Context, config: FragmentOptConfig = FragmentOptConfig()) {
        FragmentOptConfig.init(config)
        FragmentCacheManager.init(context)
    }
    
    /**
     * 清除所有磁盘缓存
     */
    suspend fun clearAllDiskCache(context: Context) {
        // 实现清除逻辑
        // 需要访问 Dao
    }
}
