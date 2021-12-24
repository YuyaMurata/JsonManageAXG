/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import exception.AISTProcessException;
import extract.SyaryoObjectExtract;
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import obj.MHeaderObject;

/**
 *
 * @author kaeru
 */
public class ServiceSummary {
    public static void main(String[] args) throws AISTProcessException {
        //データ取得
        SyaryoObjectExtract objex = new SyaryoObjectExtract("json", "KM_PC200_DB_P");
        objex.setUserDefine("KM_PC200_DB_P\\config\\user_define.json");
        objex.getSummary();
        
        outCategoryData(objex, "C1-1:エンジンOH");
        //dailyTotal(objex);
    }
    
    //
    public static void outCategoryData(SyaryoObjectExtract objex, String c) throws AISTProcessException{
        MHeaderObject h = objex.getHeader();
        
        List<String> data = objex.getDefine(c).toList().stream()
                            .map(def -> def.replace("部品.", "").split(","))
                            .filter(def -> objex.getAnalize(def[0])!=null)
                            .filter(def -> objex.getAnalize(def[0]).get("受注").get(def[1].split("#")[0])!=null)
                            .map(def -> {
                                List<String> odr = objex.getAnalize(def[0]).get("受注").get(def[1].split("#")[0]);
                                String d = odr.get(h.getHeaderIdx("受注", "受注日"));
                                return def[0]+","+def[1].split("#")[0]+","+d+","+objex.getAnalize(def[0]).getDateToSMR(d);
                            }).collect(Collectors.toList());
        
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS("PC200_CategoryData.csv")){
            data.stream().forEach(pw::println);
        }
    }
    
    public static void dailyTotal(SyaryoObjectExtract objex) throws AISTProcessException{
        MHeaderObject h = objex.getHeader();
        
        Map<String, Long> counts = objex.keySet().stream()
                .map(sid -> objex.getAnalize(sid))
                .filter(a -> a.get("受注") != null)
                .map(a -> a.get("受注"))
                .flatMap(odr -> odr.values().stream())
                .filter(odr -> odr.get(h.getHeaderIdx("受注", "作業形態コード")).equals("AD"))
                .collect(Collectors.groupingBy(odr -> odr.get(h.getHeaderIdx("受注", "受注日")), Collectors.counting()));
        
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS("PC200_サービスDAILYTOTAL.csv")){
            counts.entrySet().stream().map(e -> e.getKey()+","+e.getValue()).forEach(pw::println);
        }
    } 
}
