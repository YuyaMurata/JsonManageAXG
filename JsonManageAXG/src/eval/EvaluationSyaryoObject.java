/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval;

import eval.analizer.MSyaryoAnalizer;
import eval.item.EvaluateTemplate;
import eval.item.MainteEvaluate;
import eval.obj.ESyaryoObject;
import file.CSVFileReadWrite;
import file.ListToCSV;
import file.MapToJSON;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
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
        temp.entrySet().stream().forEach(d ->{
            def.put(d.getKey(), ListToCSV.toList(d.getValue()));
            def.put(d.getKey()+"#H", Arrays.asList(new String[]{ListToCSV.toList(d.getValue()).get(0)}));
        });
    }
    
    public void scoring(Map<String, MSyaryoObject> map){
        //メンテナンス分析
        EvaluateTemplate evalMainte = new MainteEvaluate("settings\\user\\PC200_mainteparts_interval.json", def);
        
        map.values().parallelStream().forEach(s ->{
            evalMainte.add(s);
        });
        
        //print(evalMainte);
        evalMainte._eval.values().stream().limit(10)
                .forEach(s -> print(evalMainte, s));
    }
    
    public static void main(String[] args) {
        EvaluationSyaryoObject eval = new EvaluationSyaryoObject("json", "komatsuDB_PC200_Form", "settings\\user\\PC200_parts_userdefine.json");
        Map<String, MSyaryoObject> map = eval.db.getKeyList().stream()
                                                .map(s -> eval.db.getObj(s))
                                                .collect(Collectors.toMap(s -> s.getName(), s -> s));
        
        eval.scoring(map);
    }
    
    private static void print(EvaluateTemplate eval){
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS("test_print_eval.csv")){
            pw.println("SID,DATE,SMR,"+String.join(",", eval.header("メンテナンス")));
            eval._eval.values().stream()
                    .map(s -> s.check())
                    .forEach(pw::println);
        }
    }
    
    private static void print(EvaluateTemplate evtemp, ESyaryoObject eval){
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS("C:\\Users\\zz17807\\OneDrive - Komatsu Ltd\\共同研究\\メンテ検証\\raw\\test_print_eval_"+eval.a.syaryo.getName()+"_.csv")){
            //評価結果
            pw.println("評価結果");
            pw.println("SID,DATE,SMR,"+String.join(",", evtemp.header("メンテナンス")));
            pw.println(eval.check());
            pw.println();
            
            //変換データ
            pw.println("評価SMR系列");
            pw.println("評価対象,インターバル");
            eval.data.entrySet().stream().map(e -> e.getKey()+","+String.join(",", e.getValue())).forEach(pw::println);
            pw.println();
            
            //評価利用サービス
            pw.println("評価対象となったサービス群");
            pw.println("評価対象,SID,作番,"+String.join(",", db.getHeader().getHeader("部品")));
            eval.sv.entrySet().stream().flatMap(e -> e.getValue().stream().map(d -> e.getKey()+","+d)).forEach(pw::println);
            pw.println();
            
            //評価に利用されなかったサービス
            pw.println("評価に利用されなかったサービス");
            pw.println("作番,"+String.join(",", db.getHeader().getHeader("部品")));
            eval.a.syaryo.getData("部品").entrySet().stream()
                                    .filter(e -> !eval.sv.values().stream()
                                                    .flatMap(d -> d.stream().map(di -> di.split(",")[1]))
                                                    .filter(d -> d.equals(e.getKey())).findFirst().isPresent())
                                    .map(e -> e.getKey()+","+String.join(",", e.getValue()))
                                    .forEach(pw::println);
            
        }
    }
}
