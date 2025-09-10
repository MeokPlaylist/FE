import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meokpli.R
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import android.view.GestureDetector
import android.view.ViewConfiguration   // ★ 추가

class SelectedPhotosAdapter(
    private val itemTouchHelper: ItemTouchHelper,
    private val onItemClick: ((position: Int, uri: Uri) -> Unit)? = null,
    private val onRemoveClick: ((position: Int, uri: Uri) -> Unit)? = null
) : ListAdapter<Uri, SelectedPhotosAdapter.PhotoViewHolder>(DIFF_CALLBACK) {

    init { setHasStableIds(true) }

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.img)
        private val btnRemove: ImageView = itemView.findViewById(R.id.btnRemove)

        fun bind(uri: Uri, onClick: ((Int, Uri) -> Unit)?) {
            imageView.setImageURI(uri)

            // 중복 방지
            imageView.setOnClickListener(null)
            imageView.setOnLongClickListener(null)
            imageView.setOnTouchListener(null)

            val detector = GestureDetectorCompat(
                imageView.context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDown(e: MotionEvent): Boolean {
                        // 탭/롱프레스를 우선 보장
                        imageView.parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }

                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        val pos = bindingAdapterPosition
                        if (pos != RecyclerView.NO_POSITION) onClick?.invoke(pos, uri)
                        return true
                    }

                    override fun onLongPress(e: MotionEvent) {
                        val pos = bindingAdapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            imageView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            itemTouchHelper.startDrag(this@PhotoViewHolder)
                            // 드래그는 ItemTouchHelper가 처리하므로 부모에 맡겨도 됨
                            imageView.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
            )

            // ★ 스크롤/탭/롱프레스 공존을 위한 touchSlop 로직
            val touchSlop = ViewConfiguration.get(imageView.context).scaledTouchSlop
            var downX = 0f
            var downY = 0f
            var moved = false

            imageView.setOnTouchListener { v, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = ev.x
                        downY = ev.y
                        moved = false
                        // 우선 자식(이미지)이 제스처 판단하도록 부모 인터셉트 금지
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!moved) {
                            val dx = ev.x - downX
                            val dy = ev.y - downY
                            if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) {
                                moved = true
                                // 사용자가 실제로 드래그(스크롤) 중 → 부모(RV)에 맡긴다
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        // 접근성용 클릭 이벤트
                        v.performClick()
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }

                // 제스처 분석은 계속 수행
                detector.onTouchEvent(ev)
                // false를 반환해 RV가 스크롤을 계속 받을 수 있게 한다
                false
            }

            // 삭제 버튼 (스크롤 인터셉트 보호)
            btnRemove.setOnTouchListener { v, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> v.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
            btnRemove.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onRemoveClick?.invoke(pos, uri)
            }
            btnRemove.isClickable = true
            btnRemove.isFocusable = true
            btnRemove.bringToFront()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    override fun getItemId(position: Int): Long =
        getItem(position).toString().hashCode().toLong()

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Uri>() {
            override fun areItemsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
            override fun areContentsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
        }
    }
}