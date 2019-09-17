/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.form.item;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author ZZ17807
 */
public class FormProduct {

    public static Map form(Map<String, List<String>> product, String name) {
        Map<String, List<String>> map = new TreeMap();
        String id = name.split("-")[0] + "-" + name.split("-")[2];
        String date = product.keySet().stream().findFirst().get();
        
        map.put(date, product.get(date));

        return map;
    }
}
