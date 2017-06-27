package cn.ljj;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cn.ljj.baidu.music.BaiduMusicInfo;
import cn.ljj.common.Config;
import cn.ljj.common.FileUtils;
import cn.ljj.common.Logger;
import cn.ljj.common.QCloudManager;

public class MusicWebFactory {
    private static final String TAG = MusicWebFactory.class.getSimpleName();

    private static final String KEY_PATTERN_TO_REPLACE_TITLE = "pattern_to_replace_title";
    private static final String KEY_PATTERN_TO_REPLACE_SONG_NAME = "pattern_to_replace_song_name";
    private static final String KEY_PATTERN_TO_REPLACE_SINGER = "pattern_to_replace_singer";
    private static final String KEY_PATTERN_TO_REPLACE_POSTER = "pattern_to_replace_poster";
    private static final String KEY_PATTERN_TO_REPLACE_SONG_SRC = "pattern_to_replace_song_src";
    private static final String KEY_PATTERN_TO_REPLACE_LRC = "pattern_to_replace_lrc";

    public static boolean createHtmlFromFolder(String folderPath, String outputPath) {
        BaiduMusicInfo music = getMusicInfoUnderFolder(folderPath);
        Logger.i(TAG, "createHtmlFromFolder music.title:" + ((music != null) ? music.title : null));
        if (music == null) {
            return false;
        }
        File lrcFile = new File(folderPath, music.getLrcFileName());
        String lrcContent = getFormatLrcString(lrcFile.getAbsolutePath());
        Logger.i(TAG, "createHtmlFromFolder lrcContent:" + (lrcContent == null ? 0 : lrcContent.length()));
        File musicFile = new File(folderPath, music.getMusicFileName());
        String webTitle = music.getFolderName();
        String songName = music.title;
        String singer = music.author;
        String poster = music.songPicRadio;
        String songUrl = "";
        if (musicFile.exists()) {
            String remoteFilePath = "/" + music.author + " - " + music.getMusicFileName();
            songUrl = QCloudManager.getInstance().getUrlIfFileExits("music", remoteFilePath);
            if (songUrl == null) {
                songUrl = QCloudManager.getInstance().upload("music", remoteFilePath, musicFile.getAbsolutePath());
            }
        }
        InputStream inputStream = null;
        String externalPatternPath = Config.getInstance().get("externalPattern");
        if (externalPatternPath != null) {
            File externalPattern = new File(externalPatternPath);
            if (externalPattern.exists()) {
                try {
                    inputStream = new FileInputStream(externalPattern);
                    Logger.i(TAG, "createHtmlFromFolder using external pattern! " + externalPattern.getAbsolutePath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        if (inputStream == null) {
            inputStream = MusicWebFactory.class.getResourceAsStream("pattern.html");
        }
        String html = FileUtils.getStringFromStream(inputStream);
        Logger.i(TAG, "createHtmlFromFolder html:" + (html == null ? 0 : html.length()));
        if (html == null) {
            return false;
        }
        html = html.replace(KEY_PATTERN_TO_REPLACE_TITLE, webTitle);
        html = html.replace(KEY_PATTERN_TO_REPLACE_SONG_NAME, songName);
        html = html.replace(KEY_PATTERN_TO_REPLACE_SINGER, singer);
        html = html.replace(KEY_PATTERN_TO_REPLACE_POSTER, poster);
        html = html.replace(KEY_PATTERN_TO_REPLACE_SONG_SRC, songUrl);
        html = html.replace(KEY_PATTERN_TO_REPLACE_LRC, lrcContent);
        try {
            if (outputPath == null || outputPath.length() <= 0) {
                outputPath = folderPath + File.separator + music.getFolderName() + ".html";
            }
            File htmlFile = new File(outputPath);
            FileWriter fw = new FileWriter(htmlFile);
            fw.write(html);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static BaiduMusicInfo getMusicInfoUnderFolder(String folderPath) {
        File folder = new File(folderPath);
        Logger.d(TAG, "getMusicInfoUnderFolder folder:" + folder.getAbsolutePath());
        if (!folder.exists() || !folder.isDirectory()) {
            return null;
        }
        String[] subFiles = folder.list();
        Logger.d(TAG, "subFiles:" + Arrays.toString(subFiles));
        String jsonFileName = null;
        for (String fileName : subFiles) {
            if (fileName.endsWith(".json")) {
                jsonFileName = fileName;
                break;
            }
        }
        if (jsonFileName == null) {
            return null;
        }
        Logger.d(TAG, "getMusicInfoUnderFolder jsonFileName:" + jsonFileName);
        File jsonFile = new File(folder, jsonFileName);
        JsonParser parser = new JsonParser();
        try {
            JsonObject object = parser.parse(new FileReader(jsonFile)).getAsJsonObject();
            BaiduMusicInfo music = BaiduMusicInfo.fromJson(object);
            music.setDetailInfos(object);
            return music;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getFormatLrcString(String lrcPath) {
        String lrcContent = FileUtils.getFileString(lrcPath);
        if (lrcContent == null) {
            return "";
        }
        BufferedReader br = new BufferedReader(new StringReader(lrcContent));
        ArrayList<String> lrcLineArray = new ArrayList<String>();
        try {
            String line = br.readLine();
            while (line != null) {
                if (line.startsWith("[ar:]")) {
                    line = line.substring(5, line.length());
                    line = "[00:00.00]Artist:" + line;
                    lrcLineArray.add(line);
                } else if (line.startsWith("[ti:]")) {
                    line = line.substring(5, line.length());
                    line = "[00:00.01]Title:" + line;
                    lrcLineArray.add(line);
                } else if (line.startsWith("[al:]")) {
                    line = line.substring(5, line.length());
                    line = "[00:00.02]Album:" + line;
                    lrcLineArray.add(line);
                } else if (line.startsWith("[ar:")) {
                    line = line.substring(4, line.length() - 1);
                    line = "[00:00.00]Artist:" + line;
                    lrcLineArray.add(line);
                } else if (line.startsWith("[ti:")) {
                    line = line.substring(4, line.length() - 1);
                    line = "[00:00.01]Title:" + line;
                    lrcLineArray.add(line);
                } else if (line.startsWith("[al:")) {
                    line = line.substring(4, line.length() - 1);
                    line = "[00:00.02]Album:" + line;
                    lrcLineArray.add(line);
                } else if (line.startsWith("[by:")) {

                } else if (line.startsWith("[offset:")) {

                } else {
                    String content = line;
                    int index1 = content.indexOf("[");
                    int index2 = content.indexOf("]");
                    while (index1 != -1 && index2 != -1) {
                        content = content.substring(index2 + 1);
                        index1 = content.indexOf("[");
                        index2 = content.indexOf("]");
                    }
                    String temp = line;
                    index1 = temp.indexOf("[");
                    index2 = temp.indexOf("]");
                    while (index1 != -1 && index2 != -1) {
                        String time = temp.substring(index1, index2 + 1);
                        lrcLineArray.add(time + content);
                        temp = temp.substring(index2 + 1);
                        index1 = temp.indexOf("[");
                        index2 = temp.indexOf("]");
                    }
                }
                line = br.readLine();
            }
            lrcLineArray.sort(new Comparator<String>() {

                @Override
                public int compare(String o1, String o2) {
                    return Collator.getInstance().compare(o1, o2);
                }
            });
            StringBuilder sb = new StringBuilder();
            for (String l : lrcLineArray) {
                sb.append(l).append("\n");
            }
            lrcContent = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        lrcContent = lrcContent.replace("\n", "\\n");

        lrcContent = lrcContent.replace("\\\"", "\"");
        lrcContent = lrcContent.replace("\"", "\\\"");

        lrcContent = lrcContent.replace("\\'", "'");
        lrcContent = lrcContent.replace("'", "\\'");
        return lrcContent;
    }
}
