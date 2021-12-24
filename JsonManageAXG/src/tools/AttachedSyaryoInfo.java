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
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class AttachedSyaryoInfo {
    
    public static void main(String[] args) throws AISTProcessException {
         String file = "toolsettings\\PC200-10--452000.csv";
        List<String> csv = ListToCSV.toList(file);
        System.out.println(csv);
        
        //データ取得
        SyaryoObjectExtract objex = new SyaryoObjectExtract("json", "KM_PC200_DB_P");
        objex.setUserDefine("KM_PC200_DB_P\\config\\user_define_車両除外無し.json");
        objex.getSummary();
        MHeaderObject h = objex.getHeader();
        
        //add
        csv.stream()
                .filter(l -> objex.getAnalize(l.split(",")[1]) != null)
                .forEach(l -> {
                    String sid = l.split(",")[1];
                    String sbn = l.split(",")[2].split("#")[0];
                    
                    MSyaryoAnalizer a = objex.getAnalize(sid);
                    String date = a.getSBNToDate(sbn, Boolean.TRUE);
                    
                    Integer smr = a.getDateToSMR(date);
                    
                    System.out.println(sid+","+sbn+","+date+","+smr);
                });
    }
}
