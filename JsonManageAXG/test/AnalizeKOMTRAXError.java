
import analizer.MSyaryoAnalizer;
import exception.AISTProcessException;
import extract.SyaryoObjectExtract;
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import tools.ErrorCode;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author kaeru
 */
public class AnalizeKOMTRAXError {

    public static void main(String[] args) throws AISTProcessException {
        String db = "json";
        String col = "KM_PC200_DB_P";
        
        ErrorCode.init();

        SyaryoObjectExtract objex = new SyaryoObjectExtract(db, col);
        //ユーザー定義ファイルの設定
        objex.setUserDefine(col + "\\config\\user_define_車両除外無し.json");
        String summary = objex.getSummary();

        //10型
        List<String> t10 = objex.keySet().stream()
                .filter(sid -> sid.contains("PC200-10"))
                .collect(Collectors.toList());

        MSyaryoAnalizer a = objex.getAnalize("PC200-10--456698");
        Map<String, List<String>> kme = a.get("KOMTRAX_ERROR");
        System.out.println(kme);

        //同時発報調査
        Map<String, Integer[]> agg = new HashMap<>();
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS("エラーコード発報調査_車両リスト_PC200_10_202001.csv")){
        t10.stream().map(sid -> objex.getAnalize(sid))
                .filter(s -> s.get("KOMTRAX_ERROR") != null)
                .forEach(s -> {
                    Map<String, List<String>> d = s.get("KOMTRAX_ERROR");
                    
                    //日付→CODE
                    Map<String, Set<String>> map = new HashMap();
                    for (Map.Entry<String, List<String>> e : d.entrySet()) {
                        String date = e.getKey().split("#")[0];
                        String cd = e.getValue().get(0);
                        if (map.get(date) == null) {
                            map.put(date, new HashSet<>());
                        }
                        
                        map.get(date).add(cd);
                    }

                    //発生件数
                    map.entrySet().stream().forEach(v -> {
                        String p = String.join(",", v.getValue());
                        if (agg.get(p) == null)
                            agg.put(p, new Integer[]{0, 0});
                        
                        agg.get(p)[0]++;
                        
                        //Dump
                        String codes = v.getValue().stream()
                                .map(vi -> {
                                    try{
                                        return vi+"("+ErrorCode.define.get(vi).split(",")[0]+")";
                                    }catch(NullPointerException e){
                                        System.err.println(vi);
                                        throw new NullPointerException();
                                    }
                                    })
                                .collect(Collectors.joining(","));
                        pw.println(s.get().getName()+","+v.getKey()+",\""+codes+"\"");
                    });

                    //発生台数
                    map.values().stream().distinct().forEach(v -> {
                        String p = String.join(",", v);
                        agg.get(p)[1]++;
                    });
                    
                });
        }
        
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS("エラーコード発報調査_PC200_10_202001.csv")){
            pw.println("エラーコードの組み合わせ,件数(組み合わせごと),台数");
            agg.entrySet().stream()
                .map(v -> "\""+codesAdd(v.getKey())+"\","+v.getValue()[0]+","+v.getValue()[1])
                .forEach(pw::println);
        }
    }
    
    private static String codesAdd(String v){
        return Arrays.stream(v.split(","))
                .map(vi -> vi+":"+ErrorCode.define.get(vi))
                .collect(Collectors.joining("\n"));
    }
}
