package com.xuanniao.reader.item;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.xuanniao.reader.tools.Constants;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class BookDB extends SQLiteOpenHelper {
    private static final String Tag = "BookDB";
    private static String dbName;
    public static final int DB_VERSION = 1;
    SQLiteDatabase db;

    /**
     * 声明一个构造方法来调用父类的构造方法
     * @param context 上下文，必须传入
     * @param db_name DB名字，这里也可以填入完整文件路径来操作非app目录下的db
     */
    public BookDB(Context context, String db_name) {
        super(context, db_name, null, DB_VERSION);
        db = getWritableDatabase();
        dbName = db_name;
    }

    private static BookDB instance;
    /**
     * @param context 传入上下文
     * @return 返回DBHelper对象
     */
    public synchronized static BookDB getInstance(Context context, String DATABASE_NAME){
        if (instance == null){
            instance = new BookDB(context, DATABASE_NAME);
        }
        return instance;
    }

    //继承SQLiteOpenHelper以后，需要实现的两个方法onCreate和onUpgrade
    //onCreate   创建数据库
    //onUpgrade  主要用于版本更新
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(Tag, "dbName:"+dbName);
    }

    // 数据库版本更新的时候，在onUpgrade执行来进行更新
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // 给表增加一个列,一次只能增加一个字段
        String sql = "ALTER TABLE tab_name ADD COLUMN a VARCHAR;";
        sqLiteDatabase.execSQL(sql);
    }

    // 创建表
    public void tabCreate(String tab_name) {
        //创建表的sql文
        String sql = "CREATE TABLE IF NOT EXISTS "+ tab_name +" (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "bookName VARCHAR NOT NULL," +
                "uriStr VARCHAR," +
                "author VARCHAR," +
                "synopsis VARCHAR," +
                "renewTime INTEGER," +
                "wordCount INTEGER," +
                "coverUrl VARCHAR," +
                "chapterRead VARCHAR," +
                "chapterSaved VARCHAR," +
                "chapterTotal INTEGER," +
                "bookMark INTEGER," +

                "publisher VARCHAR," +
                "classify VARCHAR," +
                "status VARCHAR," +
                "platformName VARCHAR NOT NULL," +
                "bookCode VARCHAR," +
                "mimeType VARCHAR," +
                "volumeName VARCHAR," +
                "volumeIndex INTEGER," +
                "publishDate INTEGER," +
                "downloadTime INTEGER" +
                ");";
        //执行sql文
        db.execSQL(sql);
    }

    // 删除表
    public void tabDelete(String tab_name) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("drop table '" + tab_name + "';");
    }

    // 创建新行
    public void writeItem(String tab_name, BookItem bookItem) {
        boolean a = judgeDBTabExist(tab_name);
        if (a) {
            // 表存在则判断项是否存在
            String book_name = bookItem.getBookName();
            boolean b = judgeDBTabItemExist(book_name, tab_name);
            if (b) {
                // 项存在则不写
                Log.d("写入失败","条目已在数据库中");
            } else {
                // 项不存在则写入
                insert(tab_name, bookItem);
                Log.d("写入-表存在","条目写入了表:"+tab_name);
            }
        } else {
            // 表不存在则创建后写入
            tabCreate(tab_name);
            insert(tab_name, bookItem);
            Log.d("写入-表不存在","条目写入了表:"+tab_name);
        }
    }

    // 更新行,返回更新了多少行
    public long updateItem(String tab_name, String bookCode, String field, List<Integer> list, String s, Integer i) {
        SQLiteDatabase dbw = getWritableDatabase();
        ContentValues values = new ContentValues();
        if (list != null) {
            values.put(field, listToString(list));
        } else if (s != null) {
            values.put(field, s);
        } else {
            values.put(field, i);
        }
        return dbw.update(tab_name, values, "bookCode=?", new String[]{bookCode});
    }

    // 更新行,返回更新了多少行
    public long renewLine(String tabName, String filed, String[] oldArray, String[] newArray){
        if (oldArray.length != newArray.length) {
            return -1;
        }
        SQLiteDatabase dbw = getWritableDatabase();
        List<BookItem> oldItemList = queryByField(tabName, filed, oldArray);
        Log.d(Tag, "oldItemList:" + oldItemList);
        if (oldItemList.isEmpty()) return 0L;
        if (Objects.equals(filed, "ID")) {
            for (int i = 0; i < oldArray.length; i++) {
                BookItem item = oldItemList.get(i);
                ContentValues values = cursorPutMove(item);
                values.put("ID", newArray[i]);
                dbw.update(tabName, values,filed + "=?", new String[]{oldArray[i]});
            }
        } else {
            for (int i = 0; i < oldArray.length; i++) {
                BookItem item = oldItemList.get(i);
                String setMethodString = "set" + filed.substring(0, 1).toUpperCase()
                        + filed.substring(1);//必须注意 substring 是左开右闭的
                Log.d(Tag, "set方法名:" + setMethodString);
                try {
                    item.getClass().getMethod(setMethodString, String.class).invoke(item, newArray[i]);
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                ContentValues values = cursorPutMove(item);
                dbw.update(tabName, values, filed + "=?", new String[]{oldArray[i]});
            }
        }
        return oldArray.length;
    }

    public boolean judgeDBTabExist(String tableName){
        boolean result = false;
        if(tableName == null){
            return false;
        }
        try {
            String sql = "select count(*) as c from Sqlite_master  " +
                    "where type ='table' and name ='"+tableName.trim()+"' ";
            Cursor cursor = db.rawQuery(sql, null);
            if(cursor.moveToNext()){
                int count = cursor.getInt(0);
                if(count>0){
                    result = true;
                }
            }
            cursor.close();
        } catch (Exception e) {
            // TODO: 传出错误原因
        }
        return result;
    }

    public boolean judgeDBTabItemExist(String book, String tab_name) {
        SQLiteDatabase dbw = getWritableDatabase();
        //创建游标对象
        Cursor cursor = dbw.query(tab_name, new String[]{"bookName"}, "bookName=?",
                new String[]{book}, null, null, null);
        //利用游标遍历所有数据对象
        while(cursor.moveToNext()){
            @SuppressLint("Range")
            String book_db = cursor.getString(cursor.getColumnIndex("bookName"));
            if (book_db.equals(book)) {
                return true;
            }
            Log.i("db out bookName: ",book_db);
        }
        cursor.close();
        return false;
    }

    public void insert(String tab_name, BookItem bookItem) {
        Log.d("insert","开始写入");
        SQLiteDatabase dbw = getWritableDatabase();
        // 创建存放数据的ContentValues对象
        ContentValues values = cursorPutMove(bookItem);
        //数据库执行插入命令
        long insert = dbw.insert(tab_name, "bookName", values);
        Log.d("insert", String.valueOf(insert));
    }

    // 删除行
    public long itemDeleteByID(String tabName, String[] bookIndex_Array) {
        SQLiteDatabase dbw = getWritableDatabase();
        int rowCount = rowCount(tabName);
        // 进行删除（第一个是表名
        // 第二个删除的条件  删除所有，可以用 1=1 作为条件
        // 第三个于条件相匹配的值）
        Log.d(Tag, "array:" + Arrays.toString(bookIndex_Array));
        long deleteNum = dbw.delete(tabName, "ID=?", bookIndex_Array);
        // 获取剩余行的ID，和ID要提前的步数
        List<Integer> oldIDList = new ArrayList<>();
        List<Integer> forwardList = new ArrayList<>();
        int forward = 0;
        // sql表的默认id是从1开始的
        List<String> deleteIDList = new ArrayList<>();
        Collections.addAll(deleteIDList, bookIndex_Array);
        for (int i = 1; i <= rowCount; i++) {
            if (deleteIDList.contains(String.valueOf(i))) {
                forward ++;
                continue;
            }
            oldIDList.add(i);
            forwardList.add(forward);
        }
        String[] oldIDArray = new String[oldIDList.size()];
        String[] newIDArray = new String[oldIDList.size()];
        for (int i = 0; i < oldIDList.size(); i++) {
            int oldID = oldIDList.get(i);
            oldIDArray[i] = String.valueOf(oldID);
            int f = forwardList.get(i);
            int newID = oldID - f;
            newIDArray[i] = String.valueOf(newID);
        }
        Log.d(Tag, "oldIDArray:" + Arrays.toString(oldIDArray));
        Log.d(Tag, "newIDArray:" + Arrays.toString(newIDArray));

        // 对剩余行进行更新
        long renewNum = renewLine(tabName, "ID", oldIDArray, newIDArray);
        if (renewNum + deleteNum == rowCount) {
            return deleteNum;
        } else {
            return -1;
        }
    }

    // 查询行数
    public int rowCount(String tab_name) {
        int count = -1;
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM " + tab_name;
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
            Log.d(Tag, "行数为：" + count);
        }
        cursor.close();
        return count;
    }

    /** 查询所有行数据 */
    public List<BookItem> queryAll(String tab_name){
        List<BookItem> l;
        if (judgeDBTabExist(tab_name)) {
            Cursor cursor = db.query(tab_name,null,null,
                    null,null,null,null);
            l = cursorSetMove(cursor);
            cursor.close();
            return l;
        } else {
            tabCreate(tab_name);
            return null;
        }
    }

    /** 查询所有行数据的序号 */
    public List<Integer> queryAllToIndex(String tab_name){
        Cursor cursor = db.query(tab_name,null,null,null,null,null,null);
        List<Integer> l = new ArrayList<>();
        while (cursor.moveToNext()){
            int i = cursor.getInt(0);
            l.add(i);
        }
        cursor.close();
        return l;
    }

    /** 根据唯一值字段查询：返回条目 */
    public BookItem queryByFieldItem(String tab_name, String field, String bookField){
        String[] bookFieldArray = new String[]{bookField};
        Cursor cursor = db.query(tab_name,null,field+"=?", bookFieldArray,null,null,null);
        List<BookItem> l = cursorSetMove(cursor);
        cursor.close();
        return (!l.isEmpty())? l.get(0) : null;
    }

    /** 根据唯一值字段查询：返回条目 */
    public int queryByFieldItemToIndex(String tab_name, String field, String bookField){
        String[] bookFieldArray = new String[]{bookField};
        Cursor cursor = db.query(tab_name,null,field+"=?", bookFieldArray,null,null,null);
        List<Integer> il = new ArrayList<>();
        while (cursor.moveToNext()){
            int i = cursor.getInt(0);
            il.add(i);
        }
        cursor.close();
        return (!il.isEmpty())? il.get(0) : -1;
    }

    /** 根据字段查询：返回条目列表 */
    public List<BookItem> queryByField(String tab_name, String field, String[] bookField){
        Cursor cursor = db.query(tab_name,null,field+"=?", bookField,null,null,null);
        List<BookItem> l = cursorSetMove(cursor);
        cursor.close();
        return l;
    }

    /** 根据字段查询：返回条目序号列表 */
    public List<Integer> queryByFieldToIndex(String tab_name, String field, String[] bookField){
        Cursor cursor = db.query(tab_name,null,field+"=?", bookField,null,null,null);
        List<Integer> l = new ArrayList<>();
        while (cursor.moveToNext()){
            int i = cursor.getInt(0);
            l.add(i);
        }
        cursor.close();
        return l;
    }

    /**
     * 查询指定条目的指定字段
     * @param tab_name 表名
     * @param bookCode 书目编码
     * @param field 要查找的字段名
     * @return 该字段字符串所代表的列表
     */
    public List<Integer> queryFieldWithCode(String tab_name, String bookCode, String field) {
        SQLiteDatabase dbw = getWritableDatabase();
        String sql = "SELECT " + field + " FROM " + tab_name + " WHERE bookCode = '" + bookCode + "';";
        Cursor cursor = dbw.rawQuery(sql, null);
        if (cursor != null && cursor.moveToNext()) {
            String field_result = cursor.getString(0);
            return stringToList(field_result);
        }
        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    // 写入数据库准备：put遍历游标
    private ContentValues cursorPutMove(BookItem bookItem) {
        ContentValues values = new ContentValues();
        values.put("bookName", bookItem.getBookName());
        values.put("uriStr", bookItem.getUriStr());
        values.put("author", bookItem.getAuthor());
        values.put("synopsis", bookItem.getSynopsis());
        values.put("renewTime", bookItem.getRenewTime());
        values.put("wordCount", bookItem.getWordCount());
        values.put("coverUrl", bookItem.getCoverUrl());
        String readListStr = listToString(bookItem.getChapterReadList());
        if (readListStr != null && !readListStr.isEmpty()) {
            values.put("chapterRead", readListStr);
        }
        String savedListStr = listToString(bookItem.getChapterSavedList());
        if (savedListStr != null && !savedListStr.isEmpty()) {
            Log.d(Tag, "savedListStr: " + savedListStr);
            values.put("chapterSaved", savedListStr);
        }
        values.put("chapterTotal", bookItem.getChapterTotal());
        values.put("bookMark", bookItem.getBookMark());

        values.put("publisher", bookItem.getPublisher());
        values.put("classify", bookItem.getClassify());
        values.put("status", bookItem.getClassify());
        values.put("platformName", bookItem.getPlatformName());
        values.put("bookCode", bookItem.getBookCode());
        values.put("mimeType", bookItem.getMimeType());
        values.put("volumeName", bookItem.getVolumeName());
        values.put("volumeIndex", bookItem.getVolumeIndex());
        values.put("publishDate", bookItem.getPublishDate());
        values.put("downloadTime", bookItem.getDownloadTime());
        return values;
    }

    public String listToString(List<Integer> list) {
        if (list == null || list.isEmpty()) return null;
        list.sort(Integer::compareTo); //排序
        StringBuilder sb = new StringBuilder();
        for (Integer num : list) {
            sb.append(num).append(Constants.INTEGER_DIVIDER);
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    // 读取数据库准备：set遍历游标
    private List<BookItem> cursorSetMove(Cursor cursor) {
        List<BookItem> list = new ArrayList<>();
        while (cursor.moveToNext()){
            BookItem book = new BookItem();
            book.setBookName(cursor.getString(1));
            book.setUriStr(cursor.getString(2));
            book.setAuthor(cursor.getString(3));
            book.setSynopsis(cursor.getString(4));
            book.setRenewTime(cursor.getInt(5) * 1000L);
            book.setWordCount(cursor.getInt(6));
            book.setCoverUrl(cursor.getString(7));

            List<Integer> readList = stringToList(cursor.getString(8));
            if (readList != null && !readList.isEmpty()) {
                book.setChapterReadList(readList);
            }
            List<Integer> savedList = stringToList(cursor.getString(9));
            if (savedList != null && !savedList.isEmpty()) {
                book.setChapterSavedList(savedList);
            }
            book.setChapterTotal(cursor.getInt(10));
            book.setBookMark(cursor.getInt(11));

            book.setPublisher(cursor.getString(12));
            book.setClassify(cursor.getString(13));
            book.setStatus(cursor.getString(14));
            book.setPlatformName(cursor.getString(15));
            book.setBookCode(cursor.getString(16));
            book.setMimeType(cursor.getString(17));
            book.setVolumeName(cursor.getString(18));
            book.setVolumeIndex(cursor.getInt(19));
            book.setPublishDate(cursor.getInt(20));
            book.setDownloadTime(cursor.getInt(21));
            list.add(book);
        }
        return list;
    }

    public List<Integer> stringToList(String listString) {
        if (listString == null || listString.isEmpty()) return null;
        List<Integer> list = new ArrayList<>();
        String[] listStrArray = listString.split(Constants.INTEGER_DIVIDER);
        for (String numS : listStrArray) {
            list.add(Integer.valueOf(numS));
        }
        return list;
    }

    public ArrayList<String> tablesInDB(){
        ArrayList<String> list = new ArrayList<>();
        String sql = "select name from sqlite_master where type='table'";
        Cursor cursor = getWritableDatabase().rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                String tab_name = cursor.getString(0);
                Log.d("beforeTablesInDB",tab_name);
                if (tab_name.equals("android_metadata")) {
                    continue;
                }
                if (tab_name.equals("sqlite_sequence")) {
                    continue;
                }
                if (tab_name.equals("volume_info")) {
                    continue;
                }
                list.add(tab_name);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public ArrayList<String> tablesInSourceDB(){
        ArrayList<String> list = new ArrayList<>();
        String sql = "select name from source_db.sqlite_master where type='table'";
        Cursor cursor = getWritableDatabase().rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                String tab_name = cursor.getString(0);
                if (tab_name.equals("android_metadata")) {
                    continue;
                }
                if (tab_name.equals("sqlite_sequence")) {
                    continue;
                }
                list.add(tab_name);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void copyAll(String source_db) {
        SQLiteDatabase dbw = getWritableDatabase();
        String attach = "ATTACH DATABASE '" + source_db + "' AS 'source_db';";
        dbw.execSQL(attach);
        ArrayList<String> source_tables = tablesInSourceDB();
        for (String s_tab: source_tables) {
            String create = "CREATE TABLE " + s_tab + " AS SELECT * FROM " + s_tab + ";";
            dbw.execSQL(create);
        }
    }

    // 断开当前数据库的连接
    public void dbClose() {
        SQLiteDatabase dbw = getWritableDatabase();
        dbw.close();
    }

    /**
     * 通过sql语句在数据库中执行操作
     * @param db 数据库对象
     * @param sql sql语句
     */
    public static void execSQL(SQLiteDatabase db,String sql) {
        if (db != null) {
            if (sql != null && !sql.isEmpty()) {
                db.execSQL(sql);
            }
        }
    }

    private int getCurrentTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH", Locale.CHINA);
        Log.d(Tag, "download:" + dateFormat.format(new Date()));
//        return dateFormat.format(new Date());
        return Integer.parseInt(dateFormat.format(new Date()));
    }
}
