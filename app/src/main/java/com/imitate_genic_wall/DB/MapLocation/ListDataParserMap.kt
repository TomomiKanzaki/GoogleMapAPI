package com.imitate_genic_wall.DB.MapLocation

import android.content.Context
import com.imitate_genic_wall.DB.DatabaseMap
import org.jetbrains.anko.db.MapRowParser

class ListDataParserMap(context: Context): MapRowParser<ListDataMap> {

    val databaseMap = DatabaseMap(context)

    override fun parseRow(columns: Map<String, Any?>): ListDataMap {
        return ListDataMap(columns[databaseMap.ID_LOCATION] as Long,
                columns[databaseMap.TIME] as String?,
                columns[databaseMap.NAME_LOCATION] as String?,
                columns[databaseMap.LATITUDE] as Double,
                columns[databaseMap.LONGITUDE] as Double)
    }

}