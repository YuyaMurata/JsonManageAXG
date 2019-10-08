/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval;

import eval.item.MainteEvaluate;
import file.ListToCSV;
import file.MapToJSON;
import java.util.List;
import java.util.Map;
import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class EvaluationSyaryoObject {
    private MongoDBPOJOData db;
    private Map<String, List<String>> def;
    
    public EvaluationSyaryoObject(String dbn, String collection, String userDefine) {
        db = MongoDBPOJOData.create();
        db.set(dbn, collection, MSyaryoObject.class);
        
        Map<String, String> temp = MapToJSON.toMap(userDefine);
        temp.entrySet().stream().forEach(d ->{
            def.put(d.getKey(), ListToCSV.toList(d.getValue()));
        });
    }
    
    public void scoring(Map<String, MSyaryoObject> map){
        //メンテナンス分析
        MainteEvaluate evalMainte = new MainteEvaluate("settings\\user\\PC200_mainteparts_interval.json", def);
        
        map.values().stream().forEach(s ->{
            evalMainte.add(s);
        });
    }
}
