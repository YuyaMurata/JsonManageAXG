/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extract;

import file.ListToCSV;
import file.MapToJSON;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class SyaryoObjectExtract {
    private MongoDBPOJOData db;
    public Map<String, Integer> settingsCount;
    private Map<String, MSyaryoObject> extractMap;
    private MHeaderObject header;
    private Set<String> deleteSet;
    private Map<String, List<String>> define;

    public SyaryoObjectExtract(String dbn, String collection) {
        db = MongoDBPOJOData.create();
        db.set(dbn, collection, MSyaryoObject.class);
        header = db.getHeader();
    }

    public void setUserDefine(String settingFile){
        settingsCount = new HashMap();
        deleteSet = new HashSet<>();
        
        Map<String, List<String>> settings = MapToJSON.toMap(settingFile);
        errorCheck(settings);
        
        extractMap = db.getKeyList().parallelStream().collect(Collectors.toMap(k -> k, k -> db.getObj(k)));
        
        deleteSettings(settings);
        importSettings(settings);
        printSummary();
    }

    //設定ファイルからデータ削除
    private void deleteSettings(Map<String, List<String>> settings){
        //車両IDに基づく削除
        //SID抽出
        List<String> deleteSID = settings.entrySet().parallelStream()
                .filter(def -> def.getKey().contains("#DELOBJECT_"))
                .flatMap(def -> {
                    List<String> del = def.getValue().parallelStream()
                            .map(defi -> extract(defi))
                            .flatMap(list -> list.stream().map(di -> di.split(",")[0]))
                            .distinct().collect(Collectors.toList());

                    settingsCount.put(def.getKey(), del.size());

                    return del.stream();
                }).distinct().collect(Collectors.toList());
        //SID削除
        deleteSID.stream().forEach(extractMap::remove);
        
        //データIDに基づく削除
        //SID+データID抽出
        Map<String, List<String>> deleteSIDandKey = settings.entrySet().stream()
                .filter(def -> def.getKey().contains("#DELRECORD_"))
                .flatMap(def -> {
                    List<String> del = def.getValue().parallelStream()
                            .map(defi -> extract(defi))
                            .flatMap(list -> list.stream())
                            .collect(Collectors.toList());

                    settingsCount.put(def.getKey(), del.size());

                    return del.stream();
                }).collect(Collectors.groupingBy(l -> l.split(",")[0]));
        //データ削除
        deleteRecord(deleteSIDandKey);
    }
    
    
    //分析対象データのインポート
    private void importSettings(Map<String, List<String>> settings) {        
        Map<String, List<String>> defineData = settings.entrySet().stream()
                .filter(set -> set.getKey().charAt(0) != '#')
                .flatMap(set -> {
                    Map<String, List<String>> map = new HashMap();
                    List<String> setlist = set.getValue().parallelStream()
                            .map(f -> extract(f))
                            .flatMap(list -> list.stream())
                            .collect(Collectors.toList());

                    settingsCount.put(set.getKey(), setlist.size());
                    map.put(set.getKey(), setlist);
                    
                    return map.entrySet().stream();
                }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        
        defineData.entrySet().stream().forEach(System.out::println);
        
        define = defineData;
    }
    
    //データ抽出処理
    private List<String> extract(String path){
        List<String> data;
        
        if(path.contains(".csv"))
            data = csvSettings(path);
        else
            data = dataCodeSettings(path);
        
        return data;
    }

    //SID+KEYで対象データレコードの削除と登録
    private void deleteRecord(Map<String, List<String>> records) {
        extractMap.keySet().stream()
                .forEach(sid -> {
                    //車両の抽出
                    MSyaryoObject s = extractMap.get(sid);
                    if (records.get(sid) != null) {
                        records.get(sid).stream()
                                .map(reci -> reci.split(",")[1])
                                .forEach(id -> {
                                    s.getData(id.split("\\.")[0]).remove(id.split("\\.")[1]);
                                    deleteSet.add(id.split("\\.")[0]);
                                });
                    }

                    extractMap.put(sid, s);
                });
    }

    //汎用のファイル情報取得メソッド SID+Keyを取得
    private List<String> csvSettings(String file) {
        List<String> list = ListToCSV.toList(file);
        
        if(list == null)
            return new ArrayList<>();
        
        List<String> listHeader = Arrays.asList(list.get(0).split(","));
        list.remove(0);

        //ターゲット列のデータを抽出  KEY.SBN の形式で登録
        List<String> setting = list.parallelStream()
                .map(l -> l.split(","))
                .map(l -> listHeader.stream()
                .filter(h -> h.charAt(0) != '#')
                .map(h -> h.equals("SID") ? transSID(l[listHeader.indexOf(h)]) : h.split("\\.")[0] + "." + l[listHeader.indexOf(h)]) //車両IDの正規化
                .collect(Collectors.joining(",")))
                .collect(Collectors.toList());
        return setting;
    }

    //汎用のコードマッチング情報取得メソッド SID+Keyを取得
    private List<String> dataCodeSettings(String codes) {
        String key = codes.split("\\.")[0];
        int rowID = header.getHeaderIdx(key, codes.split("\\.")[1]);
        String code = codes.split("\\.")[2];

        //入力コードに合う車両IDを抽出
        List<String> setting = new ArrayList<>();
        setting.addAll(extractMap.values().parallelStream()
                .filter(s -> s.getData(key) != null)
                .flatMap(s -> s.getData(key).entrySet().parallelStream()
                .filter(di -> di.getValue().get(rowID).equals(code))
                .map(di -> s.getName() + "," + key + "." + di.getKey()))
                .collect(Collectors.toList()));

        return setting;
    }

    //車両IDの変換
    private String transSID(String sid) {
        if (sid.split("-").length == 4) {
            return sid;
        } else {
            String kisyukiban = sid.split("-")[0] + "-" + sid.split("-")[sid.split("-").length - 1];
            Optional<String> id = extractMap.keySet().stream()
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
    private void errorCheck(Map<String, List<String>> settings){
        List<String> exception = new ArrayList<>();
        
        //ファイルチェック
        settings.entrySet().stream().forEach(f ->{
            f.getValue().stream()
                            .filter(fi -> fi.contains(".csv"))
                            .map(fi -> new File(fi))
                            .filter(fi -> !fi.exists())
                            .map(fi -> f.getKey()+": File Not Found ["+fi+"]")
                            .forEach(fi -> exception.add(fi));
        });
        
        //コードチェック
        settings.entrySet().stream().forEach(f ->{
            f.getValue().stream()
                            .filter(fi -> !fi.contains(".csv"))
                            .filter(fi -> fi.split("\\.").length < 3)
                            .map(fi -> f.getKey()+": Data Format Not Found ["+fi+"]")
                            .forEach(fi -> exception.add(fi));
        });
        
        //エラーが存在する場合の処理
        if (!exception.isEmpty()){
            System.err.println("SyaryoObjectExtractException:");
            exception.stream().map(e -> " "+e).forEach(System.err::println);
        }
    }

    //要約出力
    public void printSummary() {
        System.out.println("Map Size : " + db.getKeyList().size() + " -> " + extractMap.size());
        System.out.println("Delete/Import");
        settingsCount.entrySet().forEach(System.out::println);
        /*
        deleteSet.stream().map(h -> h.split("\\.")[0]).distinct()
                .map(h -> "  Data Size : " + h + " "
                + extractMap.keySet().parallelStream()
                        .map(sid -> db.getObj(sid))
                        .filter(s -> s.getData(h) != null)
                        .mapToInt(s -> s.getData(h).size()).sum()
                + " -> "
                + extractMap.values().parallelStream().filter(s -> s.getData(h) != null).mapToInt(s -> s.getData(h).size()).sum())
                .forEach(System.out::println);
        */
    }
    
    //MongoDBPOJOの呼び出しを模倣
    public MSyaryoObject getObj(String sid){
        return extractMap.get(sid);
    }
    
    public MHeaderObject getHeader(){
        return header;
    }
    
    //抽出処理適用後の車両マップを取得
    public Map<String, MSyaryoObject> getObjMap(){
        return extractMap;
    }
    
    //抽出処理適用後の定義ファイルを取得
    public Map<String, List<String>> getDefine(){
        return define;
    }
    
    public static void main(String[] args) {
        SyaryoObjectExtract soe = new SyaryoObjectExtract("json", "komatsuDB_PC200_Form");
        soe.setUserDefine("config\\PC200_user_define.json");
    }
}
