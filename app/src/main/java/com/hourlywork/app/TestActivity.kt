package com.hourlywork.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.hourlywork.opt.adapter.OptFragmentStateAdapter
import com.hourlywork.opt.config.FragmentOptConfig
import com.hourlywork.opt.core.FragmentOpt
import com.hourlywork.opt.core.FragmentOptHelper
import com.hourlywork.opt.core.IOptStrategy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        FragmentOpt.init(this, FragmentOptConfig(
            maxMemoryCacheCount = 3,
            debug = true
        ))

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        
        val dataList = (1..50).map { i ->
            PageData(id = "page_$i", title = "第 $i 页")
        }

        val adapter = TestPageAdapter(supportFragmentManager, lifecycle, dataList)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1
    }
}

data class PageData(val id: String, val title: String)

data class TestState(
    val title: String,
    val items: List<String>
)

// ================= ViewModel (普通 ViewModel，不再继承库基类) =================

class TestViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow<TestState?>(null)
    val uiState: StateFlow<TestState?> = _uiState
    
    fun loadData(pageId: String) {
        viewModelScope.launch {
            delay(500)
            val list = (1..100).map { "Item $it (Page: $pageId) - Data" }
            val state = TestState(
                title = "Loaded: $pageId",
                items = list
            )
            _uiState.value = state
        }
    }

    // 提供给外部设置状态的方法（用于恢复数据）
    fun restoreState(state: TestState) {
        _uiState.value = state
    }
}

// ================= Fragment (普通 Fragment，实现接口) =================

class TestFragment : Fragment(R.layout.fragment_test_opt), IOptStrategy<TestState> {

    private lateinit var tvTitle: TextView
    private lateinit var recyclerView: RecyclerView
    private val listAdapter = SimpleListAdapter()
    
    // 使用标准 ViewModel
    private val viewModel: TestViewModel by lazy {
        ViewModelProvider(this)[TestViewModel::class.java]
    }

    // 声明 Helper
    private lateinit var optHelper: FragmentOptHelper<TestState>

    companion object {
        private const val ARG_ID = "arg_id"
        private const val ARG_TITLE = "arg_title"

        fun newInstance(id: String, title: String): TestFragment {
            val fragment = TestFragment()
            val args = Bundle()
            args.putString(ARG_ID, id)
            args.putString(ARG_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 关键：一行代码接入优化库
        optHelper = FragmentOptHelper.attach(this, this)
        
        // 正常初始化加载逻辑 (业务层自己决定何时加载)
        if (viewModel.uiState.value == null) {
             val id = arguments?.getString(ARG_ID) ?: ""
             viewModel.loadData(id)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvTitle = view.findViewById(R.id.tv_title)
        recyclerView = view.findViewById(R.id.recycler_view)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = listAdapter
        
        val title = arguments?.getString(ARG_TITLE) ?: ""
        tvTitle.text = "初始化: $title"
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state != null) {
                    tvTitle.text = state.title
                    listAdapter.submitList(state.items)
                    // 不需要手动调用 restoreScrollState，Helper 会自动处理
                }
            }
        }
    }

    // --- 实现 IOptStrategy 接口 ---

    override fun getUniqueTag(): String {
        return arguments?.getString(ARG_ID) ?: ""
    }

    // 只需要提供 RecyclerView，Helper 会自动监听滚动并保存
    override fun getRecyclerView(): RecyclerView? {
        // 只要在 onViewCreated 后能获取到即可
        return view?.findViewById(R.id.recycler_view)
    }
    
    // override fun getDataClass(): Class<TestState> = TestState::class.java 
    // ^^^ 已移除，现在通过 attach<TestState> 自动获取

    override fun getCurrentData(): TestState? {
        return viewModel.uiState.value
    }

    override fun onDataRestored(data: TestState) {
        Log.d("TestFragment", "数据已恢复: ${data.title}")
        // 恢复数据到 ViewModel，UI 会自动更新
        viewModel.restoreState(data)
    }

    // override fun onInitData() {
    //     // 没有缓存，执行正常加载
    //     val id = arguments?.getString(ARG_ID) ?: ""
    //     tvTitle.text = "加载中 ($id)..."
    //     viewModel.loadData(id)
    // }
}

// ================= Adapter =================

class TestPageAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle,
    items: List<PageData>
) : OptFragmentStateAdapter<PageData>(fm, lifecycle, items) {

    override fun createFragment(position: Int, item: PageData): Fragment {
        return TestFragment.newInstance(item.id, item.title)
    }

    override fun getItemId(item: PageData): Long {
        return item.id.hashCode().toLong()
    }
}

// ================= RecyclerView Adapter =================

class SimpleListAdapter : RecyclerView.Adapter<SimpleListAdapter.VH>() {
    private val items = ArrayList<String>()

    fun submitList(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tv: TextView = view as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = TextView(parent.context)
        tv.setPadding(32, 32, 32, 32)
        tv.textSize = 16f
        tv.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tv.text = items[position]
    }

    override fun getItemCount() = items.size
}
