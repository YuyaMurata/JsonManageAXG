/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle;

import mongodb.MongoDBPOJOData;
import mongodb.MongoDBData;
import axg.obj.MHeaderObject;
import axg.obj.MSyaryoObject;
import file.MapToJSON;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17807
 */
public class MSyaryoObjectShuffle {

    //Index
    //static Map<String, Map<String, List<String>>> index = new MapToJSON().toMap("axg\\shuffle_mongo_syaryo.json");
    private static DecimalFormat df = new DecimalFormat("00");

    public static void main(String[] args) {
        //シャッフル
        shuffle("json", "komatsuDB_PC200", "axg\\shuffle_mongo_syaryo.json", "axg\\layout_mongo_syaryo.json");
    }

    public static void shuffle(String db, String collection, String shuffleSetting, String layoutSetting) {
        MongoDBPOJOData cleanDB = MongoDBPOJOData.create();
        cleanDB.set(db, collection+"_Clean", MSyaryoObject.class);
        
        //設定ファイルのヘッダ読み込み
        Map index = new MapToJSON().toMap(shuffleSetting);
        Map layout = new MapToJSON().toMap(layoutSetting);

        //Header
        MHeaderObject headerobj = cleanDB.getHeader();

        long start = System.currentTimeMillis();
        
        MongoDBPOJOData shuffleDB = MongoDBPOJOData.create();
        shuffleDB.set(db, collection+"_Shuffle", MSyaryoObject.class);
        shuffleDB.clear();
        shuffleDB.coll.insertOne(recreateHeaderObj(layout));
        
        List<String> sids = cleanDB.getKeyList();
        sids.parallelStream()
                .map(sid -> shuffleOne(headerobj, cleanDB.getObj(sid), index))
                .forEach(shuffleDB.coll::insertOne);
        
        long stop = System.currentTimeMillis();
        System.out.println("ShufflingTime="+(stop-start)+"ms");

        shuffleDB.close();
        cleanDB.close();
    }
    
    private static MSyaryoObject shuffleOne(MHeaderObject header, MSyaryoObject syaryo, Map<String, Map<String, List<String>>> index){
            Map<String, Map<String, List<String>>> map = new LinkedHashMap();
            
            //System.out.println(obj.getName());
            index.entrySet().stream().forEach(idx -> {
                //subkey
                Map<String, List<String>> subIdx = idx.getValue();
                subIdx.entrySet().stream().forEach(idx2 -> {
                    //initialize
                    if (map.get(idx.getKey()) == null) {
                        map.put(idx.getKey(), new LinkedHashMap<>());
                    }

                    //update map
                    Map<String, List<String>> subMap = idxMapping(map.get(idx.getKey()), idx2.getKey(), idx2.getValue(), header, syaryo);
                    map.put(idx.getKey(), subMap);

                    //
                    //testPrint(idx2.getKey() + ":" + idx2.getValue(), subMap);
                });
            });
            
            MSyaryoObject obj = new MSyaryoObject();
            obj.setName(syaryo.getName());
            obj.setMap(map);
            obj.recalc();
            
            return obj;
    }

    public static void testPrint(String idx, Map<String, List<String>> map) {
        System.out.println(idx);

        if (map.isEmpty()) {
            System.out.println("Data Nothing");
        } else {
            map.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).forEach(System.out::println);
        }

        System.out.println("");
    }

    private static Map<String, List<String>> idxMapping(Map<String, List<String>> dataMap, String subKey, List<String> idxList, MHeaderObject header, MSyaryoObject ref) {
        if (subKey.contains(".")) {
            //複数レコードでデータを作成
            String dataKey = subKey.split("\\.")[0];
            //System.out.println("subKey="+ref.getData(dataKey));
            if (ref.getData(dataKey) != null) {
                ref.getData(dataKey).values().stream().forEach(r -> {
                    String key = idxToData(subKey, header, r);
                    List data = idxList.stream()
                            .map(idx -> idxToData(idx, header, r))
                            .collect(Collectors.toList());
                    dataMap.put(duplicateKey(key, dataMap), data);
                });
            }
        } else {
            //単一レコードでデータを作成
            List data = idxList.stream()
                    .map(idx -> idxToData(idx, header, ref.getDataOne(idx.split("\\.")[0])))
                    .collect(Collectors.toList());
            
            //全て空のデータは無視
            if(data.stream().filter(d -> !d.equals("")).findFirst().isPresent())
                dataMap.put(duplicateKey(subKey, dataMap), data);
        }

        return dataMap;
    }

    //インデックス情報をデータに変換
    private static String idxToData(String idx, MHeaderObject header, List<String> refdata) {
        if (refdata == null) {
            return "";
        }

        //参照先データが存在する
        if (idx.contains(".")) {
            
            String key = idx.split("\\.")[0];
            
            String data = refdata.get(header.getHeaderIdx(key, idx));
            
            //空白列の除去
            if(data.replace(" ", "").equals(""))
                return "";
            else
                return data;
            
            //参照先データが存在しない
        } else {
            return idx;
        }
    }

    public static MHeaderObject recreateHeaderObj(Map<String, Map<String, List<String>>> layout) {
        //Map<String, Map<String, List<String>>> layout = new MapToJSON().toMap("axg\\layout_mongo_syaryo.json");

        List header = new ArrayList();
        header.add("id ");
        layout.values().stream().forEach(l -> {
            l.entrySet().stream().forEach(le -> {
                header.add(le.getKey());
                header.addAll(le.getValue());
            });
        });

        return new MHeaderObject(header);
    }
    
    private static String duplicateKey(String key, Map map) {
        int cnt = 0;
        String k = key;
        while (map.get(k) != null) {
            k = key + "#" + df.format(++cnt);
        }
        return k;
    }
}
