/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle.form.item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17807
 */
public class FormWork extends FormItem{

    //作業明細を整形
    public static Map form(Map<String, List<String>> data, List<String> odrSBN, List indexList) {
        if (check(data)) {
            return null;
        }
        
        if (odrSBN == null) {
            //System.out.println("Not found Work!");
            return null;
        }

        Map<String, List<String>> map = new LinkedHashMap();

        int db = indexList.indexOf("作業.DB");
        int cd = indexList.indexOf("作業.作業コード");
        int workMeisai = indexList.indexOf("作業.作業明細番号");

        for (String sbn : odrSBN) {
            //重複作番を取り出す
            List<String> sbnGroup = data.keySet().stream()
                    .filter(s -> s.split("#")[0].equals(sbn.toString()))
                    .collect(Collectors.toList());

            //KOMPAS 作業情報が存在するときは取り出す
            Optional<List<String>> kom = sbnGroup.stream()
                    .map(s -> data.get(s))
                    .filter(l -> l.get(db).equals("作業(KOMPAS)"))
                    .findFirst();

            if (kom.isPresent()) {
                sbnGroup.stream()
                        .filter(s -> !data.get(s).get(db).equals("サービス経歴(KOMPAS)"))
                        .forEach(s -> {
                            String id = s.split("#")[0] + "#" + data.get(s).get(workMeisai);
                            map.put(id, data.get(s));
                        });
            } else {
                sbnGroup.stream()
                        .filter(s -> !data.get(s).get(cd).equals("")) //作業コードが存在しないときは削除
                        .forEach(s -> {
                            String id = s.split("#")[0] + "#" + data.get(s).get(workMeisai);
                            map.put(id, data.get(s));
                        });
            }
        }

        Integer[] kosu = new Integer[]{
            indexList.indexOf("作業.標準工数"),
            indexList.indexOf("作業.請求工数"),
            indexList.indexOf("作業.指示工数")
        };
        int price = indexList.indexOf("作業.請求金額");

        //工数・金額の整形処理
        for (String sbn : map.keySet()) {
            List<String> list = map.get(sbn);
            for (int i : kosu) {
                if (!list.get(i).equals("")) {
                    list.set(i, String.valueOf(Double.valueOf(list.get(i)).floatValue()));
                }
            }
            if (!list.get(price).equals("")) {
                list.set(price, String.valueOf(Double.valueOf(list.get(price)).intValue()));
            }
        }

        return map;
    }
}
