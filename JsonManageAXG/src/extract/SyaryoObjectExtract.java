/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extract;

import file.ListToCSV;
import file.MapToJSON;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class SyaryoObjectExtract {

    public MongoDBPOJOData db;
    public Map<String, List<String>> settings;
    public Map<String, Integer> settingsCount;
    private List<String> keyList;
    private Map<String, MSyaryoObject> extractMap;
    private MHeaderObject header;

    public SyaryoObjectExtract(String dbn, String collection) {
        db = MongoDBPOJOData.create();
        db.set(dbn, collection, MSyaryoObject.class);
        
        keyList = db.getKeyList();
        header = db.getHeader();
    }

    public void setUserDefine(String settingFile) {
        settings = MapToJSON.toMap(settingFile);
        deleteSettings(settings);
        //importSettings(settings).entrySet().forEach(System.out::println);
    }

    //設定ファイルからデータ削除
    private void deleteSettings(Map<String, List<String>> settings) {
        settingsCount = new HashMap<>();

        //車両IDに基づく削除
        /*settings.entrySet().stream()
                .filter(def -> def.getKey().contains("#DELOBJCT_"))
                .forEach(def -> {
                    def.getValue().stream().forEach(defi -> {
                        List<String> delete = csvSettings(defi);
                        if(delete == null)
                            delete = dataCodeSettings(defi);
                        
                        delete.stream().map(d -> transSID(d)).forEach(keyList::remove);
                        settingsCount.put(def.getKey(), delete.size());
                    });
                });
         */
        //データIDに基づく削除
        settings.entrySet().stream()
                .filter(def -> def.getKey().contains("#DELRECORD_"))
                .forEach(def -> {
                    def.getValue().stream().forEach(defi -> {
                        List<String> delete = csvSettings(defi);
                        if (delete == null) {
                            delete = dataCodeSettings(defi);
                        }

                        settingsCount.put(def.getKey(), delete.size());
                    });
                });

        System.out.println(settingsCount);
    }

    private void deleteRecord(List<String> records) {
        //削除ヘッダ SID+..
        List<String> recHeader = Arrays.asList(records.get(0).split(","));
        int idx = recHeader.indexOf("SID");

        records.stream().map(rec -> rec.split(","))
                .filter(rec -> keyList.contains(transSID(rec[idx])))
                .forEach(rec -> {
                    //車両の抽出
                    MSyaryoObject s = db.getObj(transSID(rec[idx]));

                    //データ削除
                    List<String> delkey = recHeader.stream()
                                                .filter(rh -> !rh.contains("#"))
                                                .filter(rh -> !rh.equals("SID"))
                                                .collect(Collectors.toList());
                    
                });

    }

    private List<String> csvSettings(String file) {
        if (!file.contains(".csv")) {
            return null;
        }

        List<String> list = ListToCSV.toList(file);
        List<String> listHeader = Arrays.asList(list.get(0).split(","));

        //ターゲット列のデータを抽出
        List<String> setting = list.parallelStream()
                .map(l -> l.split(","))
                .map(l -> listHeader.stream()
                .filter(h -> !h.contains("#"))
                .map(h -> l[listHeader.indexOf(h)])
                .collect(Collectors.joining(",")))
                .distinct()
                .collect(Collectors.toList());

        return setting;
    }

    private List<String> dataCodeSettings(String codes) {
        String key = codes.split("\\.")[0];
        int rowID = header.getHeaderIdx(key, codes.split("\\.")[1]);
        String code = codes.split("\\.")[2];

        //入力コードに合う車両IDを抽出
        List<String> setting = keyList.stream()
                .map(sid -> db.getObj(sid))
                .filter(s -> s.getData(key) != null)
                .filter(s -> s.getData(key).values().stream()
                .filter(si -> si.get(rowID).equals(code))
                .findFirst().isPresent())
                .map(s -> s.getName())
                .distinct()
                .collect(Collectors.toList());
        return setting;
    }

    //車両IDの変換
    private String transSID(String sid) {
        if (sid.split("-").length == 4) {
            return sid;
        } else {
            String kisyukiban = sid.split("-")[0] + "-" + sid.split("-")[sid.split("-").length - 1];
            Optional<String> id = keyList.stream()
                    .filter(s -> kisyukiban.equals(s.split("-")[0] + "-" + s.split("-")[s.split("-").length - 1]))
                    .findFirst();

            if (id.isPresent()) {
                return id.get();
            } else {
                return null;
            }
        }
    }

    private Map<String, String> importSettings(Map<String, List<String>> settings) {
        Map def = new HashMap<>();

        settings.entrySet().stream().forEach(d -> {
            d.getValue().stream()
                    .filter(di -> !di.contains("#")).forEach(di -> {
                def.put(d.getKey(), ListToCSV.toList(di));
                def.put(d.getKey() + "#H", Arrays.asList(new String[]{ListToCSV.toList(di).get(0)}));
            });
        }
        );

        return def;
    }

    private List<String> csvFileSettings(String file) {
        if (!file.contains(".csv")) {
            return null;
        } else {
            return ListToCSV.toList(file);
        }
    }

    public static void main(String[] args) {
        SyaryoObjectExtract soe = new SyaryoObjectExtract("json", "komatsuDB_PC200_Form");
        soe.setUserDefine("config\\PC200_user_define.json");
    }
}
