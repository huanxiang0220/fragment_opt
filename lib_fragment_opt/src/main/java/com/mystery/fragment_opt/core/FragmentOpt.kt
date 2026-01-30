package com.mystery.fragment_opt.core

import android.content.Context
import com.mystery.fragment_opt.config.FragmentOptConfig

object FragmentOpt {
    
    /**
     * 初始化库
     * 建议在 Application onCreate 中调用
     */
    fun init(context: Context, config: FragmentOptConfig = FragmentOptConfig()) {
        FragmentOptConfig.init(config)
        FragmentCacheManager.init(context)
    }

}