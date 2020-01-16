/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package file;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import exception.AISTProcessException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ZZ17390
 */
public class MapToJSON {

    public static void toJSON(String filename, Map index) throws AISTProcessException {
        try (JsonWriter writer = new JsonWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "SJIS")))) {
            writer.setIndent("  ");

            Gson gson = new Gson();
            gson.toJson(index, Map.class, writer);

        } catch (IOException e) {
            throw new AISTProcessException("出力先フォルダが存在しません："+filename);
        }
    }

    
    public static Map toMapSJIS(String filename) throws AISTProcessException {
        Map<String, String> index;
        try (JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(new FileInputStream(filename), "SJIS")))) {

            Type type = new TypeToken<Map>() {
            }.getType();

            Gson gson = new Gson();
            index = gson.fromJson(reader, type);
        } catch (Exception e) {
            throw new AISTProcessException("読み込みファイルが存在しません or JSON形式でありません："+filename);
        }

        return index;
    }
}
