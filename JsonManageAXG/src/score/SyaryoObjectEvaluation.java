/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package score;

import score.cluster.ClusteringESyaryo;
import score.item.AgeSMREvaluate;
import score.item.EvaluateTemplate;
import score.item.MainteEvaluate;
import score.item.UseEvaluate;
import score.obj.ESyaryoObject;
import score.survive.SurvivalESyaryo;
import extract.SyaryoObjectExtract;
import file.CSVFileReadWrite;
import file.DataConvertionUtil;
import file.MapToJSON;
import java.io.PrintWriter;
import java.util.Map;
import java.util.stream.Collectors;
import obj.MHeaderObject;
import py.PythonCommand;

/**
 *
 * @author ZZ17807
 */
public class SyaryoObjectEvaluation {
    private SyaryoObjectExtract extract;

    public SyaryoObjectEvaluation(SyaryoObjectExtract extract) {
        this.extract = extract;
    }

    public void scoring(Map mainteSettings, Map useSettings, Map agesmrSettings, String outPath) {
        //メンテナンス分析
        
        EvaluateTemplate evalMainte = new MainteEvaluate(mainteSettings, extract.getDefine());
        
        //使われ方分析
        EvaluateTemplate evalUse = new UseEvaluate(useSettings, extract.getHeader());
        
        //経年/SMR分析
        EvaluateTemplate evalAgeSMR = new AgeSMREvaluate(agesmrSettings, extract.getDefine());
        
        extract.getObjMap().values().parallelStream().forEach(s -> {
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
    
    public void compare(String[] pathScore){
        PythonCommand.py("py\\compare_score.py", pathScore);
    }

    public static void main(String[] args) {
        SyaryoObjectExtract soe = new SyaryoObjectExtract("json", "komatsuDB_PC200_Form"); //"PC200_DB_Form" with GPS "komatsuDB_PC200_Form"
        soe.setUserDefine("project\\komatsuDB_PC200\\config\\PC200_user_define.json");
        
        SyaryoObjectEvaluation eval = new SyaryoObjectEvaluation(soe);
        System.out.println("スコアリング開始");
        
        Map mainte = MapToJSON.toMap("project\\komatsuDB_PC200\\config\\PC200_maintenance.json");
        Map use = MapToJSON.toMap("project\\komatsuDB_PC200\\config\\PC200_use.json");
        Map agesmr = MapToJSON.toMap("project\\komatsuDB_PC200\\config\\PC200_agesmr.json");
        eval.scoring(
                mainte, 
                use, 
                agesmr, 
                "out");
        
        //比較
        eval.compare(new String[]{"out", "1_1", "2_1", "3_1"});
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
    private static void print(EvaluateTemplate evtemp, ESyaryoObject eval, MHeaderObject header) {
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
            pw.println("評価対象,SID,作番," + String.join(",", header.getHeader("部品"))+",日付,SMR");
            eval.sv.entrySet().stream()
                                .flatMap(e -> e.getValue().stream()
                                            .map(d -> e.getKey() + "," + d+","+eval.a.getSBNToDate(d.split(",")[1], true)+","+eval.a.getDateToSMR(eval.a.getSBNToDate(d.split(",")[1], true))))
                                .forEach(pw::println);
            pw.println();

            //評価に利用されなかったサービス
            pw.println("評価に利用されなかったサービス");
            pw.println("作番," + String.join(",", header.getHeader("部品"))+",日付,SMR");
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
