package com.zacharee1.rctdremoverforlg.misc

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.ButtonBarLayout
import android.view.LayoutInflater
import android.view.View
import com.zacharee1.rctdremoverforlg.R
import fr.castorflex.android.circularprogressbar.CircularProgressBar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import org.zeroturnaround.exec.listener.ProcessListener
import java.util.*

class ProgressFragment : DialogFragment() {
    private var mPositiveRunnable: Runnable? = null
    private var mFailRunnable: Runnable? = null

    private val mExecutor = ProcessExecutor()

    private var mTitleRes: Int? = 0
    private var mPositiveRes: Int? = 0
    private var mNegativeRes: Int? = 0

    private var mCommands = ArrayList<String>()

    private var mListener: OnOperationFinishListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mTitleRes = arguments?.getInt("title")
        mPositiveRes = arguments?.getInt("positive")
        mNegativeRes = arguments?.getInt("negative")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        return AlertDialog.Builder(context as Context)
                .setView(R.layout.terminal_output_layout)
                .setTitle(mTitleRes as Int)
                .setPositiveButton(R.string.working, null)
                .setNegativeButton(mNegativeRes as Int) { _, _ -> mExecutor.stop() }
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

        mCommands = ArrayList(Arrays.asList(*commands))
    }

    fun setPositiveRunnable(runnable: Runnable?) {
        mPositiveRunnable = runnable
    }

    fun setFailRunnable(runnable: Runnable?) {
        mFailRunnable = runnable
    }

    fun setFinishCallback(listener: OnOperationFinishListener?) {
        mListener = listener
    }

    @SuppressLint("CheckResult")
    fun executeCommands() {
        Observable.fromCallable({
                SuUtils.suAndPrintToView(
                mExecutor,
                activity,
                dialog.findViewById<View>(R.id.terminal_view) as TerminalView,
                mCommands
            )})
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { result ->
                val positive = (dialog as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
                positive.isEnabled = true

                (positive.parent as ButtonBarLayout).removeView((positive.parent as ButtonBarLayout).findViewById(R.id.loader))

                if (result?.exitValue == 0) {
                    positive.setText(mPositiveRes as Int)
                    positive.setOnClickListener {
                        if (mPositiveRunnable != null) Handler(Looper.getMainLooper()).post(mPositiveRunnable)
                        dismiss()
                    }
                    dialog.setTitle(R.string.done)
                } else {
                    positive.setText(R.string.retry)
                    positive.setOnClickListener {
                        dismiss()
                        if (mFailRunnable != null) Handler(Looper.getMainLooper()).post(mFailRunnable)
                    }
                    dialog.setTitle(R.string.failed)
                }

                mListener?.onOperationFinish()
            }
    }

    interface OnOperationFinishListener {
        fun onOperationFinish()
    }
}