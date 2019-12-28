/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import analizer.MSyaryoAnalizer;
import exception.AISTProcessException;
import file.CSVFileReadWrite;
import file.ListToCSV;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;
import static tools.MakeUserDefine.db;

/**
 *
 * @author ZZ17807
 */
public class AttachedSyaryoInfo {
    public static MongoDBPOJOData db;
    private static List<String> keyList;
    
    public static void main(String[] args) throws AISTProcessException {
        db = MongoDBPOJOData.create();
        db.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);
        keyList = db.getKeyList();
        
        MSyaryoAnalizer.initialize(db.getHeader(), db.getObjMap());
        
        //add
        addInfo("PC200_エンジンOV.csv", "AttachedInfo_PC200_エンジンOV.csv");
    }
    
    private static void addInfo(String file, String out) throws AISTProcessException{
        //日付の追加
        //SMRの追加
        
        //SID+作番
        Map<String, String> setting = new HashMap<>();
        ListToCSV.toList(file).stream().forEach(s -> setting.put(s.split(",")[0]+","+s.split(",")[1].split("#")[0], s));
        List<String> h = Arrays.asList(setting.get("SID,部品.作番").split(","));
        setting.remove("SID,部品.作番");
        
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS(out)){
            pw.println("SID,作番,日付,SMR,SVSMR,"+String.join(",", h));
            setting.entrySet().stream().forEach(e -> {
                String sid = e.getKey().split(",")[0];
                String sbn = e.getKey().split(",")[1].split("#")[0];
                MSyaryoAnalizer s = new MSyaryoAnalizer(db.getObj(sid));
                String date = s.getSBNToDate(sbn, true);
                Integer smr = s.getDateToSMR(date);
                Integer svsmr = s.getDateToSVSMR(date);
                pw.println(e.getKey().split("#")[0]+","+date+","+smr+","+svsmr+","+e.getValue());
            });
        }
    }
}
