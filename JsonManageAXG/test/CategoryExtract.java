
import file.CSVFileReadWrite;
import file.ListToCSV;
import file.MapToJSON;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ZZ17807
 */
public class CategoryExtract {
    private static MongoDBPOJOData shDB;
    
    public static void main(String[] args) {
        shDB = MongoDBPOJOData.create();
        shDB.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);
        
        //カテゴリ表の読み込み
        Map<String, String> category = categoryMapping(ListToCSV.toList("file\\PC200_部品カテゴリ表.csv"));
        
        //データからカテゴリを抽出
        MHeaderObject h = shDB.getHeader();
        Map<String, List<String>> extract = new HashMap();
        extract.put("カテゴリ外", new ArrayList<>());
        shDB.getKeyList().stream().map(sid -> shDB.getObj(sid))
                .filter(s -> s.getData("部品") != null)
                .forEach(s ->{
                    s.getData("部品").entrySet().stream().forEach(d -> {
                        String key = d.getValue().get(h.getHeaderIdx("部品", "部品名称"))+","+
                                        d.getValue().get(h.getHeaderIdx("部品", "品番"));
                        String cat = category.get(key);
                        
                        if(cat != null){
                            if(extract.get(cat) == null)
                                extract.put(cat, new ArrayList<>());
                            extract.get(cat).add(s.getName()+","+cat+","+d.getKey()+","+String.join(",", d.getValue()));
                        }else{
                            extract.get("カテゴリ外").add(s.getName()+",カテゴリ外,"+d.getKey()+","+String.join(",", d.getValue()));
                        }
                    });
                });
        
        extract.entrySet().stream().forEach(e ->{
            try(PrintWriter pw = CSVFileReadWrite.writerSJIS("file\\"+e.getKey().replace("?", "確認")+".csv")){
                //header
                pw.println("SID,#Category,部品.作番,"+h.getHeader("部品").stream().map(hi -> "#"+hi).collect(Collectors.joining(",")));
                
                e.getValue().stream().forEach(pw::println);
            }
        });
    }
    
    private static Map<String, String> categoryMapping(List<String> list){
        List<String> header = Arrays.asList(list.get(0).split(","));
        list.remove(0);
        
        //品名,品番
        int pname = header.indexOf("部品.部品名称");
        int pno = header.indexOf("部品.品番");
        
        //カテゴリ-サブカテゴリ
        int cat = header.indexOf("カテゴリ");
        int subcat = header.indexOf("サブカテゴリ");
        
        Map<String, String> map = list.stream().map(l -> l.split(","))
                                    .filter(l -> l.length > 2)
                                    .collect(Collectors.toMap(l -> 
                                            l[pname]+","+l[pno], 
                                            l -> l.length > 3?l[cat]+"-"+l[subcat]:l[cat]+"-"));
        
        return map;
    }
}
