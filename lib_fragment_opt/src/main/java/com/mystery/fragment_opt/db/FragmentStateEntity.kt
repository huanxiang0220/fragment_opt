package com.mystery.fragment_opt.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fragment_state_cache")
data class FragmentStateEntity(
    @PrimaryKey
    val tag: String,
    
    /**
     * 序列化后的 ViewModel 业务数据
     */
    val dataJson: String?,
    
    /**
     * RecyclerView 滚动位置 (LayoutManager.onSaveInstanceState)
     * 或者简单的 position index
     */
    val scrollPosition: Int = 0,
    
    /**
     * 滚动偏移量
     */
    val scrollOffset: Int = 0,
    
    /**
     * 当前加载的页码
     */
    val pageIndex: Int = 1,
    
    /**
     * 最后活跃时间，用于淘汰策略
     */
    val lastActiveTime: Long = System.currentTimeMillis(),

    /**
     * 上次不可见时间
     */
    val lastHiddenTime: Long = 0
)
