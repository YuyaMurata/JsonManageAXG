/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.template;

import file.MapToJSON;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/**
 *
 * @author kaeru
 */
public class EvalSettingsTemplate {
    public static void main(String[] args) {
        createUse("json", "komatsuDB_PC200_Form", "eval_use_template.json");
    }
    
    private static void createUse(String db, String collection, String file) {
        MongoDBPOJOData mongo = MongoDBPOJOData.create();
        mongo.set(db, collection, MSyaryoObject.class);

        MHeaderObject hobj = mongo.getHeader();
        MSyaryoObject syaryo = mongo.getKeyList().stream()
                                    .map(sid -> mongo.getObj(sid))
                                    .filter(s -> s.getData("LOADMAP_DATE_SMR") != null)
                                    .findFirst().get();
        
        Map<String, Map<String, Map<String, String>>> temp = new HashMap();
        temp.put("評価項目", new LinkedHashMap<>());
        syaryo.getMap().entrySet().stream()
                        .filter(e -> e.getKey().contains("LOADMAP"))
                        .forEach(e -> {
                            Map<String, String> map = new LinkedHashMap();
                            List<String> h = hobj.getHeader(e.getKey());
                            map.put("HEADER", String.join(",", h));
                            e.getValue().entrySet().stream()
                                            .forEach(d ->{
                                                map.put(d.getKey(), d.getValue().stream().map(di -> "1").collect(Collectors.joining(",")));
                                            });
                            temp.get("評価項目").put(e.getKey(), map);
                        });
        
        MapToJSON.toJSON(file, temp);
    }
}
