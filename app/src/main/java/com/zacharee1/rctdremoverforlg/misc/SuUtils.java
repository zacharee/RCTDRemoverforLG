package com.zacharee1.rctdremoverforlg.misc;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ScrollView;
import android.widget.TextView;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SuUtils
{
    public static Process sudo(String... strings) throws IOException {
        Process su = Runtime.getRuntime().exec("su");

        try{
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            BufferedReader inputReader = new BufferedReader(new InputStreamReader(su.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(su.getErrorStream()));

            for (String s : strings) {
                Log.e("Sudo Exec", s);
                outputStream.writeBytes(s + "\n");
                outputStream.flush();

                String line;

//                while ((line = inputReader.readLine()) != null) {
//                    Log.e("Sudo", line);
//                }
//
//                while ((line = errorReader.readLine()) != null) {
//                    Log.e("Sudo", line);
//                }
            }

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            try {
                su.waitFor();
                Log.e("Done", "Done");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e("No Root?", e.getMessage());
            }
            outputStream.close();
        } catch (IOException e){
            e.printStackTrace();
            throw new IOException(e);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return su;
    }

    public static String sudoForResult(String...strings) {
        String res = "";
        DataOutputStream outputStream = null;
        InputStream response = null;
        try{
            Process su = Runtime.getRuntime().exec("su");
            outputStream = new DataOutputStream(su.getOutputStream());
            response = su.getInputStream();

            for (String s : strings) {
                outputStream.writeBytes(s+"\n");
                outputStream.flush();
            }

            outputStream.writeBytes("exit\n");
            outputStream.flush();

            res = readFully(response);

            outputStream.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return res;
    }

    public static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;

        while ((length = is.read(buffer)) > 5) {
            baos.write(buffer, 0, length);

            if (length == 6) break;
        }

        return baos.toString("UTF-8");
    }

    public static String suAndPrintToView(final ProcessExecutor processExecutor, final Activity activity, final TerminalView addTo, ArrayList<String> commands) {
        final StringBuilder builder = new StringBuilder();

        try {
            processExecutor.command("su", "-c", TextUtils.join(" ; ", commands))
                    .redirectOutput(new LogOutputStream() {
                        @Override
                        protected void processLine(final String line) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addTo.addText(line + "\n");
                                    builder.append(line);
                                    final ViewParent scroll = (addTo.getParent()).getParent();

                                    if (scroll instanceof ScrollView) {
                                        ((ScrollView) scroll).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                ((ScrollView) scroll).fullScroll(ScrollView.FOCUS_DOWN);
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    })
                    .execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    public static boolean testSudo() {
        StackTraceElement st = null;

        try{
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes("exit\n");
            outputStream.flush();

            DataInputStream inputStream = new DataInputStream(su.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            while (bufferedReader.readLine() != null) {
                bufferedReader.readLine();
            }

            su.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            for (StackTraceElement s : e.getStackTrace()) {
                st = s;
                if (st != null) break;
            }
        }

        return st == null;
    }
}
