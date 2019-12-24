/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle.form.item;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author ZZ17807
 */
public class FormDead extends FormItem{
    public static Map form(Map<String, List<String>> data, String leastdate, List indexList) {
        if (check(data)) {
            return null;
        }

        Map<String, List<String>> map = new TreeMap();
        String date = "0";
        for (String d : data.keySet()) {
            if (d.length() > 7) {
                //System.out.println(d);
                if (Integer.valueOf(date) < Integer.valueOf(d.split("#")[0])) {
                    date = d.split("#")[0];
                }
            }
        }

        if (!date.equals("0") && Integer.valueOf(leastdate) <= Integer.valueOf(date)) {
            map.put(date, data.get(date));
            return map;
        } else {
            return null;
        }
    }
}
