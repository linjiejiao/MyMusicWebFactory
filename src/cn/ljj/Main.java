package cn.ljj;

import java.io.File;

import cn.ljj.common.FileUtils;
import cn.ljj.common.Logger;

public class Main {
    private static final String TAG = MusicWebFactory.class.getSimpleName();

    public static void main(String[] args) {
        String outputPath = FileUtils.getConfigs("config.ini").get("htmlDestFolder");
        if (args != null && args.length > 0) {
            String path = args[0];
            if (args.length > 1) {
                outputPath = args[1];
            }
            if (outputPath == null) {
                outputPath = ".";
            }
            MusicWebFactory.createHtmlFromFolder(path, outputPath);
        } else {
            if (outputPath == null) {
                outputPath = ".";
            }
            File folder = new File(".");
            String[] subFileList = folder.list();
            for (String subFile : subFileList) {
                File musicFolder = new File(folder, subFile);
                if (musicFolder.isDirectory()) {
                    String htmlFileName = subFile + ".html";
                    File htmlFile = new File(outputPath + File.separator + htmlFileName);
                    if (!htmlFile.exists()) {
                        MusicWebFactory.createHtmlFromFolder(musicFolder.getAbsolutePath(), htmlFile.getAbsolutePath());
                        Logger.i(TAG, "main new htmlFile: " + htmlFile.getAbsolutePath());
                    } else {
                        Logger.e(TAG, "main htmlFile already exits! " + htmlFile.getAbsolutePath());
                    }
                }
            }
        }
    }
}
