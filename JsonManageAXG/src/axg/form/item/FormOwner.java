/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.form.item;

import axg.form.MSyaryoObjectFormatting;
import axg.form.rule.DataRejectRule;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17807
 */
public class FormOwner {

    public static Map form(Map<String, List<String>> owner, List indexList, Map<String, String> honsyIndex,  DataRejectRule reject) {
        if (owner == null) {
            //System.out.println("Not found owner!");
            return null;
        }
        
        Integer company = indexList.indexOf("顧客.会社コード");
        Integer ownerID = indexList.indexOf("顧客.納入先コード");

        //本社コード揃え
        for (String d : owner.keySet()) {
            List list = owner.get(d);
            String com = list.get(company).toString();
            String id = list.get(ownerID).toString();

            if (honsyIndex.get(com + "_" + id) != null) {
                id = honsyIndex.get(com + "_" + id).split("_")[0];
            }

            //reject.addKUEC(id, d.split("#")[0]); //KUECを登録

            list.set(ownerID, id);
        }

        //ID重複排除 ##排除
        //System.out.println(owner.values().stream().map(l -> l.get(ownerID)).collect(Collectors.toList()));
        List owners = owner.values().stream()
                .map(l -> l.get(ownerID))
                .filter(id -> !id.contains("##")) //工場IDが振られていない
                .filter(id -> !id.equals("")) //IDが存在する
                .collect(Collectors.toList());
        //System.out.println(owners);
        owners = MSyaryoObjectFormatting.exSeqDuplicate(owners);

        if (owners.isEmpty()) {
            //System.out.println("使用顧客が存在しない車両(後で削除)");
            return null;
        }

        Map<String, List<String>> map = new TreeMap();
        int i = 0;
        for (String date : owner.keySet()) {
            if (date.length() >= 8) {
                String id = owner.get(date).get(ownerID).toString();
                if (id.equals(owners.get(i))) {
                    map.put(date, owner.get(date));
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
