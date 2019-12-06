/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval;

import eval.analizer.MSyaryoAnalizer;
import eval.cluster.ClusteringESyaryo;
import eval.item.AgeSMREvaluate;
import eval.item.EvaluateTemplate;
import eval.item.MainteEvaluate;
import eval.item.UseEvaluate;
import eval.obj.ESyaryoObject;
import eval.survive.SurvivalESyaryo;
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
import py.PythonCommand;

/**
 *
 * @author ZZ17807
 */
public class SyaryoObjectEvaluation {

    public static MongoDBPOJOData db;
    private Map<String, List<String>> def;

    public SyaryoObjectEvaluation(String dbn, String collection, String userDefine) {
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

    public void scoring(Map<String, MSyaryoObject> map, String mainteSettingFile, String useSettingFile, String agesmrSettingFile, String outPath) {
        //メンテナンス分析
        Map mainteSettings = MapToJSON.toMap(mainteSettingFile);
        EvaluateTemplate evalMainte = new MainteEvaluate(mainteSettings, def);
        
        //使われ方分析
        Map useSettings = MapToJSON.toMap(useSettingFile);
        EvaluateTemplate evalUse = new UseEvaluate(useSettings, db.getHeader());
        
        //経年/SMR分析
        Map agesmrSettings = MapToJSON.toMap(agesmrSettingFile);
        EvaluateTemplate evalAgeSMR = new AgeSMREvaluate(agesmrSettings, def);
        
        
        map.values().parallelStream().forEach(s -> {
            evalMainte.add(s);
            evalUse.add(s);
            evalAgeSMR.add(s);
        });
        
        //クラスタリング
        ClusteringESyaryo.cluster(evalMainte._eval.values());
        ClusteringESyaryo.cluster(evalUse._eval.values());
        
        //スコアリング
        evalMainte.scoring();
        evalUse.scoring();
        
        //故障解析
        //SurvivalESyaryo.survival(evalMainte, evalUse, evalAgeSMR, "out");
        SurvivalESyaryo.acmfailure(evalMainte, evalUse, evalAgeSMR, outPath);
        
        print(evalMainte, outPath+"\\mainte_score.csv");
        MainteEvaluate.printImage(outPath+"\\mainte_score.csv", "AGE", "AVG", "SCORE");
        AgeSMREvaluate.printImage(outPath);
        print(evalUse, outPath+"\\use_score.csv");
        print(evalAgeSMR, outPath+"\\agesmr_score.csv");
        
        /*List<String> slist = ListToCSV.toList("file\\comp_oilfilter_PC200.csv");
        evalMainte._eval.values().stream().filter(s -> slist.contains(s.a.get().getName()))
                .forEach(s -> print(evalMainte, s));
        */
    }
    
    public static void compare(String[] pathScore){
        PythonCommand.py("py\\compare_score.py", pathScore);
    }

    public static void main(String[] args) {
        SyaryoObjectEvaluation eval = new SyaryoObjectEvaluation("json", "komatsuDB_PC200_Form", "settings\\user\\PC200_parts_userdefine.json");
        Map<String, MSyaryoObject> map = eval.db.getKeyList().stream()
                .map(s -> eval.db.getObj(s))
                .collect(Collectors.toMap(s -> s.getName(), s -> s));
        
        System.out.println("スコアリング開始");
        eval.scoring(map, 
                "settings\\user\\PC200_mainteparts_interval.json", 
                "settings\\user\\PC200_use_pumpmax.json", 
                "settings\\user\\PC200_agesmr.json", 
                "out");
        
        //比較
        SyaryoObjectEvaluation.compare(new String[]{"out", "1_0", "2_0", "3_0"});
    }

    private static void print(EvaluateTemplate eval, String file) {
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS(file)) {
            pw.println("SID,DATE,AGE,SMR," + eval._header.entrySet().stream()
                                                .flatMap(h -> h.getValue().stream()
                                                                .map(hv -> h.getKey()+"_"+hv))
                                                .collect(Collectors.joining(","))+",AVG,CID,SCORE");
            eval._eval.values().stream()
                    .map(s -> s.check())
                    .forEach(pw::println);
        }
    }

    //メンテナンスのみ
    private static void print(EvaluateTemplate evtemp, ESyaryoObject eval) {
        String file = "file\\test_print_eval_" + eval.a.syaryo.getName() + ".csv";
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS(file)) {
            //評価結果
            pw.println("評価結果");
            pw.println("SID,DATE,AGE,SMR," + evtemp._header.entrySet().stream()
                                                .flatMap(h -> h.getValue().stream()
                                                                .map(hv -> h.getKey()+"_"+hv))
                                                .collect(Collectors.joining(","))+",CID");
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
