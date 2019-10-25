/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.form.item;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class FormLoadMap {
    //KOMTRAXデータの整形 (値の重複除去、日付の整形、小数->整数)
    public static void form(MSyaryoObject syaryo, MHeaderObject header) {
        //List<String> kmList = syaryo.getMap().keySet().stream().filter(k -> k.contains("KOMTRAX_")).collect(Collectors.toList());
        if (syaryo.getData("LOADDMAP_DATE_SMR") == null) {
            return;
        }
        
        syaryo.setData("KOMTRAX_SMR", formEngTrq(syaryo.getData("LOADDMAP_エンジン水温VS作動油温"), header.getHeader("LOADDMAP_エンジン水温VS作動油温")));
    }
    
    private static Map formEngTrq(Map<String, List<String>> load, List<String> index) {
        return load.entrySet().stream()
                                .sorted(Comparator.comparing(e -> Integer.valueOf(e.getKey().replace("_", "")), Comparator.naturalOrder()))
                                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1,e2) -> e1, LinkedHashMap::new));
    }
}
