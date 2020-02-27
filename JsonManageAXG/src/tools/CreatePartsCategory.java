/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import exception.AISTProcessException;
import file.CSVFileReadWrite;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class CreatePartsCategory {

    public static void main(String[] args) throws AISTProcessException, IOException {
        //カテゴリMap category - 品番,品名 
        Map<String, List<String[]>> category = new HashMap();

        //CSVファイル取得
        try (BufferedReader br = CSVFileReadWrite.readerSJIS("PC200_部品分類_新.csv")) {
            List<String> h = Arrays.asList(br.readLine().split(","));
            String str;
            while ((str = br.readLine()) != null) {
                String[] s = str.split(",", -1);
                String key = s[h.indexOf("カテゴリ")] + "-" + s[h.indexOf("サブカテゴリ")];
                String[] hbhn = new String[]{s[h.indexOf("部品.品番")], s[h.indexOf("部品.部品名称")]};
                if (category.get(key) == null) {
                    category.put(key, new ArrayList());
                }
                category.get(key).add(hbhn);
            }
        }

        //データ取得
        String dkey = "部品";
        MongoDBPOJOData db = MongoDBPOJOData.create();
        db.set("json", "KM_PC200_DB_Form", MSyaryoObject.class);

        int hnbn = db.getHeader().getHeaderIdx(dkey, "品番");
        int hnm = db.getHeader().getHeaderIdx(dkey, "部品名称");

        category.entrySet().parallelStream().forEach(c -> {
            try (PrintWriter pw = CSVFileReadWrite.writerSJIS(c.getKey() + ".csv")) {
                System.out.println(c.getKey() + ":" + c.getValue().size());
                pw.println("SID,部品.作番," + String.join(",#", db.getHeader().getHeader(dkey)));
            } catch (AISTProcessException ex) {
                ex.printStackTrace();
            }
        });
        db.getKeyList().parallelStream().map(sid -> (MSyaryoObject) db.getObj(sid))
                .filter(s -> s.getData(dkey) != null)
                //.peek(s -> System.out.println(s.getName() + ":" + s.getCount(dkey)))
                .forEach(s -> {
                    category.entrySet().parallelStream().forEach(c -> {
                        try (PrintWriter pw = CSVFileReadWrite.addwriter(c.getKey() + ".csv")) {
                            c.getValue().stream().forEach(ci -> {
                                s.getData(dkey).entrySet().stream()
                                        .filter(d -> d.getValue().get(hnbn).equals(ci[0]) && d.getValue().get(hnm).equals(ci[1]))
                                        .map(d -> s.getName() + "," + d.getKey() + "," + String.join(",", d.getValue()))
                                        .forEach(pw::println);
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                });
    }
}
