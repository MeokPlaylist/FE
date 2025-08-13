package com.example.meokpli.common

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.meokpli.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UserNotFoundDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireContext()
        val v = LayoutInflater.from(ctx).inflate(R.layout.dialog_user_not_found, null)

        // 필요시 동적 메시지 세팅 (arguments로 전달받을 수 있게)
        val title = arguments?.getString(ARG_TITLE) ?: "존재하지 않는 아이디예요"
        val msg = arguments?.getString(ARG_MESSAGE) ?: "아이디(이메일)를 다시 확인해 주세요.\n회원이 아니라면 회원가입을 진행해 주세요."

        v.findViewById<TextView>(R.id.tvTitle).text = title
        v.findViewById<TextView>(R.id.tvMessage).text = msg

        v.findViewById<MaterialButton>(R.id.btnConfirm).setOnClickListener {
            dismissAllowingStateLoss()
            (activity as? OnConfirmListener)?.onUserNotFoundConfirm()  // 필요하면 콜백
        }

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setView(v)
            .create()

        // 살짝 어둡게
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setDimAmount(0.5f)
        return dialog
    }

    interface OnConfirmListener {
        fun onUserNotFoundConfirm() {}
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_MESSAGE = "arg_message"

        fun new(title: String? = null, message: String? = null) = UserNotFoundDialogFragment().apply {
            arguments = Bundle().apply {
                if (title != null) putString(ARG_TITLE, title)
                if (message != null) putString(ARG_MESSAGE, message)
            }
        }
    }
}
