package com.imitate_genic_wall.DB

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

class DatabaseMap(context: Context) : ManagedSQLiteOpenHelper(context, "DataBase", null, 1){

    val TAG = DatabaseMap::class.java.simpleName

    //tableName
    val TABLE_NAME_LOCATION = "tableNameLocation"
    //column
    val ID_LOCATION = "Id"
    val TIME = "time"
    val NAME_LOCATION = "NameLocation"
    val LATITUDE = "latitude"
    val LONGITUDE = "longitude"

    override fun onCreate(db: SQLiteDatabase?) {
        db?.createTable(TABLE_NAME_LOCATION, true,
                ID_LOCATION to INTEGER + PRIMARY_KEY,
                TIME to TEXT,
                NAME_LOCATION to TEXT,
                LATITUDE to INTEGER,
                LONGITUDE to INTEGER)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

}