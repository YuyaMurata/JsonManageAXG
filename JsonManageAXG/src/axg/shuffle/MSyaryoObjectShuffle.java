/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle;

import mongodb.MongoDBCleansingData;
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
import mongodb.MongoDBCreateIndexes;

/**
 *
 * @author ZZ17807
 */
public class MSyaryoObjectShuffle {

    //Index
    //static Map<String, Map<String, List<String>>> index = new MapToJSON().toMap("axg\\shuffle_mongo_syaryo.json");
    private static DecimalFormat df = new DecimalFormat("00");

    public static void main(String[] args) {
        //元データのレイアウト
        //createHeaderMapFile();

        //テンプレート生成
        //createLayoutHeader(index);
        
        //シャッフル
        shuffle("json", "komatsuDB_PC200", new MapToJSON().toMap("axg\\shuffle_mongo_syaryo.json"), new MapToJSON().toMap("axg\\layout_mongo_syaryo.json"));
    }

    //シャッフル用ファイルを作成するための元ファイル作成
    public static void createHeaderMapFile() {
        MongoDBData mongo = MongoDBData.create();
        mongo.set("json", "komatsuDB_PC200");

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

        new MapToJSON().toJSON("mongo_syaryo.json", head);

        System.out.println(hin);
        mongo.close();
    }

    public static void shuffle(String db, String collection, Map<String, Map<String, List<String>>> index, Map<String, Map<String, List<String>>> layout) {
        MongoDBCleansingData cleanDB = MongoDBCleansingData.create();
        cleanDB.set(db, collection+"_Clean", MSyaryoObject.class);

        //Header
        MHeaderObject headerobj = cleanDB.getHeader();
        System.out.println(headerobj.map);

        long start = System.currentTimeMillis();
        
        MongoDBCleansingData shuffleDB = MongoDBCleansingData.create();
        shuffleDB.set("db", collection+"_Shuffle", MSyaryoObject.class);
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
            String data = refdata.get(header.map.get(key).indexOf(idx));
            
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

        new MapToJSON().toJSON("axg\\layout_mongo_syaryo.json", map);
    }

    private static String idxToShuffleIdx(String key, String idx) {
        if (idx.contains(".")) {
            return key + "." + idx.split("\\.")[1];
        } else {
            return key + "." + idx;
        }
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
