/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle.form.item;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17807
 */
public class FormSMR extends FormItem{
    //サービスのSMRを整形
    public static Map form(Map<String, List<String>> data, List indexList, String checkNumber) {
        if (check(data)) {
            return null;
        }
        
        int db = indexList.indexOf("SMR.DB");
        int smridx = indexList.indexOf("SMR.サービスメータ");

        //日付重複除去
        List<String> dateList = data.keySet().stream()
                .filter(s -> !s.equals("")) //日付が存在しない
                .map(s -> s.split("#")[0])
                .distinct()
                .collect(Collectors.toList());
        
        Map<String, List<String>> map = new TreeMap();
        //Boolean zeroflg = false;
        for (String date : dateList) {
            String stdate = date;

            //重複日付を取り出す
            List<String> dateGroup = data.keySet().stream()
                    .filter(s -> s.contains(stdate))
                    .filter(s -> !data.get(s).get(smridx).equals("")) //SMRが存在しない
                    .filter(s -> !data.get(s).get(smridx).equals("999")) //怪しい数値の削除
                    .filter(s -> !data.get(s).get(smridx).equals("9999")) //怪しい数値の削除
                    .filter(s -> !data.get(s).get(smridx).equals(checkNumber)) //怪しい数値の削除
                    .collect(Collectors.toList());

            //欠損データのみのため
            if (dateGroup.isEmpty()) {
                continue;
            }

            if (date.length() > 8) {
                date = date.substring(0, 8);
            }

            for (String dg : dateGroup) {
                List<String> list = data.get(dg);
                //if(list.get(db).contains("KOMTRAX")){
                //    list.set(smridx, String.valueOf(Double.valueOf(list.get(smridx))/60));
                //}
                    
                if (map.get(date) == null) {
                    map.put(date, list);
                } else {
                    if (Double.valueOf(map.get(date).get(smridx)) < Double.valueOf(list.get(smridx))) {
                        map.put(date, list);
                    }
                }
                
                //整数値に変換
                list.set(smridx, String.valueOf(Double.valueOf(list.get(smridx)).intValue()));
            }
        }
        
        if (map.isEmpty()) {
            return null;
        }

        return map;
    }
}
