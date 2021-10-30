package com.zacharee1.rctdremoverforlg.misc

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ButtonBarLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.zacharee1.rctdremoverforlg.R
import fr.castorflex.android.circularprogressbar.CircularProgressBar
import java.util.*
import java.util.concurrent.Executors

class ProgressFragment : DialogFragment() {
    private var positiveRunnable: Runnable? = null
    private var failRunnable: Runnable? = null

    private val executor = Executors.newSingleThreadExecutor()

    private var titleRes: Int? = 0
    private var positiveRes: Int? = 0
    private var negativeRes: Int? = 0

    private var commands = ArrayList<String>()
    private var listener: OnOperationFinishListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleRes = arguments?.getInt("title")
        positiveRes = arguments?.getInt("positive")
        negativeRes = arguments?.getInt("negative")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        return MaterialAlertDialogBuilder(context as Context)
                .setView(R.layout.terminal_output_layout)
                .setTitle(titleRes as Int)
                .setPositiveButton(R.string.working, null)
                .setNegativeButton(negativeRes as Int) { _, _ -> executor.shutdownNow() }
                .setCancelable(false)
                .create()
    }

    override fun onStart() {
        super.onStart()
        val positive = (dialog as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
        positive.isEnabled = false

        val buttonBarLayout = positive.parent as ButtonBarLayout
        val loader = LayoutInflater.from(activity).inflate(R.layout.loader, buttonBarLayout, false) as CircularProgressBar
        loader.id = R.id.loader

        buttonBarLayout.addView(loader, 0)

        executeCommands()
    }

    fun show(manager: FragmentManager, tag: String, vararg commands: String) {
        show(manager, tag)

        this.commands = ArrayList(listOf(*commands))
    }

    fun setPositiveRunnable(runnable: Runnable?) {
        positiveRunnable = runnable
    }

    fun setFailRunnable(runnable: Runnable?) {
        failRunnable = runnable
    }

    fun setFinishCallback(listener: OnOperationFinishListener?) {
        this.listener = listener
    }

    @SuppressLint("CheckResult")
    fun executeCommands() {
        val outputView = dialog!!.findViewById<TerminalView>(R.id.terminal_view)
        val scroller = dialog!!.findViewById<ScrollView>(R.id.scroller)

        val callbackList = object : CallbackList<String>() {
            override fun onAddElement(e: String) {
                outputView.addText(("$e\n"))

                scroller.post { scroller.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }

        Shell.su(*commands.toTypedArray())
            .to(callbackList)
            .submit(executor) { result ->
                val positive = (dialog as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
                positive.isEnabled = true

                (positive.parent as ButtonBarLayout).removeView((positive.parent as ButtonBarLayout).findViewById(R.id.loader))

                if (result.isSuccess) {
                    positive.setText(positiveRes as Int)
                    positive.setOnClickListener {
                        if (positiveRunnable != null) Handler(Looper.getMainLooper()).post(positiveRunnable)
                        dismiss()
                    }
                    dialog!!.setTitle(R.string.done)
                } else {
                    positive.setText(R.string.retry)
                    positive.setOnClickListener {
                        dismiss()
                        if (failRunnable != null) Handler(Looper.getMainLooper()).post(failRunnable)
                    }
                    dialog!!.setTitle(R.string.failed)
                }

                listener?.onOperationFinish()
            }
    }

    interface OnOperationFinishListener {
        fun onOperationFinish()
    }
}