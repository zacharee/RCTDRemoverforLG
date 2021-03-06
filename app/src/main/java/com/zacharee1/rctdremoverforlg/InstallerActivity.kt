package com.zacharee1.rctdremoverforlg

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.*
import android.preference.PreferenceManager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.zacharee1.rctdremoverforlg.misc.ProgressFragment
import com.zacharee1.rctdremoverforlg.misc.SuUtils
import com.zacharee1.rctdremoverforlg.misc.SwitchViewWithText
import com.zacharee1.rctdremoverforlg.misc.Utils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.util.*

class InstallerActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    companion object {
        val AIK_VERSION = 3.1F

        val NO_AIK = -1
        val OLD_AIK = 1
        val AIK = 0
    }

    private var rctd: SwitchViewWithText? = null
    private var triton: SwitchViewWithText? = null
    private var ccmd: SwitchViewWithText? = null

    private var mPrefs: SharedPreferences? = null

    private var mRefreshLayout: SwipeRefreshLayout? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_checkroot)

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        Observable.fromCallable({SuUtils.testSudo()})
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { bool ->
                    if (bool) {
                        setup()
                    } else {
                        finish()
                        Toast.makeText(this, resources.getText(R.string.need_root), Toast.LENGTH_SHORT).show()
                    }
                }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        for (i in 0..permissions.lastIndex) {
            val permission = permissions[i]
            val result = grantResults[i]

            if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE && result == PackageManager.PERMISSION_GRANTED) {
                checkAikStatus()
            } else {
                finish()
            }
        }
    }

    override fun onRefresh() {
        checkStatus()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    @SuppressLint("CheckResult")
    private fun checkStatus() {
        val rctd = findViewById<TextView>(R.id.status_report)
        val triton = findViewById<TextView>(R.id.triton_status_report)
        val ccmd = findViewById<TextView>(R.id.ccmd_status_report)

        rctd.setTextColor(Color.YELLOW)
        triton.setTextColor(Color.YELLOW)
        ccmd.setTextColor(Color.YELLOW)

        rctd.setText(R.string.checking)
        triton.setText(R.string.checking)
        ccmd.setText(R.string.checking)

        if (!(mRefreshLayout?.isRefreshing as Boolean)) mRefreshLayout?.isRefreshing = true

        Observable.fromCallable({checkStatusAsync()})
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {bools ->
                    rctd.setText(if (bools[0]) R.string.not_found else R.string.running)
                    rctd.setTextColor(if (bools[0]) Color.GREEN else Color.RED)

                    triton.setText(if (bools[1]) R.string.not_found else R.string.running)
                    triton.setTextColor(if (bools[1]) Color.GREEN else Color.RED)

                    ccmd.setText(if (bools[2]) R.string.not_found else R.string.running)
                    ccmd.setTextColor(if (bools[2]) Color.GREEN else Color.RED)

                    Handler(Looper.getMainLooper()).postDelayed({ mRefreshLayout?.isRefreshing = false }, 300)
                }
    }

    private fun checkStatusAsync(): BooleanArray {
        val rctResult = SuUtils.sudoForResult("ps | grep rctd")
        val rctNotThere = rctResult.isEmpty() || !rctResult.contains("/sbin/rctd")

        val tritonResult = SuUtils.sudoForResult("ps | grep triton")
        val tritonNotThere = tritonResult.isEmpty() || !tritonResult.contains("/system/bin/triton")

        val ccmdResult = SuUtils.sudoForResult("ps | grep ccmd")
        val ccmdNotThere = ccmdResult.isEmpty() || !ccmdResult.contains("/system/bin/ccmd")

        return booleanArrayOf(rctNotThere, tritonNotThere, ccmdNotThere)
    }

    private fun setup() {
        if (checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 10)
        } else {
            checkAikStatus()
        }
    }

    private fun setupSwipeRefresh() {
        mRefreshLayout = findViewById(R.id.swipe_parent)
        mRefreshLayout?.setColorSchemeResources(
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
        )
        mRefreshLayout?.setOnRefreshListener(this)
    }

    private fun setSwitchChecksAndListeners() {
        val patchRctd = mPrefs?.getBoolean("rctd", true)
        val patchCcmd = mPrefs?.getBoolean("ccmd", false)
        val patchTriton = mPrefs?.getBoolean("triton", true)

        rctd?.isChecked = patchRctd as Boolean
        ccmd?.isChecked = patchCcmd as Boolean
        triton?.isChecked = patchTriton as Boolean

        rctd?.setOnCheckedChangeListener { _, b -> mPrefs?.edit()?.putBoolean("rctd", b)?.apply() }

        ccmd?.setOnCheckedChangeListener{ _, b -> mPrefs?.edit()?.putBoolean("ccmd", b)?.apply() }

        triton?.setOnCheckedChangeListener{ _, b -> mPrefs?.edit()?.putBoolean("triton", b)?.apply() }
    }

    private fun setToMainScreen() {
        runOnUiThread {
            setContentView(R.layout.activity_installer)

            rctd = findViewById(R.id.enable_patch_rctd)
            ccmd = findViewById(R.id.enable_patch_ccmd)
            triton = findViewById(R.id.enable_patch_triton)

            setSwitchChecksAndListeners()
            setupSwipeRefresh()

            checkStatus()

            val appBy = findViewById<TextView>(R.id.app_made_by)
            val aikBy = findViewById<TextView>(R.id.aik_by)

            appBy.movementMethod = LinkMovementMethod.getInstance()
            aikBy.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    @SuppressLint("CheckResult")
    private fun checkAikStatus() {
        Observable.fromCallable({checkAikStatusAsync()})
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { status ->
                    when (status) {
                        NO_AIK -> {
                            (findViewById<View>(R.id.textView) as TextView).text = resources.getString(R.string.installing_aik)
                            installAik()
                        }
                        AIK -> {
                            setToMainScreen()
                        }
                        OLD_AIK -> {
                            AlertDialog.Builder(this)
                                    .setTitle(R.string.old_aik)
                                    .setMessage(R.string.old_aik_msg)
                                    .setPositiveButton(R.string.yes) { _, _ -> installAik() }
                                    .setNegativeButton(R.string.no) { _, _ -> setToMainScreen() }
                                    .setCancelable(false)
                                    .show()
                        }
                    }
                }
    }

    private fun checkAikStatusAsync(): Int {
        try {
            val aikProc = Runtime.getRuntime().exec("aik")

            val outputStream = DataOutputStream(aikProc.outputStream)

            outputStream.writeBytes("exit\n")
            outputStream.flush()

            aikProc.waitFor()
        } catch (e: Exception) {
            return NO_AIK
        }


        return if (Utils.getAikVersion() >= AIK_VERSION) AIK else OLD_AIK
    }

    @SuppressLint("CheckResult")
    private fun installAik() {
        Observable.fromCallable({installAikAsync()})
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { bool ->
                    if (bool) {
                        AlertDialog.Builder(this)
                                .setTitle(resources.getString(R.string.reboot))
                                .setMessage(resources.getString(R.string.install_aik))
                                .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                                    try {
                                        SuUtils.sudo("reboot recovery")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .setNegativeButton(resources.getString(R.string.no)) { _, _ -> finish() }
                                .setCancelable(false)
                                .show()
                    } else {
                        AlertDialog.Builder(this)
                                .setTitle(R.string.failed)
                                .setMessage(R.string.aik_install_fail)
                                .setPositiveButton(R.string.ok) { _, _ -> finish() }
                                .show()
                    }
                }
    }

    private fun installAikAsync(): Boolean {
        val assetManager = assets
        val aik = "AIK.zip"

        val `in`: InputStream
        val out: FileOutputStream

        try {
            `in` = assetManager.open(aik)
            val dest = Environment.getExternalStorageDirectory().absolutePath + "/AndroidImageKitchen/"
            val outDir = File(dest)
            createDir(outDir)
            val outFile = File(dest, aik)
            out = FileOutputStream(outFile)

            copyFile(`in`, out)

            `in`.close()

            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }


        try {
            SuUtils.sudo("echo '--update_package=/sdcard/0/AndroidImageKitchen/AIK.zip' >> /cache/recovery/command")
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }


        return true
    }

    fun handlePatch(v: View) {
        val patchRctd = rctd?.isChecked
        val patchCcmd = ccmd?.isChecked
        val patchTriton = triton?.isChecked

        val executeMod = "executemod.sh"

        val assetManager = assets

        val `in`: InputStream
        val out: FileOutputStream

        try {
            `in` = assetManager.open(executeMod)
            val dest = Environment.getExternalStorageDirectory().absolutePath + "/AndroidImageKitchen/"
            val outDir = File(dest)
            createDir(outDir)
            val outFile = File(dest, executeMod)
            out = FileOutputStream(outFile)

            copyFile(`in`, out)

            `in`.close()

            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()

            handleException(e)
        }


        showExecuteDialog(
                R.string.flash,
                R.string.cancel,
                R.string.patching_image,
                Runnable { handleFlash(v) },
                Runnable { handlePatch(v) },
                "cp /sdcard/AndroidImageKitchen/$executeMod /data/local/AIK-mobile/. || exit 1",
                "cd /data/local/AIK-mobile/ || exit 1",
                "chmod 0755 $executeMod || exit 1",
                "./" + executeMod + " " + patchRctd + " " + patchCcmd + " " + patchTriton + " " + Build.DEVICE
        )
    }

    fun handleFlash(v: View) {
        showExecuteDialog(
                R.string.reboot,
                R.string.cancel,
                R.string.flashing_image,
                Runnable {
                    try {
                        SuUtils.sudo("reboot")
                    } catch (e: Exception) {
                    }
                },
                Runnable { handleFlash(v) },
                "dd if=/storage/emulated/0/AndroidImageKitchen/boot.img of=/dev/block/bootdevice/by-name/boot || exit 1",
                "echo Done!"
        )
    }

    fun handleBackup(v: View) {
        showExecuteDialog(
                R.string.done,
                R.string.cancel,
                R.string.backing_up,
                null,
                Runnable { handleBackup(v) },
                "mkdir /sdcard/AndroidImageKitchen/Backups/",
                "dd if=/dev/block/bootdevice/by-name/boot of=/sdcard/AndroidImageKitchen/Backups/" +
                        Date().toString().replace(" ", "_") +
                        ".img || exit 1",
                "echo Done!"
        )
    }

    private fun showExecuteDialog(
            positiveRes: Int,
            negativeRes: Int,
            titleRes: Int,
            positiveHandler: Runnable?,
            failHandler: Runnable?,
            vararg commands: String
    ) {
        val fragment = ProgressFragment()
        fragment.setPositiveRunnable(positiveHandler)
        fragment.setFailRunnable(failHandler)

        val args = Bundle()
        args.putInt("title", titleRes)
        args.putInt("positive", positiveRes)
        args.putInt("negative", negativeRes)

        fragment.arguments = args
        fragment.show(supportFragmentManager, "progress", *commands)
    }

    private fun handleException(e: Exception) {
        runOnUiThread {
            setToMainScreen()

            AlertDialog.Builder(this)
                    .setTitle(R.string.failed)
                    .setMessage(Html.fromHtml(Arrays.toString(e.stackTrace).replace("\n", "<br>")))
                    .setPositiveButton(R.string.ok, null)
                    .show()
        }
    }

    @Throws(IOException::class)
    private fun createDir(dir: File) {
        if (dir.exists()) {
            if (!dir.isDirectory) {
                throw IOException("Can't create directory, a file is in the way")
            }
        } else {
            dir.mkdirs()
            if (!dir.isDirectory) {
                throw IOException("Unable to create directory")
            }
        }
    }

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int

        do {
            read = `in`.read(buffer)

            if (read == -1) break

            out.write(buffer, 0, read)
        } while (true)
    }
}