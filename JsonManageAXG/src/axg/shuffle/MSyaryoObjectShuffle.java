/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle;

import axg.form.SyaryoObjectFormatting;
import axg.mongodb.MongoDBCleansingData;
import axg.mongodb.MongoDBData;
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
        MongoDBCleansingData mongo = MongoDBCleansingData.create();
        mongo.set(db, collection+"_Clean", MSyaryoObject.class);

        //Header
        MHeaderObject headerobj = mongo.getHeader();
        System.out.println(headerobj.map);

        //
        //System.out.println(index);
        //SyaryoObjectFormatting sobjf = new SyaryoObjectFormatting();

        MongoDBCleansingData mongo2 = MongoDBCleansingData.create();
        mongo2.set("db", collection+"_Shuffle", MSyaryoObject.class);
        mongo2.clear();
        mongo2.coll.insertOne(recreateHeaderObj(layout));
        
        List<String> sids = mongo.getKeyList();
        sids.parallelStream().forEach(sid -> {
            MSyaryoObject obj = mongo.getObj(sid); //"PC200-10- -452437"
            Map<String, Map<String, List<String>>> map = new LinkedHashMap();

            System.out.println(obj.getName());
            index.entrySet().stream().forEach(idx -> {
                //System.out.println("Main:"+idx.getKey());
                //subkey
                Map<String, List<String>> subIdx = idx.getValue();
                subIdx.entrySet().stream().forEach(idx2 -> {
                    //initialize
                    if (map.get(idx.getKey()) == null) {
                        map.put(idx.getKey(), new LinkedHashMap<>());
                    }

                    //update map
                    Map<String, List<String>> subMap = idxMapping(map.get(idx.getKey()), idx2.getKey(), idx2.getValue(), headerobj, obj);
                    map.put(idx.getKey(), subMap);

                    //
                    //testPrint(idx2.getKey() + ":" + idx2.getValue(), subMap);
                });
            });

            MSyaryoObject newobj = new MSyaryoObject();
            newobj.setName(obj.getName());
            newobj.setMap(map);
            
            //check
            //mongo2.getHeader().print();
            //SyaryoObjectFormatting.form(newheaderobj, newobj);
            newobj.recalc();
            //newobj.print();

            mongo2.coll.insertOne(newobj);
        });

        mongo2.close();
        mongo.close();
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
