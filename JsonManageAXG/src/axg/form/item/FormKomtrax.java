/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.form.item;

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
public class FormKomtrax {

    //KOMTRAXデータの整形 (値の重複除去、日付の整形、小数->整数)
    public static void form(MSyaryoObject syaryo, MHeaderObject header) {
        //List<String> kmList = syaryo.getMap().keySet().stream().filter(k -> k.contains("KOMTRAX_")).collect(Collectors.toList());
        if (syaryo.getData("KOMTRAX_SMR") == null) {
            return;
        }
        
        syaryo.setData("KOMTRAX_SMR", formKMSMR(syaryo.getData("KOMTRAX_SMR"), header.getHeader("KOMTRAX_SMR"), syaryo.getData("生産")));
    }

    private static Map formKMSMR(Map<String, List<String>> smr, List<String> index, Map<String, List<String>> product) {
        TreeMap<Integer, List<String>> map = new TreeMap<>();

        int dbIdx = index.indexOf("KOMTRAX_SMR.DB");
        int smrIdx = index.indexOf("KOMTRAX_SMR.SMR_VALUE");
        int unitIdx = index.indexOf("KOMTRAX_SMR.DAILY_UNIT");
        
        //初期値の設定
        String initDate = product.keySet().stream().findFirst().get().split("#")[0];
        
        //KOMTRAX_ACT を取得
        Map<String, List<String>> actSMR = smr.entrySet().parallelStream()
                .filter(e -> e.getValue().get(dbIdx).equals("KOMTRAX_ACT"))
                .sorted(Comparator.comparing(e -> Integer.valueOf(e.getKey().split("#")[0]), Comparator.naturalOrder()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1,e2) -> e1, LinkedHashMap::new));
        Optional<String> actStartDate = actSMR.keySet().stream().findFirst();
        
        //KOMTRAX_ACTスタート以前のSMRを取得
        String initSMR = "0";
        if (actStartDate.isPresent()) {
            Integer stdate = Integer.valueOf(actStartDate.get().split("#")[0]);
            smr.entrySet().stream()
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
                .filter(e -> Integer.valueOf(initDate) <= e.getKey())
                .forEach(e -> {
            Integer k = e.getKey();
            List<String> v = e.getValue();

            Double smrValue = Double.valueOf(v.get(smrIdx)) / 60d;
            v.set(smrIdx, String.valueOf(smrValue.intValue()));

            newMap.put(k.toString(), v);
        });
        
        List<String> initValue = Arrays.asList(new String[]{"EQP車両", "0", "1"});
        newMap.put(initDate, initValue);
        
        return newMap;
    }

    //KOMTRAXデータを整数値に変換(SMR, FUEL_CONSUME)
    /*private static List<String> getTransformValue(String id, List<String> kmvalue) {
        if (id.contains("SMR")) {
            kmvalue.set(0, String.valueOf(Double.valueOf(kmvalue.get(0)).intValue()));
        } else if (id.contains("FUEL")) {
            kmvalue.set(0, String.valueOf(Double.valueOf(kmvalue.get(0))));
        } else if (id.contains("GPS")) {
            //緯度
            kmvalue.set(0, String.valueOf(Double.valueOf(MapPathData.compValue(kmvalue.get(0)))));
            //経度
            kmvalue.set(1, String.valueOf(Double.valueOf(MapPathData.compValue(kmvalue.get(1)))));
        }

        return kmvalue;
    }*/
    
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
