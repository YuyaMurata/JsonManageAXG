/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package score.template;

import file.MapToJSON;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/**
 *
 * @author kaeru
 */
public class ScoringSettingsTemplate {
    public static void main(String[] args) {
        createUse("json", "komatsuDB_PC200_Form", "config");
        createMainte("json", "komatsuDB_PC200_Form", "config");
        createAgeSMR("json", "komatsuDB_PC200_Form", "config");
    }
    
    private static void createUse(String db, String collection, String path) {
        String file = path+"\\score_use_template.json";
        
        MongoDBPOJOData mongo = MongoDBPOJOData.create();
        mongo.set(db, collection, MSyaryoObject.class);

        MHeaderObject hobj = mongo.getHeader();
        MSyaryoObject syaryo = mongo.getKeyList().stream()
                                    .map(sid -> mongo.getObj(sid))
                                    .filter(s -> s.getData("LOADMAP_DATE_SMR") != null)
                                    .findFirst().get();
        
        Map temp = new LinkedHashMap();
        temp.put("#EVALUATE", "ENABLE");
        temp.put("#COMMENT", "データの列ごとの合計で計算する場合はSUM:ROWをHEADER下に追加,行ごとの合計はCOLUMNとする");
        
        temp.put("評価項目", new LinkedHashMap<>());
        syaryo.getMap().entrySet().stream()
                        .filter(e -> e.getKey().contains("LOADMAP"))
                        .forEach(e -> {
                            Map<String, String> map = new LinkedHashMap();
                            List<String> h = hobj.getHeader(e.getKey());
                            map.put("#SCORE", "スコアリングの評価対象");
                            map.put("HEADER", h.stream().map(hi -> hi.split("\\.")[1]).collect(Collectors.joining(",")));
                            e.getValue().entrySet().stream()
                                            .forEach(d ->{
                                                map.put(d.getKey(), d.getValue().stream().map(di -> "1").collect(Collectors.joining(",")));
                                            });
                            ((Map<String, Map<String, String>>)temp.get("評価項目")).put(e.getKey(), map);
                        });
        
        MapToJSON.toJSON(file, temp);
    }
    
    private static void createMainte(String db, String collection, String path) {
        String file = path+"\\score_maintenance_template.json";
        Map temp = new LinkedHashMap();
        temp.put("#EVALUATE", "ENABLE");
        temp.put("#COMMENT", "ユーザ定義ファイルにより設定された項目のみ評価項目に設定可能");
        temp.put("評価項目", "インターバル");
        
        MapToJSON.toJSON(file, temp);
    }
    
    private static void createAgeSMR(String db, String collection, String path) {
        String file = path+"\\score_agesmr_template.json";
        Map temp = new LinkedHashMap();
        temp.put("#EVALUATE", "ENABLE");
        temp.put("#COMMENT", "ユーザ定義ファイルにより設定された項目のみ評価項目に設定可能");
        temp.put("評価項目", "左データに対するコメント入力箇所(入力は無くても良い)");
        temp.put("#VISUAL_X", "SMR");
        temp.put("#DIVIDE_X", "100");
        
        MapToJSON.toJSON(file, temp);
    }
}
