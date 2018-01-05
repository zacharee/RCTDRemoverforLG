package com.zacharee1.rctdremoverforlg;

import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.SensorManager;
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

import org.zeroturnaround.exec.ProcessExecutor;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;

public class InstallerActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

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

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (SuUtils.testSudo()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setup();
                        }
                    });
                } else {
                    finish();
                    Toast.makeText(InstallerActivity.this, getResources().getString(R.string.need_root), Toast.LENGTH_LONG).show();
                }
            }
        }).start();
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                checkStatusFromThread();
            }
        }).start();
    }

    public void checkStatusFromThread() {

        final TextView report = findViewById(R.id.status_report);
        final TextView triton = findViewById(R.id.triton_status_report);
        final TextView ccmd = findViewById(R.id.ccmd_status_report);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(true);

                report.setText(R.string.checking);
                triton.setText(R.string.checking);
                ccmd.setText(R.string.checking);

                report.setTextColor(Color.YELLOW);
                triton.setTextColor(Color.YELLOW);
                ccmd.setTextColor(Color.YELLOW);
            }
        });

        final String rctResult = SuUtils.sudoForResult("ps | grep rctd");
        final boolean rctNotThere = rctResult.isEmpty() ||
                (!rctResult.contains("/sbin/rctd"));

        final String tritonResult = SuUtils.sudoForResult("ps | grep triton");
        final boolean tritonNotThere = tritonResult.isEmpty() ||
                (!tritonResult.contains("/system/bin/triton"));

        final String ccmdResult = SuUtils.sudoForResult("ps | grep ccmd");
        final boolean ccmdNotThere = ccmdResult.isEmpty() ||
                !ccmdResult.contains("/system/bin/ccmd");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                report.setText(rctNotThere ? R.string.not_found : R.string.running);
                report.setTextColor(rctNotThere ? Color.GREEN : Color.RED);

                triton.setText(tritonNotThere ? R.string.not_found : R.string.running);
                triton.setTextColor(tritonNotThere ? Color.GREEN : Color.RED);

                ccmd.setText(ccmdNotThere ? R.string.not_found : R.string.running);
                ccmd.setTextColor(ccmdNotThere ? Color.GREEN : Color.RED);
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 500);
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

    private void checkAikStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process aikProc = Runtime.getRuntime().exec("aik");

                    DataOutputStream outputStream = new DataOutputStream(aikProc.getOutputStream());

                    outputStream.writeBytes("exit\n");
                    outputStream.flush();

                    aikProc.waitFor();

                    setToMainScreen();
                } catch (Exception e) {
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.textView)).setText(getResources().getString(R.string.installing_aik));
                        }
                    });

                    installAik();
                }
            }
        }).start();
    }

    public void installAik() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

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
                    in = null;

                    out.flush();
                    out.close();
                    out = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    SuUtils.sudo("echo '--update_package=/sdcard/0/AndroidImageKitchen/AIK.zip' >> /cache/recovery/command");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(InstallerActivity.this)
                                .setTitle(getResources().getString(R.string.reboot))
                                .setMessage(getResources().getString(R.string.install_aik))
                                .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        try {
                                            SuUtils.sudo("reboot recovery");
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                })
                                .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        finish();
                                    }
                                })
                                .setCancelable(false)
                                .show();
                    }
                });
            }
        }).start();
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
            in = null;

            out.flush();
            out.close();
            out = null;
        } catch (final Exception e) {
            e.printStackTrace();

            handleException(e);
        }

        showExecuteDialog(
                R.string.flash,
                R.string.cancel,
                R.string.patching_image,
                new Runnable() {
                    @Override
                    public void run() {
                        handleFlash(v);
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        handlePatch(v);
                    }
                },
                "cp /sdcard/AndroidImageKitchen/" + executeMod + " /data/local/AIK-mobile/.",
                "cd /data/local/AIK-mobile/",
                "chmod 0755 " + executeMod,
                "./" + executeMod + " " + patchRctd + " " + patchCcmd + " " + patchTriton
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
                "dd if=/sdcard/AndroidImageKitchen/boot.img of=/dev/block/bootdevice/by-name/boot",
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
                        ".img",
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
