/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import file.CSVFileReadWrite;
import file.ListToCSV;
import file.MapToJSON;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class MakeUserDefine {
    public static MongoDBPOJOData db;
    private static List<String> keyList;
    
    public static void main(String[] args) {
        db = MongoDBPOJOData.create();
        db.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);
        keyList = db.getKeyList();
        
        //品名から
        
        //品番から(正規表現利用)
        Map<String, String> data = MapToJSON.toMap("toolsettings\\PC200_mainteparts_define.json");
        data.entrySet().stream().forEach(d ->{
            partsNo(d.getKey(), "PC200_"+d.getValue()+".csv");
        });
        
        //品番+品名から
        
        
        //作番から
    }
    
    //品番
    private static void partsNo(String rep, String out){
        String dkey = "部品";
        int idx = db.getHeader().getHeaderIdx(dkey, dkey+".品番");
        
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS(out)){
            pw.println("SID,部品.作番,"+String.join(",", db.getHeader().getHeader(dkey)));
            keyList.stream().map(k -> db.getObj(k))
                    .filter(s -> s.getData(dkey) != null)
                    .peek(s -> System.out.println(s.getName()+":"+s.getCount(dkey)))
                    .forEach(s ->{
                        s.getData(dkey).entrySet().stream()
                                .filter(d -> d.getValue().get(idx).matches(rep))
                                .map(d -> s.getName()+","+d.getKey()+","+String.join(",", d.getValue()))
                                .forEach(pw::println);
                    });
        }
    }
    
    //SID+SBN
    private static void sbn(String dkey, String sbnfile){
        List<String> sbns = ListToCSV.toList(sbnfile);
        
    }
    
    private static String sidCheck(String sid){
        String key = null;
        if(sid.split("-").length == 4)
            key = sid;
        else{
            Optional<String> temp = keyList.stream()
                        .map(k -> k.split(",")[0]+"-"+k.split(",")[3])
                        .filter(k -> k.equals(sid.split("-")[0]+"-"+sid.split("-")[sid.split("-").length-1]))
                        .findFirst();
            if(temp.isPresent())
                key = temp.get();
        }
        
        return key;
    }
}
