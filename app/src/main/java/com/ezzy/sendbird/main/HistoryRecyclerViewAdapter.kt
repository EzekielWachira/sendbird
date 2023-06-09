package com.ezzy.sendbird.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ezzy.sendbird.R
import com.ezzy.sendbird.call.CallService
import com.ezzy.sendbird.utils.EndResultUtils
import com.ezzy.sendbird.utils.ImageUtils
import com.ezzy.sendbird.utils.TimeUtils
import com.ezzy.sendbird.utils.UserInfoUtils
import com.sendbird.calls.DirectCallLog
import com.sendbird.calls.DirectCallUser
import com.sendbird.calls.DirectCallUserRole

internal class HistoryRecyclerViewAdapter(private val mContext: Context) :
    RecyclerView.Adapter<HistoryRecyclerViewAdapter.HistoryViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(mContext)
    private val mDirectCallLogs: MutableList<DirectCallLog> = ArrayList()

    fun setCallLogs(callLogs: List<DirectCallLog>?) {
        mDirectCallLogs.clear()
        mDirectCallLogs.addAll(callLogs!!)
    }

    fun addCallLogs(callLogs: List<DirectCallLog>?) {
        mDirectCallLogs.addAll(callLogs!!)
    }

    fun addLatestCallLog(callLog: DirectCallLog) {
        mDirectCallLogs.add(0, callLog)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val itemView: View =
            mInflater.inflate(R.layout.fragment_history_recycler_view_item, parent, false)
        return HistoryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val callLog = mDirectCallLogs[position]
        val myRole = callLog.myRole
        val user: DirectCallUser?
        if (myRole == DirectCallUserRole.CALLER) {
            user = callLog.callee
            if (callLog.isVideoCall) {
                holder.imageViewIncomingOrOutgoing.setBackgroundResource(R.drawable.icon_call_video_outgoing_filled)
            } else {
                holder.imageViewIncomingOrOutgoing.setBackgroundResource(R.drawable.icon_call_voice_outgoing_filled)
            }
        } else {
            user = callLog.caller
            if (callLog.isVideoCall) {
                holder.imageViewIncomingOrOutgoing.setBackgroundResource(R.drawable.icon_call_video_incoming_filled)
            } else {
                holder.imageViewIncomingOrOutgoing.setBackgroundResource(R.drawable.icon_call_voice_incoming_filled)
            }
        }
        if (user != null) {
            ImageUtils.displayCircularImageFromUrl(
                mContext,
                user.profileUrl,
                holder.imageViewProfile
            )
        }
        UserInfoUtils.setNickname(mContext, user, holder.textViewNickname)
        UserInfoUtils.setUserId(mContext, user, holder.textViewUserId)
        val endResult: String = EndResultUtils.getEndResultString(mContext, callLog.endResult)
        val endResultAndDuration =
            endResult + mContext.getString(R.string.calls_and_character) + TimeUtils.getTimeStringForHistory(
                callLog.duration
            )
        holder.textViewEndResultAndDuration.text = endResultAndDuration
        holder.textViewStartAt.setText(TimeUtils.getDateString(callLog.startedAt))
        holder.imageViewVideoCall.setOnClickListener { view: View? ->
            CallService.dial(
                mContext,
                user!!.userId,
                true
            )
        }
        holder.imageViewVoiceCall.setOnClickListener { view: View? ->
            CallService.dial(
                mContext,
                user!!.userId,
                false
            )
        }
    }

    override fun getItemCount(): Int {
        return mDirectCallLogs.size
    }

    internal class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewIncomingOrOutgoing: ImageView
        val imageViewProfile: ImageView
        val textViewNickname: TextView
        val textViewUserId: TextView
        val textViewEndResultAndDuration: TextView
        val textViewStartAt: TextView
        val imageViewVideoCall: ImageView
        val imageViewVoiceCall: ImageView

        init {
            imageViewIncomingOrOutgoing =
                itemView.findViewById(R.id.image_view_incoming_or_outgoing)
            imageViewProfile = itemView.findViewById(R.id.image_view_profile)
            textViewNickname = itemView.findViewById(R.id.text_view_nickname)
            textViewUserId = itemView.findViewById(R.id.text_view_user_id)
            textViewEndResultAndDuration =
                itemView.findViewById(R.id.text_view_end_result_and_duration)
            textViewStartAt = itemView.findViewById(R.id.text_view_start_at)
            imageViewVideoCall = itemView.findViewById(R.id.image_view_video_call)
            imageViewVoiceCall = itemView.findViewById(R.id.image_view_voice_call)
        }
    }
}
