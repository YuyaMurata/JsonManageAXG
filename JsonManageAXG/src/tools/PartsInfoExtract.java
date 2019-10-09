/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import file.CSVFileReadWrite;
import file.ListToCSV;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;
import static tools.MakeUserDefine.db;

/**
 *
 * @author ZZ17807
 */
public class PartsInfoExtract {
    public static MongoDBPOJOData db;
    private static String PATH = "settings\\user\\userparts\\";
    
    public static void main(String[] args) {
        db = MongoDBPOJOData.create();
        db.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);
        
        String[] uids = new String[]{"SID","部品.作番","部品.部品明細番号","部品.部品明細番号追番"};
        Map<String, String> map = getPartsList(uids);
        notexists(map, uids);
    }
    
    //ユーザー定義部品リストを取得
    private static Map<String, String> getPartsList(String[] uidNames){
        Map map = new HashMap();
        
        try {
            Files.list(Paths.get(PATH)).forEach(f ->{
                List<String> line = ListToCSV.toList(f.toString());
                List<String> h = Arrays.asList(line.get(0).split(","));
                List<Integer> uid = Arrays.stream(uidNames).map(id -> h.indexOf(id)).collect(Collectors.toList());
                line.stream().forEach(l ->{
                    String key = uid.stream().map(i -> l.split(",")[i].split("#")[0]).collect(Collectors.joining(","));
                    map.put(key, l);
                });
            });
            
        } catch (IOException ex) {
        }
        
        return map;
    }
    
    private static void notexists(Map<String, String> map, String[] uidNames){
        String dkey = "部品";
        
        MHeaderObject h = db.getHeader();
        List<Integer> uid = Arrays.stream(uidNames)
                                .filter(id -> h.getHeaderIdx(dkey, id) > -1)
                                .map(id -> h.getHeaderIdx(dkey, id)).collect(Collectors.toList());
        
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS("PC200_部品リスト_ユーザー定義を除く.csv")){
            pw.println("SID,部品.作番,"+String.join(",", h.getHeader(dkey)));
        db.getKeyList().stream()
                .map(s -> db.getObj(s))
                .filter(s -> s.getData(dkey) != null)
                .forEach(s ->{
                    s.getData(dkey).entrySet().stream()
                            .filter(e -> map.get(ukey(s.getName(), uid, e)) == null)
                            .map(e -> s.getName()+","+e.getKey()+","+String.join(",", e.getValue()))
                            .forEach(pw::println);
                });
        }
    }
    
    private static String ukey(String name, List<Integer> uid, Map.Entry<String, List<String>> e){
        String id = uid.stream().map(i -> ((List)e.getValue()).get(i).toString()).collect(Collectors.joining(","));
        return name+","+e.getKey().split("#")[0]+","+id;
    }
}
