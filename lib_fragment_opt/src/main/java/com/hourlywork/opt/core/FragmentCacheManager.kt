package com.hourlywork.opt.core

import android.util.Log
import android.util.LruCache
import com.google.gson.Gson
import com.hourlywork.opt.config.FragmentOptConfig
import com.hourlywork.opt.db.FragmentStateEntity
import com.hourlywork.opt.db.OptDatabase
import com.hourlywork.opt.viewmodel.InternalStateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Fragment 数据缓存管理器 (重构版)
 * 适配 InternalStateViewModel
 */
object FragmentCacheManager {
    
    private const val TAG = "FragmentCacheManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: OptDatabase
    
    // 内存缓存数据包装类
    data class CacheItem(
        val tag: String,
        val data: Any?, // 业务数据
        val scrollPosition: Int,
        val scrollOffset: Int,
        val pageIndex: Int,
        val timestamp: Long = System.currentTimeMillis()
    )

    // LRU 缓存 (可被回收)
    private lateinit var memoryCache: LruCache<String, CacheItem>
    
    // 常驻内存缓存 (不被 LRU 回收)
    private val keepAliveCache = ConcurrentHashMap<String, CacheItem>()

    // 记录 App 启动时间戳，用于判断是否是跨进程恢复
    private val appStartTime = System.currentTimeMillis()

