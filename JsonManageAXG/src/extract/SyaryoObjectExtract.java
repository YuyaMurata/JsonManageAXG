/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extract;

import exception.AISTProcessException;
import analizer.MSyaryoAnalizer;
import axg.shuffle.form.MSyaryoObjectFormatting;
import axg.shuffle.form.util.FormInfoMap;
import axg.shuffle.form.util.FormalizeUtils;
import file.FileMD5;
import file.ListToCSV;
import file.MapToJSON;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;
import thread.ExecutableThreadPool;

/**
 * 車両データの抽出 パフォーマンスチューニングを行う
 *
 * @author ZZ17807
 */
public class SyaryoObjectExtract {

    private MongoDBPOJOData orgDB;
    private MongoDBPOJOData extDB;
    private MongoDBPOJOData defDB;
    private String userDefFileHash;
    //private Map<String, MSyaryoAnalizer> analizeMap;
    private MHeaderObject header;
    private List<String> keys;
    private Map<String, List<String>> settings;
    private FormInfoMap info;
    private Map<String, String> kisyKibanToSID;

    public SyaryoObjectExtract(String dbn, String collection) throws AISTProcessException {
        if (!collection.contains("_Form")) {
            collection = collection + "_Form";
        }

        if (orgDB == null) {
            orgDB = MongoDBPOJOData.create();
            orgDB.set(dbn, collection, MSyaryoObject.class);
        }

        orgDB.check();

        header = orgDB.getHeader();
        keys = orgDB.getKeyList();

        //整形時の情報を取得
        info = FormalizeUtils.getFormInfo(dbn + "." + collection);
        if (info == null) {
            info = MSyaryoObjectFormatting.setFormInfo(orgDB, dbn + "." + collection);
        }
    }

    private void setSyaryoAnalizer() throws AISTProcessException {
        MSyaryoAnalizer.initialize(header, info);
        //analizeMap = null;
    }

    //ユーザー定義ファイルの適用
    public void setUserDefine(String settingFile) throws AISTProcessException {
        System.out.println("ユーザー定義ファイルの読み込み.");

        //ユーザー定義ファイルのハッシュ値の取得
        this.userDefFileHash = FileMD5.hash(settingFile);
        this.settings = MapToJSON.toMapSJIS(settingFile);

        errorCheck(settings);

        //車両分析器の作成
        setSyaryoAnalizer();
    }

    //車両IDの変換
    private String transSID(String sid) {
        if (sid.split("-").length == 4) {
            if (keys.indexOf(sid) < 0) {
                return sid.replace(" ", "");
            }
            return sid;
        } else {
            String kisyukiban = sid.split("-")[0] + "-" + sid.split("-")[sid.split("-").length - 1];
            return kisyKibanToSID.get(kisyukiban);
        }
    }

