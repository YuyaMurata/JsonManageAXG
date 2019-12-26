/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle;

import axg.check.CheckSettings;
import axg.shuffle.form.MSyaryoObjectFormatting;
import exception.AISTProcessException;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;
import file.MapToJSON;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mongodb.MongoDBData;

/**
 *
 * @author ZZ17807
 */
public class MSyaryoObjectShuffle {

    private String db, collection;

    public MSyaryoObjectShuffle(String db, String collection) {
        this.db = db;
        this.collection = collection;
    }

    //例外処理
    private void checkSettings(MHeaderObject h, Map<String, Map<String, List<String>>> shuffleSetting, Map<String, Map<String, List<String>>> layoutSetting) throws AISTProcessException {
        CheckSettings.check(h, "シャッフル", shuffleSetting);
        CheckSettings.check(shuffleSetting, layoutSetting);
    }

    //シャッフリング実行
    public void shuffle(String shuffleSetting, String layoutSetting) throws AISTProcessException {
        MongoDBPOJOData cleanDB = MongoDBPOJOData.create();
        cleanDB.set(db, collection + "_Clean", MSyaryoObject.class);

        //設定ファイルの読み込み
        Map index = MapToJSON.toMapSJIS(shuffleSetting); //シャッフリング用
        Map layout = MapToJSON.toMapSJIS(layoutSetting); //ヘッダ用

        //Header
        MHeaderObject hobj = cleanDB.getHeader();

        //設定の検証
        checkSettings(hobj, index, layout);
        
        long start = System.currentTimeMillis();

        //シャッフリング用 Mongoコレクションを生成
        MongoDBPOJOData shuffleDB = MongoDBPOJOData.create();
        shuffleDB.set(db, collection + "_Shuffle", MSyaryoObject.class);
        shuffleDB.clear();
        shuffleDB.coll.insertOne(recreateHeaderObj(layout));

        //シャッフリング実行
        List<String> sids = cleanDB.getKeyList();
        sids.parallelStream()
                .map(sid -> shuffleOne(hobj, cleanDB.getObj(sid), index))
                .forEach(shuffleDB.coll::insertOne);

        long stop = System.currentTimeMillis();
        System.out.println("ShufflingTime=" + (stop - start) + "ms");

        shuffleDB.close();

        //中間コレクション削除
        //cleanDB.clear();
        cleanDB.close();

        //整形処理
        MSyaryoObjectFormatting.form(db, collection);
    }

    //1台のシャッフリング
    private MSyaryoObject shuffleOne(MHeaderObject header, MSyaryoObject syaryo, Map<String, Map<String, List<String>>> index) {
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

    //テスト用
    private void testPrint(String idx, Map<String, List<String>> map) {
        System.out.println(idx);

        if (map.isEmpty()) {
            System.out.println("Data Nothing");
        } else {
            map.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).forEach(System.out::println);
        }

        System.out.println("");
    }

    //インデックスのマッピング
    private Map<String, List<String>> idxMapping(Map<String, List<String>> dataMap, String subKey, List<String> idxList, MHeaderObject header, MSyaryoObject ref) {
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
            if (data.stream().filter(d -> !d.equals("")).findFirst().isPresent()) {
                dataMap.put(duplicateKey(subKey, dataMap), data);
            }
        }

        return dataMap;
    }

    //インデックス情報をデータに変換
    private String idxToData(String idx, MHeaderObject header, List<String> refdata) {
        if (refdata == null) {
            return "";
        }

        //参照先データが存在する
        if (idx.contains(".")) {

            String key = idx.split("\\.")[0];

            String data = null;
            try {
                data = refdata.get(header.getHeaderIdx(key, idx));
            } catch (Exception e) {
                System.err.println(idx);
                return "";
            }

            //空白列の除去
            if (data.replace(" ", "").equals("")) {
                return "";
            } else {
                return data;
            }

            //参照先データが存在しない
        } else {
            return idx;
        }
    }

    //ヘッダオブジェクトを指定レイアウトで作成
    private MHeaderObject recreateHeaderObj(Map<String, Map<String, List<String>>> layout) {
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

    //重複キーにインデックス番号を付加
    private String duplicateKey(String key, Map map) {
        DecimalFormat df = new DecimalFormat("00");
        int cnt = 0;
        String k = key;
        while (map.get(k) != null) {
            k = key + "#" + df.format(++cnt);
        }
        return k;
    }

    //テンプレート生成
    public static String[] createTemplate(String db, String collection, String templatePath) {
        String file = templatePath + "\\shuffle_template.json";
        String file2 = templatePath + "\\layout_template.json";
        MongoDBData mongo = MongoDBData.create();
        mongo.set(db, collection);

        List<String> hin = mongo.getHeader();
        Map<String, Map<String, List<String>>> head = new LinkedHashMap();
        Boolean flg = true;
        for (String s : hin) {
            if (s.equals("id ")) {
                continue;
            }

            //System.out.println(s);
            String k = s.split("\\.")[0];

            if (head.get(k) == null) {
                head.put(k, new LinkedHashMap());
                head.get(k).put(k + "SubKey", new ArrayList<>());
                flg = false;
            }

            if (flg) {
                head.get(k).get(k + "SubKey").add(s);
            } else {
                flg = true;
            }
        }

        MapToJSON.toJSON(file, head);
        MapToJSON.toJSON(file2, head);

        //ファイル名の取得
        String[] files = new String[2];
        files[0] = file;
        files[1] = file2;

        mongo.close();

        return files;
    }
}
