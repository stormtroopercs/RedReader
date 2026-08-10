/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.quantumbadger.redreader.io

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.quantumbadger.redreader.common.UnexpectedInternalStateException
import org.quantumbadger.redreader.io.RawObjectDB
import org.quantumbadger.redreader.io.WritableObject.WritableField
import org.quantumbadger.redreader.io.WritableObject.WritableObjectKey
import org.quantumbadger.redreader.io.WritableObject.WritableObjectTimestamp
import org.quantumbadger.redreader.io.WritableObject.WritableObjectVersion
import java.lang.Boolean
import java.lang.Long
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.util.LinkedList
import java.util.Locale
import kotlin.Array
import kotlin.Exception
import kotlin.Int
import kotlin.RuntimeException
import kotlin.String
import kotlin.TODO
import kotlin.Throws
import kotlin.arrayOf
import kotlin.arrayOfNulls
import kotlin.compareTo
import kotlin.toString

class RawObjectDB<K, E : WritableObject<K?>?>(
    context: Context,
    dbFilename: String?,
    clazz: Class<E?>
) : SQLiteOpenHelper() {
    private val clazz: Class<E?>

    private val fields: Array<Field>
    private val fieldNames: Array<String?>

    init {
        super(
            context.getApplicationContext(),
            dbFilename,
            null,
            TODO("Cannot convert element")
        )<E> RawObjectDB . Companion . getDbVersion < E ? > (clazz)

        this.clazz = clazz

        val fields = LinkedList<Field?>()
        for (field in clazz.getDeclaredFields()) {
            if ((field.getModifiers() and Modifier.TRANSIENT) == 0 && !field.isAnnotationPresent(
                    WritableObjectKey::class.java
                ) && !field.isAnnotationPresent(WritableObjectTimestamp::class.java) && field.isAnnotationPresent(
                    WritableField::class.java
                )
            ) {
                field.setAccessible(true)
                fields.add(field)
            }
        }

        this.fields = fields.toTypedArray<Field?>()

        fieldNames = arrayOfNulls<String>(this.fields.size + 2)
        for (i in this.fields.indices) {
            fieldNames[i] = this.fields[i].getName()
        }
        fieldNames[this.fields.size] = FIELD_ID
        fieldNames[this.fields.size + 1] = FIELD_TIMESTAMP
    }

    private fun getFieldTypeString(fieldType: Class<*>?): String {
        if (fieldType == Int::class.java || fieldType == Long::class.java || fieldType == Integer.TYPE || fieldType == Long.TYPE) {
            return " INTEGER"
        } else if (fieldType == Boolean::class.java
            || fieldType == Boolean.TYPE
        ) {
            return " INTEGER"
        } else {
            return " TEXT"
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val query = StringBuilder("CREATE TABLE ")
        query.append(TABLE_NAME)
        query.append('(')
        query.append(FIELD_ID)
        query.append(" TEXT PRIMARY KEY ON CONFLICT REPLACE,")
        query.append(FIELD_TIMESTAMP)
        query.append(" INTEGER")

        for (field in fields) {
            query.append(',')
            query.append(field.getName())
            query.append(getFieldTypeString(field.getType()))
        }

        query.append(')')

        Log.i("RawObjectDB", "Query string: " + query.toString())

        db.execSQL(query.toString())
    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
    }

    @get:Synchronized
    val all: MutableCollection<E?>
        get() {
            getReadableDatabase().use { db ->
                try {
                    db.query(
                        TABLE_NAME,
                        fieldNames,
                        null,
                        null,
                        null,
                        null,
                        null
                    ).use { cursor ->
                        val result = LinkedList<E?>()
                        while (cursor.moveToNext()) {
                            result.add(readFromCursor(cursor))
                        }
                        return result
                    }
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
        }

    @Synchronized
    fun getById(id: K?): E? {
        val queryResult = getByField(FIELD_ID, id.toString())
        if (queryResult.size != 1) {
            return null
        } else {
            return queryResult.get(0)
        }
    }

    @Synchronized
    fun getByField(field: String, value: String?): ArrayList<E?> {
        getReadableDatabase().use { db ->
            try {
                db.query(
                    TABLE_NAME,
                    fieldNames,
                    String.format(Locale.US, "%s=?", field),
                    arrayOf<String?>(value),
                    null,
                    null,
                    null
                ).use { cursor ->
                    val result = ArrayList<E?>(cursor.getCount())
                    while (cursor.moveToNext()) {
                        result.add(readFromCursor(cursor))
                    }
                    return result
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    @Throws(
        IllegalAccessException::class,
        InstantiationException::class,
        InvocationTargetException::class
    )
    private fun readFromCursor(cursor: Cursor): E? {
        val obj: E?
        try {
            val constructor = clazz.getConstructor(WritableObject.CreationData::class.java)
            val id = cursor.getString(fields.size)
            val timestamp = cursor.getLong(fields.size + 1)
            obj = constructor.newInstance(WritableObject.CreationData(id, timestamp))
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        }

        for (i in fields.indices) {
            val field = fields[i]
            val fieldType = field.getType()

            if (fieldType == String::class.java) {
                field.set(obj, if (cursor.isNull(i)) null else cursor.getString(i))
            } else if (fieldType == Int::class.java) {
                field.set(obj, if (cursor.isNull(i)) null else cursor.getInt(i))
            } else if (fieldType == Integer.TYPE) {
                field.setInt(obj, cursor.getInt(i))
            } else if (fieldType == kotlin.Long::class.java) {
                field.set(obj, if (cursor.isNull(i)) null else cursor.getLong(i))
            } else if (fieldType == Long.TYPE) {
                field.setLong(obj, cursor.getLong(i))
            } else if (fieldType == kotlin.Boolean::class.java) {
                field.set(obj, if (cursor.isNull(i)) null else cursor.getInt(i) != 0)
            } else if (fieldType == Boolean.TYPE) {
                field.setBoolean(obj, cursor.getInt(i) != 0)
            } else if (fieldType == WritableHashSet::class.java) {
                field.set(
                    obj,
                    if (cursor.isNull(i))
                        null
                    else
                        WritableHashSet.Companion.unserializeWithMetadata(
                            cursor.getString(
                                i
                            )
                        )
                )
            } else {
                throw UnexpectedInternalStateException(
                    "Invalid readFromCursor field type "
                            + fieldType.javaClass.getCanonicalName()
                )
            }
        }

        return obj
    }

    @Synchronized
    fun put(`object`: E?) {
        val db = getWritableDatabase()

        try {
            val values = ContentValues(fields.size + 1)
            val result = db.insertOrThrow(
                TABLE_NAME,
                null,
                toContentValues(`object`, values)
            )

            if (result < 0) {
                throw RuntimeException("Database write failed")
            }
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } finally {
            db.close()
        }
    }

    @Synchronized
    fun putAll(objects: MutableCollection<E?>) {
        val db = getWritableDatabase()

        try {
            val values = ContentValues(fields.size + 1)

            for (`object` in objects) {
                val result = db.insertOrThrow(
                    TABLE_NAME,
                    null,
                    toContentValues(`object`, values)
                )
                if (result < 0) {
                    throw RuntimeException("Bulk database write failed")
                }
            }
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } finally {
            db.close()
        }
    }

    @Throws(IllegalAccessException::class)
    private fun toContentValues(obj: E?, result: ContentValues): ContentValues {
        result.put(FIELD_ID, obj!!.getKey().toString())
        result.put(FIELD_TIMESTAMP, obj.getTimestamp().toUtcMs())

        for (i in fields.indices) {
            val field = fields[i]
            val fieldType = field.getType()

            if (fieldType == String::class.java) {
                result.put(fieldNames[i], field.get(obj) as String?)
            } else if (fieldType == Int::class.java) {
                result.put(fieldNames[i], field.get(obj) as Int?)
            } else if (fieldType == Integer.TYPE) {
                result.put(fieldNames[i], field.getInt(obj))
            } else if (fieldType == kotlin.Long::class.java) {
                result.put(fieldNames[i], field.get(obj) as kotlin.Long?)
            } else if (fieldType == Long.TYPE) {
                result.put(fieldNames[i], field.getLong(obj))
            } else if (fieldType == kotlin.Boolean::class.java) {
                val `val` = field.get(obj) as kotlin.Boolean?
                result.put(fieldNames[i], if (`val` == null) null else (if (`val`) 1 else 0))
            } else if (fieldType == Boolean.TYPE) {
                result.put(fieldNames[i], if (field.getBoolean(obj)) 1 else 0)
            } else if (fieldType == WritableHashSet::class.java) {
                result.put(
                    fieldNames[i],
                    (field.get(obj) as WritableHashSet).serializeWithMetadata()
                )
            } else {
                throw UnexpectedInternalStateException()
            }
        }

        return result
    }

    companion object {
        private const val TABLE_NAME = "objects"
        private const val FIELD_ID = "RawObjectDB_id"
        private const val FIELD_TIMESTAMP = "RawObjectDB_timestamp"

        private fun <E> getDbVersion(clazz: Class<E?>): Int {
            for (field in clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(WritableObjectVersion::class.java)) {
                    field.setAccessible(true)
                    try {
                        return field.getInt(null)
                    } catch (e: IllegalAccessException) {
                        throw RuntimeException(e)
                    }
                }
            }
            throw UnexpectedInternalStateException("Writable object has no DB version")
        }
    }
}
