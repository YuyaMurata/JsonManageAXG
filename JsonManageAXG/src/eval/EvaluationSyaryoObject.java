/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval;

import eval.analizer.MSyaryoAnalizer;
import eval.cluster.ClusteringESyaryo;
import eval.item.EvaluateTemplate;
import eval.item.MainteEvaluate;
import eval.obj.ESyaryoObject;
import file.CSVFileReadWrite;
import file.DataConvertionUtil;
import file.ListToCSV;
import file.MapToJSON;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class EvaluationSyaryoObject {

    public static MongoDBPOJOData db;
    private Map<String, List<String>> def;

    public EvaluationSyaryoObject(String dbn, String collection, String userDefine) {
        db = MongoDBPOJOData.create();
        db.set(dbn, collection, MSyaryoObject.class);
        MSyaryoAnalizer.initialize(dbn, collection);

        Map<String, String> temp = MapToJSON.toMap(userDefine);
        def = new HashMap<>();
        temp.entrySet().stream().forEach(d -> {
            def.put(d.getKey(), ListToCSV.toList(d.getValue()));
            def.put(d.getKey() + "#H", Arrays.asList(new String[]{ListToCSV.toList(d.getValue()).get(0)}));
        });
    }

    public void scoring(Map<String, MSyaryoObject> map) {
        //メンテナンス分析
        EvaluateTemplate evalMainte = new MainteEvaluate("settings\\user\\PC200_mainteparts_interval.json", def);

        map.values().parallelStream().forEach(s -> {
            evalMainte.add(s);
        });
        
        //クラスタリング
        ClusteringESyaryo.cluster(evalMainte._eval.values());
        
        print(evalMainte, true);
        //evalMainte._eval.values().stream().limit(100)
        //        .forEach(s -> print(evalMainte, s));
    }

    public static void main(String[] args) {
        EvaluationSyaryoObject eval = new EvaluationSyaryoObject("json", "komatsuDB_PC200_Form", "settings\\user\\PC200_parts_userdefine.json");
        Map<String, MSyaryoObject> map = eval.db.getKeyList().stream()
                .map(s -> eval.db.getObj(s))
                .collect(Collectors.toMap(s -> s.getName(), s -> s));

        eval.scoring(map);
    }

    private static void print(EvaluateTemplate eval, Boolean flg) {
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS("file\\PC200_mainte_eval.csv")) {
            pw.println("SID,DATE,AGE,SMR," + String.join(",", eval.header("メンテナンス"))+",AVG,CID");
            eval._eval.values().stream()
                    .map(s -> s.check())
                    .forEach(pw::println);
        }
        
        if(flg){
            
        }
    }

    private static void print(EvaluateTemplate evtemp, ESyaryoObject eval) {
        String file = "file\\test_print_eval_" + eval.a.syaryo.getName() + ".csv";
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS(file)) {
            //評価結果
            pw.println("評価結果");
            pw.println("SID,DATE,AGE,SMR," + String.join(",", evtemp.header("メンテナンス"))+",CID");
            pw.println(eval.check());
            pw.println();

            //変換データ
            pw.println("評価SMR系列");
            pw.println("評価対象,インターバル");
            eval.data.entrySet().stream().map(e -> e.getKey() + "," + String.join(",", e.getValue())).forEach(pw::println);
            pw.println();

            //評価利用サービス
            pw.println("評価対象となったサービス群");
            pw.println("評価対象,SID,作番," + String.join(",", db.getHeader().getHeader("部品"))+",日付,SMR");
            eval.sv.entrySet().stream()
                                .flatMap(e -> e.getValue().stream()
                                            .map(d -> e.getKey() + "," + d+","+eval.a.getSBNToDate(d.split(",")[1], true)+","+eval.a.getDateToSMR(eval.a.getSBNToDate(d.split(",")[1], true))))
                                .forEach(pw::println);
            pw.println();

            //評価に利用されなかったサービス
            pw.println("評価に利用されなかったサービス");
            pw.println("作番," + String.join(",", db.getHeader().getHeader("部品"))+",日付,SMR");
            if (eval.a.syaryo.getData("部品") != null) {
                eval.a.syaryo.getData("部品").entrySet().stream()
                        .filter(e -> !eval.sv.values().stream()
                        .flatMap(d -> d.stream().map(di -> di.split(",")[1]))
                        .filter(d -> d.equals(e.getKey())).findFirst().isPresent())
                        .map(e -> e.getKey() + "," + String.join(",", e.getValue())+","+eval.a.getSBNToDate(e.getKey(), true)+","+eval.a.getDateToSMR(eval.a.getSBNToDate(e.getKey(), true)))
                        .forEach(pw::println);
            }
        }
        
        try {
            //excelファイルの生成
            DataConvertionUtil.csvToEXCEL(file, file.replace("csv", "xls"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
