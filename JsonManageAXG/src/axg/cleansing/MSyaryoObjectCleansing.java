/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.cleansing;

import axg.shuffle.form.util.FormalizeUtils;
import file.CSVFileReadWrite;
import mongodb.MongoDBPOJOData;
import mongodb.MongoDBData;
import obj.MHeaderObject;
import obj.MSyaryoObject;
import file.MapToJSON;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17807
 */
public class MSyaryoObjectCleansing {
    String db;
    String collection;
    
    //Header
    MHeaderObject hobj;
    Map<String, Map<String, List<String>>> ruleMap;
    Map<String, Map<String, Integer>> previousMap;
    Map<String, Map<String, Integer>> cleansingResults;
    Map<String, List<String>> removeLog;

    public MSyaryoObjectCleansing(String db, String collection){
        this.db = db;
        this.collection = collection;
    }
    
    public static void main(String[] args) {
        //clean("json", "PC200_DB", "config\\cleansing_settings.json");
        //System.out.println(cleansingResults);
        //logPrint("log");
        MSyaryoObjectCleansing clean = new MSyaryoObjectCleansing("json", "komatsuDB_TEST");
        clean.createTemplate("test");
    }

    public void clean(String cleanSetting) {
        MongoDBData originDB = MongoDBData.create();
        originDB.set(db, collection);

        //元マスタデータのレイアウト出力
        String layoutpath = cleanSetting.substring(0, cleanSetting.lastIndexOf("\\"));
        createMaterLayout(layoutpath + "\\master_layout.json");

        //設定ファイルとヘッダ読み込み
        ruleMap = new MapToJSON().toMap(cleanSetting);
        hobj = originDB.getHeaderObj();
        cleansingResults = new HashMap<>();
        removeLog = ruleMap.keySet().stream().collect(Collectors.toMap(r -> r, r -> new ArrayList<>(), (r1, r2) -> r2, ConcurrentHashMap::new));

        //クレンジング用Mongoコレクション作成
        MongoDBPOJOData cleanDB = MongoDBPOJOData.create();
        cleanDB.set(db, collection + "_Clean", MSyaryoObject.class);
        cleanDB.clear();

        long start = System.currentTimeMillis();

        cleanDB.coll.insertOne(hobj);

        //車両のクレンジング実行
        originDB.getKeyList().parallelStream()
                .map(sid -> cleanOne(originDB.get(sid)))
                .filter(obj -> obj != null)
                .forEach(cleanDB.coll::insertOne);

        long stop = System.currentTimeMillis();

        System.out.println("CleansingTime=" + (stop - start) + "ms");

        originDB.close();
        cleanDB.close();
    }

    //1台のクレンジング
    public MSyaryoObject cleanOne(MSyaryoObject obj) {
        Map<String, Integer> check = new HashMap<>();

        ruleMap.entrySet().stream().forEach(c -> {
            String key = c.getKey();
            Map rule = c.getValue();

            List<String> removeKey = removeData(key, obj.getData(key), rule);

            //SID,Records_N,Remove_N,remove_key List
            int n = obj.getData(key) == null ? 0 : obj.getData(key).size();
            check.put(key, removeKey.size());
            
            //logging
            removeKey.stream()
                    .map(skey -> obj.getName()+","+String.join(",", obj.getData(key).get(skey)))
                    .forEach(str -> removeLog.get(key).add(str));

            obj.removeAll(key, removeKey);
            //System.out.println(obj.get(key));
        });

        //System.out.println(obj.getName()+" - "+check);
        if (obj.getData("車両") == null) {
            return null;
        }
        
        previousMap.put(obj.getName(), obj.getCount());
        
        obj.recalc();

        cleansingResults.put(obj.getName(), obj.getCount());

        return obj;
    }

    //データの削除
    private List<String> removeData(String key, Map<String, List<String>> data, Map<String, List<String>> rule) {
        if (data == null) {
            //System.out.print(",,,,");
            return new ArrayList<>();
        }

        List<String> removeSubKey = data.entrySet().stream()
                .filter(d -> rule.entrySet().parallelStream()
                .filter(r -> removeLogic(r, d.getValue(), hobj))
                .findFirst().isPresent())
                .map(s -> s.getKey())
                .collect(Collectors.toList());

        //System.out.print(","+(data.size()-removeSubKey.size())+","+data.size()+"," + removeSubKey.size()+",");
        return removeSubKey;
    }

    private Boolean removeLogic(Map.Entry<String, List<String>> unirule, List<String> data, MHeaderObject hobj) {

        if (!unirule.getValue().get(0).contains(".")) {
            //単純比較
            return !unirule.getValue().contains(data.get(hobj.getHeaderIdx(unirule.getKey().split("\\.")[0], unirule.getKey())));
        } else {
            //参照比較
            return !unirule.getValue().stream()
                    .filter(ru
                            -> num(data.get(hobj.getHeaderIdx(ru.split("\\.")[0], ru.split("\\.")[0] + "." + ru.split("\\.")[1])), data.get(hobj.getHeaderIdx(unirule.getKey().split("\\.")[0], unirule.getKey())))
                    < Integer.valueOf(ru.split("\\.")[2]))
                    .findFirst().isPresent();
        }
    }

    private Integer num(String d1, String d2) {
        if (d1.contains("/")) {
            return Math.abs(FormalizeUtils.dsub(
                    FormalizeUtils.dateFormalize(d1),
                    FormalizeUtils.dateFormalize(d2)
            ));
        } else {
            return Math.abs(Integer.valueOf(d1) - Integer.valueOf(d2));
        }
    }

    public void logPrint(String logFilePath) {
        removeLog.entrySet().stream().forEach(s -> {
            try (PrintWriter pw = CSVFileReadWrite.writerSJIS(logFilePath+"\\cleansing_log_"+s.getKey()+".csv")) {
                pw.println("SID,"+String.join(",", hobj.getHeader(s.getKey()))); 
                s.getValue().stream().forEach(pw::println);
            }
        });
    }
    
    public String getSummary() {
        return null;
    }
    
    //テンプレート生成
    private void createMaterLayout(String file) {
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

        MapToJSON.toJSON(file, head);

        System.out.println(hin);
        mongo.close();
    }
    
    //テンプレート生成
    public String createTemplate(String templatePath){
        MongoDBData mongo = MongoDBData.create();
        mongo.set(db, collection);
        hobj = mongo.getHeaderObj();
        
        String fileName = templatePath+"\\cleansing_setting_template.json";
        Map<String, Map<String, List<String>>> map = new HashMap<>();
        System.out.println(hobj.getHeaderMap());
        hobj.getHeaderMap().entrySet().stream().forEach(h ->{
            map.put(h.getKey(), 
                h.getValue().stream().distinct().collect(Collectors.toMap(hi -> hi, hi -> new ArrayList()))
            );
        });
        
        MapToJSON.toJSON(fileName, map);
        
        mongo.close();
        return fileName;
    }
}
