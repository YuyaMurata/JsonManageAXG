/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.cleansing;

import axg.check.CheckSettings;
import axg.shuffle.form.util.FormalizeUtils;
import exception.AISTProcessException;
import file.CSVFileReadWrite;
import mongodb.MongoDBPOJOData;
import mongodb.MongoDBData;
import obj.MHeaderObject;
import obj.MSyaryoObject;
import file.MapToJSON;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17807
 */
public class MSyaryoObjectCleansing {
    
    private String db;
    private String collection;

    //Header
    private MHeaderObject hobj;
    private Map<String, Map<String, List<String>>> ruleMap;
    private Map<String, Map<String, Integer>> previousMap;
    private Map<String, Map<String, Integer>> cleansingResults;
    private Map<String, List<String>> removeLog;
    
    public MSyaryoObjectCleansing(String db, String collection) {
        this.db = db;
        this.collection = collection;
    }

    //例外処理
    private void checkSettings(MHeaderObject h, Map<String, Map<String, List<String>>> cleanSetting) throws AISTProcessException {
        CheckSettings.check(h, "クレンジング", cleanSetting);
    }
    
    public void clean(String cleanSetting) throws AISTProcessException {
        MongoDBData originDB = MongoDBData.create();
        originDB.set(db, collection);
        originDB.check();

        //元マスタデータのレイアウト出力
        String layoutpath = cleanSetting.substring(0, cleanSetting.lastIndexOf("\\"));
        createMaterLayout(layoutpath + "\\master_layout.json");
        
        //設定ファイルとヘッダ読み込み
        ruleMap = MapToJSON.toMapSJIS(cleanSetting);
        hobj = originDB.getHeaderObj();
        previousMap = new LinkedHashMap<>();
        cleansingResults = new LinkedHashMap<>();

        //検証
        checkSettings(hobj, ruleMap);
        
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
    private MSyaryoObject cleanOne(MSyaryoObject obj) {
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
                    .map(skey -> obj.getName() + "," + String.join(",", obj.getData(key).get(skey)))
                    .forEach(str -> removeLog.get(key).add(str));
            
            obj.removeAll(key, removeKey);
            //System.out.println(obj.get(key));
        });
        
        previousMap.put(obj.getName(), obj.getCount());
        
        obj.recalc();
        
        cleansingResults.put(obj.getName(), obj.getCount());
        
        if (obj.getData("車両") == null) {
            return null;
        }
        
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
        if (unirule.getValue().isEmpty()) {
            return false;
        }
        
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
    
    public void logPrint(String logFilePath) throws AISTProcessException {
        for (String dkey : removeLog.keySet()) {
            if (hobj.getHeader(dkey) != null) {
                PrintWriter pw = CSVFileReadWrite.writerSJIS(logFilePath + "\\cleansing_log_" + dkey + ".csv");
                pw.println("SID," + String.join(",", hobj.getHeader(dkey)));
                removeLog.get(dkey).stream().forEach(pw::println);
            } else {
                System.err.println(dkey + "データがインポートされていません");
            }
            
        }
        System.out.println("クレンジング　ログ出力");
    }
    
    public String getSummary() {
        Map<String, Integer> sumBefore = new LinkedHashMap<>();
        Map<String, Integer> sumAfter = new LinkedHashMap<>();
        Map<String, Integer> sum = new HashMap<>();
        sum.put("クレンジング対象台数", 0);
        sum.put("クレンジング対象件数", 0);
        
        previousMap.keySet().stream().forEach(k -> {
            Map<String, Integer> before = previousMap.get(k);
            Map<String, Integer> after = cleansingResults.get(k);
            before.keySet().stream().forEach(d -> {
                if (sumBefore.get(d) == null) {
                    sumBefore.put(d, 0);
                    sumAfter.put(d, 0);
                }
                
                sumBefore.put(d, sumBefore.get(d) + before.get(d));
                sumAfter.put(d, sumAfter.get(d) + (after.get(d) != null ? after.get(d) : 0));
            });
            
            Optional<String> diff = before.keySet().stream()
                    .filter(d -> !before.get(d).equals(after.get(d)))
                    .findFirst();
            
            if (diff.isPresent()) {
                sum.put("クレンジング対象台数", sum.get("クレンジング対象台数") + 1);
            }
        });
        
        sum.put("クレンジング対象件数", removeLog.values().stream().mapToInt(l -> l.size()).sum());
        
        StringBuilder sb = new StringBuilder();

        //データ全体のサマリ
        sb.append(sum.entrySet().stream().map(s -> s.getKey() + ":" + s.getValue()).collect(Collectors.joining("   ")));

        //データの各項目のサマリ
        sb.append("\nデータIDごとのクレンジング件数\n");
        //項目名
        sb.append(sumBefore.keySet().stream().sorted().map(s -> s + " : " + sumBefore.get(s) + "  ->  " + sumAfter.get(s)).collect(Collectors.joining("\n")));
        
        return sb.toString();
    }

    //テンプレート生成
    private void createMaterLayout(String file) throws AISTProcessException {
        MongoDBData mongo = MongoDBData.create();
        mongo.set(db, collection);
        
        List<String> hin = mongo.getHeader();
        Map<String, Map<String, List<String>>> head = new HashMap<>();
        Boolean flg = true;
        for (String s : hin) {
            if (s.equals("id ")) {
                continue;
            }

            //System.out.println(s);
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

        //System.out.println(hin);
        mongo.close();
    }

    //テンプレート生成
    public String createTemplate(String templatePath) throws AISTProcessException {
        String fileName = templatePath + "\\cleansing_template.json";
        
        MongoDBData mongo = MongoDBData.create();
        mongo.set(db, collection);
        mongo.check();
        
        MHeaderObject hobj = mongo.getHeaderObj();
        
        Map<String, Map<String, List<String>>> map = new LinkedHashMap();
        //System.out.println(hobj.map);
        hobj.map.entrySet().stream().forEach(h -> {
            map.put(h.getKey(),
                    h.getValue().stream()
                            .distinct()
                            .collect(Collectors.toMap(hi -> hi, hi -> new ArrayList(), (hi1, hi2) -> hi1, LinkedHashMap::new))
            );
        });
        
        MapToJSON.toJSON(fileName, map);
        
        mongo.close();
        return fileName;
    }
}
