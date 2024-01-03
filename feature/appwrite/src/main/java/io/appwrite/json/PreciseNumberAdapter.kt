package io.appwrite.json

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.*
import com.google.gson.stream.JsonWriter
import java.io.IOException

internal class PreciseNumberAdapter : TypeAdapter<Any?>() {

    private val delegate = Gson()
        .getAdapter(Any::class.java)

    @Throws(IOException::class)
    override fun write(out: JsonWriter?, value: Any?) {
        delegate.write(out, value)
    }

    @Throws(IOException::class)
    override fun read(input: JsonReader): Any? {
        return when (input.peek()) {
            BEGIN_ARRAY -> {
                val list = mutableListOf<Any?>()
                input.beginArray()
                while (input.hasNext()) {
                    list.add(read(input))
                }
                input.endArray()
                list
            }

            BEGIN_OBJECT -> {
                val map = mutableMapOf<String, Any?>()
                input.beginObject()
                while (input.hasNext()) {
                    map[input.nextName()] = read(input)
                }
                input.endObject()
                map
            }

            STRING -> {
                input.nextString()
            }

            NUMBER -> {
                val numberString = input.nextString()
                if (numberString.indexOf('.') != -1) {
                    numberString.toDouble()
                } else {
                    numberString.toLong()
                }
            }

            BOOLEAN -> {
                input.nextBoolean()
            }

            NULL -> {
                input.nextNull()
                null
            }

            else -> {
                throw IllegalStateException()
            }
        }
    }
}
