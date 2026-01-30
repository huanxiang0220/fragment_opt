# Fragment 优化库 (lib_fragment_opt)

该库提供了针对大量 Fragment 复用场景（如 ViewPager + 50+ Fragments）的内存优化方案。
核心功能：
1. **三级缓存**：ViewModel 内存 -> LRU 内存缓存 -> Room 磁盘存储。
2. **状态自动恢复**：自动保存和恢复 ViewModel 业务数据及 RecyclerView 滚动位置。
3. **内存控制**：通过 LRU 限制持有数据的 Fragment 数量，防止 OOM。

## 接入指南

### 1. 初始化
在 Application 中初始化：
```kotlin
FragmentOpt.init(this, FragmentOptConfig(
    maxMemoryCacheCount = 5, // 仅保留最近 5 个 Fragment 的数据在内存
    debug = true
))
```

### 2. ViewModel 改造
继承 `OptBaseViewModel`，实现状态存取接口：
```kotlin
class MyViewModel : OptBaseViewModel<MyState>() {
    override fun createInitialState() = MyState()
    override fun getPersistData() = uiState.value
}
```

### 3. Fragment 改造
继承 `OptFragment`：
```kotlin
class MyFragment : OptFragment<MyState, MyViewModel>(R.layout.fragment_my) {
    override fun getUniqueTag() = arguments?.getString("id") ?: ""
    override fun getRecyclerView() = binding.recyclerView
    override fun getDataClass() = MyState::class.java
    
    override fun onDataRestored(data: MyState, isFromCache: Boolean) {
        // 恢复数据到 UI
        adapter.submitList(data.list)
        // 恢复滚动位置
        restoreScrollState()
    }
    
    override fun onInitData() {
        // 首次加载网络请求
        viewModel.loadData()
    }
}
```

### 4. Adapter 使用

#### 对于 ViewPager2
继承 `OptFragmentStateAdapter`：
```kotlin
class MyAdapter(fm: FragmentManager, lifecycle: Lifecycle) 
    : OptFragmentStateAdapter<MyData>(fm, lifecycle) {
    
    override fun createFragment(position: Int, item: MyData): Fragment {
        return MyFragment.newInstance(item.id)
    }
    
    override fun getItemId(item: MyData): Long = item.id.hashCode().toLong()
}
```

#### 对于旧版 ViewPager
继承 `OptLegacyFragmentStateAdapter`：
```kotlin
class MyLegacyAdapter(fm: FragmentManager) 
    : OptLegacyFragmentStateAdapter<MyData>(fm) {
    
    override fun createFragment(position: Int, item: MyData): Fragment {
        return MyFragment.newInstance(item.id)
    }
}
```
