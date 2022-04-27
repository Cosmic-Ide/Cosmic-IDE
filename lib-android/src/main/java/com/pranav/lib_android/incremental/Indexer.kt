package com.pranav.lib_android.incremental

import com.pranav.lib_android.util.FileUtil

import org.json.JSONObject

import java.io.File

class Indexer(fileName: String) {
    
    private var json: JSONObject
    
    private var filePath: String
    
    init {
        filePath = FileUtil.getCacheDir() + fileName + ".json" // append json file extension
        val indexFile = File(filePath)
        if (!indexFile.exists()) indexFile.writeText("")
        val index = indexFile.readText();
        json = JSONObject(index!!)
    }
    
    fun put(key: String, value: String) {
        json.put(key, value)
    }
    
    fun put(key: String, value: Long) {
        json.put(key, value)
    }
    
    fun notHas(key: String): Boolean {
        return !json.has(key)
    }
    
    fun getString(key: String) = json.getString(key)
    
    fun getLong(key: String) = json.getLong(key)
    
    override fun toString() = json.toString(4)
    
    fun flush() {
        File(indexFile).writeText(toString())
    }
}