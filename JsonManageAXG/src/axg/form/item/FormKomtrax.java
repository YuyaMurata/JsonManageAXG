/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.form.item;

import axg.form.SyaryoObjectFormatting;
import axg.obj.MSyaryoObject;
import google.map.MapPathData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17807
 */
public class FormKomtrax {

    //KOMTRAXデータの整形 (値の重複除去、日付の整形、小数->整数)
    public static void form(MSyaryoObject syaryo) {
        List<String> kmList = syaryo.getMap().keySet().stream().filter(k -> k.contains("KOMTRAX_")).collect(Collectors.toList());
        if(kmList.isEmpty())
            return ;
        
        String stdate = syaryo.getData("出荷").keySet().stream().findFirst().get();
        
        for (String id : kmList) {
            if (syaryo.getData(id) == null) {
                continue;
            }
            
            //Formalize Date
            Map<String, List<String>> newMap = new TreeMap();
            String tmp = "";
            for (String date : syaryo.getData(id).keySet()) {

                String d = date.split("#")[0];
                
                String str = syaryo.getData(id).get(date).toString();

                //出荷より前のセンサー情報を消す
                if (Integer.valueOf(d) < Integer.valueOf(stdate)) {
                    continue;
                }

                if (!tmp.equals(str)) {
                    List value = getTransformValue(id, syaryo.getData(id).get(date));
                    if (id.equals("KOMTRAX_SMR")) {
                        value.set(0, String.valueOf(Integer.valueOf(value.get(0).toString())/60));
                        newMap.put(d, value);
                    } else if (id.equals("KOMTRAX_ACT")) {  //ACT_DATAの構造が変わると使えない
                        if (newMap.get(d) == null) {
                            newMap.put(d, value);
                        } else {
                            Integer dup = Integer.valueOf(newMap.get(d).get(0)) + Integer.valueOf(value.get(0).toString());
                            newMap.get(d).set(0, dup.toString());
                        }
                    } else {
                        newMap.put(SyaryoObjectFormatting.dup(d, newMap), value);
                    }
                    tmp = str;
                }
            }
            
            if(!newMap.isEmpty())
                syaryo.setData(id, newMap);
            else
                syaryo.getMap().remove(id);
        }
        
        Map<String, List<String>> newMap = transACTSMRData(syaryo.getData("KOMTRAX_ACT"), syaryo.getData("KOMTRAX_SMR"));
        if(!newMap.isEmpty())
            syaryo.setData("KOMTRAX_ACT", newMap);
    }

    //KOMTRAXデータを整数値に変換(SMR, FUEL_CONSUME)
    private static List<String> getTransformValue(String id, List<String> kmvalue) {
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
    }

    //ACT_DATAの累積変換
    private static Map transACTSMRData(Map<String, List<String>> act, Map<String, List<String>> smr) {
        Map<String, Double> map = new TreeMap();

        if (act == null) {
            return map;
        }

        act.entrySet().forEach(s -> {
            map.put(s.getKey(), calcActSMR(s.getValue()));
        });

        //初期SMRをACTに追加
        if (smr != null) {
            try{
            Integer date = Integer.valueOf(map.keySet().stream().findFirst().get());
            List<String> smrList = smr.keySet().stream().filter(k -> Integer.valueOf(k) <= date).collect(Collectors.toList());
            if (!smrList.isEmpty()) {
                String initSMR = smrList.get(smrList.size() - 1);
                Double vinitSMR = Double.valueOf(smr.get(initSMR).get(0));
                if (map.get(initSMR) == null) {
                    map.put(initSMR, vinitSMR);
                } else {
                    Double v = map.get(initSMR) + vinitSMR;
                    map.put(initSMR, v);
                }
            }
            }catch(Exception e){
                System.err.println(act);
                e.printStackTrace();
                System.exit(0);
            }
        }

        //累積値に変換
        Double acm = 0d;
        Map<String, List<String>> actmap = new TreeMap();
        for (String d : map.keySet()) {
            acm += map.get(d);
            List v = new ArrayList();
            v.add(String.valueOf(acm.intValue()));
            v.add(String.valueOf(map.get(d).intValue()));
            actmap.put(d, v);
        }

        return actmap;
    }

    private static Double calcActSMR(List<String> actList) {
        Integer value = Integer.valueOf(actList.get(0));
        Integer unit = Integer.valueOf(actList.get(1));

        return value / unit / 60d;
    }
}
