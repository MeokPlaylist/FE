package com.example.meokpli.Main.Search

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.Auth.Network
import com.example.meokpli.Main.Resettable
import com.example.meokpli.R
import com.example.meokpli.databinding.FragmentSearchUserBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchUserFragment : Fragment(R.layout.fragment_search_user), Resettable {

    private lateinit var etSearch: EditText
    private lateinit var recyclerUsers: RecyclerView
    private lateinit var recyclerRecent: RecyclerView
    private lateinit var layoutRecent: View
    private lateinit var adapter: UserAdapter
    private lateinit var recentAdapter: RecentAdapter

    private var searchJob: Job? = null
    private var lastKeyword: String? = null // 마지막 검색어 저장용

    private var currentPage = 0
    private val pageSize = 10
    private var isLoading = false
    private var hasNext = true

    private var _binding: FragmentSearchUserBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch = view.findViewById(R.id.etSearch)
        recyclerUsers = view.findViewById(R.id.recyclerUsers)
        recyclerRecent = view.findViewById(R.id.recyclerRecent)
        layoutRecent = view.findViewById(R.id.layoutRecent)

        // 검색 결과 어댑터
        adapter = UserAdapter(mutableListOf()) { user ->
            val bundle = Bundle().apply { putString("arg_nickname", user.nickname) }
            requireParentFragment().findNavController()
                .navigate(R.id.action_searchUserFragment_to_otherProfileFragment, bundle)
        }
        recyclerUsers.layoutManager = LinearLayoutManager(requireContext())
        recyclerUsers.adapter = adapter

        recyclerUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val lm = rv.layoutManager as LinearLayoutManager
                val lastVisible = lm.findLastVisibleItemPosition()
                val total = lm.itemCount

                if (!isLoading && hasNext && lastVisible >= total - 3) {
                    runSearch(lastKeyword ?: return, nextPage = true)
                }
            }
        })

        // 최근 검색어 어댑터
        recentAdapter = RecentAdapter(
            onClick = { keyword: String ->
                etSearch.setText(keyword)
                runSearch(keyword)
                saveRecentSearch(keyword)
            },
            onDelete = { keyword: String ->
                deleteRecentSearch(keyword)
            }
        )
        recyclerRecent.layoutManager = LinearLayoutManager(requireContext())
        recyclerRecent.adapter = recentAdapter

        // 앱 시작 시 최근 검색 로드
        loadRecentSearches()

        // savedInstanceState에서 복원
        savedInstanceState?.getString("lastKeyword")?.let { restored ->
            etSearch.setText(restored)
            runSearch(restored) // 검색 결과 다시 실행
        }

        // 엔터 입력 이벤트 감지
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val keyword = etSearch.text.toString().trim()
                if (keyword.isNotEmpty()) {
                    runSearch(keyword)
                    saveRecentSearch(keyword) // 엔터 누를 때만 저장
                }
                true
            } else false
        }

        // 검색창 입력 이벤트 감지
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString().trim()

                // 이전 검색 Job 취소
                searchJob?.cancel()

                searchJob = lifecycleScope.launch {
                    delay(300) // 0.3초 디바운스
                    if (keyword.isEmpty()) {
                        // 검색어 없으면 → 최근검색 보이기
                        layoutRecent.visibility = View.VISIBLE
                        recyclerUsers.visibility = View.GONE
                    } else {
                        runSearch(keyword)   // 검색만 실행
                    }
                }
            }
        })
    }
    //검색 초기화
    override fun resetToDefault() {
        etSearch.setText("") // 검색어 초기화
        adapter.submitList(emptyList()) // 검색 결과 비우기
        layoutRecent.visibility = View.VISIBLE // 최근 검색 다시 보이기
        recyclerUsers.visibility = View.GONE
    }

    // Fragment 상태 저장
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lastKeyword = etSearch.text.toString().trim()
        outState.putString("lastKeyword", lastKeyword)
    }

    // 검색 실행
    @RequiresApi(Build.VERSION_CODES.O)
    private fun runSearch(keyword: String, resetPage: Boolean = false, nextPage: Boolean = false) {
        val api = Network.userApi(requireContext())

        if (resetPage) {
            currentPage = 0
            hasNext = true
            adapter.clearItems()
        }
        if (nextPage) currentPage++

        isLoading = true
        lifecycleScope.launch {
            try {
                val response = api.searchUsers(keyword, currentPage, pageSize)

                layoutRecent.visibility = View.GONE
                recyclerUsers.visibility = View.VISIBLE

                adapter.submitList(response.content)
                hasNext = response.hasNext
                lastKeyword = keyword
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // SharedPreferences에 저장
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun saveRecentSearch(keyword: String) {
        val prefs = requireContext().getSharedPreferences("recent_search", 0)
        val list = prefs.getStringSet("keywords", linkedSetOf()) ?: linkedSetOf()
        val mutableList = list.toMutableList()

        // 중복 제거 + 최대 10개 유지
        mutableList.remove(keyword)
        mutableList.add(0, keyword)
        if (mutableList.size > 10) mutableList.removeLast()

        prefs.edit().putStringSet("keywords", mutableList.toSet()).apply()
        Log.d("RecentSearch", "저장됨: $mutableList")
        loadRecentSearches()
    }

    private fun deleteRecentSearch(keyword: String) {
        val prefs = requireContext().getSharedPreferences("recent_search", 0)
        val list = prefs.getStringSet("keywords", linkedSetOf())?.toMutableSet() ?: mutableSetOf()

        list.remove(keyword)
        prefs.edit().putStringSet("keywords", list).apply()

        loadRecentSearches()
    }

    // SharedPreferences에서 불러오기
    private fun loadRecentSearches() {
        val prefs = requireContext().getSharedPreferences("recent_search", 0)
        val list = prefs.getStringSet("keywords", linkedSetOf()) ?: linkedSetOf()
        val result = list.toList()
        Log.d("RecentSearch", "불러옴: $result")
        recentAdapter.submitList(result)
    }
}
