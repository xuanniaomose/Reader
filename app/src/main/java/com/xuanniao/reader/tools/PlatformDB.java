package com.xuanniao.reader.tools;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.xuanniao.reader.ui.book.PlatformItem;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class PlatformDB extends SQLiteOpenHelper {
    private static final String Tag = "PlatformDB";
    private static String dbName;
    public static final int DB_VERSION = 1;
    SQLiteDatabase db;

    /**
     * 声明一个构造方法来调用父类的构造方法
     * @param context 上下文，必须传入
     * @param db_name DB名字，这里也可以填入完整文件路径来操作非app目录下的db
     */
    public PlatformDB(Context context, String db_name) {
        super(context, db_name, null, DB_VERSION);
        db = getWritableDatabase();
        dbName = db_name;
    }

    private static PlatformDB instance;
    /**
     * @param context 传入上下文
     * @return 返回DBHelper对象
     */
    public synchronized static PlatformDB getInstance(Context context, String DATABASE_NAME){
        if (instance == null){
            instance = new PlatformDB(context, DATABASE_NAME);
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
                "platformName VARCHAR NOT NULL," +
                "platformUrl VARCHAR," +
                "searchPath VARCHAR," +
                "platformCookie VARCHAR," +
                "charsetName VARCHAR," +

                "resultPage VARCHAR," +
                "resultError VARCHAR," +
                "resultPageFormat VARCHAR," +

                "catalogPage VARCHAR," +
                "catalogError VARCHAR," +
                "catalogPageFormat VARCHAR," +

                "chapterPage VARCHAR," +
                "chapterError VARCHAR," +
                "chapterPageFormat VARCHAR" +
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
    public void writeItem(String tab_name, PlatformItem platformItem) {
        boolean a = judgeDBTabExist(tab_name);
        if (a) {
            // 表存在则判断项是否存在
            String platformName = platformItem.getPlatformName();
            boolean b = judgeDBTabItemExist("platformName", platformName, tab_name);
            if (b) {
                // 项存在则不写
                Log.d("写入失败","条目已在数据库中");
            } else {
                // 项不存在则写入
                insert(tab_name, platformItem);
                Log.d("写入-表存在","条目写入了表:"+tab_name);
            }
        } else {
            // 表不存在则创建后写入
            tabCreate(tab_name);
            insert(tab_name, platformItem);
            Log.d("写入-表不存在","条目写入了表:"+tab_name);
        }
    }

    // 更新行,返回更新了多少行
    public long updateItem(String tabName, int id, String field, String s) {
        SQLiteDatabase dbw = getWritableDatabase();
        ContentValues values = new ContentValues();
        boolean a = judgeDBTabExist(tabName);
        if (a) {
            // 表存在则判断项是否存在
            boolean b = judgeDBTabItemExist("ID", String.valueOf(id), tabName);
            if (b) {
                // 项存在则更新
                Log.d("写入-表存在","更新了表:"+tabName);
                values.put(field, s);
                return dbw.update(tabName, values, field + "=?", new String[]{s});
            } else {
                // 项不存在则写入
                insertOnlyOneField(tabName, field, s);
                Log.d("写入-表存在","条目写入了表:"+tabName);
                return 1;
            }
        } else {
            // 表不存在则创建后写入
            tabCreate(tabName);
            insertOnlyOneField(tabName, field, s);
            Log.d("写入-表不存在","条目写入了表:"+tabName);
            return 1;
        }
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
        } catch (Exception e) {
            // TODO: 传出错误原因
        }
        return result;
    }

    public boolean judgeDBTabItemExist(String field, String s, String tab_name) {
        SQLiteDatabase dbw = getWritableDatabase();
        //创建游标对象
        Cursor cursor = dbw.query(tab_name, new String[]{field}, field + "=?",
                new String[]{s}, null, null, null);
        //利用游标遍历所有数据对象
        while(cursor.moveToNext()){
            @SuppressLint("Range")
            String platform_db = cursor.getString(cursor.getColumnIndex(field));
            if (platform_db.equals(s)) {
                return true;
            }
            Log.i("db out field: ", platform_db);
        }
        cursor.close();
        return false;
    }

    public void insert(String tab_name, PlatformItem platformItem) {
        Log.d("insert","开始写入");
        SQLiteDatabase dbw = getWritableDatabase();
        // 创建存放数据的ContentValues对象
        ContentValues values = cursorPutMove(platformItem);
        //数据库执行插入命令
        long insert = dbw.insert(tab_name, "platformUrl", values);
        Log.d("insert", String.valueOf(insert));
    }

    public void insertOnlyOneField(String tabName, String field, String s) {
        SQLiteDatabase dbw = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(field, s);
        //数据库执行插入命令
        long insert = dbw.insert(tabName, "platformUrl", values);
        Log.d("insert", String.valueOf(insert));
    }

    // 删除行
    public long itemDeleteByID(String tab_name, String[] platformIndex_Array) {
        // 第一个是表名
        // 第二个删除的条件  删除所有，可以用 1=1 为条件
        // 第三个于条件相匹配的值
        SQLiteDatabase dbw = getWritableDatabase();
        long r = dbw.delete(tab_name, "ID=?", platformIndex_Array);
        if (r > 0) {
            // 删除一项后，之后的每项自增字段（ID）都要-1
            String sql_updateID = "UPDATE " + tab_name + " SET 'ID'=(ID-1) WHERE ID>"
                    + platformIndex_Array[platformIndex_Array.length - 1] + ";";
            dbw.execSQL(sql_updateID);
            // 把自增字段的开始序号设置为最后一项+1，这样后面输入的每一个条目才能被赋予正确的序号
            List<Integer> ints = queryAllToIndex(tab_name);
            int i = ints.get(ints.size() - 1);
            String sql_seq = "update sqlite_sequence set seq=" + i + " where name='" + tab_name + "';";
            dbw.execSQL(sql_seq);
        }
        return r;
    }

    /** 查询所有行数据 */
    public List<PlatformItem> queryAll(String tabName){
        List<PlatformItem> l;
        if (judgeDBTabExist(tabName)) {
            Cursor cursor = db.query(tabName,null,null,
                    null,null,null,null);
            l = cursorSetMove(cursor);
            cursor.close();
            return l;
        } else {
            tabCreate(tabName);
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

    /** 根据唯一值字段查询：返回歌曲条目 */
    public PlatformItem queryByFieldItem(String tab_name, String field, String platformField){
        String[] platformFieldArray = new String[]{platformField};
        Cursor cursor = db.query(tab_name,null,field+"=?", platformFieldArray,null,null,null);
        List<PlatformItem> l = cursorSetMove(cursor);
        cursor.close();
        return (!l.isEmpty())? l.get(0) : null;
    }

    /** 根据唯一值字段查询：返回歌曲条目 */
    public int queryByFieldItemToIndex(String tab_name, String field, String platformField){
        String[] platformFieldArray = new String[]{platformField};
        Cursor cursor = db.query(tab_name,null,field+"=?", platformFieldArray,null,null,null);
        List<Integer> il = new ArrayList<>();
        while (cursor.moveToNext()){
            int i = cursor.getInt(0);
            il.add(i);
        }
        cursor.close();
        return (!il.isEmpty())? il.get(0) : -1;
    }

    /** 根据字段查询：返回歌曲条目列表 */
    public List<PlatformItem> queryByField(String tab_name, String field, String[] platformField){
        Cursor cursor = db.query(tab_name,null,field+"=?", platformField,null,null,null);
        List<PlatformItem> l = cursorSetMove(cursor);
        cursor.close();
        return l;
    }

    /** 根据字段查询：返回歌曲序号列表 */
    public List<Integer> queryByFieldToIndex(String tab_name, String field, String[] platformField){
        Cursor cursor = db.query(tab_name,null,field+"=?", platformField,null,null,null);
        List<Integer> l = new ArrayList<>();
        while (cursor.moveToNext()){
            int i = cursor.getInt(0);
            l.add(i);
        }
        cursor.close();
        return l;
    }

    // 根据字段查询：返回指定字段的值的列表
    public List<Integer> queryFieldToList(String tab_name, String field) {
        SQLiteDatabase dbw = getWritableDatabase();
        String sql = "SELECT " + field + " FROM " + tab_name +";";
        Cursor cursor = dbw.rawQuery(sql,null);
        List<Integer> l = new ArrayList<>();
        while (cursor.moveToNext()) {
            int field_result = cursor.getInt(0);
            l.add(field_result);
        }
        cursor.close();
        return l;
    }

    // 写入数据库准备：put遍历游标
    private ContentValues cursorPutMove(PlatformItem platformItem) {
        ContentValues values = new ContentValues();
        values.put("ID", platformItem.getID());
        values.put("platformName", platformItem.getPlatformName());
        values.put("platformUrl", platformItem.getPlatformUrl());
        values.put("searchPath", platformItem.getSearchPath());
        values.put("platformCookie", platformItem.getPlatformCookie());
        values.put("charsetName", platformItem.getCharsetName());

        values.put("resultPage", String.join(",", platformItem.getResultPage()));
        values.put("resultError", platformItem.getResultError());
        values.put("resultPageFormat", platformItem.getResultPageFormat());

        values.put("catalogPage", String.join(",", platformItem.getCatalogPage()));
        values.put("catalogError", platformItem.getCatalogError());
        values.put("catalogPageFormat", platformItem.getCatalogPageFormat());

        values.put("chapterPage", String.join(",", platformItem.getChapterPage()));
        values.put("chapterError", platformItem.getChapterError());
        values.put("chapterPageFormat", platformItem.getChapterPageFormat());


        return values;
    }

    // 读取数据库准备：set遍历游标
    private List<PlatformItem> cursorSetMove(Cursor cursor) {
        List<PlatformItem> list = new ArrayList<>();
        while (cursor.moveToNext()){
            PlatformItem platform = new PlatformItem();
            platform.setID(Integer.parseInt(cursor.getString(0)));
            platform.setPlatformName(cursor.getString(1));
            platform.setPlatformUrl(cursor.getString(2));
            platform.setSearchPath(cursor.getString(3));
            platform.setPlatformCookie(cursor.getString(4));
            platform.setCharsetName(cursor.getString(5));

            platform.setResultPage(cursor.getString(6).split(","));
            platform.setResultError(cursor.getString(7));
            platform.setResultPageFormat(cursor.getString(8));

            platform.setCatalogPage(cursor.getString(9).split(","));
            platform.setCatalogError(cursor.getString(10));
            platform.setCatalogPageFormat(cursor.getString(11));

            platform.setChapterPage(cursor.getString(12).split(","));
            platform.setChapterError(cursor.getString(13));
            platform.setChapterPageFormat(cursor.getString(14));
            list.add(platform);
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
