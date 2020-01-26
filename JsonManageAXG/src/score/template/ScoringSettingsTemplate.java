/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package score.template;

import exception.AISTProcessException;
import file.MapToJSON;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/**
 *
 * @author kaeru
 */
public class ScoringSettingsTemplate {

    public static String[] createTemplate(String db, String collection, String outPath) throws AISTProcessException {
        System.out.println("実行の確認");
        
        System.out.println("db="+db);
        System.out.println("col="+collection);
        
        MongoDBPOJOData mongo = MongoDBPOJOData.create();
        
        if(!collection.contains("_Form"))
            collection = collection+"_Form";
        
        mongo.set(db, collection, MSyaryoObject.class);
        mongo.check();
        
        String[] files = new String[3];
        files[0] = createMainte(mongo, outPath);
        files[1] = createUse(mongo, outPath);
        files[2] = createAgeSMR(mongo, outPath);
        
        mongo.close();
        
        return files;
    }

    private static String createUse(MongoDBPOJOData mongo, String path) throws AISTProcessException {
        String file = path + "\\score_use_template.json";

        MHeaderObject hobj = mongo.getHeader();
        Optional<MSyaryoObject> syaryo = mongo.getKeyList().stream()
                .map(sid -> mongo.getObj(sid))
                .filter(s -> s.getData("LOADMAP_DATE_SMR") != null)
                .findFirst();

        Map temp = new LinkedHashMap();
        temp.put("#EVALUATE", "ENABLE");
        temp.put("#COMMENT", "データの列ごとの合計で計算する場合はSUM:ROWをHEADER下に追加,行ごとの合計はCOLUMNとする");

        temp.put("評価項目", new LinkedHashMap<>());
        
        if (syaryo.isPresent()) {
            syaryo.get().getMap().entrySet().stream()
                    .filter(e -> e.getKey().contains("LOADMAP"))
                    .forEach(e -> {
                        Map<String, String> map = new LinkedHashMap();
                        List<String> h = hobj.getHeader(e.getKey());
                        map.put("#SCORE", "スコアリングの評価対象");
                        map.put("HEADER", h.stream().map(hi -> hi.split("\\.")[1]).collect(Collectors.joining(",")));
                        e.getValue().entrySet().stream()
                                .forEach(d -> {
                                    map.put(d.getKey(), d.getValue().stream().map(di -> "1").collect(Collectors.joining(",")));
                                });
                        ((Map<String, Map<String, String>>) temp.get("評価項目")).put(e.getKey(), map);
                    });
        }else{
            Map<String, Map> t1 = new LinkedHashMap();
            Map<String, String> t2 = new LinkedHashMap();
            t2.put("#SCORE", "スコアリングの評価対象");
            t2.put("HEADER", "ROW1,ROW2");
            t2.put("SUM", "COLUMN/ROW");
            t2.put("COLUMN1(mask:(1,1)と(1,2)を1倍する)", "1,1");
            t1.put("データ名", t2);
            ((Map<String, Map>)temp.get("評価項目")).putAll(t1);
        }

        System.out.println(file+"を生成．");
        MapToJSON.toJSON(file, temp);
        
        return file;
    }

    private static String createMainte(MongoDBPOJOData mongo, String path) throws AISTProcessException {
        String file = path + "\\score_maintenance_template.json";
        
        Map temp = new LinkedHashMap();
        temp.put("#EVALUATE", "ENABLE");
        temp.put("#COMMENT", "ユーザ定義ファイルにより設定された項目のみ評価項目に設定可能");
        temp.put("評価項目", "インターバル");

        System.out.println(file+"を生成．");
        MapToJSON.toJSON(file, temp);
        
        return file;
    }

    private static String createAgeSMR(MongoDBPOJOData mongo, String path) throws AISTProcessException {
        String file = path + "\\score_agesmr_template.json";
        
        Map temp = new LinkedHashMap();
        temp.put("#EVALUATE", "ENABLE");
        temp.put("#COMMENT", "ユーザ定義ファイルにより設定された項目のみ評価項目に設定可能");
        temp.put("評価項目", "左データに対するコメント入力箇所(入力は無くても良い)");
        temp.put("#VISUAL_X", "SMR");
        temp.put("#DIVIDE_X", "100");

        System.out.println(file+"を生成．");
        MapToJSON.toJSON(file, temp);
        
        return file;
    }
}
