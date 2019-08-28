/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.form.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17807
 */
public class FormParts {

    //部品明細を整形
    public static Map form(Map<String, List<String>> parts, List odrSBN, List indexList, List partsOrder) {
        if (parts == null || odrSBN == null) {
            //System.out.println("Not found Parts!");
            return null;
        }

        Map<String, List<String>> map = new LinkedHashMap();

        int db = indexList.indexOf("部品.部品(KOMPAS)");
        int cd = indexList.indexOf("部品.品番");

        for (Object sbn : odrSBN) {
            //重複作番を取り出す
            List<String> sbnGroup = parts.keySet().stream()
                    .filter(s -> s.contains(sbn.toString()))
                    .collect(Collectors.toList());

            //KOMPAS 部品情報が存在するときは取り出す
            Optional<List<String>> kom = sbnGroup.stream()
                    .map(s -> parts.get(s))
                    .filter(l -> l.get(db).equals("部品(KOMPAS)"))
                    .findFirst();
            if (kom.isPresent()) {
                sbnGroup.stream()
                        .filter(s -> !parts.get(s).get(db).equals("サービス経歴(KOMPAS)"))
                        .forEach(s -> map.put(s, parts.get(s)));
            } else {
                sbnGroup.stream()
                        .filter(s -> !parts.get(s).get(cd).equals(""))
                        .forEach(s -> map.put(s, parts.get(s)));
            }
        }

        int quant = indexList.indexOf("部品.受注数量");
        int cancel = indexList.indexOf("部品.キャンセル数量");
        int price = indexList.indexOf("部品.請求金額");
        List<String> cancels = new ArrayList();
        //金額の整形処理・キャンセル作番の削除
        for (String sbn : map.keySet()) {
            List<String> list = map.get(sbn);

            //サービス経歴から持ってきた情報は処理しない
            if (!list.get(quant).equals("")) {
                Integer q = Integer.valueOf(list.get(quant));
                Integer c = Integer.valueOf(list.get(cancel).equals("") ? "0" : list.get(cancel));
                if (q.equals(c)) {
                    cancels.add(sbn);
                } else {
                    if (c > q) {
                        System.err.println(sbn + ":" + q + ", cancel=" + c);
                        System.exit(0);
                    }

                    list.set(quant, String.valueOf(q - c));
                    list.set(cancel, "0");
                }
            }

            if (!list.get(price).equals("")) {
                list.set(price, String.valueOf(Double.valueOf(list.get(price)).intValue()));
            }
        }
        cancels.stream().forEach(s -> map.remove(s));

        return map;
    }
}
