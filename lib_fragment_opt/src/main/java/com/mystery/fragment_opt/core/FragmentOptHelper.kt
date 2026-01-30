package com.mystery.fragment_opt.core

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStateAtLeast
import androidx.recyclerview.widget.LinearLayoutManager
import com.mystery.fragment.lifecycle.FragmentLifeOwner
import com.mystery.fragment.lifecycle.IFragmentLifecycleCallbacks
import com.mystery.fragment_opt.viewmodel.InternalStateViewModel
import kotlinx.coroutines.launch

/**
 * 优化库辅助类
 * 负责绑定 Fragment 生命周期，自动执行状态保存和恢复
 * 替代了原有的继承方式
 */
class FragmentOptHelper<T : Any>(
    private val fragment: Fragment,
    private val strategy: IOptStrategy<T>,
    private val dataClass: Class<T>
) : DefaultLifecycleObserver, IFragmentLifecycleCallbacks {

    private lateinit var internalViewModel: InternalStateViewModel

    companion object {
        /**
         * 绑定 Fragment
         * 建议在 Fragment.onCreate 中调用
         */
        inline fun <reified T : Any> attach(fragment: Fragment, strategy: IOptStrategy<T>): FragmentOptHelper<T> {
            val helper = FragmentOptHelper(fragment, strategy, T::class.java)
            fragment.lifecycle.addObserver(helper)
            
            // 自动适配 FragmentLifeOwner
            if (fragment is FragmentLifeOwner) {
                fragment.getFragmentLifecycle().registerLifecycleCallbacks(helper)
            }
            
            return helper
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        // 获取库内部专用的 ViewModel
        // 注意：这里使用 Fragment 作为 ViewModelStoreOwner，
        // 确保 ViewModel 生命周期跟随 Fragment（但在配置变更或 ViewPager 销毁 View 时可能存活）
        internalViewModel = ViewModelProvider(fragment)[InternalStateViewModel::class.java]

        // 启动协程恢复数据
        fragment.lifecycleScope.launch {
            // 1. 先进行异步的数据恢复操作 (查内存/Room)
            val restored = FragmentCacheManager.restoreState(
                strategy.getUniqueTag(),
                internalViewModel,
                dataClass
            )
            
            // 2. 关键修改：等待 Fragment 至少达到 CREATED 状态之后
            // 实际上为了确保 onViewCreated 执行，我们通常需要等待 View 创建
            // 但 Lifecycle 没有直接对应 ViewCreated 的状态。
            // 实际上，Fragment 的 ViewLifecycleOwner 会在 onViewCreated 后变为 INITIALIZED
            // 这里我们使用一个简单策略：在主线程队列中排队，或者监听 ViewLifecycleOwner
            
            // 为了保证 onDataRestored 在 onViewCreated 之后执行，
            // 我们可以利用 ViewLifecycleOwner 的生命周期，或者简单地使用 whenStateAtLeast(STARTED)
            // 因为 onViewCreated 一定在 onStart 之前执行。
            // 让 onDataRestored 在 onStart 时执行是安全的，此时 View 肯定好了。
            
            // 如果希望更早一点（在 onStart 之前，但在 onViewCreated 之后），
            // 可以观察 viewLifecycleOwnerLiveData
            
            fragment.lifecycle.whenStateAtLeast(Lifecycle.State.CREATED) {
                // 此时 onCreate 已完成。
                // 如果我们想确保 onViewCreated 执行，最好是等到 ViewLifecycleOwner 可用
                // 或者直接在此处判断，因为协程恢复通常有耗时，大概率已经 onViewCreated 了
                
                // 更稳妥的方式：等待 View 创建
                if (fragment.view == null) {
                    // 如果 View 还没创建，可以通过观察 viewLifecycleOwnerLiveData 来回调
                    // 但为了简化，我们这里使用 whenStateAtLeast(STARTED) 确保 View 已就绪
                    // 或者直接在这里不做处理，改为在 onStart 中分发？
                    // 不，我们还是希望尽早恢复。
                }
            }

            // 最终方案：
            // 我们将回调逻辑包装在 viewLifecycleOwner 的观察中，或者简单地等到 STARTED
            // 考虑到用户体验，等到 STARTED 可能稍微晚了一点点（用户可能看到空页面闪烁）
            // 但这最安全。
            // 另一种方式是：手动检查 fragment.view 是否为 null
            
            if (restored) {
                internalViewModel.isRestored = true
                @Suppress("UNCHECKED_CAST")
                val data = internalViewModel.cachedData as? T
                if (data != null) {
                    // 确保在 View 创建后回调
                    awaitViewCreatedAndDispatch(data)
                }
            } else {
                internalViewModel.isRestored = false
                // 初始化数据通常包含网络请求，可以在 onCreate 就开始，不需要等 View
                // strategy.onInitData() // 已移除
            }
        }
    }
    
    private fun awaitViewCreatedAndDispatch(data: T) {
        // 如果当前 View 已经创建，直接分发
        if (fragment.view != null) {
            dispatchRestore(data)
            return
        }
        
        // 否则，监听 viewLifecycleOwnerLiveData
        fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
            if (viewLifecycleOwner != null) {
                // View 刚刚创建 (onViewCreated 刚开始或刚结束)
                // 使用 lifecycleScope 确保在主线程且活跃
                viewLifecycleOwner.lifecycleScope.launch {
                    dispatchRestore(data)
                }
                // 移除观察者，避免重复回调 (LiveData 粘性事件特性刚好符合需求，但也需要移除)
                fragment.viewLifecycleOwnerLiveData.removeObservers(fragment)
            }
        }
    }
    
    private fun dispatchRestore(data: T) {
        strategy.onDataRestored(data)
        tryRestoreScrollPosition()
    }

    /**
     * 手动更新最后不可见时间
     * 建议在 Fragment 变为不可见时调用 (如 onPause, onHiddenChanged)
     *
     * @param time 时间戳，默认当前时间
     */
    fun updateLastHiddenTime(time: Long = System.currentTimeMillis()) {
        if (::internalViewModel.isInitialized) {
            internalViewModel.lastHiddenTime = time
        }
    }

    /**
     * 获取最后一次保存的不可见时间
     * 即使 Fragment 被销毁重建，只要数据恢复成功，此值也会被恢复
     */
    fun getLastHiddenTime(): Long {
        return if (::internalViewModel.isInitialized) {
            internalViewModel.lastHiddenTime
        } else {
            0L
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        // Log.d("FragmentOptHelper", "onResume")
    }
    
    override fun onFragmentResume() {
        val lastHiddenTime = getLastHiddenTime()
        if (lastHiddenTime > 0) {
            val now = System.currentTimeMillis()
            if (now - lastHiddenTime > strategy.getAutoRefreshDuration()) {
                strategy.onFragmentLongTimeBackground(now - lastHiddenTime)
            }
            // 重置，避免重复触发
            updateLastHiddenTime(0)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        saveScrollState()
    }
    
    override fun onFragmentPause() {
        // 当通过 FragmentLifeOwner 触发不可见时，自动保存时间戳和滚动位置
        updateLastHiddenTime()
        saveScrollState()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        // 自动反注册
        if (fragment is FragmentLifeOwner) {
            fragment.getFragmentLifecycle().unregisterLifecycleCallbacks(this)
        }
        
        val isFinishing = fragment.activity?.isFinishing == true
        val tag = strategy.getUniqueTag()

        // 如果 Activity 正在结束，且策略配置为不保留状态
        if (isFinishing && !strategy.shouldRetainStateAfterActivityFinish()) {
            FragmentCacheManager.clear(tag)
            return
        }

        // 保存当前状态到 ViewModel (内存)
        // 此时 ViewModel 尚未销毁
        val currentData = strategy.getCurrentData()
        internalViewModel.cachedData = currentData
        
        // 保存滚动位置
        saveScrollState()

        // 如果是 Activity 结束，且策略要求保留数据，则强制重置滚动位置为 0
        // 这样可以确保下次进入时位置是新的，但数据是旧的
        if (isFinishing) {
            internalViewModel.scrollPosition = 0
            internalViewModel.scrollOffset = 0
        }

        // 将状态提交给 CacheManager (决定是留内存还是存 Room)
        // 如果 strategy.shouldKeepInMemory() 为 true，则会存入 keepAliveCache 而非 memoryCache (LRU)
        // 如果 isFinishing 为 true，则强制写入磁盘，防止进程被杀导致数据丢失
        FragmentCacheManager.saveState(
            tag, 
            internalViewModel,
            strategy.shouldKeepInMemory(),
            saveToDisk = isFinishing
        )
        
        // 模拟释放 ViewModel 内存（如果 CacheManager 决定移出内存）
        // 但由于 ViewModelProvider 机制，真正的 ViewModel 清理要等 Fragment 彻底销毁
        // 这里我们只能做到通知 CacheManager 备份数据
    }

    private fun saveScrollState() {
        // 优先尝试自定义保存 (已移除，通过 getCurrentData 统一处理)
        // val customState = strategy.onSaveCustomScrollState()
        
        // 默认 RecyclerView 逻辑
        val rv = strategy.getRecyclerView() ?: return
        val layoutManager = rv.layoutManager as? LinearLayoutManager ?: return

        val position = layoutManager.findFirstVisibleItemPosition()
        val view = layoutManager.findViewByPosition(position)
        val offset = view?.top ?: 0

        internalViewModel.scrollPosition = position
        internalViewModel.scrollOffset = offset
    }

    private fun tryRestoreScrollPosition() {
        // 优先尝试自定义恢复 (已移除，通过 onDataRestored 统一处理)
        // if (strategy.onRestoreCustomScrollState(...)) return

        // 默认 RecyclerView 逻辑
        val rv = strategy.getRecyclerView() ?: return
        
        // 如果 RecyclerView 已经有 Adapter 并且 Layout 好了，直接滚动
        // 否则可能需要等待数据 submit
        
        // 简单策略：Post 到队列尾部，给 Adapter 一点时间 submitList
        rv.post {
            val layoutManager = rv.layoutManager as? LinearLayoutManager ?: return@post
            if (internalViewModel.scrollPosition >= 0) {
                layoutManager.scrollToPositionWithOffset(
                    internalViewModel.scrollPosition,
                    internalViewModel.scrollOffset
                )
            }
        }
    }
}
