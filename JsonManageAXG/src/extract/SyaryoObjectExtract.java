/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extract;

import compress.SnappyMap;
import exception.AISTProcessException;
import analizer.MSyaryoAnalizer;
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
import java.util.TreeMap;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/**
 * 車両データの抽出
 *
 * @author ZZ17807
 */
public class SyaryoObjectExtract {

    public Map<String, Integer> settingsCount;
    private int masterSize;
    private Map<String, byte[]> compressMap;
    private Map<String, MSyaryoObject> extractMap;
    private Map<String, MSyaryoAnalizer> analizeMap;
    private MHeaderObject header;
    private Set<String> deleteSet;
    private Map<String, List<String>> define;
    private Map<String, byte[]> csv;

    public SyaryoObjectExtract(String dbn, String collection) throws AISTProcessException {
        if (!collection.contains("_Form")) {
            collection = collection + "_Form";
        }

        MongoDBPOJOData db = MongoDBPOJOData.create();
        db.set(dbn, collection, MSyaryoObject.class);
        db.check();

        header = db.getHeader();

        compressMap = db.getKeyList().parallelStream()//.limit(10) //テスト用
                .map(sid -> db.getObj(sid))
                .collect(Collectors.toMap(s -> s.getName(), s -> compress(s)));

        masterSize = compressMap.size();
        
        db.close();
    }

    private void setSyaryoAnalizer() throws AISTProcessException {
        MSyaryoAnalizer.initialize(header, extractMap);
        analizeMap = null;
    }

    //ユーザー定義ファイルの適用
    public void setUserDefine(String settingFile) throws AISTProcessException {
        System.out.println("ユーザー定義ファイルの読み込み.");
        settingsCount = new HashMap();
        deleteSet = new HashSet<>();

        //解凍
        extractMap = compressMap.values().parallelStream()
                .map(s -> (MSyaryoObject) decompress(s))
                .collect(Collectors.toMap(s -> s.getName(), s -> s));
        System.out.println("車両オブジェクトの解凍.");

        Map<String, List<String>> settings = MapToJSON.toMapSJIS(settingFile);
        this.csv = readCSVSettings(settings);
        errorCheck(settings);

        deleteSettings(settings);
        importSettings(settings);

        //車両分析器の作成
        setSyaryoAnalizer();
    }

