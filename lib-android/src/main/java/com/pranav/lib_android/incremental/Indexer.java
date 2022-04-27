package com.pranav.lib_android.incremental;

import com.pranav.lib_android.util.FileUtil;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;

public class Indexer {
    
    private JSONObject json;
    
    private String filePath;
    
    public Indexer(String fileName) throws JSONException{
        filePath = FileUtil.getCacheDir() + fileName + ".json";
        load();
    }
    
    public void load() throws JSONException {
        File indexFile = new File(filePath);
        if (!indexFile.exists()) FileUtil.writeFile(filePath, "{}");
        String index = FileUtil.readFile(indexFile);
        json = new JSONObject(index);
    }
    
    public void put(String key, String value) throws JSONException {
        json.put(key, value);
    }
    
    public void put(String key, long value) throws JSONException {
        json.put(key, value);
    }
    
    public boolean notHas(String key) throws JSONException {
        return !json.has(key);
    }
    
    public String getString(String key) throws JSONException {
        return json.getString(key);
    }
    
    public long getLong(String key) throws JSONException {
        return json.getLong(key);
    }
    
    public String asString() throws JSONException {
        json.toString(4);
    }
    
    public void flush() {
        try {
            FileUtil.writeFile(filePath, asString());
        } catch (IOException e) {}
    }
}