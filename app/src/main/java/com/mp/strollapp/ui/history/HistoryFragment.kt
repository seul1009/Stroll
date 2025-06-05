package com.mp.strollapp.ui.history

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mp.strollapp.R
import com.mp.strollapp.data.walk.WalkRecordDatabase
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private lateinit var db: WalkRecordDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WalkHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = WalkHistoryAdapter()
        recyclerView.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = WalkRecordDatabase.getInstance(requireContext())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = WalkHistoryAdapter()
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            val records = db.walkRecordDao().getAll()
            Log.d("HistoryFragment", "불러온 기록 개수: ${records.size}")
            adapter.submitList(records)

            adapter.setOnItemClickListener { record ->
                val intent = Intent(requireContext(), PathMapActivity::class.java)
                intent.putExtra("path", record.path)
                intent.putExtra("distance", record.distance)
                intent.putExtra("duration", record.duration)
                intent.putExtra("timestamp", record.timestamp)
                startActivity(intent)
            }
        }

    }
}
