package com.mp.strollapp.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mp.strollapp.R
import com.mp.strollapp.data.walk.WalkRecordEntity

class WalkHistoryAdapter : RecyclerView.Adapter<WalkHistoryViewHolder>() {

    private var walkList = listOf<WalkRecordEntity>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkHistoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_walk_record, parent, false)
        return WalkHistoryViewHolder(view)
    }

    override fun getItemCount(): Int = walkList.size

    override fun onBindViewHolder(holder: WalkHistoryViewHolder, position: Int) {
        holder.bind(walkList[position])
    }

    fun submitList(list: List<WalkRecordEntity>) {
        walkList = list
        notifyDataSetChanged()
    }
}
