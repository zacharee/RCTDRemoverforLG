package com.zacharee1.rctdremoverforlg;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.zacharee1.rctdremoverforlg.misc.ProgressFragment;
import com.zacharee1.rctdremoverforlg.misc.SuUtils;
import com.zacharee1.rctdremoverforlg.misc.SwitchViewWithText;
import com.zacharee1.rctdremoverforlg.misc.TerminalView;
import com.zacharee1.rctdremoverforlg.misc.Utils;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.listener.ProcessDestroyer;
import org.zeroturnaround.exec.listener.ProcessListener;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class InstallerActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    public static final float AIK_VERSION = 3.0F;

    public static final int NO_AIK = -1;
    public static final int OLD_AIK = 1;
    public static final int AIK = 0;

    private SwitchViewWithText rctd;
    private SwitchViewWithText ccmd;
    private SwitchViewWithText triton;

    private SharedPreferences mPrefs;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_checkroot);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                if (SuUtils.testSudo()) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            setup();
//                        }
//                    });
//                } else {
//                    finish();
//                    Toast.makeText(InstallerActivity.this, getResources().getString(R.string.need_root), Toast.LENGTH_LONG).show();
//                }
//            }
//        }).start();

        Observable.fromCallable(() -> SuUtils.testSudo())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bool -> {
                    if (bool) {
                        setup();
                    } else {
                        finish();
                        Toast.makeText(InstallerActivity.this, getResources().getString(R.string.need_root), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            String perm = permissions[i];
            int result = grantResults[i];

            if (perm.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && result == PackageManager.PERMISSION_GRANTED) {
                checkAikStatus();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onRefresh() {
        checkStatus();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    public void checkStatus() {
        final TextView report = findViewById(R.id.status_report);
        final TextView triton = findViewById(R.id.triton_status_report);
        final TextView ccmd = findViewById(R.id.ccmd_status_report);

        report.setTextColor(Color.YELLOW);
        triton.setTextColor(Color.YELLOW);
        ccmd.setTextColor(Color.YELLOW);

        report.setText(R.string.checking);
        triton.setText(R.string.checking);
        ccmd.setText(R.string.checking);

        if (!swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(true);

        Observable.fromCallable(() -> checkStatusAsync())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bools -> {
                    report.setText(bools[0] ? R.string.not_found : R.string.running);
                    report.setTextColor(bools[0] ? Color.GREEN : Color.RED);

                    triton.setText(bools[1] ? R.string.not_found : R.string.running);
                    triton.setTextColor(bools[1] ? Color.GREEN : Color.RED);

                    ccmd.setText(bools[2] ? R.string.not_found : R.string.running);
                    ccmd.setTextColor(bools[2] ? Color.GREEN : Color.RED);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                    }, 500);
                });
    }

    public Boolean[] checkStatusAsync() {
        final String rctResult = SuUtils.sudoForResult("ps | grep rctd");
        final boolean rctNotThere = rctResult.isEmpty() ||
                (!rctResult.contains("/sbin/rctd"));

        final String tritonResult = SuUtils.sudoForResult("ps | grep triton");
        final boolean tritonNotThere = tritonResult.isEmpty() ||
                (!tritonResult.contains("/system/bin/triton"));

        final String ccmdResult = SuUtils.sudoForResult("ps | grep ccmd");
        final boolean ccmdNotThere = ccmdResult.isEmpty() ||
                !ccmdResult.contains("/system/bin/ccmd");

        return new Boolean[] {rctNotThere, tritonNotThere, ccmdNotThere};
    }

    private void setup() {
        if (checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 10);
        } else {
            checkAikStatus();
        }
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout = findViewById(R.id.swipe_parent);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.color_1,
                R.color.color_2,
                R.color.color_3,
                R.color.color_4,
                R.color.color_5,
                R.color.color_6,
                R.color.color_7,
                R.color.color_8,
                R.color.color_9,
                R.color.color_a,
                R.color.color_b,
                R.color.color_c
        );
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    private void setSwitchChecksAndListeners() {
        boolean patchRctd = mPrefs.getBoolean("rctd", true);
        boolean patchCcmd = mPrefs.getBoolean("ccmd", false);
        boolean patchTriton = mPrefs.getBoolean("triton", true);

        rctd.setChecked(patchRctd);
        ccmd.setChecked(patchCcmd);
        triton.setChecked(patchTriton);

        rctd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mPrefs.edit().putBoolean("rctd", b).apply();
            }
        });

        ccmd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mPrefs.edit().putBoolean("ccmd", b).apply();
            }
        });

        triton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mPrefs.edit().putBoolean("triton", b).apply();
            }
        });
    }

    private void setToMainScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.activity_installer);

                rctd = findViewById(R.id.enable_patch_rctd);
                ccmd = findViewById(R.id.enable_patch_ccmd);
                triton = findViewById(R.id.enable_patch_triton);

                setSwitchChecksAndListeners();
                setupSwipeRefresh();

                checkStatus();

                TextView appBy = findViewById(R.id.app_made_by);
                TextView aikBy = findViewById(R.id.aik_by);

                appBy.setMovementMethod(LinkMovementMethod.getInstance());
                aikBy.setMovementMethod(LinkMovementMethod.getInstance());
            }
        });
    }

    @SuppressLint("CheckResult")
    private void checkAikStatus() {
        Observable.fromCallable(() -> checkAikStatusAsync())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                    switch (status) {
                        case AIK:
                            setToMainScreen();
                            break;
                        case NO_AIK:
                            ((TextView) findViewById(R.id.textView)).setText(getResources().getString(R.string.installing_aik));
                            installAik();
                            break;
                        case OLD_AIK:
                            new AlertDialog.Builder(InstallerActivity.this)
                                    .setTitle(R.string.old_aik)
                                    .setMessage(R.string.old_aik_msg)
                                    .setPositiveButton(R.string.yes, (dialogInterface, i) -> installAik())
                                    .setNegativeButton(R.string.no, (dialogInterface, i) -> setToMainScreen())
                                    .setCancelable(false)
                                    .show();
                            break;
                    }
                });
    }

    private int checkAikStatusAsync() {
        try {
            Process aikProc = Runtime.getRuntime().exec("aik");

            DataOutputStream outputStream = new DataOutputStream(aikProc.getOutputStream());

            outputStream.writeBytes("exit\n");
            outputStream.flush();

            aikProc.waitFor();
        } catch (Exception e) {
            return NO_AIK;
        }

        return Utils.getAikVersion() >= AIK_VERSION ? AIK : OLD_AIK;
    }

    @SuppressLint("CheckResult")
    public void installAik() {
        Observable.fromCallable(() -> installAikAsync())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bool -> {
                    if (bool) {
                        new AlertDialog.Builder(InstallerActivity.this)
                                .setTitle(getResources().getString(R.string.reboot))
                                .setMessage(getResources().getString(R.string.install_aik))
                                .setPositiveButton(getResources().getString(R.string.yes), (dialogInterface, i) ->  {
                                    try {
                                        SuUtils.sudo("reboot recovery");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                })
                                .setNegativeButton(getResources().getString(R.string.no), (dialogInterface, i) -> finish())
                                .setCancelable(false)
                                .show();
                    } else {
                        //TODO show install failed dialog here....
                    }
                });
    }

    private boolean installAikAsync() {
        AssetManager assetManager = getAssets();
        final String aik = "AIK.zip";

        InputStream in;
        FileOutputStream out;

        try {
            in = assetManager.open(aik);
            String dest = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AndroidImageKitchen/";
            File outDir = new File(dest);
            createDir(outDir);
            File outFile = new File(dest, aik);
            out = new FileOutputStream(outFile);

            copyFile(in, out);

            in.close();

            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        try {
            SuUtils.sudo("echo '--update_package=/sdcard/0/AndroidImageKitchen/AIK.zip' >> /cache/recovery/command");
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    public void handlePatch(final View v) {
        final boolean patchRctd = rctd.isChecked();
        final boolean patchCcmd = ccmd.isChecked();
        final boolean patchTriton = triton.isChecked();

        final String executeMod = "executemod.sh";

        AssetManager assetManager = getAssets();

        InputStream in;
        FileOutputStream out;

        try {
            in = assetManager.open(executeMod);
            String dest = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AndroidImageKitchen/";
            File outDir = new File(dest);
            createDir(outDir);
            File outFile = new File(dest, executeMod);
            out = new FileOutputStream(outFile);

            copyFile(in, out);

            in.close();

            out.flush();
            out.close();
        } catch (final Exception e) {
            e.printStackTrace();

            handleException(e);
        }

        showExecuteDialog(
                R.string.flash,
                R.string.cancel,
                R.string.patching_image,
                () -> handleFlash(v),
                () -> handlePatch(v),
                "cp /sdcard/AndroidImageKitchen/" + executeMod + " /data/local/AIK-mobile/. || exit 1",
                "cd /data/local/AIK-mobile/ || exit 1",
                "chmod 0755 " + executeMod + " || exit 1",
                "./" + executeMod + " " + patchRctd + " " + patchCcmd + " " + patchTriton + " " + Build.DEVICE
        );
    }

    public void handleFlash(final View v) {
        showExecuteDialog(
                R.string.reboot,
                R.string.cancel,
                R.string.flashing_image,
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SuUtils.sudo("reboot");
                        } catch (Exception e) {

                        }
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        handleFlash(v);
                    }
                },
                "dd if=/storage/emulated/0/AndroidImageKitchen/boot.img of=/dev/block/bootdevice/by-name/boot || exit 1",
                "echo Done!"
        );
    }

    public void handleBackup(final View v) {
        showExecuteDialog(
                R.string.done,
                R.string.cancel,
                R.string.backing_up,
                null,
                new Runnable() {
                    @Override
                    public void run() {
                        handleBackup(v);
                    }
                },
                "mkdir /sdcard/AndroidImageKitchen/Backups/",
                "dd if=/dev/block/bootdevice/by-name/boot of=/sdcard/AndroidImageKitchen/Backups/" +
                        new Date().toString().replace(" ", "_") +
                        ".img || exit 1",
                "echo Done!"
        );
    }

    private void showExecuteDialog(
            final int positiveRes,
            final int negativeRes,
            int titleRes,
            final Runnable positiveHandler,
            final Runnable failHandler,
            final String... commands
    ) {
        ProgressFragment fragment = new ProgressFragment();
        fragment.setPositiveRunnable(positiveHandler);
        fragment.setFailRunnable(failHandler);

        Bundle args = new Bundle();
        args.putInt("title", titleRes);
        args.putInt("positive", positiveRes);
        args.putInt("negative", negativeRes);

        fragment.setArguments(args);
        fragment.show(getFragmentManager(), "progress", commands);
    }

    private void handleException(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setToMainScreen();

                new AlertDialog.Builder(InstallerActivity.this)
                        .setTitle(R.string.failed)
                        .setMessage(Html.fromHtml(Arrays.toString(e.getStackTrace()).replace("\n", "<br>")))
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });
    }

    public void createDir(File dir) throws IOException {
        if (dir.exists())
        {
            if (!dir.isDirectory())
            {
                throw new IOException("Can't create directory, a file is in the way");
            }
        } else
        {
            dir.mkdirs();
            if (!dir.isDirectory())
            {
                throw new IOException("Unable to create directory");
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
