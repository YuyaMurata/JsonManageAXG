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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class CreatePartsCategory {

    static String categoryFile = "toolsettings\\0_PC200_部品分類_新.csv";

    private static Map<String, List<String[]>> getCategryToData() throws AISTProcessException, IOException {
        //カテゴリMap category - 品番,品名 
        Map<String, List<String[]>> category = new HashMap();

        //CSVファイル取得
        try (BufferedReader br = CSVFileReadWrite.readerSJIS(categoryFile)) {
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

        return category;
    }

    private static Map<String, String> getDataToCategry() throws AISTProcessException, IOException {
        //カテゴリMap 品番,品名 - category
        Map<String, String> category = new HashMap();

        //CSVファイル取得
        try (BufferedReader br = CSVFileReadWrite.readerSJIS(categoryFile)) {
            List<String> h = Arrays.asList(br.readLine().split(","));
            String str;
            while ((str = br.readLine()) != null) {
                String[] s = str.split(",", -1);
                String c = s[h.indexOf("カテゴリ")] + "," + s[h.indexOf("サブカテゴリ")];
                String hbhn = s[h.indexOf("部品.品番")] + "," + s[h.indexOf("部品.部品名称")];
                category.put(hbhn, c);
            }
        }

        return category;
    }

    private static Map<String, Set<String>> getPNoToCategry() throws AISTProcessException, IOException {
        //カテゴリMap 品番 - category
        Map<String, Set<String>> category = new HashMap();

        //CSVファイル取得
        try (BufferedReader br = CSVFileReadWrite.readerSJIS(categoryFile)) {
            List<String> h = Arrays.asList(br.readLine().split(","));
            String str;
            while ((str = br.readLine()) != null) {
                String[] s = str.split(",", -1);
                String c = s[h.indexOf("カテゴリ")] + "," + s[h.indexOf("サブカテゴリ")];
                String hbhn = s[h.indexOf("部品.品番")];

                if (hbhn.length() < 5) {
                    continue;
                }

                if (category.get(hbhn) == null) {
                    category.put(hbhn, new TreeSet<>());
                }
                category.get(hbhn).add(c);
            }
        }

        return category;
    }

    private static Map<String, Set<String>> getPNameToCategry() throws AISTProcessException, IOException {
        //カテゴリMap 品番 - category
        Map<String, Set<String>> category = new HashMap();

        //CSVファイル取得
        try (BufferedReader br = CSVFileReadWrite.readerSJIS(categoryFile)) {
            List<String> h = Arrays.asList(br.readLine().split(","));
            String str;
            while ((str = br.readLine()) != null) {
                String[] s = str.split(",", -1);
                String c = s[h.indexOf("カテゴリ")] + "," + s[h.indexOf("サブカテゴリ")];
                String hbhn = s[h.indexOf("部品.部品名称")];
                if (hbhn.length() < 3) {
                    continue;
                }
                if (category.get(hbhn) == null) {
                    category.put(hbhn, new TreeSet<>());
                }
                category.get(hbhn).add(c);
            }
        }

        return category;
    }

    public static void main(String[] args) throws AISTProcessException, IOException {
        //データ取得
        MongoDBPOJOData db = MongoDBPOJOData.create();
        db.set("json", "KM_PC200_DB_P_Form", MSyaryoObject.class);
        wipCategoryOutput(db);
    }

    private static void wipCategoryOutput(MongoDBPOJOData db) throws AISTProcessException, IOException {
        Map<String, String> category = getDataToCategry();
        Map<String, Set<String>> pnoCategory = getPNoToCategry();
        Map<String, Set<String>> pnameCategory = getPNameToCategry();

        int hnbn = db.getHeader().getHeaderIdx("部品", "品番");
        int hnm = db.getHeader().getHeaderIdx("部品", "部品名称");

        try (PrintWriter pw = CSVFileReadWrite.writerSJIS("PC200_部品_カテゴリ.csv")) {
            //header
            pw.println("SID,作番," + String.join(",", db.getHeader().getHeader("部品")) + ",カテゴリ,サブカテゴリ,候補");

            db.getKeyList().parallelStream().map(sid -> (MSyaryoObject) db.getObj(sid))
                    .filter(s -> s.getData("部品") != null)
                    //.peek(s -> System.out.println(s.getName() + ":" + s.getCount(dkey)))
                    .flatMap(s -> {
                        return s.getData("部品").entrySet().stream()
                                .map(p -> s.getName() + ","
                                + p.getKey() + ","
                                + String.join(",", p.getValue()) + ","
                                + join(category, pnoCategory, pnameCategory, p.getValue().get(hnbn) + "," + p.getValue().get(hnm)));
                    }).forEach(pw::println);
        }
    }

    private static String join(Map<String, String> category, Map<String, Set<String>> c2, Map<String, Set<String>> c3, String key) {
        if (category.get(key) != null) {
            return category.get(key);
        } else {
            Set<String> cand = new TreeSet<>();
            String c = ",";
            
            Set<String> pno = c2.get(key.split(",",2)[0]);
            if (pno != null) {
                if (pno.size() == 1) {
                    c = String.join(",", pno);
                    cand.add("品番による候補入力");
                } else {
                    cand.addAll(pno);
                }
            }
            
            Set<String> pname = c3.get(key.split(",",2)[1]);
            if (pname != null) {
                if (pname.size() == 1) {
                    if (c.equals(",")) {
                        c = String.join(",", pname);
                        cand.add("品名による候補入力");
                    } else {
                        cand.addAll(pno);
                    }
                }
            }

            return c + "," + cand.stream().map(ci -> ci.replace(",", "-")).collect(Collectors.joining("_"));
        }
    }

    private static void categoryOutput(MongoDBPOJOData db) throws AISTProcessException, IOException {
        Map<String, List<String[]>> category = getCategryToData();

        int hnbn = db.getHeader().getHeaderIdx("部品", "品番");
        int hnm = db.getHeader().getHeaderIdx("部品", "部品名称");
        db.getKeyList().parallelStream().map(sid -> (MSyaryoObject) db.getObj(sid))
                .filter(s -> s.getData("部品") != null)
                //.peek(s -> System.out.println(s.getName() + ":" + s.getCount(dkey)))
                .forEach(s -> {
                    category.entrySet().parallelStream().forEach(c -> {
                        try (PrintWriter pw = CSVFileReadWrite.addwriter(c.getKey() + ".csv")) {
                            c.getValue().stream().forEach(ci -> {
                                s.getData("部品").entrySet().stream()
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
