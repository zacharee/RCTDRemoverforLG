package com.zacharee1.rctdremoverforlg;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Environment;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.zacharee1.rctdremoverforlg.misc.SuUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;

public class InstallerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_checkroot);

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

    private void setup() {
        if (checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 10);
        } else {
            installAikIfNeeded();
        }
    }

    private void installAikIfNeeded() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process aikProc = Runtime.getRuntime().exec("aik");

                    DataOutputStream outputStream = new DataOutputStream(aikProc.getOutputStream());

                    outputStream.writeBytes("exit\n");
                    outputStream.flush();

                    aikProc.waitFor();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("TAG", "Setting ContentView");
                            setContentView(R.layout.activity_installer);

                            checkStatus();

                            TextView appBy = findViewById(R.id.app_made_by);
                            TextView aikBy = findViewById(R.id.aik_by);

                            appBy.setMovementMethod(LinkMovementMethod.getInstance());
                            aikBy.setMovementMethod(LinkMovementMethod.getInstance());
                        }
                    });
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
        setContentView(R.layout.layout_checkroot);
        ((TextView)findViewById(R.id.textView)).setText(getResources().getString(R.string.patching_image));

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                int id = v.getId();
                String aik = null;

                switch (id) {
                    case R.id.installRCT:
                        aik = "executemodRCT.sh";
                        break;
                    case R.id.installTriton:
                        aik = "executemodTriton.sh";
                        break;
                }

                AssetManager assetManager = getAssets();

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
                } catch (final Exception e) {
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setContentView(R.layout.activity_installer);

                            new AlertDialog.Builder(InstallerActivity.this)
                                    .setTitle(R.string.patch_fail)
                                    .setMessage(Html.fromHtml(Arrays.toString(e.getStackTrace()).replace("\n", "<br>")))
                                    .setPositiveButton(R.string.ok, null)
                                    .show();
                        }
                    });
                }

                try {
                    final String executeResult = SuUtils.sudoForResult("cp /sdcard/AndroidImageKitchen/" + aik + " /data/local/AIK-mobile/.",
                            "aik",
                            "chmod 0755 " + aik,
                            "./" + aik);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setContentView(R.layout.activity_installer);

                            checkStatus();

                            new AlertDialog.Builder(InstallerActivity.this)
                                    .setTitle(getResources().getString(R.string.done))
                                    .setMessage(Html.fromHtml("<b>" + getResources().getString(R.string.patch_done) + "</b>" +
                                            "<br><br>" +
                                            "<b>" + getResources().getString(R.string.log) + "</b>" +
                                            "<br><br>" +
                                            executeResult.replace("\n", "<br>")))
                                    .setPositiveButton(getResources().getString(R.string.ok), null)
                                    .show();
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setContentView(R.layout.activity_installer);

                            new AlertDialog.Builder(InstallerActivity.this)
                                    .setTitle(R.string.patch_fail)
                                    .setMessage(Html.fromHtml(Arrays.toString(e.getStackTrace()).replace("\n", "<br>")))
                                    .setPositiveButton(R.string.ok, null)
                                    .show();
                        }
                    });
                }
            }
        }).start();
    }

    public void handleFlash(View v) {
        setContentView(R.layout.layout_checkroot);
        ((TextView) findViewById(R.id.textView)).setText(getResources().getString(R.string.flashing_image));

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    SuUtils.sudo("dd if=/sdcard/AndroidImageKitchen/boot.img of=/dev/block/bootdevice/by-name/boot");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setContentView(R.layout.activity_installer);

                            checkStatus();

                            new AlertDialog.Builder(InstallerActivity.this)
                                    .setTitle(getResources().getString(R.string.flashed))
                                    .setMessage(getResources().getString(R.string.flash_done))
                                    .setPositiveButton(getResources().getString(R.string.reboot), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            try {
                                                SuUtils.sudo("reboot");
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                                    .setNegativeButton(getResources().getString(R.string.later), null)
                                    .show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void handleBackup(View v) {
        setContentView(R.layout.layout_checkroot);
        ((TextView) findViewById(R.id.textView)).setText(getResources().getString(R.string.backing_up));

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SuUtils.sudo("mkdir /sdcard/AndroidImageKitchen/Backups/",
                            "dd if=/dev/block/bootdevice/by-name/boot of=/sdcard/AndroidImageKitchen/Backups/" + new Date().toString().replace(" ", "_") + ".img");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setContentView(R.layout.activity_installer);

                            checkStatus();

                            new AlertDialog.Builder(InstallerActivity.this)
                                    .setTitle(getResources().getString(R.string.backup))
                                    .setMessage(getResources().getString(R.string.backup_done))
                                    .setPositiveButton(getResources().getString(R.string.ok), null)
                                    .show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void checkStatus(View v) {
        v.animate().rotationBy(360).setDuration(500).start();
        checkStatus();
    }

    public void checkStatus() {
        final TextView report = findViewById(R.id.status_report);
        final TextView triton = findViewById(R.id.triton_status_report);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final String rctResult = SuUtils.sudoForResult("ps | grep rctd");
                final boolean rctNotThere = rctResult.isEmpty() ||
                        (!rctResult.contains("/sbin/rctd"));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        report.setText(rctNotThere ? R.string.not_found : R.string.running);
                        report.setTextColor(rctNotThere ? Color.GREEN : Color.RED);
                    }
                });

                final String tritonResult = SuUtils.sudoForResult("ps | grep triton");
                final boolean tritonNotThere = tritonResult.isEmpty() ||
                        (!tritonResult.contains("/system/bin/triton"));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        triton.setText(tritonNotThere ? R.string.not_found : R.string.running);
                        triton.setTextColor(tritonNotThere ? Color.GREEN : Color.RED);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            String perm = permissions[i];
            int result = grantResults[i];

            if (perm.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && result == PackageManager.PERMISSION_GRANTED) {
                installAikIfNeeded();
            } else {
                finish();
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
}
