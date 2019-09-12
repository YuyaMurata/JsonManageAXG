/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.form.item;

import obj.MSyaryoObject;
import java.util.Comparator;
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

        //String stdate = syaryo.getData("出荷").keySet().stream().findFirst().get();
        formKMSMR(syaryo.getData("KOMTRAX_SMR"), header.getHeader("KOMTRAX_SMR"));
        System.out.println(syaryo.getName());
    }

    private static void formKMSMR(Map<String, List<String>> smr, List<String> index) {
        TreeMap<Integer, List<String>> map = new TreeMap<>();

        int dbIdx = index.indexOf("KOMTRAX_SMR.DB");
        int smrIdx = index.indexOf("KOMTRAX_SMR.SMR_VALUE");
        int unitIdx = index.indexOf("KOMTRAX_SMR.DAILY_UNIT");

        //KOMTRAX_ACT を取得
        Map<String, List<String>> actSMR = smr.entrySet().parallelStream()
                .filter(e -> e.getValue().get(dbIdx).equals("KOMTRAX_ACT"))
                .sorted(Comparator.comparing(e -> Integer.valueOf(e.getKey().split("#")[0]), Comparator.naturalOrder()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        Optional<String> actStartDate = actSMR.keySet().stream().findFirst();

        //KOMTRAX_ACTスタート以前のSMRを取得
        String initSMR = "0";
        if (actStartDate.isPresent()) {
            Integer stdate = Integer.valueOf(actStartDate.get().split("#")[0]);
            smr.entrySet().parallelStream()
                    .filter(e -> Integer.valueOf(e.getKey().split("#")[0]) <= stdate)
                    .filter(e -> !e.getValue().get(dbIdx).equals("KOMTRAX_ACT"))
                    .forEach(e -> map.put(Integer.valueOf(e.getKey().split("#")[0]), e.getValue()));

            //KOMTRAX_ACTのSMRを累積SMRに変換
            initSMR = map.floorEntry(stdate).getValue().get(smrIdx);
        }
        
        //ACT_DATEのSMR変換
        actToSMR(actSMR, initSMR, smrIdx, unitIdx);
        actSMR.entrySet().parallelStream()
                .forEach(e -> map.put(Integer.valueOf(e.getKey().split("#")[0]), e.getValue()));

        //マージしたものをKOMTRAX_SMRデータに上書き
        Map<String, List<String>> newMap = new TreeMap<>();
        map.entrySet().parallelStream().forEach(e -> {
            Integer k = e.getKey();
            List<String> v = e.getValue();

            Double smrValue = Double.valueOf(v.get(smrIdx)) / 60d;
            v.set(smrIdx, String.valueOf(smrValue.intValue()));

            newMap.put(k.toString(), v);
        });

        smr = new TreeMap<>();
        smr.putAll(newMap);
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
        Map<String, Double> map = new TreeMap();

        act.entrySet().parallelStream().forEach(e -> {
            map.put(e.getKey(), calcActSMR(e.getValue(), actIdx, unitIdx));
        });

        Double acm = 0d;
        if (initSMR != null) {
            acm = Double.valueOf(initSMR);
        }
        
        //累積値に変換
        Map<String, List<String>> newMap = new TreeMap();
        for (String d : map.keySet()) {
        try{
            acm += map.get(d);
            
        }catch(NullPointerException e){
            System.err.println(d+":"+map.get(d));
            e.printStackTrace();
            System.exit(0);
        }
            List<String> v = act.get(d);
            v.set(actIdx, String.valueOf(acm.intValue()));
            newMap.put(d, v);
        }
        act.putAll(newMap);
    }

    private static Double calcActSMR(List<String> actList, int actIdx, int unitIdx) {
        Double value = Double.valueOf(actList.get(actIdx));
        Double unit = Double.valueOf(actList.get(unitIdx));

        return value / unit;
    }
}