    //ファイル or データキー が存在しない場合に例外処理
    private void errorCheck(Map<String, List<String>> settings) throws AISTProcessException {
        List<String> exception = new ArrayList<>();

        //コードチェック
        settings.entrySet().stream()
                .filter(f -> !f.getKey().contains("#SCENARIO"))
                .forEach(f -> {
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

    //設定ファイルからデータ削除
    private void deleteSettings(Map<String, List<String>> settings) {
        //車両IDに基づく削除
        //SID抽出
        List<String> deleteSID = settings.entrySet().stream()
                .filter(def -> def.getKey().contains("#DELOBJECT_"))
                .flatMap(def -> {
                    List<String> del = def.getValue().stream()
                            .map(defi -> extract(defi))
                            .flatMap(list -> list.stream().map(di -> di.split(",")[0]))
                            .distinct().collect(Collectors.toList());

                    //定義DBに登録
                    defDB.coll.insertOne(new CompressExtractionDefineFile(def.getKey(), del));

                    return del.stream();
                }).distinct().collect(Collectors.toList());

        //データIDに基づく削除
        //SID+データID抽出
        Map<String, List<String>> deleteSIDandKey = settings.entrySet().stream()
                .filter(def -> def.getKey().contains("#DELRECORD_"))
                .flatMap(def -> {
                    List<String> del = def.getValue().stream()
                            .map(defi -> extract(defi))
                            .flatMap(list -> list.stream())
                            .collect(Collectors.toList());

                    //定義DBに登録
                    defDB.coll.insertOne(new CompressExtractionDefineFile(def.getKey(), del));

                    return del.stream();
                }).collect(Collectors.groupingBy(l -> l.split(",")[0]));

        //SID+データ削除
        deleteRecord(deleteSID, deleteSIDandKey);

    }

    //SID+KEYで対象データレコードの削除と登録
    private void deleteRecord(List<String> deleteSID, Map<String, List<String>> records) {
        try {
            Map<String, String> notAnalizeSyaryo = new ConcurrentHashMap<>();
            ExecutableThreadPool.getInstance().getPool().submit(()
                    -> orgDB.getKeyList().parallelStream()
                            .filter(sid -> !deleteSID.contains(sid))
                            .map(sid -> (MSyaryoObject) orgDB.getObj(sid))
                            .map(s -> {
                                if (records.get(s.getName()) != null) {
                                    records.get(s.getName()).stream()
                                            .map(reci -> reci.split(",")[1])
                                            .forEach(id -> s.getData(id.split("\\.")[0]).remove(id.split("\\.")[1]));
                                }
                                MSyaryoAnalizer a = new MSyaryoAnalizer(s);
                                
                                //分析不可車両の記録
                                if(!a.enable)
                                    notAnalizeSyaryo.put(s.getName(), "");
                                
                                return a;
                            })
                            .filter(s -> s.enable)
                            .map(s -> new CompressExtractionObject(s))
                            .forEach(extDB.coll::insertOne)).get();
            
            //分析不可能車両を定義DBに保存
            defDB.coll.insertOne((new CompressExtractionDefineFile("#DELOBJECT_分析不可車両", new ArrayList<>(notAnalizeSyaryo.keySet()))));
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
            System.exit(0);
            System.err.println(ex.getMessage());
        }
    }

    //分析対象データのインポート
    private void importSettings(Map<String, List<String>> settings) {
        settings.entrySet().stream()
                .filter(set -> set.getKey().charAt(0) != '#')
                .map(set -> new CompressExtractionDefineFile(
                set.getKey(),
                set.getValue().stream()
                        .map(f -> extract(f))
                        .filter(list -> list != null) //存在しないファイルを読み込まない
                        .flatMap(list -> list.stream())
                        .collect(Collectors.toList())
        )).forEach(defDB.coll::insertOne);
    }

    //データ抽出処理
    private List<String> extract(String path) {
        List<String> data;
        if (path.contains(".csv")) {
            data = fileToSimplyList(path);
        } else {
            data = dataCodeSettings(path);
        }

        return data;
    }

    private List<String> fileToSimplyList(String path) {
        try {
            List<String> exceptionItem = new ArrayList<>();
            List<String> list = ListToCSV.toList(path);
            List<String> listHeader = Arrays.asList(list.get(0).split(","));
            list.remove(0); //Delete Header

            //ヘッダが存在するか確認
            listHeader.stream().filter(h -> !h.equals("SID"))
                    .filter(h -> h.charAt(0) != '#')
                    .filter(h -> !header.getHeader().contains(h))
                    .forEach(exceptionItem::add);
            if (!exceptionItem.isEmpty()) {
                System.err.println("読み込みファイルのヘッダーに誤りがあります");
                System.err.println(exceptionItem);
                throw new AISTProcessException("定義中の参照ファイルの項目がヘッダに存在しません");
            }

            return ExecutableThreadPool.getInstance().getPool().submit(()
                    -> list.parallelStream()
                            .map(l -> l.split(","))
                            .map(l -> listHeader.stream()
                            .filter(h -> h.charAt(0) != '#')
                            .map(h -> h.equals("SID") ? transSID(l[listHeader.indexOf(h)]) : h.split("\\.")[0] + "." + l[listHeader.indexOf(h)]) //車両IDの正規化
                            .collect(Collectors.joining(",")))
                            .collect(Collectors.toList())).get();
        } catch (AISTProcessException | InterruptedException | ExecutionException ex) {
            //ex.printStackTrace();
            System.err.println(ex.getMessage());
            return null;
        }
    }

    //汎用のコードマッチング情報取得メソッド SID+Keyを取得
    private List<String> dataCodeSettings(String codes) {
        String key = codes.split("\\.")[0];
        //System.out.println("extract code=" + codes);
        int rowID = header.getHeaderIdx(key, codes.split("\\.")[1]);
        String code = codes.replace(codes.split("\\.")[0] + "." + codes.split("\\.")[1] + ".", "");

        //入力コードに合う車両IDを抽出
        try {
            return ExecutableThreadPool.getInstance().getPool().submit(()
                    -> orgDB.getKeyList().parallelStream()
                            .map(sid -> (MSyaryoObject) orgDB.getObj(sid))
                            .filter(s -> s.getData(key) != null)
                            .flatMap(s -> s.getData(key).entrySet().stream()
                            .filter(di -> di.getValue().get(rowID).matches(code))
                            .map(di -> s.getName() + "," + key + "." + di.getKey()))
                            .collect(Collectors.toList())).get();
        } catch (InterruptedException | ExecutionException ex) {
            System.err.println(ex.getMessage());
            return null;
        }
    }

    public void scenarioSetting(Map<String, List<String>> settings) {
        settings.entrySet().stream()
                .filter(def -> def.getKey().contains("#SCENARIO_"))
                .forEach(def -> {
                    //定義DBに登録
                    defDB.coll.insertOne(new CompressExtractionDefineFile(def.getKey(), def.getValue()));
                });
    }

    public String getDataList() {
        StringBuilder sb = new StringBuilder();

        List<String> rec;
        sb.append("定義データ項目\n");
        rec = settings.keySet().stream()
                .filter(s -> s.charAt(0) != '#')
                .map(s -> "  " + s)
                .sorted()
                .collect(Collectors.toList());
        if (rec.isEmpty()) {
            sb.append("None");
        } else {
            sb.append(String.join("\n", rec));
        }

        sb.append("\n\n削除データ項目\n");
        rec = settings.keySet().stream()
                .filter(s -> s.contains("#DELRECORD_"))
                .map(s -> "  " + s.replace("#DELRECORD_", ""))
                .sorted()
                .collect(Collectors.toList());
        if (rec.isEmpty()) {
            sb.append("None");
        } else {
            sb.append(String.join("\n", rec));
        }

        return sb.toString();
    }

    public String getObjectList() {
        StringBuilder sb = new StringBuilder();

        List<String> rec;

        sb.append("削除オブジェクト項目\n");
        rec = settings.keySet().stream()
                .filter(s -> s.contains("#DELOBJECT_"))
                .map(s -> "  " + s.replace("#DELOBJECT_", ""))
                .collect(Collectors.toList());
        if (rec.isEmpty()) {
            sb.append("None");
        } else {
            sb.append(String.join("\n", rec));
        }

        return sb.toString();
    }

    //抽出ボタン押下処理
    public String getSummary() {
        createExtractionDB();

        return summary();
    }

    //抽出オブジェクトDB
    private void createExtractionDB() {
        String dbn = "extraction";
        String col = userDefFileHash;

        if (extDB == null) {
            extDB = MongoDBPOJOData.create();
            extDB.set(dbn, col, CompressExtractionObject.class);
        }

        dbn = "definition";
        if (defDB == null) {
            defDB = MongoDBPOJOData.create();
            defDB.set(dbn, col, CompressExtractionDefineFile.class);
        }

        //確認用
        //extDB.clear();
        //defDB.clear();

        try {
            extDB.check();
            defDB.check();
        } catch (AISTProcessException e) {
            System.out.println("抽出オブジェクト管理用コレクションの作成");
            System.out.println("ユーザー定義ファイル管理用コレクションの作成");
            //Sid -> Key Map
            kisyKibanToSID = keys.stream().collect(
                    Collectors.toMap(
                            k -> k.split("-")[0] + "-" + k.split("-")[k.split("-").length - 1],
                            k -> k));
            long start = System.currentTimeMillis();
            deleteSettings(settings);
            long stop = System.currentTimeMillis();
            importSettings(settings);
            scenarioSetting(settings);
            long fin = System.currentTimeMillis();

            System.out.println("削除:" + (stop - start));
            System.out.println("挿入:" + (fin - stop));
        }
    }

    //抽出ボタン押下後のサマリー
    private String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("車両数の変化：変化前,変化後\n");
        sb.append(info.getInfo("MACHINE_KIND"));
        sb.append(":");
        sb.append(orgDB.getKeyList().size());
        sb.append(",");
        sb.append(extDB.getKeyList().size());

        List<String> rec;

        sb.append("\n\n定義データ項目,レコード数\n");
        rec = defDB.getKeyList().stream()
                .filter(item -> item.charAt(0) != '#')
                .map(item -> getDefine(item))
                .map(def -> "  " + def.getName() + "," + def.toList().size())
                .sorted()
                .collect(Collectors.toList());
        sb.append(recToString(rec));

        sb.append("\n\n削除データ項目,レコード数\n");
        rec = defDB.getKeyList().stream()
                .filter(item -> item.contains("#DELRECORD_"))
                .map(item -> getDefine(item))
                .map(def -> "  " + def.getName().replace("#DELRECORD_", "") + "," + def.toList().size())
                .sorted()
                .collect(Collectors.toList());
        sb.append(recToString(rec));

        sb.append("\n\n削除オブジェクト項目,レコード数\n");
        rec = defDB.getKeyList().stream()
                .filter(item -> item.contains("#DELOBJECT_"))
                .map(item -> getDefine(item))
                .map(def -> "  " + def.getName().replace("#DELOBJECT_", "") + "," + def.toList().size())
                .collect(Collectors.toList());
        sb.append(recToString(rec));

        sb.append("\n\nシナリオ設定情報,値\n");
        rec = defDB.getKeyList().stream()
                .filter(item -> item.contains("#SCENARIO_"))
                .map(item -> getDefine(item))
                .map(def -> "  " + def.getName().replace("#SCENARIO_", "") + "," + def.toList().get(0))
                .collect(Collectors.toList());
        sb.append(recToString(rec));

        return sb.toString();
    }

    private String recToString(List<String> rec) {
        if (!rec.isEmpty()) {
            return String.join("\n", rec);
        } else {
            return "  None,None";
        }
    }

    //ヘッダ取得
    public MHeaderObject getHeader() {
        return header;
    }

    //抽出処理適用後の車両分析器を取得
    public List<String> keySet() {
        return extDB.getKeyList();
    }

    public CompressExtractionObject getAnalize(String sid) {
        return (CompressExtractionObject) extDB.getObj(sid);
    }

    //抽出処理適用後の定義ファイルを取得
    public List<String> getDefineItem() {
        return defDB.getKeyList().stream()
                .filter(k -> k.charAt(0) != '#')
                .sorted()
                .collect(Collectors.toList());
    }

    public CompressExtractionDefineFile getDefine(String item) {
        return (CompressExtractionDefineFile) defDB.getObj(item);
    }
}
