/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval;

import eval.analizer.MSyaryoAnalizer;
import eval.item.MainteEvaluate;
import file.ListToCSV;
import file.MapToJSON;
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
    public MongoDBPOJOData db;
    private Map<String, List<String>> def;
    
    public EvaluationSyaryoObject(String dbn, String collection, String userDefine) {
        db = MongoDBPOJOData.create();
        db.set(dbn, collection, MSyaryoObject.class);
        MSyaryoAnalizer.initialize(db.getHeader());
        
        Map<String, String> temp = MapToJSON.toMap(userDefine);
        def = new HashMap<>();
        temp.entrySet().stream().forEach(d ->{
            def.put(d.getKey(), ListToCSV.toList(d.getValue()));
            def.put(d.getKey()+"#H", Arrays.asList(new String[]{ListToCSV.toList(d.getValue()).get(0)}));
        });
    }
    
    public void scoring(Map<String, MSyaryoObject> map){
        //メンテナンス分析
        MainteEvaluate evalMainte = new MainteEvaluate("settings\\user\\PC200_mainteparts_interval.json", def);
        
        map.values().stream().forEach(s ->{
            evalMainte.add(s);
        });
        
        evalMainte._eval.stream()
                .map(s -> s.check())
                .forEach(System.out::println);
    }
    
    public static void main(String[] args) {
        EvaluationSyaryoObject eval = new EvaluationSyaryoObject("json", "komatsuDB_PC200_Form", "settings\\user\\PC200_parts_userdefine.json");
        Map<String, MSyaryoObject> map = eval.db.getKeyList().stream()
                                                .map(s -> eval.db.getObj(s)).limit(10)
                                                .collect(Collectors.toMap(s -> s.getName(), s -> s));
        eval.scoring(map);
    }
}
