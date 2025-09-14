package com.meokpli.app.main

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.meokpli.app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EditContentDialog : DialogFragment() {

    companion object {
        const val REQUEST_KEY = "edit_content_result"
        const val KEY_FEED_ID = "feed_id"
        const val KEY_NEW_CONTENT = "new_content"

        private const val ARG_FEED_ID = "arg_feed_id"
        private const val ARG_INITIAL = "arg_initial"

        fun newInstance(feedId: Long, initial: String) = EditContentDialog().apply {
            arguments = bundleOf(ARG_FEED_ID to feedId, ARG_INITIAL to initial)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_content, null, false)

        val feedId = requireArguments().getLong(ARG_FEED_ID)
        val initial = requireArguments().getString(ARG_INITIAL).orEmpty()

        val et = v.findViewById<EditText>(R.id.etContent)
        val btnConfirm = v.findViewById<TextView>(R.id.btnConfirm)
        val counter = v.findViewById<TextView>(R.id.tvCounter)

        et.setText(initial)
        et.setSelection(et.text?.length ?: 0)
        counter.text = "${et.length()}/500"

        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                counter.text = "${s?.length ?: 0}/500"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnConfirm.setOnClickListener {
            val text = et.text?.toString()?.trim().orEmpty()
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(KEY_FEED_ID to feedId, KEY_NEW_CONTENT to text)
            )
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext(), R.style.App_MdcAlertDialog)
            .setView(v)
            .create()
    }
}