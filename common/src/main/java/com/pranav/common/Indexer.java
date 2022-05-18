package com.pranav.common;

import com.pranav.common.util.FileUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class Indexer {

    private JSONObject json;

    private String filePath;

    public Indexer(String fileName) throws JSONException {
        filePath = FileUtil.getCacheDir() + fileName + ".json";
        load();
    }

    public void load() throws JSONException {
        var indexFile = new File(filePath);
        try {
            if (!indexFile.exists()) {
                FileUtil.writeFile(filePath, "{}");
            }
            var index = FileUtil.readFile(indexFile);
            json = new JSONObject(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        return json.toString(4);
    }

    public void flush() {
        try {
            FileUtil.writeFile(filePath, asString());
        } catch (Exception e) {
        }
    }
}
