package com.hourlywork.opt.core

import androidx.recyclerview.widget.RecyclerView

/**
 * 优化库接入接口
 * 任何 Fragment 只要实现此接口，并注册 Helper 即可获得状态保存/恢复能力
 */
interface IOptStrategy<T : Any> {
    
    /**
     * 获取当前 Fragment 的唯一 Tag
     */
    fun getUniqueTag(): String
    
    /**
     * 提供 RecyclerView 实例
     * 库会监听此 RecyclerView 的 Adapter 并在数据加载后自动恢复滚动位置
     * 如果返回 null，则不会执行默认的滚动恢复逻辑，可以配合 onScrollStateRestored 自行处理
     */
    fun getRecyclerView(): RecyclerView?
    
    /**
     * 获取当前需要持久化的数据
     * 通常是 ViewModel 中的 State 或 List 数据
     */
    fun getCurrentData(): T?

    /**
     * 数据恢复回调
     * @param data 恢复的数据
     */
    fun onDataRestored(data: T)

    /**
     * 是否常驻内存（不被 LRU 回收）
     * 默认为 false。如果返回 true，该 Fragment 的状态将单独存储，不会因为缓存满而被移除。
     */
    fun shouldKeepInMemory(): Boolean = false

    /**
     * Activity 结束时是否保留状态
     * 默认为 true。
     * - true: Activity 销毁后，数据依然保留在缓存/数据库中，下次进入 Activity 时会自动恢复数据（但滚动位置会被重置）。
     * - false: Activity 销毁时，彻底清除该 Fragment 的所有缓存数据。下次进入相当于全新启动。
     */
    fun shouldRetainStateAfterActivityFinish(): Boolean = true
}
