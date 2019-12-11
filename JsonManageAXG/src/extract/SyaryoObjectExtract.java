/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extract;

import file.ListToCSV;
import file.MapToJSON;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private List<String> exception;
    private Set<String> deleteSet;

    public SyaryoObjectExtract(String dbn, String collection) {
        db = MongoDBPOJOData.create();
        db.set(dbn, collection, MSyaryoObject.class);

        keyList = db.getKeyList();
        header = db.getHeader();
    }

    public void setUserDefine(String settingFile) throws SyaryoObjectExtractException {
        exception = new ArrayList<>();
        
        settings = MapToJSON.toMap(settingFile);
        deleteSettings(settings);
        //importSettings(settings).entrySet().forEach(System.out::println);
        printSummary();
    }

    //設定ファイルからデータ削除
    private void deleteSettings(Map<String, List<String>> settings) throws SyaryoObjectExtractException {
        settingsCount = new HashMap();
        extractMap = new HashMap<>();
        deleteSet = new HashSet<>();

        //車両IDに基づく削除
        List<String> deleteSID = settings.entrySet().parallelStream()
                .filter(def -> def.getKey().contains("#DELOBJECT_"))
                .flatMap(def -> {
                    List<String> del = def.getValue().parallelStream()
                            .map(defi -> defi.contains(".csv")
                            ? csvSettings(defi) : dataCodeSettings(defi))
                            .flatMap(list -> list.stream().map(di -> di.split(",")[0]))
                            .distinct().collect(Collectors.toList());
                    
                    settingsCount.put(def.getKey(), del.size());
                    
                    return del.stream();
                }).distinct().collect(Collectors.toList());

        if(errorCheck())
            deleteSID.stream().forEach(keyList::remove);

        //データIDに基づく削除
        Map<String, List<String>> deleteSIDandKey = settings.entrySet().stream()
                .filter(def -> def.getKey().contains("#DELRECORD_"))
                .flatMap(def -> {
                    List<String> del = def.getValue().parallelStream()
                            .map(defi -> defi.contains(".csv")
                            ? csvSettings(defi) : dataCodeSettings(defi))
                            .flatMap(list -> list.stream())
                            .collect(Collectors.toList());
                    
                    settingsCount.put(def.getKey(), del.size());
                    
                    return del.stream();
                }).collect(Collectors.groupingBy(l -> l.split(",")[0]));
        
        if(errorCheck())
            deleteRecord(deleteSIDandKey);
        
        System.out.println(settingsCount);
    }

    //SID+KEYで対象データレコードの削除と登録
    private void deleteRecord(Map<String, List<String>> records) {
        keyList.stream()
                .forEach(sid -> {
                    //車両の抽出
                    MSyaryoObject s = db.getObj(sid);
                    if(records.get(sid) != null)
                        records.get(sid).stream()
                            .map(reci -> reci.split(",")[1])
                            .forEach(id -> {
                                s.getData(id.split("\\.")[0]).remove(id.split("\\.")[1]);
                                deleteSet.add(id.split("\\.")[0]);
                            });
                    
                    extractMap.put(sid, s);
                });
    }

    //汎用のファイル情報取得メソッド SID+Keyを取得
    private List<String> csvSettings(String file) {
        List<String> list = ListToCSV.toList(file);

        //例外処理
        if (list == null) {
            exception.add("File Not Found:[" + file + "]");
            return new ArrayList<>();
        }

        List<String> listHeader = Arrays.asList(list.get(0).split(","));
        list.remove(0);
        
        //ターゲット列のデータを抽出  KEY.SBN の形式で登録
        List<String> setting = list.parallelStream()
                .map(l -> l.split(","))
                .map(l -> listHeader.stream()
                    .filter(h -> h.charAt(0) != '#')
                    .map(h -> h.equals("SID")?transSID(l[listHeader.indexOf(h)]):h.split("\\.")[0]+"."+l[listHeader.indexOf(h)]) //車両IDの正規化
                .collect(Collectors.joining(",")))
                .collect(Collectors.toList());
        return setting;
    }

    //汎用のコードマッチング情報取得メソッド SID+Keyを取得
    private List<String> dataCodeSettings(String codes) {
        String key = codes.split("\\.")[0];
        int rowID = header.getHeaderIdx(key, codes.split("\\.")[1]);
        String code = codes.split("\\.")[2];

        //例外処理
        if (rowID < 0) {
            exception.add("Data Row Name Not Found:[" + codes + "]");
            return new ArrayList<>();
        }

        //入力コードに合う車両IDを抽出
        List<String> setting = new ArrayList<>();
        setting.add("SID," + codes);
        setting.addAll(keyList.parallelStream()
                .map(sid -> db.getObj(sid))
                .filter(s -> s.getData(key) != null)
                .flatMap(s -> s.getData(key).entrySet().parallelStream()
                    .filter(di -> di.getValue().get(rowID).equals(code))
                .map(di -> s.getName() + "," + key+"."+di.getKey()))
                .collect(Collectors.toList()));

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
    
    //ファイル or データキー が存在しない場合に例外処理
    private Boolean errorCheck() throws SyaryoObjectExtractException{
        if (exception.isEmpty())
            return true;
        else
            throw new SyaryoObjectExtractException(exception.toString());
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
    
    public void printSummary(){
        System.out.println("Map Size : "+db.getKeyList().size()+" -> "+extractMap.size());
        deleteSet.stream().map(h -> h.split("\\.")[0]).distinct()
                .map(h -> "  Data Size : "+h+" "+
                        extractMap.keySet().parallelStream()
                                .map(sid -> db.getObj(sid))
                                .filter(s -> s.getData(h) != null)
                                .mapToInt(s -> s.getData(h).size()).sum()
                                +" -> "+
                        extractMap.values().parallelStream().filter(s -> s.getData(h) != null).mapToInt(s -> s.getData(h).size()).sum())
                .forEach(System.out::println);
    }

    public static void main(String[] args) throws SyaryoObjectExtractException {
        SyaryoObjectExtract soe = new SyaryoObjectExtract("json", "komatsuDB_PC200_Form");
        soe.setUserDefine("config\\PC200_user_define.json");
    }
}
