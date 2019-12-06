/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extract;

import file.ListToCSV;
import file.MapToJSON;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class SyaryoObjectExtract {

    public MongoDBPOJOData db;
    public Map<String, List<String>> settings;
    public Map<String, Integer> settingsCount;
    private List<String> keyList;

    public SyaryoObjectExtract(String dbn, String collection) {
        db = MongoDBPOJOData.create();
        db.set(dbn, collection, MSyaryoObject.class);
        keyList = db.getKeyList();
    }

    public void setUserDefine(String settingFile) {
        settings = MapToJSON.toMap(settingFile);
        importSettings(settings).entrySet().forEach(System.out::println);
    }
    
    private void deleteSettings(Map<String, List<String>> settings){
        settings.entrySet().stream()
                .filter(def -> def.getKey().contains("#DELOBJ_"))
                .forEach(def -> {
                    def.getValue().stream().forEach(defi -> {
                        List<String> delList = ListToCSV.toList(defi);
                        settingsCount.put(def.getKey(), delList.size());
                    });
        });
    }
    
    private Map<String, String> importSettings(Map<String, List<String>> settings) {
        Map def = new HashMap<>();

        settings.entrySet().stream().forEach(d -> {
            d.getValue().stream()
                    .filter(di -> !di.contains("#")).forEach(di -> {
                def.put(d.getKey(), ListToCSV.toList(di));
                def.put(d.getKey() + "#H", Arrays.asList(new String[]{ListToCSV.toList(di).get(0)}));
            });
        }
    );
        
    return def ;
}

public static void main(String[] args) {
        SyaryoObjectExtract soe = new SyaryoObjectExtract("json", "komatsuDB_PC200_Form");
        soe.setUserDefine("config\\PC200_user_define.json");
    }
}
