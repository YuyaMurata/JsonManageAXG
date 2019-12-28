/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle.form.item;

import java.util.ArrayList;
import java.util.Arrays;
import obj.MSyaryoObject;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import obj.MHeaderObject;

/**
 *
 * @author ZZ17807
 */
public class FormKomtrax extends FormItem{

    //KOMTRAXデータの整形 (値の重複除去、日付の整形、小数->整数)
    public static void form(MSyaryoObject syaryo, MHeaderObject header) {
        syaryo.setData("KOMTRAX_SMR", formKMSMR(syaryo.getData("KOMTRAX_SMR"), header.getHeader("KOMTRAX_SMR")));
    }

    private static Map formKMSMR(Map<String, List<String>> data, List<String> index) {
        if(check(data))
            return null;
        
        TreeMap<Integer, List<String>> map = new TreeMap<>();

        int dbIdx = index.indexOf("KOMTRAX_SMR.DB");
        int smrIdx = index.indexOf("KOMTRAX_SMR.SMR_VALUE");
        int unitIdx = index.indexOf("KOMTRAX_SMR.DAILY_UNIT");
        
        //KOMTRAX_ACT を取得
        Map<String, List<String>> actSMR = data.entrySet().parallelStream()
                .filter(e -> e.getValue().get(dbIdx).equals("KOMTRAX_ACT"))
                .sorted(Comparator.comparing(e -> Integer.valueOf(e.getKey().split("#")[0]), Comparator.naturalOrder()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1,e2) -> e1, LinkedHashMap::new));
        Optional<String> actStartDate = actSMR.keySet().stream().findFirst();
        
        //KOMTRAX_ACTスタート以前のSMRを取得
        String initSMR = "0";
        if (actStartDate.isPresent()) {
            Integer stdate = Integer.valueOf(actStartDate.get().split("#")[0]);
            data.entrySet().stream()
                    .filter(e -> Integer.valueOf(e.getKey().split("#")[0]) <= stdate)
                    .filter(e -> !e.getValue().get(dbIdx).equals("KOMTRAX_ACT"))
                    .forEach(e -> map.put(Integer.valueOf(e.getKey().split("#")[0]), e.getValue()));
            
            //KOMTRAX_SMRからACTの累積を計算するための初期値
            if(!map.isEmpty())
                initSMR = map.floorEntry(stdate).getValue().get(smrIdx);
        }
        
        //ACT_DATEのSMR変換
        actToSMR(actSMR, initSMR, smrIdx, unitIdx);
        actSMR.entrySet().stream()
                .forEach(e -> map.put(Integer.valueOf(e.getKey().split("#")[0]), e.getValue()));

        //マージしたものをKOMTRAX_SMRデータに上書き
        Map<String, List<String>> newMap = new TreeMap<>();
        map.entrySet().stream()
                .forEach(e -> {
            Integer k = e.getKey();
            List<String> v = e.getValue();

            Double smrValue = Double.valueOf(v.get(smrIdx)) / 60d;
            v.set(smrIdx, String.valueOf(smrValue.intValue()));

            newMap.put(k.toString(), v);
        });
        
        //List<String> initValue = Arrays.asList(new String[]{"EQP車両", "0", "1"});
        //newMap.put(initDate, initValue);
        
        return newMap;
    }
    
    //ACT_DATAの累積変換
    private static void actToSMR(Map<String, List<String>> act, String initSMR, int actIdx, int unitIdx) {
        //変換　ACT_SMR / DAILY_UNIT
        Map<String, Double> map = act.entrySet().parallelStream()
                            .collect(Collectors.toMap(
                                    e -> e.getKey(), 
                                    e -> calcActSMR(e.getValue(), actIdx, unitIdx), 
                                    (e1, e2) -> e1, 
                                    LinkedHashMap::new));
        
        //累積値に変換
        Double acm = Double.valueOf(initSMR);
        for (String d : map.keySet()) {
            acm += map.get(d);
            act.get(d).set(actIdx, String.valueOf(acm.intValue()));
        }
    }

    private static Double calcActSMR(List<String> actList, int actIdx, int unitIdx) {
        Double value = Double.valueOf(actList.get(actIdx));
        Double unit = Double.valueOf(actList.get(unitIdx));
        
        return value / unit;
    }
}
