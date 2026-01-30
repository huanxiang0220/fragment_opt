package com.hourlywork.opt.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

/**
 * 针对旧版 ViewPager 的 Fragment 复用适配器
 * 基于 FragmentStatePagerAdapter 实现，适用于 Fragment 数量较多(50+)的场景
 *
 * @param T 数据项类型
 */
abstract class OptLegacyFragmentStateAdapter<T>(
    fragmentManager: FragmentManager,
    items: List<T> = emptyList()
) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_SET_USER_VISIBLE_HINT) {

    private val mItems = ArrayList<T>(items)

    /**
     * 更新数据源
     */
    fun updateData(newItems: List<T>) {
        mItems.clear()
        mItems.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = mItems.size

    override fun getItem(position: Int): Fragment {
        return createFragment(position, mItems[position])
    }

    /**
     * 创建 Fragment
     * 注意：应当将 items[position] 中的唯一标识传递给 Fragment 作为 Tag
     */
    abstract fun createFragment(position: Int, item: T): Fragment

    /**
     * ViewPager 的 PagerAdapter 默认根据 position 也就是 item 的位置来判断是否改变
     * 如果要支持动态刷新，建议重写 getItemPosition
     */
    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    /**
     * 获取指定位置的数据项
     */
    fun getItemData(position: Int): T {
        return mItems[position]
    }
}
