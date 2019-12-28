/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle.form.item;

import static axg.shuffle.form.item.FormItem.check;
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
public class FormParts extends FormItem{

    //部品明細を整形
    public static Map form(Map<String, List<String>> data, List<String> odrSBN, List indexList) {
        if (check(data)) {
            return null;
        }
        if (odrSBN == null) {
            return null;
        }

        Map<String, List<String>> map = new LinkedHashMap();

        int db = indexList.indexOf("部品.DB");
        int cd = indexList.indexOf("部品.品番");
        int partsMeisai = indexList.indexOf("部品.部品明細番号");
        int partsMeisaiadd = indexList.indexOf("部品.部品明細番号追番");

        for (String sbn : odrSBN) {
            //重複作番を取り出す
            List<String> sbnGroup = data.keySet().stream()
                    .filter(s -> s.split("#")[0].equals(sbn))
                    .collect(Collectors.toList());

            //KOMPAS 部品情報が存在するときは取り出す
            Optional<List<String>> kom = sbnGroup.stream()
                    .map(s -> data.get(s))
                    .filter(l -> l.get(db).equals("部品(KOMPAS)"))
                    .findFirst();
            if (kom.isPresent()) {
                sbnGroup.stream()
                        .filter(s -> !data.get(s).get(db).equals("サービス経歴(KOMPAS)"))
                        .forEach(s -> {
                            //作番に明細番号を付加 明細+追番
                            String id = s.split("#")[0]+"#"+data.get(s).get(partsMeisai)+data.get(s).get(partsMeisaiadd);
                            map.put(id, data.get(s));
                        });
            } else {
                sbnGroup.stream()
                        .filter(s -> !data.get(s).get(cd).equals(""))
                        .forEach(s -> {
                            //作番に明細番号を付加 明細+追番
                            String id = s.split("#")[0]+"#"+data.get(s).get(partsMeisai)+data.get(s).get(partsMeisaiadd);
                            map.put(s, data.get(s));
                        });
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
                        //System.err.println(sbn + ":" + q + ", cancel=" + c);
                        cancels.add(sbn);
                    } else {
                        list.set(quant, String.valueOf(q - c));
                        list.set(cancel, "0");
                    }
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
