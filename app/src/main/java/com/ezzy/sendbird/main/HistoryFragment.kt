package com.ezzy.sendbird.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.ezzy.sendbird.databinding.FragmentHistoryBinding
import com.ezzy.sendbird.utils.BindingFragment
import com.ezzy.sendbird.utils.ToastUtils
import com.sendbird.calls.DirectCallLog
import com.sendbird.calls.DirectCallLogListQuery
import com.sendbird.calls.SendBirdCall.createDirectCallLogListQuery
import com.sendbird.calls.SendBirdException
import com.sendbird.calls.handler.DirectCallLogListQueryResultHandler


class HistoryFragment : BindingFragment<FragmentHistoryBinding>() {

    private lateinit var mDirectCallLogListQuery: DirectCallLogListQuery

    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentHistoryBinding::inflate

    private lateinit var mRecyclerViewLinearLayoutManager: LinearLayoutManager
    private val mRecyclerViewHistoryAdapter: HistoryRecyclerViewAdapter by lazy { HistoryRecyclerViewAdapter(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mRecyclerViewLinearLayoutManager = LinearLayoutManager(context)
        setUpUi()
    }

    fun setUpUi() {
       with(binding) {
           recyclerViewHistory.apply {
               layoutManager = mRecyclerViewLinearLayoutManager
               adapter = mRecyclerViewHistoryAdapter
           }.addOnScrollListener(object : RecyclerView.OnScrollListener() {
               override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                   super.onScrolled(recyclerView, dx, dy)
                   val lastItemPosition: Int =
                       mRecyclerViewLinearLayoutManager.findLastVisibleItemPosition()
                   if (lastItemPosition >= 0 && lastItemPosition == mRecyclerViewHistoryAdapter.itemCount - 1) {
                       if (::mDirectCallLogListQuery.isInitialized && mDirectCallLogListQuery.hasNext() && !mDirectCallLogListQuery.isLoading) {
                           progressBar.visibility = View.VISIBLE
                           mDirectCallLogListQuery.next(DirectCallLogListQueryResultHandler { list: List<DirectCallLog>?, e: SendBirdException? ->
                               progressBar.visibility = View.GONE
                               if (e != null) {
                                   ToastUtils.showToast(context, e.message)
                                   return@DirectCallLogListQueryResultHandler
                               }
                               if (list!!.isNotEmpty()) {
                                   val positionStart: Int =
                                       mRecyclerViewHistoryAdapter.itemCount
                                   mRecyclerViewHistoryAdapter.addCallLogs(list)
                                   mRecyclerViewHistoryAdapter.notifyItemRangeChanged(
                                       positionStart,
                                       list.size
                                   )
                               }
                           })
                       }
                   }
               }
           })
       }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.recyclerViewHistory.visibility = View.GONE
        binding.linearLayoutEmpty.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        mDirectCallLogListQuery = createDirectCallLogListQuery(DirectCallLogListQuery.Params().setLimit(20))
        mDirectCallLogListQuery.next(DirectCallLogListQueryResultHandler { list: List<DirectCallLog>?, e: SendBirdException? ->
            binding.progressBar.visibility = View.GONE
            if (e != null) {
                ToastUtils.showToast(context, e.message)
                return@DirectCallLogListQueryResultHandler
            }
            if (list!!.isNotEmpty()) {
                binding.recyclerViewHistory.visibility = View.VISIBLE
                binding.linearLayoutEmpty.visibility = View.GONE
                mRecyclerViewHistoryAdapter.setCallLogs(list)
                mRecyclerViewHistoryAdapter.notifyDataSetChanged()
            } else {
                binding.recyclerViewHistory.visibility = View.GONE
                binding.linearLayoutEmpty.visibility = View.VISIBLE
            }
        })

    }

    fun addLatestCallLog(callLog: DirectCallLog?) {
        mRecyclerViewHistoryAdapter.addLatestCallLog(callLog!!)
        mRecyclerViewHistoryAdapter.notifyDataSetChanged()
    }
}