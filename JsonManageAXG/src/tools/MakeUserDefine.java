/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import exception.AISTProcessException;
import file.CSVFileReadWrite;
import file.ListToCSV;
import file.MapToJSON;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class MakeUserDefine {
    public static MongoDBPOJOData db;
    private static List<String> keyList;
    
    public static void main(String[] args) throws AISTProcessException {
        db = MongoDBPOJOData.create();
        db.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);
        keyList = db.getKeyList();
        
        //品名から
        
        //品番から(正規表現利用)
        /*Map<String, String> data = MapToJSON.toMap("toolsettings\\PC200_mainteparts_define.json");
        data.entrySet().stream().forEach(d ->{
            partsNo(d.getKey(), "PC200_"+d.getValue()+".csv");
        });*/
        
        //品番+品名ファイルから
        Map<String, String> data = MapToJSON.toMapSJIS("toolsettings\\PC200_parts_userdefine.json");
        data.entrySet().stream().forEach(d ->{
            try {
                partsNameNo(d.getValue(), "PC200_"+d.getKey()+".csv");
            } catch (AISTProcessException ex) {
                Logger.getLogger(MakeUserDefine.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        //作番から
        //sbn("受注", "toolsettings\\PC200_エンジン.csv", "PC200_エンジン_作番抽出.csv");
    }
    
    //品番
    private static void partsNo(String rep, String out) throws AISTProcessException{
        String dkey = "部品";
        int idx = db.getHeader().getHeaderIdx(dkey, dkey+".品番");
        
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS(out)){
            pw.println("SID,部品.作番,"+String.join(",", db.getHeader().getHeader(dkey)));
            keyList.stream().map(k -> (MSyaryoObject)db.getObj(k))
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
    
    //品名+品番
    private static void partsNameNo(String file, String out) throws AISTProcessException{
        String dkey = "部品";
        int idx1 = db.getHeader().getHeaderIdx(dkey, dkey+".部品名称");
        int idx2 = db.getHeader().getHeaderIdx(dkey, dkey+".品番");
        
        System.out.println(file);
        Map<String, String> setting = ListToCSV.toList(file).stream().collect(Collectors.toMap(s -> s.split(",")[0], s -> s));
        
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS(out)){
            pw.println("SID,部品.作番,"+String.join(",", db.getHeader().getHeader(dkey)));
            keyList.stream().map(k -> (MSyaryoObject)db.getObj(k))
                    .filter(s -> s.getData(dkey) != null)
                    .peek(s -> System.out.println(s.getName()+":"+s.getCount(dkey)))
                    .forEach(s ->{
                        s.getData(dkey).entrySet().stream()
                                //.peek(d -> System.out.println(d.getValue().get(idx1)+","+d.getValue().get(idx2)))
                                .filter(d -> setting.get(d.getValue().get(idx1)+d.getValue().get(idx2)) != null)
                                .map(d -> s.getName()+","+d.getKey()+","+String.join(",", d.getValue()))
                                .forEach(pw::println);
                    });
        }
    }
    
    //SID+SBN
    private static void sbn(String dkey, String file, String out) throws AISTProcessException{
        Map<String, String> setting = ListToCSV.toList(file).stream().collect(Collectors.toMap(s -> s.split(",")[0]+s.split(",")[2].split("#")[0], s -> s));
        
        int idx = db.getHeader().getHeaderIdx(dkey, dkey+".作番");
        
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS(out)){
            pw.println("SID,作番,"+String.join(",", db.getHeader().getHeader(dkey)));
            keyList.stream().map(k -> (MSyaryoObject)db.getObj(k))
                    .filter(s -> s.getData(dkey) != null)
                    .peek(s -> System.out.println(s.getName()+":"+s.getCount(dkey)))
                    .forEach(s ->{
                        s.getData(dkey).entrySet().stream()
                                .filter(d -> setting.get(s.getName()+d.getKey()) != null)
                                .map(d -> s.getName()+","+d.getKey()+","+String.join(",", d.getValue()))
                                .forEach(pw::println);
                    });
        }
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
