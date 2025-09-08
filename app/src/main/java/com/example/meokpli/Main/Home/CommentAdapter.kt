package com.example.meokpli.comments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.meokpli.Main.Home.Comment
import com.example.meokpli.R

class CommentAdapter(
    private val onReplyClick: (Comment) -> Unit,
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.VH>() {

    private val diff = object : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(o: Comment, n: Comment) = o.id == n.id
        override fun areContentsTheSame(o: Comment, n: Comment) = o == n
    }
    private val differ = AsyncListDiffer(this, diff)
    val currentList get() = differ.currentList
    fun submitList(list: List<Comment>) = differ.submitList(list)

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: ImageView = v.findViewById(R.id.ivAvatar)
        val name: TextView = v.findViewById(R.id.tvName)
        val date: TextView = v.findViewById(R.id.tvDate)
        val body: TextView = v.findViewById(R.id.tvBody)
        val btnMore: ImageView = v.findViewById(R.id.btnMore)
        val tvReplyHint: TextView = v.findViewById(R.id.tvReplyHint)
    }

    override fun onCreateViewHolder(p: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_comment, p, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val c = currentList[pos]
        h.name.text = c.author
        h.date.text = c.createdAt
        h.body.text = c.content
        if (c.avatarUrl != null) h.avatar.load(c.avatarUrl)
        else h.avatar.setImageResource(R.drawable.ic_profile_red)

        h.tvReplyHint.setOnClickListener { onReplyClick(c) }
        h.btnMore.setOnClickListener { anchor -> showActionsPopup(anchor, c) }
    }

    override fun getItemCount() = currentList.size

    /** 점 3개(anchor) 기준으로 정확히 아래에 카드 팝업 표시 */
    private fun showActionsPopup(anchor: View, comment: Comment) {
        val ctx = anchor.context
        val view = LayoutInflater.from(ctx).inflate(R.layout.popup_comment_actions, null, false)

        val popup = PopupWindow(
            view,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isFocusable = true
            isOutsideTouchable = true
            elevation = 20f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        view.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
            popup.dismiss(); onEditClick(comment)
        }
        view.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            popup.dismiss(); onDeleteClick(comment)
        }

        // anchor(btnMore)의 화면 좌표
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)

        // 팝업 콘텐츠 크기 측정
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupW = view.measuredWidth

        // 아이콘 바로 아래, 우측 정렬처럼 보이게 정밀 배치
        val x = location[0] - (popupW - anchor.width)
        val y = location[1] + anchor.height

        popup.showAtLocation(anchor, GravityTOP_START, x, y)
    }

    private val GravityTOP_START = Gravity.TOP or Gravity.START
}
