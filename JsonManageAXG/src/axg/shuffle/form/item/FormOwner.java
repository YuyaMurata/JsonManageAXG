/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle.form.item;

import static axg.shuffle.form.item.FormItem.check;
import axg.shuffle.form.rule.DataRejectRule;
import axg.shuffle.form.util.FormalizeUtils;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17807
 */
public class FormOwner extends FormItem{

    public static Map form(Map<String, List<String>> data, List indexList, DataRejectRule reject) {
        if (check(data)) {
            return null;
        }
        
        Integer ownerID = indexList.indexOf("顧客.納入先コード");
        if (ownerID < 0) {
            return null;
        }

        //ID重複排除 ##排除
        List owners = data.values().stream()
                .map(l -> l.get(ownerID))
                .filter(id -> !id.contains("##")) //工場IDが振られていない
                .filter(id -> !id.equals("")) //IDが存在する
                .collect(Collectors.toList());
        //System.out.println(owners);
        owners = FormalizeUtils.exSeqDuplicate(owners);

        if (owners.isEmpty()) {
            //System.out.println("使用顧客が存在しない車両(後で削除)");
            return null;
        }

        Map<String, List<String>> map = new TreeMap();
        int i = 0;
        for (String date : data.keySet()) {
            if (date.length() >= 8) {
                String id = data.get(date).get(ownerID);
                if (id.equals(owners.get(i))) {
                    map.put(date, data.get(date));
                    i++;
                    if (owners.size() <= i) {
                        break;
                    }
                }
            }
        }

        return map;
    }
}
