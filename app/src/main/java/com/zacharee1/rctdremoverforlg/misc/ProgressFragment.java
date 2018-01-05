package com.zacharee1.rctdremoverforlg.misc;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ButtonBarLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.zacharee1.rctdremoverforlg.InstallerActivity;
import com.zacharee1.rctdremoverforlg.R;

import org.zeroturnaround.exec.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;

import fr.castorflex.android.circularprogressbar.CircularProgressBar;


public class ProgressFragment extends DialogFragment {
    private Runnable mPositiveRunnable;
    private Runnable mFailRunnable;

    private ProcessExecutor mExecutor = new ProcessExecutor();

    private int mTitleRes;
    private int mPositiveRes;
    private int mNegativeRes;

    private ArrayList<String> mCommands = new ArrayList<>();

    private OnOperationFinishListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTitleRes = getArguments().getInt("title");
        mPositiveRes = getArguments().getInt("positive");
        mNegativeRes = getArguments().getInt("negative");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setView(R.layout.terminal_output_layout)
                .setTitle(mTitleRes)
                .setPositiveButton(R.string.working, null)
                .setNegativeButton(mNegativeRes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mExecutor.stop();
                    }
                })
                .setCancelable(false)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Button positive = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        positive.setEnabled(false);

        ButtonBarLayout buttonBarLayout = (ButtonBarLayout) positive.getParent();
        CircularProgressBar loader = (CircularProgressBar) LayoutInflater.from(getActivity()).inflate(R.layout.loader, buttonBarLayout, false);
        loader.setId(R.id.loader);

        buttonBarLayout.addView(loader, 0);

        executeCommands();
    }

    public void show(FragmentManager manager, String tag, String... commands) {
        show(manager, tag);

        mCommands = new ArrayList<>(Arrays.asList(commands));
    }

    public void setPositiveRunnable(Runnable runnable) {
        mPositiveRunnable = runnable;
    }

    public void setFailRunnable(Runnable runnable) {
        mFailRunnable = runnable;
    }

    public void setFinishCallback(OnOperationFinishListener listener) {
        mListener = listener;
    }

    public void executeCommands() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String result = SuUtils.suAndPrintToView(
                        mExecutor,
                        getActivity(),
                        (TerminalView) getDialog().findViewById(R.id.terminal_view),
                        mCommands
                );

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final Button positive = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
                        positive.setEnabled(true);

                        ((ButtonBarLayout) positive.getParent()).removeView(((ButtonBarLayout) positive.getParent()).findViewById(R.id.loader));

                        if (result.endsWith("Done!")) {
                            positive.setText(mPositiveRes);
                            positive.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (mPositiveRunnable != null) new Handler(Looper.getMainLooper()).post(mPositiveRunnable);
                                    dismiss();
                                }
                            });
                        } else {
                            positive.setText(R.string.retry);
                            positive.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    dismiss();
                                    if (mFailRunnable != null) new Handler(Looper.getMainLooper()).post(mFailRunnable);
                                }
                            });
                        }
                    }
                });

                if (mListener != null) mListener.onOperationFinish();
            }
        }).start();
    }

    public interface OnOperationFinishListener {
        void onOperationFinish();
    }
}
