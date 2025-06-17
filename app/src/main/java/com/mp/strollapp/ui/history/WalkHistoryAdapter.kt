package com.mp.strollapp.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mp.strollapp.R
import com.mp.strollapp.data.walk.WalkRecordEntity

// RecyclerView 어댑터: 산책 기록 리스트를 표시
class WalkHistoryAdapter : RecyclerView.Adapter<WalkHistoryViewHolder>() {

    // 현재 표시 중인 산책 기록 리스트
    private var walkList = listOf<WalkRecordEntity>()
    // 아이템 클릭 리스너
    private var onItemClickListener: ((WalkRecordEntity) -> Unit)? = null

    // ViewHolder 생성 (레이아웃 연결)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkHistoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_walk_record, parent, false)
        return WalkHistoryViewHolder(view)
    }

    // 리스트 크기 반환
    override fun getItemCount(): Int = walkList.size

    // ViewHolder에 데이터 바인딩 및 클릭 리스너 설정
    override fun onBindViewHolder(holder: WalkHistoryViewHolder, position: Int) {
        val record = walkList[position]
        holder.bind(record) // ViewHolder 내부의 bind()로 데이터 세팅

        // 항목 클릭 시 외부 리스너 호출
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(record)
        }
    }

    // 외부에서 데이터 리스트를 설정할 때 사용
    fun submitList(list: List<WalkRecordEntity>) {
        walkList = list
        notifyDataSetChanged()
    }

    // 외부에서 클릭 리스너를 설정할 때 사용
    fun setOnItemClickListener(listener: (WalkRecordEntity) -> Unit) {
        onItemClickListener = listener
    }
}
