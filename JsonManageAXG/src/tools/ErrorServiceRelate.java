/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import analizer.MSyaryoAnalizer;
import exception.AISTProcessException;
import extract.SyaryoObjectExtract;
import file.CSVFileReadWrite;
import file.ListToCSV;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import obj.MHeaderObject;

/**
 *
 * @author kaeru
 */
public class ErrorServiceRelate {
    public static void main(String[] args) throws AISTProcessException {
        String file = "toolsettings\\PC200解析＿勘田加工４.csv";
        List<String> csv = ListToCSV.toList(file);
        System.out.println(csv);
        
        //データ取得
        SyaryoObjectExtract objex = new SyaryoObjectExtract("json", "KM_PC200_DB_P");
        objex.setUserDefine("KM_PC200_DB_P\\config\\user_define_車両除外無し.json");
        objex.getSummary();
        MHeaderObject h = objex.getHeader();
        
        csv.stream()
                .filter(l -> objex.getAnalize(l.split(",")[0]) != null)
                .forEach(l -> {
                    String sid = l.split(",")[0];
                    String date = l.split(",")[2];
                    Integer smr = Integer.valueOf(l.split(",")[3]);
                    
                    MSyaryoAnalizer a = objex.getAnalize(sid);
                    Map<String, List<String>> km = a.get("KOMTRAX_ERROR");
                    Map<String,List<String>> pkm = km.entrySet().stream()
                                                        .filter(e -> (smr-24) < a.getDateToSMR(e.getKey().split("#")[0])
                                                                        && a.getDateToSMR(e.getKey().split("#")[0]) <= smr)
                                                        .sorted(Collections.reverseOrder(Map.Entry.comparingByKey()))
                                                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1,e2)->e2, LinkedHashMap::new));
                    
                    if(pkm.isEmpty()){
                        pkm = km.entrySet().stream()
                                                        .filter(e -> Integer.valueOf(e.getKey().split("#")[0]) <= Integer.valueOf(date))
                                                        .sorted(Collections.reverseOrder(Map.Entry.comparingByKey()))
                                                        .limit(3)
                                                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1,e2)->e2, LinkedHashMap::new));
                        System.out.println(date+"(直近3発報):"+pkm);
                    }else{
                        System.out.println(date+"(24h):"+pkm);
                    }
                });
    }
}
