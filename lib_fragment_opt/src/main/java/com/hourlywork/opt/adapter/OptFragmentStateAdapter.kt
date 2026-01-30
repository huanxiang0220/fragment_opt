package com.hourlywork.opt.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * 通用 Fragment 复用适配器
 * @param T 数据项类型
 */
abstract class OptFragmentStateAdapter<T>(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    items: List<T> = emptyList()
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val mItems = ArrayList<T>(items)

    fun updateData(newItems: List<T>) {
        mItems.clear()
        mItems.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = mItems.size

    override fun createFragment(position: Int): Fragment {
        return createFragment(position, mItems[position])
    }

    /**
     * 创建 Fragment
     * 注意：应当将 items[position] 中的唯一标识传递给 Fragment 作为 Tag
     */
    abstract fun createFragment(position: Int, item: T): Fragment

    /**
     * 必须重写此方法以支持 Fragment 复用
     * 返回 item 的唯一 ID
     */
    override fun getItemId(position: Int): Long {
        return getItemId(mItems[position])
    }

    override fun containsItem(itemId: Long): Boolean {
        return mItems.any { getItemId(it) == itemId }
    }

    /**
     * 获取 Item 的唯一 ID
     */
    abstract fun getItemId(item: T): Long
}
