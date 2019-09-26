/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.cleansing;

import mongodb.MongoDBPOJOData;
import mongodb.MongoDBData;
import obj.MHeaderObject;
import obj.MSyaryoObject;
import file.MapToJSON;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17807
 */
public class MSyaryoObjectCleansing {

    //Header
    static MHeaderObject hobj;
    static Map<String, Map<String, List<String>>> ruleMap;
    
    public static void main(String[] args) {
        clean("json", "komatsuDB_TEST", "settings\\cleansing_settings.json");
    }
    
    public static void clean(String db, String collection, String cleanSetting) {
        MongoDBData originDB = MongoDBData.create();
        originDB.set(db, collection);
        
        //設定ファイルとヘッダ読み込み
        ruleMap = new MapToJSON().toMap(cleanSetting); 
        hobj = originDB.getHeaderObj();
        
        //クレンジング用Mongoコレクション作成
        MongoDBPOJOData cleanDB = MongoDBPOJOData.create();
        cleanDB.set(db, collection+"_Clean", MSyaryoObject.class);
        cleanDB.clear();
        
        long start = System.currentTimeMillis();
        
        cleanDB.coll.insertOne(hobj);
        
        //車両のクレンジング実行
        originDB.getKeyList().parallelStream()
                .map(sid -> cleanOne(originDB.get(sid)))
                .filter(obj -> obj != null)
                .forEach(cleanDB.coll::insertOne);
        
        long stop = System.currentTimeMillis();
        
        System.out.println("CleansingTime="+(stop-start)+"ms");
        
        originDB.close();
        cleanDB.close();
    }

    //1台のクレンジング
    public static MSyaryoObject cleanOne(MSyaryoObject obj) {
        Map<String, Integer> check = new HashMap<>();
        
        ruleMap.entrySet().stream().forEach(c -> {
            String key = c.getKey();
            Map rule = c.getValue();
            
            List<String> removeKey = removeData(key, obj.getData(key), rule);
            
            //SID,Records_N,Remove_N,remove_key List
            int n = obj.getData(key) == null ? 0 : obj.getData(key).size();
            check.put(key, removeKey.size());

            obj.removeAll(key, removeKey);
            //System.out.println(obj.get(key));
        });
        
        //System.out.println(obj.getName()+" - "+check);
        
        if(obj.getData("車両") == null)
            return null;
        
        obj.recalc();
        
        return obj;
    }

    //データの削除
    private static List<String> removeData(String key, Map<String, List<String>> data, Map<String, List<String>> rule) {
        if (data == null) {
            //System.out.print(",,,,");
            return new ArrayList<>();
        }
        
        List<String> removeSubKey = data.entrySet().stream()
                .filter(d -> rule.entrySet().parallelStream()
                    .filter(r -> !r.getValue().contains(d.getValue().get(hobj.getHeaderIdx(key, r.getKey()))))
                    .findFirst().isPresent())
                .map(s -> s.getKey())
                .collect(Collectors.toList());

        //System.out.print(","+(data.size()-removeSubKey.size())+","+data.size()+"," + removeSubKey.size()+",");
        return removeSubKey;
    }
}
