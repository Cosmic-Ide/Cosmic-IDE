package com.pranav.lib_android.incremental

import com.pranav.lib_android.util.FileUtil

import org.json.JSONObject

import java.io.File

class Indexer(fileName: String) {
    
    private var json: JSONObject
    
    private var filePath: String
    
    init {
        filePath = FileUtil.getCacheDir() + fileName + ".json" // append json file extension
        val index = File(filePath).readText();
        json = JSONObject(index!!)
    }
    
    fun putString(key: String, value: String) {
        json.put(key, value)
    }
    
    fun putLong(key: String, value: Long) {
        json.put(key, value)
    }
    
    fun notHas(key: String): Boolean {
        return !json.has(key)
    }
    
    fun getString(key: String) = json.getString(key)
    
    fun getLong(key: String) = json.getLong(key)
    
    override fun toString(): String {
        return json.toString(4)
    }
    
    fun flush() {
        FileUtil.writeFile(filePath, toString())
    }
}