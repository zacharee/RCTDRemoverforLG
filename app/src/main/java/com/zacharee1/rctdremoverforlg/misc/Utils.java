package com.zacharee1.rctdremoverforlg.misc;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public final static String TAG = "Utils RCTD Removal";

    public static void copyFile(String inputPath, String inputFile, String outputPath) {
        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File(outputPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file (You have now copied the file)
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public static int pxToDp(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static float getAikVersion() {
        File file = new File("/data/local/AIK-mobile/bin/","module.prop");

        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                if (line.contains("version=")) {
                    text.append(line);
                }
            }
            br.close();
        } catch (IOException e) {}

        Log.e("AIK MODULE", text.toString());

        String version = text.toString().replace("version=", "");

        return Float.valueOf(version);
    }
}
