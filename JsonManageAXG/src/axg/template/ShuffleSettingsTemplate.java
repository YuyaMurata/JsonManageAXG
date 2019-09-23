/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.template;

import file.MapToJSON;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mongodb.MongoDBData;

/**
 *
 * @author ZZ17807
 */
public class ShuffleSettingsTemplate {
    public static void main(String[] args) {
        create("json", "komatsuDB_TEST", "settings\\mongo_syaryo_template.json");
    }
    
    //シャッフル用ファイルを作成するための元ファイル作成
    public static void create(String db, String collection, String file) {
        MongoDBData mongo = MongoDBData.create();
        mongo.set(db, collection);

        List<String> hin = mongo.getHeader();
        Map<String, Map<String, List<String>>> head = new HashMap<>();
        Boolean flg = true;
        for (String s : hin) {
            if (s.equals("id ")) {
                continue;
            }

            System.out.println(s);

            String k = s.split("\\.")[0];

            if (head.get(k) == null) {
                head.put(k, new HashMap<>());
                head.get(k).put(k + ".subKey", new ArrayList<>());
                flg = false;
            }

            if (flg) {
                head.get(k).get(k + ".subKey").add(s);
            } else {
                flg = true;
            }
        }

        new MapToJSON().toJSON(file, head);

        System.out.println(hin);
        mongo.close();
    }
    
    public static void createLayoutHeader(Map<String, Map<String, List<String>>> shuffleIndex) {
        Map<String, Map<String, List<String>>> map = new LinkedHashMap();

        shuffleIndex.entrySet().stream()
                .forEach(e -> {
                    e.getValue().entrySet().stream()
                            .filter(e2 -> !e2.getValue().contains(""))
                            .limit(1)
                            .forEach(e2 -> {
                                String key = e.getKey();
                                String subKey = idxToShuffleIdx(key, e2.getKey());
                                List<String> subList = e2.getValue().stream().map(e2v -> idxToShuffleIdx(e.getKey(), e2v)).collect(Collectors.toList());

                                map.put(key, new HashMap<>());
                                map.get(key).put(subKey, subList);
                            });
                });

        new MapToJSON().toJSON("layout_mongo_syaryo.json", map);
    }
    
    private static String idxToShuffleIdx(String key, String idx) {
        if (idx.contains(".")) {
            return key + "." + idx.split("\\.")[1];
        } else {
            return key + "." + idx;
        }
    }
}