    //設定ファイルからデータ削除
    private void deleteSettings(Map<String, List<String>> settings) {
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
        Map<String, List<String>> defineData = settings.entrySet().parallelStream()
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
                }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (a,b) -> b, TreeMap::new));

        //defineData.entrySet().stream().forEach(System.out::println);
        define = defineData;
    }

    //データ抽出処理
    private List<String> extract(String path) {
        List<String> data;

        if (path.contains(".csv")) {
            System.out.println(path);
            data = (List<String>) decompress(this.csv.get(path));
        } else {
            data = dataCodeSettings(path);
        }

        return data;
    }

    //SID+KEYで対象データレコードの削除と登録
    private void deleteRecord(Map<String, List<String>> records) {
        extractMap.values().parallelStream()
                .forEach(s -> {
                    if (records.get(s.getName()) != null) {
                        records.get(s.getName()).parallelStream()
                                .map(reci -> reci.split(",")[1])
                                .forEach(id -> {
                                    s.getData(id.split("\\.")[0]).remove(id.split("\\.")[1]);
                                    deleteSet.add(id.split("\\.")[0]);
                                });
                    }

                    extractMap.put(s.getName(), s);
                });
    }

    //汎用のファイル情報取得メソッド SID+Keyを取得
    private Map<String, byte[]> readCSVSettings(Map<String, List<String>> settings) throws AISTProcessException {
        //読み込むCSVリストを取得
        List<String> files = settings.values().stream()
                .flatMap(s -> s.stream())
                .filter(f -> f.contains(".csv"))
                .collect(Collectors.toList());

        List<String> exception = new ArrayList<>();
        List<String> exceptionItem = new ArrayList<>();

        Map<String, byte[]> csvSettings = new HashMap<>();

        for (String f : files) {
            try {
                List<String> list = ListToCSV.toList(f);

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

                //ヘッダが存在するか確認
                listHeader.stream().filter(h -> !h.equals("SID"))
                        .filter(h -> h.charAt(0) != '#')
                        .filter(h -> !header.getHeader().contains(h))
                        .forEach(exceptionItem::add);

                csvSettings.put(f, compress(setting));
            } catch (AISTProcessException e) {
                System.err.println(f);
                exception.add(f);
            }
        }

        //ファイルが存在し場合の処理
        if (!exception.isEmpty()) {
            System.out.println("定義中の参照ファイルが存在しません："+exception);
            //throw new AISTProcessException("定義ファイル内の設定ファイルが存在しません：" + exception);
        }
        
        if (!exceptionItem.isEmpty() && exception.isEmpty()) {
            throw new AISTProcessException("定義中の参照ファイルの項目がヘッダに存在しません：" + exceptionItem);
        }

        return csvSettings;
    }

    //汎用のコードマッチング情報取得メソッド SID+Keyを取得
    private List<String> dataCodeSettings(String codes) {
        String key = codes.split("\\.")[0];
        System.out.println(codes);
        int rowID = header.getHeaderIdx(key, codes.split("\\.")[1]);
        String code = codes.replace(codes.split("\\.")[0] + "." + codes.split("\\.")[1] + ".", "");

        //入力コードに合う車両IDを抽出
        List<String> setting = new ArrayList<>();
        setting.addAll(extractMap.values().parallelStream()
                .filter(s -> s.getData(key) != null)
                .flatMap(s -> s.getData(key).entrySet().parallelStream()
                .filter(di -> di.getValue().get(rowID).matches(code))
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
            Optional<String> id = extractMap.keySet().parallelStream()
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
    private void errorCheck(Map<String, List<String>> settings) throws AISTProcessException {
        List<String> exception = new ArrayList<>();

        //コードチェック
        settings.entrySet().stream().forEach(f -> {
            f.getValue().stream()
                    .filter(fi -> !fi.contains(".csv"))
                    .filter(fi -> fi.split("\\.").length < 3)
                    .map(fi -> f.getKey() + ": Data Format Not Found [" + fi + "]")
                    .forEach(fi -> exception.add(fi));
        });

        //エラーが存在する場合の処理
        if (!exception.isEmpty()) {
            throw new AISTProcessException("定義ファイル内のデータ設定に誤りがあります：" + exception);
        }
    }

    public MHeaderObject getHeader() {
        return header;
    }

    //抽出処理適用後の車両分析器を取得
    public Map<String, MSyaryoAnalizer> getObjMap() {
        if (analizeMap == null) {
            System.out.println("Setting Syaryo Analizer!");
            analizeMap = extractMap.values().parallelStream()
                    .map(s -> new MSyaryoAnalizer(s))
                    .collect(Collectors.toMap(sa -> sa.syaryo.getName(), sa -> sa));
        }

        return analizeMap;
    }

    //抽出処理適用後の定義ファイルを取得
    public List<String> getDefineItem() {
        return define.keySet().stream()
                .filter(k -> k.charAt(0) != '#')
                .collect(Collectors.toList());
    }

    public Map<String, List<String>> getDefine() {
        return define;
    }

    /**
     * データ圧縮
     */
    private byte[] compress(Object obj) {
        return SnappyMap.toSnappy(obj);
    }

    /**
     * データ解凍
     */
    private Object decompress(byte[] b) {
        if(b == null)
            return new ArrayList();
        return SnappyMap.toObject(b);
    }

    public String getDataList() {
        StringBuilder sb = new StringBuilder();
        sb.append("定義データ項目\n");
        sb.append(settingsCount.entrySet().stream()
                .filter(s -> s.getKey().charAt(0) != '#')
                .map(s -> s.getKey() + "=" + s.getValue())
                .sorted()
                .collect(Collectors.joining("\n")));
        
        sb.append("\n\n削除データ項目\n");
        List<String> del = settingsCount.entrySet().stream()
                .filter(s -> s.getKey().contains("#DELRECORD_"))
                .map(s -> s.getKey().replace("#DELRECORD_", "") + "=" + s.getValue())
                .sorted()
                .collect(Collectors.toList());
        sb.append(del.stream().collect(Collectors.joining("\n")));
        
        return sb.toString();
    }

    public String getObjectList() {
        StringBuilder sb = new StringBuilder();
        sb.append("削除オブジェクト項目\n");

        sb.append(settingsCount.entrySet().stream()
                .filter(s -> s.getKey().contains("#DELOBJECT_"))
                .map(s -> s.getKey().replace("#DELOBJECT_", "") + "=" + s.getValue())
                .collect(Collectors.joining("\n")));

        return sb.toString();
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("車両数の変化：\n");
        sb.append("   " + masterSize + "  ->  ");
        sb.append(extractMap.size() + "\n\n");

        sb.append("データ数の変化：\n");
        List<String> del = deleteSet.stream().map(h -> h.split("\\.")[0]).distinct()
                .map(h -> "  Data Size : " + h + " "
                + extractMap.values().parallelStream()
                        .filter(s -> s.getCount(h) != null)
                        .mapToInt(s -> s.getCount(h)).sum()
                + " -> "
                + extractMap.values().parallelStream()
                        .filter(s -> s.getData(h) != null)
                        .mapToInt(s -> s.getData(h).size()).sum())
                .collect(Collectors.toList());

        if (!del.isEmpty()) {
            sb.append(del.stream().collect(Collectors.joining("\n")));
        } else {
            sb.append("    削除されたデータはありません．");
        }

        return sb.toString();
    }

    public static void main(String[] args) throws AISTProcessException {
        SyaryoObjectExtract soe = new SyaryoObjectExtract("json", "testDB_Form");
        soe.setUserDefine("config\\PC200_user_define.json");
    }
}
