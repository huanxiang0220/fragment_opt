package com.mystery.fragment_opt.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 库内部使用的 ViewModel，用于持有状态
 * 用户不可见，自动绑定
 */
internal class InternalStateViewModel : ViewModel() {
    
    // 内存中的业务数据 (Json String 形式，或者是 Any 对象)
    // 为了泛型通用性，这里存 Any?，在 Helper 中强转
    var cachedData: Any? = null

    // 列表滚动位置
    var scrollPosition: Int = 0
    var scrollOffset: Int = 0
    
    // 分页加载页码
    var pageIndex: Int = 1
    
    // 标记是否已恢复
    var isRestored: Boolean = false
    
    fun clearMemory() {
        cachedData = null
        isRestored = false
    }
}
