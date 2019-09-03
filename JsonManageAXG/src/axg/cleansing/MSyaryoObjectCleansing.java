/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.cleansing;

import mongodb.MongoDBPOJOData;
import mongodb.MongoDBData;
import axg.obj.MHeaderObject;
import axg.obj.MSyaryoObject;
import java.util.ArrayList;
import java.util.Arrays;
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
    
    public static void main(String[] args) {
        clean("json", "komatsuDB_PC200", "8,8N1,10");
    }
    
    public static void clean(String db, String collection, String typ) {
        MongoDBData originDB = MongoDBData.create();
        originDB.set(db, collection);
        
        hobj = originDB.getHeaderObj();
        
        //New Mongo Collection
        MongoDBPOJOData cleanDB = MongoDBPOJOData.create();
        cleanDB.set(db, collection+"_Clean", MSyaryoObject.class);
        cleanDB.clear();
        
        long start = System.currentTimeMillis();
        
        cleanDB.coll.insertOne(hobj);
        
        //車両のクレンジング実行
        originDB.getKeyList().parallelStream()
                .filter(sid -> Arrays.asList(typ.split(",")).contains(sid.split("-")[1]+sid.split("-")[2].replace(" ", "")))
                .map(sid -> cleanOne(originDB.get(sid)))
                .forEach(cleanDB.coll::insertOne);
        
        long stop = System.currentTimeMillis();
        
        System.out.println("CleansingTime="+(stop-start)+"ms");
        
        originDB.close();
        cleanDB.close();
    }

    public static MSyaryoObject cleanOne(MSyaryoObject obj) {
        Map<String, Integer> check = new HashMap<>();
        aggregateMap().entrySet().stream().forEach(c -> {
            String key = c.getKey();
            Map rule = c.getValue();
            
            List<String> removeKey = removeData(key, obj.getData(key), rule);
            
            //SID,Records_N,Remeve_N,remove_key List
            int n = obj.getData(key) == null ? 0 : obj.getData(key).size();
            check.put(key, removeKey.size());

            obj.removeAll(key, removeKey);
            //System.out.println(obj.get(key));
        });
        
        //System.out.println(obj.getName()+" - "+check);
        
        obj.recalc();
        
        return obj;
    }

    
    //クレンジングルール
    private static Map<String, Map> aggregateMap() {
        return new HashMap() {
            {
                put("売上", sellRule());
                put("サービス経歴", serviceRule());
                put("受注", orderRule());
                put("顧客", customerRule());
                put("顧客_S", customer_sRule());
                put("KOMPAS車両", syaryoRule());
                put("部品", partsRule());
                put("作業", workRule());
            }
        };
    }

    private static Map sellRule() {
        return new HashMap() {
            {
                put("売上.受注売上区分", Arrays.asList("2"));
                put("売上.見込実績区分", Arrays.asList("2"));
                put("売上.折半元折半先区分", Arrays.asList("1"));
                put("売上.本体赤黒区分", Arrays.asList("1"));
                put("売上.受注売上実績最新フラグ", Arrays.asList("1"));
                put("売上.論理削除フラグ", Arrays.asList("0"));
                put("売上.本体案件取引進捗区分", Arrays.asList("151", "153", "155"));
            }
        };
    }

    private static Map serviceRule() {
        return new HashMap() {
            {
                put("サービス経歴.発生区分", Arrays.asList("1"));
                put("サービス経歴.赤黒区分", Arrays.asList("0"));
            }
        };
    }

    private static Map orderRule() {
        return new HashMap() {
            {
                put("受注.売上計上フラグ", Arrays.asList("1"));
            }
        };
    }

    private static Map syaryoRule() {
        return new HashMap() {
            {
                put("KOMPAS車両.論理削除フラグ", Arrays.asList("0"));
            }
        };
    }

    private static Map workRule() {
        return new HashMap() {
            {
                put("作業.論理削除フラグ", Arrays.asList("0"));
            }
        };
    }
    
    private static Map customerRule() {
        return new HashMap() {
            {
                put("顧客.論理削除フラグ", Arrays.asList("0"));
            }
        };
    }
    
    private static Map customer_sRule() {
        return new HashMap() {
            {
                put("顧客_S.論理削除フラグ", Arrays.asList("0"));
            }
        };
    }

    private static Map partsRule() {
        return new HashMap() {
            {
                put("部品.論理削除フラグ", Arrays.asList("0"));
            }
        };
    }
    
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
