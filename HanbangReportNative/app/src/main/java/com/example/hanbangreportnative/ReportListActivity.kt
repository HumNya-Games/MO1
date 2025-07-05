package com.example.hanbangreportnative

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.AdapterView
import android.content.Intent
import androidx.core.content.ContextCompat
import android.graphics.drawable.GradientDrawable

class ReportListActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var tvNoData: TextView
    private lateinit var layoutEditButtons: LinearLayout
    private lateinit var btnSelectAll: Button
    private lateinit var btnDelete: Button
    private lateinit var tabWaiting: TextView
    private lateinit var tabCompleted: TextView
    private lateinit var btnMoveToWaiting: Button

    private var currentTab = "신고 대기" // 기본 탭
    private var reportList = mutableListOf<ReportData>()
    private lateinit var adapter: ReportListAdapter
    private var fromFloatingBall = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_list)
        fromFloatingBall = intent.getBooleanExtra("from_floating_ball", false)

        initViews()
        setupListeners()
        loadData()
        updateUI()
        applyTabGroupBackground()
        applyEditButtonAreaMarginAndTint()
        applyTabButtonWidth()
        applyEditButtonPadding()
        updateEditButtons()
    }

    private fun initViews() {
        listView = findViewById(R.id.report_list_view)
        tvNoData = findViewById(R.id.tv_no_data)
        layoutEditButtons = findViewById(R.id.layout_edit_buttons)
        btnSelectAll = findViewById(R.id.btn_select_all)
        btnDelete = findViewById(R.id.btn_delete)
        tabWaiting = findViewById(R.id.tab_waiting)
        tabCompleted = findViewById(R.id.tab_completed)
        btnMoveToWaiting = findViewById(R.id.btn_move_to_waiting)

        adapter = ReportListAdapter(this, mutableListOf())
        listView.adapter = adapter

        findViewById<BottomNavBar>(R.id.bottom_nav_bar).setCurrentScreen(1)
    }

    private fun setupListeners() {
        // 백 버튼
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            handleBack()
        }

        // 탭 클릭 리스너
        tabWaiting.setOnClickListener {
            if (currentTab != "신고 대기") {
                currentTab = "신고 대기"
                updateTabUI()
                filterAndDisplayData()
            }
        }

        tabCompleted.setOnClickListener {
            if (currentTab != "신고 완료") {
                currentTab = "신고 완료"
                updateTabUI()
                filterAndDisplayData()
            }
        }

        // 편집 버튼 리스너
        btnSelectAll.setOnClickListener {
            toggleSelectAll()
            updateEditButtons()
        }

        btnMoveToWaiting.setOnClickListener {
            moveSelectedToWaiting()
        }

        btnDelete.setOnClickListener {
            if (currentTab == "신고 대기") {
                moveSelectedToWaiting() // 신고 대기 탭에서는 신고 완료로 변경
            } else {
                deleteSelectedItems() // 신고 완료 탭에서는 삭제
            }
        }
    }

    private fun loadData() {
        reportList = ReportDataStore.loadList(this)
    }

    private fun updateUI() {
        updateTabUI()
        filterAndDisplayData()
    }

    private fun updateTabUI() {
        // 상단 탭 버튼 배경 동적 적용
        tabWaiting.background = if (currentTab == "신고 대기") ContextCompat.getDrawable(this, R.drawable.list_tap_button_on) else null
        tabCompleted.background = if (currentTab == "신고 완료") ContextCompat.getDrawable(this, R.drawable.list_tap_button_on) else null
        tabWaiting.setTextColor(Color.WHITE)
        tabCompleted.setTextColor(Color.WHITE)
        updateEditButtons()
    }

    private fun filterAndDisplayData() {
        val filteredList = reportList.filter { it.type == currentTab }
        
        if (filteredList.isEmpty()) {
            listView.visibility = View.GONE
            tvNoData.visibility = View.VISIBLE
            layoutEditButtons.visibility = View.GONE
        } else {
            listView.visibility = View.VISIBLE
            tvNoData.visibility = View.GONE
            layoutEditButtons.visibility = View.VISIBLE
            
            adapter.updateData(filteredList)
            updateEditButtons()
        }
    }

    private fun toggleSelectAll() {
        val allSelected = adapter.getAllItems().all { it.selected }
        
        if (allSelected) {
            // 모두 해제
            adapter.getAllItems().forEach { it.selected = false }
            updateEditButtons()
        } else {
            // 모두 선택
            adapter.getAllItems().forEach { it.selected = true }
            updateEditButtons()
        }
        
        adapter.notifyDataSetChanged()
    }

    private fun updateEditButtons() {
        val items = adapter.getAllItems()
        val allSelected = items.isNotEmpty() && items.all { it.selected }
        
        if (currentTab == "신고 대기") {
            // 신고 대기 탭: 2버튼 구조 (모두선택/신고완료)
            btnSelectAll.visibility = View.VISIBLE
            btnMoveToWaiting.visibility = View.GONE
            btnDelete.visibility = View.VISIBLE
            
            // 모두 선택/전체 해제 버튼
            btnSelectAll.text = if (allSelected) "전체 해제" else "모두 선택"
            btnSelectAll.setBackgroundResource(R.drawable.sellect_all_botton1)
            btnSelectAll.setTextColor(if (allSelected) 0xFF888888.toInt() else 0xFFFFFFFF.toInt())
            
            // 신고 완료 버튼
            btnDelete.text = "신고 완료"
            btnDelete.setBackgroundResource(R.drawable.delete_botton_2)
            btnDelete.setTextColor(0xFFFFFFFF.toInt())
            
        } else {
            // 신고 완료 탭: 3버튼 구조 (모두선택/대기로이동/선택삭제)
            btnSelectAll.visibility = View.VISIBLE
            btnMoveToWaiting.visibility = View.VISIBLE
            btnDelete.visibility = View.VISIBLE
            
            // 모두 선택/전체 해제 버튼
            btnSelectAll.text = if (allSelected) "전체 해제" else "모두 선택"
            btnSelectAll.setBackgroundResource(R.drawable.sellect_all_botton3)
            btnSelectAll.setTextColor(if (allSelected) 0xFF888888.toInt() else 0xFFFFFFFF.toInt())
            
            // 대기로 이동 버튼
            btnMoveToWaiting.text = "대기로 이동"
            btnMoveToWaiting.setBackgroundResource(R.drawable.sellect_all_botton4)
            btnMoveToWaiting.setTextColor(0xFFFFFFFF.toInt())
            
            // 선택 삭제 버튼
            btnDelete.text = "선택 삭제"
            btnDelete.setBackgroundResource(R.drawable.sellect_all_botton5)
            btnDelete.setTextColor(0xFFFFFFFF.toInt())
        }
        
        // 버튼 가중치 업데이트
        updateButtonWeights()
    }

    private fun moveSelectedToWaiting() {
        val selectedItems = adapter.getAllItems().filter { it.selected }
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "선택된 항목이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (currentTab == "신고 대기") {
            // 신고 대기 탭: 신고 완료로 변경
            selectedItems.forEach { it.type = "신고 완료"; it.selected = false }
            Toast.makeText(this, "선택된 항목이 신고 완료되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            // 신고 완료 탭: 대기로 이동
            selectedItems.forEach { it.type = "신고 대기"; it.selected = false }
            Toast.makeText(this, "선택된 항목이 대기로 이동되었습니다.", Toast.LENGTH_SHORT).show()
        }
        
        saveData()
        filterAndDisplayData()
        updateEditButtons()
    }

    private fun deleteSelectedItems() {
        val selectedItems = adapter.getAllItems().filter { it.selected }
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "선택된 항목이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        // 삭제
        val deletedCount = selectedItems.size
        reportList.removeAll(selectedItems.toSet())
        ReportDataStore.increaseDeletedCount(this, deletedCount)
        saveData()
        filterAndDisplayData()
        Toast.makeText(this, "선택된 항목이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        updateEditButtons()
    }

    private fun saveData() {
        ReportDataStore.updateList(this, reportList)
    }

    private fun handleBack() {
        if (fromFloatingBall) {
            // 플로팅 볼 서비스 재시작
            val intent = Intent(this, FloatingBallService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            moveTaskToBack(true) // 앱을 백그라운드로 이동
        }
        finish()
    }

    override fun onBackPressed() {
        handleBack()
    }

    private fun applyTabGroupBackground() {
        val tabGroup = findViewById<LinearLayout>(R.id.tab_group)
        // 하단 버튼과 유사한 gradient+라운드+외곽선 배경 생성
        val radius = 8f // ← 라운드 정도 조절 (직각에 가깝게)
        val strokeWidth = 3 // ← 외곽선 두께 조절
        val strokeColor = 0xFF2B2B3D.toInt() // ← 외곽선 색상(진한 회색)
        val colors = intArrayOf(0xFF2B2B3D.toInt(), 0xFF181828.toInt()) // ← gradient 색상(보라~남색)
        val bgDrawable = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
        bgDrawable.cornerRadius = radius
        bgDrawable.setStroke(strokeWidth, strokeColor)
        tabGroup.background = bgDrawable
        // 좌우/상하 padding 조절(예시: 12dp, 4dp)
        val horizontalPadding = (12 * resources.displayMetrics.density).toInt() // ← 좌우 padding
        val verticalPadding = (4 * resources.displayMetrics.density).toInt() // ← 상하 padding
        tabGroup.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        // width를 wrap_content로 중앙 정렬(레이아웃 XML도 수정 필요)
        val params = tabGroup.layoutParams
        params.width = LinearLayout.LayoutParams.WRAP_CONTENT
        tabGroup.layoutParams = params
    }

    private fun applyTabButtonWidth() {
        // 탭 버튼 width 조절 (120dp → 140dp 등)
        val tabWidth = 140 // ← 탭 버튼 width(dp), 여기서 조절
        val lp1 = findViewById<TextView>(R.id.tab_waiting).layoutParams
        lp1.width = (tabWidth * resources.displayMetrics.density).toInt()
        findViewById<TextView>(R.id.tab_waiting).layoutParams = lp1
        val lp2 = findViewById<TextView>(R.id.tab_completed).layoutParams
        lp2.width = (tabWidth * resources.displayMetrics.density).toInt()
        findViewById<TextView>(R.id.tab_completed).layoutParams = lp2
    }

    private fun applyEditButtonAreaMarginAndTint() {
        val editArea = findViewById<LinearLayout>(R.id.layout_edit_buttons)
        val params = editArea.layoutParams as ViewGroup.MarginLayoutParams
        params.marginStart = 24 // ← 좌우 margin 조절
        params.marginEnd = 24
        editArea.layoutParams = params
        // 하위 버튼 tint 제거
        for (i in 0 until editArea.childCount) {
            val btn = editArea.getChildAt(i)
            if (btn is Button) {
                btn.backgroundTintList = null
            }
        }
    }

    private fun applyEditButtonPadding() {
        val editArea = findViewById<LinearLayout>(R.id.layout_edit_buttons)
        val padding = 8 // ← 좌우 padding(dp), 여기서 조절
        editArea.setPadding(
            (padding * resources.displayMetrics.density).toInt(),
            editArea.paddingTop,
            (padding * resources.displayMetrics.density).toInt(),
            editArea.paddingBottom
        )
        // 하위 버튼 padding/minWidth/minHeight 보정
        for (i in 0 until editArea.childCount) {
            val btn = editArea.getChildAt(i)
            if (btn is Button) {
                btn.setPadding(0, 0, 0, 0)
                btn.minWidth = 0
                btn.minHeight = 0
            }
        }
        
        // 탭별 버튼 가중치 조정
        updateButtonWeights()
    }
    
    private fun updateButtonWeights() {
        if (currentTab == "신고 대기") {
            // 신고 대기 탭: 2버튼 구조 (1:1)
            btnSelectAll.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            btnDelete.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        } else {
            // 신고 완료 탭: 3버튼 구조 (1:1:1)
            btnSelectAll.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            btnMoveToWaiting.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            btnDelete.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    inner class ReportListAdapter(
        private val context: Context,
        private var items: List<ReportData>
    ) : BaseAdapter() {

        fun updateData(newItems: List<ReportData>) {
            items = newItems
            notifyDataSetChanged()
        }

        fun getAllItems(): List<ReportData> = items

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): ReportData = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_report_list, parent, false)
            val item = getItem(position)

            val ivSelect = view.findViewById<ImageView>(R.id.iv_select)
            val ivThumbnail = view.findViewById<ImageView>(R.id.iv_thumbnail)
            val tvViolationType = view.findViewById<TextView>(R.id.tv_violation_type)
            val tvDateTime = view.findViewById<TextView>(R.id.tv_datetime)
            val tvLocation = view.findViewById<TextView>(R.id.tv_location)

            // 선택 상태에 따른 배경/아이콘 적용
            if (item.selected) {
                view.setBackgroundResource(R.drawable.list_active)
                ivSelect.visibility = View.VISIBLE
                ivSelect.setImageResource(R.drawable.checkbox_active)
            } else {
                view.setBackgroundResource(R.drawable.list)
                ivSelect.visibility = View.VISIBLE
                ivSelect.setImageResource(R.drawable.checkbox)
            }

            // 데이터 설정
            tvViolationType.text = item.violationType
            tvDateTime.text = item.datetime
            tvLocation.text = item.location
            ivThumbnail.setImageResource(resources.getIdentifier(item.thumbnail, "drawable", packageName))

            // 아이템 클릭 리스너
            view.setOnClickListener {
                item.selected = !item.selected
                notifyDataSetChanged()
                updateEditButtons()
            }

            return view
        }
    }
}