    fun init(context: android.content.Context) {
        database = OptDatabase.getDatabase(context)
        val maxSize = FragmentOptConfig.get().maxMemoryCacheCount
        
        memoryCache = object : LruCache<String, CacheItem>(maxSize) {
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: CacheItem,
                newValue: CacheItem?
            ) {
                if (evicted) {
                    if (FragmentOptConfig.get().debug) {
                        Log.d(TAG, "LRU evicted: $key, saving to Room")
                    }
                    saveToRoom(oldValue)
                }
            }
        }
    }

    /**
     * 保存 Fragment 状态
     * @param isKeep 是否常驻内存
     */
    internal fun saveState(
        tag: String,
        viewModel: InternalStateViewModel,
        isKeep: Boolean = false,
        saveToDisk: Boolean = false
    ) {
        val data = viewModel.cachedData
        val item = CacheItem(
            tag = tag,
            data = data,
            scrollPosition = viewModel.scrollPosition,
            scrollOffset = viewModel.scrollOffset,
            pageIndex = viewModel.pageIndex
        )
        
        if (isKeep) {
            // 存入常驻缓存
            keepAliveCache[tag] = item
            // 确保不出现在 LRU 中 (避免重复)
            memoryCache.remove(tag)
            if (FragmentOptConfig.get().debug) {
                Log.d(TAG, "Saved state to KeepAlive cache: $tag")
            }
        } else {
            // 存入 LRU 缓存
            memoryCache.put(tag, item)
            // 确保不出现在常驻缓存中 (防止状态切换残留)
            keepAliveCache.remove(tag)
            
            if (FragmentOptConfig.get().debug) {
                Log.d(TAG, "Saved state to LRU cache: $tag")
            }
        }

        // 强制写入 Room (例如 Activity 结束时，防止进程被杀导致内存缓存丢失)
        if (saveToDisk) {
            saveToRoom(item)
        }
    }

    /**
     * 恢复 Fragment 状态
     */
    internal suspend fun <T : Any> restoreState(
        tag: String,
        viewModel: InternalStateViewModel,
        clazz: Class<T>
    ): Boolean {
        // 1. 尝试从常驻内存缓存获取
        val keepItem = keepAliveCache[tag]
        if (keepItem != null) {
            if (FragmentOptConfig.get().debug) {
                Log.d(TAG, "Restored from KeepAlive Cache: $tag")
            }
            applyState(viewModel, keepItem)
            return true
        }

        // 2. 尝试从 LRU 内存缓存获取
        val memoryItem = memoryCache.get(tag)
        if (memoryItem != null) {
            if (FragmentOptConfig.get().debug) {
                Log.d(TAG, "Restored from Memory Cache: $tag")
            }
            applyState(viewModel, memoryItem)
            return true
        }

        // 3. 尝试从 Room 获取
        val dbEntity = database.fragmentStateDao().getState(tag)
        if (dbEntity != null) {
            if (FragmentOptConfig.get().debug) {
                Log.d(TAG, "Restored from Disk Cache: $tag")
            }
            // 反序列化
            try {
                val data = Gson().fromJson(dbEntity.dataJson, clazz)
                viewModel.cachedData = data
                
                // 关键逻辑：判断是否是上一次 App 进程留下的数据
                // 如果 entity.lastActiveTime < appStartTime，说明是上次进程留下的 -> 重置滚动位置
                if (dbEntity.lastActiveTime < appStartTime) {
                    if (FragmentOptConfig.get().debug) {
                        Log.d(TAG, "Detect cross-process restore, resetting scroll position for: $tag")
                    }
                    viewModel.scrollPosition = 0
                    viewModel.scrollOffset = 0
                } else {
                    viewModel.scrollPosition = dbEntity.scrollPosition
                    viewModel.scrollOffset = dbEntity.scrollOffset
                }
                
                viewModel.pageIndex = dbEntity.pageIndex
                
                // 重新放入 LRU (默认)
                // 注意：这里我们暂时不知道是否 keep，因为 restoreState 没传 isKeep。
                // 但没关系，下次 saveState 时会根据最新的 strategy 决定去处。
                // 暂时放入 LRU 是安全的，因为如果是 keep 的，下次 save 会挪过去。
                val newItem = CacheItem(
                    tag = tag,
                    data = data,
                    scrollPosition = dbEntity.scrollPosition,
                    scrollOffset = dbEntity.scrollOffset,
                    pageIndex = dbEntity.pageIndex
                )
                memoryCache.put(tag, newItem)
                
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return false
    }

    private fun applyState(
        viewModel: InternalStateViewModel,
        item: CacheItem
    ) {
        viewModel.cachedData = item.data
        viewModel.scrollPosition = item.scrollPosition
        viewModel.scrollOffset = item.scrollOffset
        viewModel.pageIndex = item.pageIndex
    }

    private fun saveToRoom(item: CacheItem) {
        scope.launch {
            val json = try {
                Gson().toJson(item.data)
            } catch (e: Exception) {
                null
            }
            
            val entity = FragmentStateEntity(
                tag = item.tag,
                dataJson = json,
                scrollPosition = item.scrollPosition,
                scrollOffset = item.scrollOffset,
                pageIndex = item.pageIndex,
                lastActiveTime = System.currentTimeMillis()
            )
            database.fragmentStateDao().saveState(entity)
        }
    }
    
    fun clear(tag: String) {
        memoryCache.remove(tag)
        keepAliveCache.remove(tag)
        scope.launch {
            database.fragmentStateDao().clearState(tag)
        }
    }

    /**
     * 重置缓存中的滚动位置，但保留业务数据
     * 用于用户退出页面后，清除历史滚动位置，但下次进入时如果还有缓存数据，依然可以恢复数据（可选）
     * 或者在某些业务场景下只重置位置
     */
    fun resetScrollState(tag: String) {
        // 更新 LRU 缓存
        val memoryItem = memoryCache.get(tag)
        if (memoryItem != null) {
            val newItem = memoryItem.copy(
                scrollPosition = 0,
                scrollOffset = 0
            )
            memoryCache.put(tag, newItem)
        }

        // 更新 KeepAlive 缓存
        val keepItem = keepAliveCache[tag]
        if (keepItem != null) {
            val newItem = keepItem.copy(
                scrollPosition = 0,
                scrollOffset = 0
            )
            keepAliveCache[tag] = newItem
        }

        // 更新数据库
        scope.launch {
            val dbEntity = database.fragmentStateDao().getState(tag)
            if (dbEntity != null) {
                val newEntity = dbEntity.copy(
                    scrollPosition = 0,
                    scrollOffset = 0
                )
                database.fragmentStateDao().saveState(newEntity)
            }
        }
    }
}
