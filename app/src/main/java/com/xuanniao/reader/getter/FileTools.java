package com.xuanniao.reader.getter;

import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import com.xuanniao.reader.tools.BookDB;
import com.xuanniao.reader.tools.Constants;
import com.xuanniao.reader.tools.JsonRead;
import com.xuanniao.reader.ui.BookItem;
import com.xuanniao.reader.ui.CatalogItem;
import com.xuanniao.reader.ui.ChapterItem;
import com.xuanniao.reader.ui.book.PlatformItem;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileTools {
    private static final String Tag = "FileTools";
    private static final int figures = 4;

    /**
     * 创建新书文件夹
     * @param catalogItem 目录
     * @return 创建结果
     */
    public static boolean newBook(Context context, CatalogItem catalogItem) {
        String catalogFileName = "0000目录.txt";
        try {
            DocumentFile catalogFile = getDocumentFile(context, catalogItem.getBookName(), catalogFileName);
            OutputStream out = context.getContentResolver().openOutputStream(catalogFile.getUri());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            List<String> chapterCodeList = catalogItem.getChapterCodeList();
            List<String> chapterTitleList = catalogItem.getChapterTitleList();
            writer.write("bookCode:" + catalogItem.getBookCode() + "\r\n");
            writer.write("platformName:" + catalogItem.getPlatformName() + "\r\n");
            for (int i = 0; i < chapterCodeList.size(); i++) {
                writer.write((i + 1) + "/x/" + chapterTitleList.get(i)
                        + "/x/" + chapterCodeList.get(i) + "\r\n");
            }
            writer.close();
            out.close();
            return true;
        } catch (IOException e) {
            Log.e(Tag, e.getMessage());
            return false;
        }
    }

    /**
     * 写入详情
     * @param context  上下文
     * @param bookItem 书目
     * @return
     */
    public static boolean infoSave(Context context, BookItem bookItem) {
        String catalogFileName = "0000目录.txt";
        try {
            DocumentFile catalogFile = getDocumentFile(context, bookItem.getBookName(), catalogFileName);
            OutputStream out = context.getContentResolver().openOutputStream(catalogFile.getUri());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            String author = bookItem.getAuthor();
            if (author != null) writer.write("author:" + author + "\r\n");
            String classify = bookItem.getClassify();
            if (classify != null) writer.write("classify:" + classify + "\r\n");
            String status = bookItem.getStatus();
            if (status != null) writer.write("status:" + status + "\r\n");
            long renewTimeLong = bookItem.getRenewTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE);
            String renewTime = dateFormat.format(renewTimeLong);
            if (renewTime != null) writer.write("renewTime:" + renewTime + "\r\n");
            String synopsis = bookItem.getSynopsis();
            if (synopsis != null) writer.write("synopsis:" + synopsis + "\r\n");
            String coverUrl = bookItem.getCoverUrl();
            if (coverUrl != null) writer.write("coverUrl:" + coverUrl + "\r\n");
            writer.write("coverUrl:" + bookItem.getCoverUrl() + "\r\n");
            CoverGetter coverGetter = new CoverGetter(context);
            coverGetter.execute(coverUrl);
            writer.close();
            out.close();
            return true;
        } catch (IOException e) {
            Log.e(Tag, e.getMessage());
            return false;
        }
    }

    /**
     * 存储章节
     * @param context 上下文
     * @param chapterItem 章节
     * @return 存储结果
     */
    public static int chapterSave(Context context, ChapterItem chapterItem) {
        String formatNum = figuresNum(chapterItem.getBookName(), chapterItem.getChapterNum());
        if (formatNum.equals("-1") || formatNum.equals("-2")) {
            return Integer.parseInt(formatNum);
        }
        int emptyNum = 0;
        for (String s : chapterItem.getChapter()) {
            if (s.trim().isEmpty()) {
                emptyNum += 1;
            }
        }
        Log.d(Tag, "emptyNum:" + emptyNum + " | size:" + chapterItem.getChapter().size());
        if (emptyNum == chapterItem.getChapter().size()) {
            return Integer.parseInt(formatNum);
        }
        String chapterFileName = formatNum + chapterItem.getTitle() + ".txt";
        try {
            DocumentFile chapterFile = getDocumentFile(context, chapterItem.getBookName(), chapterFileName);
            if (chapterFile == null) return -2;
            OutputStream out = context.getContentResolver().openOutputStream(chapterFile.getUri());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            for (String s : chapterItem.getChapter()) {
                writer.write(s + "\r\n");
            }
            writer.close();
            return 0;
        } catch (IOException e) {
            Log.e(Tag, e.getMessage());
            return -3;
        }
    }

    /**
     * 读取本地平台信息的方法
     * @param context 上下文
     * @param uri uri
     */
    public static List<PlatformItem> loadLocalPlatformData(Context context, Uri uri) {
        List<PlatformItem> platformList = new ArrayList<>();
        if (uri == null) return null;
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            int tempbyte;
            while ((tempbyte = isr.read()) != -1) {
                sb.append((char) tempbyte);
            }
            is.close();
            String platformJson = sb.toString();
            if (platformJson.equals("0")) {
                PlatformItem platformItem = new PlatformItem();
                platformItem.setPlatformName("0");
                platformList.add(platformItem);
            } else {
//                Log.d(Tag, "json：" + platformJson);
//                JSONObject j = JSONObject.parseObject(platformJson);
                JSONObject j = new JSONObject(platformJson);
                platformList = JsonRead.JsonToPlatformList(j);
                if (platformList == null || platformList.isEmpty()) {
                    Toast.makeText(context, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
                }
            }
            return platformList;
        } catch (IOException e) {
            Toast.makeText(context, "读取平台数据失败", Toast.LENGTH_SHORT).show();
            return null;
        } catch (JSONException e) {
            Toast.makeText(context, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * 读取本地书籍的方法
     * @param context 上下文
     * @param uri 书籍uri
     * @return 书目
     */
    public static BookItem loadLocalBook(Context context, Uri uri) {
        BookItem bookItem = new BookItem();
        // 遍历文件夹
        DocumentFile directory = DocumentFile.fromTreeUri(context, uri);
        if (directory != null && directory.isDirectory()) {
            bookItem.setBookName(directory.getName());
            int figures = 4;
            for (DocumentFile file : directory.listFiles()) {
                if (file.isFile() && file.getName().contains("目录")) {
                    try {
                        InputStream inputStream = context.getContentResolver().openInputStream(file.getUri());
                        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                        String line;
                        while ((line = br.readLine()) != null) {
                            Log.d(Tag, "line:" + line);
                            if (line.contains("bookCode")) {
                                bookItem.setBookCode(line.replace("bookCode:", ""));
                            } else if (line.contains("platformName")) {
                                bookItem.setPlatformName(line.replace("platformName:", ""));
                            }
                        }
                    } catch (IOException ignored) {}
                }
            }
            List<Integer> savedChapterList = new ArrayList<>();
            for (DocumentFile file : directory.listFiles()) {
                if (file.isFile() && file.getName() != null && !file.getName().equals("0000目录")) {
                    String fileName = file.getName();
                    int num = Integer.parseInt(fileName.substring(0, figures));
                    savedChapterList.add(num);
                }
            }
            bookItem.setChapterSavedList(savedChapterList);
            BookDB bdb = BookDB.getInstance(context, Constants.DB_BOOK);
            bdb.writeItem(Constants.TAB_BOOK, bookItem);
        }
        return bookItem;
    }

    /**
     * 读取本地目录的方法
     * @param context 上下文
     * @param bookName 书名
     */
    public static CatalogItem loadLocalCatalog(Context context, String bookName) {
        CatalogItem catalogItem = new CatalogItem();
        catalogItem.setBookName(bookName);
        String catalogFileName = "0000目录.txt";
        try {
            DocumentFile catalogFile = getDocumentFile(context, bookName, catalogFileName);
            InputStream ins = context.getContentResolver().openInputStream(catalogFile.getUri());
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));
            String line;
            while ((line = br.readLine()) != null) {
//                Log.d(Tag, "line:" + line);
                if (line.contains("bookCode")) {
                    catalogItem.setBookCode(line.replace("bookCode:", ""));
                } else if (line.contains("platformName")) {
                    catalogItem.setPlatformName(line.replace("platformName:", ""));
                } else {
                    String[] l = line.split("/x/");
                    catalogItem.addChapterTitle(l[1]);
                    catalogItem.addChapterCode(l[2]);
                }
            }
            return catalogItem;
        } catch (IOException e) {
            Log.e(Tag, e.getMessage());
            return null;
        }
    }

    /**
     * 读取本地章节的方法
     * @param context 上下文
     * @param bookName 书名
     * @param chapterNum 第几章
     * @param chapterTitle 章节标题
     */
    static ChapterItem loadLocalChapter(Context context, String bookName, int chapterNum, String chapterTitle) {
        ChapterItem chapterItem = new ChapterItem();
        chapterItem.setBookName(bookName);
        chapterItem.setChapterNum(chapterNum);
        String formatNum = figuresNum(bookName, chapterNum);
        if (formatNum.equals("-1") || formatNum.equals("-2")) {
            chapterItem.setTitle("");
            chapterItem.addParagraph("");
            return chapterItem;
        }
        String chapterFileName = formatNum + chapterTitle + ".txt";
        chapterItem.setIsLocal(1);
        try {
            DocumentFile chapterFile = getDocumentFile(context, bookName, chapterFileName);
            InputStream ins = context.getContentResolver().openInputStream(chapterFile.getUri());
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));
            String line;
            while ((line = br.readLine()) != null) {
                chapterItem.addParagraph(line);
            }
            chapterItem.setTitle(chapterTitle);
            return chapterItem;
        } catch (IOException e) {
            chapterItem.setTitle("");
            chapterItem.addParagraph("");
            return chapterItem;
        }
    }

    /**
     * 从本地assets文件夹获取内容
     * @param platformItem 平台列表
     * @param bookName 书籍名称
     * @param bookCode 书籍代码
     * @param chapterCode 章节代码
     */
    private void loadLocalAssets(Context context, boolean isManual, PlatformItem platformItem,
                                 String bookName, String bookCode, int chapterNum, String chapterCode) {
        String fileName = null;
        fileName = (bookCode == null)? bookName : bookCode;
        fileName = (chapterCode == null)? fileName : chapterCode;
        fileName = fileName + ".html";
        Log.d(Tag, "fileName:" + fileName);
        String htmlContent;
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line).append("\r\n");
            }
            htmlContent = stringBuilder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CatalogItem catalogItem = CatalogGetter.htmlToCatalog(platformItem, htmlContent);
        if (catalogItem != null && !catalogItem.getChapterCodeList().isEmpty()) {
            newBook(context, catalogItem);
        }
    }

    /**
     * 读取本地封面的方法
     * @param context  上下文
     * @param imageUrl 封面链接
     */
    static Bitmap loadLocalCover(Context context, String imageUrl) {
        String filename = String.valueOf(imageUrl.hashCode());
        File imageFile = new File(context.getCacheDir(), filename);
        if (!imageFile.exists()) return null;
        return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
    }

    /**
     * 格式化数字，在前面补零，使之成为指定位数的数字
     * @param bookName 书名
     * @param num 需要在前面补零的数字
     * @return -1说明有目录文件但是没有有用的目录内容，-2说明没有目录文件
     */
    static String figuresNum(String bookName, int num) {
//        List<CatalogItem> catalogList = loadLocalCatalog(bookName);
//        if (catalogList != null && !catalogList.isEmpty()) {
//            String s = catalogList.get(0).getChapterTitle();
//            if (s != null && !s.isEmpty()) {
//                int figures = String.valueOf(catalogList.size()).length();
                int figures = 4;
                Log.d(Tag, "chapterNum:" + num);
                String formatStr = String.format("%0" + figures + "d", num);
                return formatStr;
//            } else {
//                return -1;
//            }
//        } else {
//            return -2;
//        }
    }

    public boolean loadSavedTreeUri(BookItem bookItem, Context context) {
        Uri savedUri = Uri.parse(bookItem.getUriStr());
        if (savedUri != null) {
            // 检查是否仍有权限
            List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();
            for (UriPermission permission : permissions) {
                if (permission.getUri().equals(savedUri) && permission.isReadPermission()) {
                    return true;
                } else {
                    Intent permissionIntent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    context.startActivity(permissionIntent);
                }
            }
        }
        return false;
    }

    static DocumentFile getDocumentFile(Context context, String bookName, String fileName) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String uriString = sp.getString("file_authorize", "");
        if (uriString.isEmpty()) return null;
        Uri topUri = Uri.parse(uriString);
        DocumentFile topDir = DocumentFile.fromTreeUri(context, topUri);
        if (topDir == null || !topDir.exists()) return null;
        DocumentFile bookDir = topDir.findFile(bookName);
        if (bookDir == null || !bookDir.isDirectory()) {
            bookDir = topDir.createDirectory(bookName);
            if (bookDir != null) Log.d(Tag, "文件夹创建成功: " + bookDir.getUri());
        }
        DocumentFile file = bookDir.findFile(fileName);
        if (file == null || !file.isFile()) {
            file = (fileName.equals("cover.jpg"))?
                    bookDir.createFile("image/jpeg", fileName) :
                    bookDir.createFile("text/plain", fileName);
            if (file != null) Log.d(Tag, "文件创建成功");
        }
        return file;
    }
}
