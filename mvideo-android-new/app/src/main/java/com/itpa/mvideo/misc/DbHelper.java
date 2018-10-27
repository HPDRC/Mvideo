package com.itpa.mvideo.misc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "mvideo";

    private static final String generalTableName = "general";

    public static final String keyBusIndex = "bus_index";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table general(name text primary key not null, value text not null);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS general;");
        onCreate(db);
    }

    public static String getFromGeneral(SQLiteDatabase db, String name) {
        Cursor cursor = db.query(generalTableName, new String[]{"value"}, "name = ?", new String[]{name}, null, null, null);
        if (!cursor.moveToFirst())
            return null;
        String result = cursor.getString(0);
        cursor.close();
        return result;
    }

    public static void deleteFromGeneral(SQLiteDatabase db, String name) {
        db.delete(generalTableName, "name = ?", new String[]{name});
    }

    public static void insertOrUpdateGeneral(SQLiteDatabase db, String name, String value) {
        try {
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("value", value);
            db.insertOrThrow(generalTableName, null, values);
        } catch (Exception ex) {
            ContentValues values = new ContentValues();
            values.put("value", value);
            db.update(generalTableName, values, "name = ?", new String[]{name});
        }
    }
}
