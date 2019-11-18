/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.survive;

import eval.item.AgeSMREvaluate;
import eval.item.EvaluateTemplate;
import eval.obj.ESyaryoObject;
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17807
 */
public class SurvivalESyaryo {
    public static Map<String, List<String>> results = new HashMap<>();
    public static String PATH = "";
    public static String IMG_PATH = "";
    public static String X = "";
    
    public static void survival(EvaluateTemplate mainte, EvaluateTemplate use, EvaluateTemplate agesmr){
        //ファイル出力パス
        PATH = agesmr._settings.get("#PATH_FILE");
        IMG_PATH = agesmr._settings.get("#PATH_IMAGEFILE");
        
        //出力ファイル設定
        X = agesmr._settings.get("#VISUAL_X");

        //生存分析用のデータ作成
        Map<String, List<String>> data = new HashMap<>();
        agesmr._eval.keySet().stream().forEach(s ->{
            List<String> sv = new ArrayList<>();
            
            //各評価器のスコア取得
            sv.add(mainte._eval.get(s).score.toString());
            sv.add(use._eval.get(s).score.toString());
            sv.add(agesmr._eval.get(s).score.toString());
            
            //生存解析に必要なデータ取得
            List<String> d = Arrays.stream(agesmr._eval.get(s).getPoint())
                                    .boxed().map(v -> String.valueOf(v.intValue()))
                                    .collect(Collectors.toList());
            sv.addAll(d);
            
            //データ作成
            data.put(s, sv);
        });
        
        analize(data);
        results.entrySet().stream()
                            .map(r -> r.getKey()+":"+r.getValue())
                            .forEach(System.out::println);
    }
    
    private static void analize(Map<String, List<String>> data) {
        int idx_m = 0;
        int idx_u = 1;
        int idx_smr = 5;
        int idx_fstat = 6;

        //グループ分類
        Map<String, Map<String, List<String>>> group = new HashMap<>();
        data.entrySet().stream().forEach(d -> {
            String sid = d.getKey();
            String key = d.getValue().get(idx_m) + "," + d.getValue().get(idx_u);
            String smr = d.getValue().get(idx_smr);
            String fstat = d.getValue().get(idx_fstat);
            
            if (group.get(key) == null) {
                group.put(key, new HashMap<>());
            }
            
            group.get(key).put(sid, Arrays.asList(new String[]{smr, fstat}));
        });

        //グループごとの故障率
        Map<String, List<String>> map = new TreeMap<>();
        group.entrySet().stream().forEach(g -> map.putAll(km(g.getKey(), g.getValue())));
        map.entrySet().stream().forEach(e ->{
            String s = e.getValue().get(e.getValue().size()-1);
            List<String> d = new ArrayList(data.get(e.getKey()));
            d.add(s);
            
            results.put(e.getKey(), d);
        });
    }
    
    private static Map<String, List<String>> km(String gkey, Map<String, List<String>> g) {
        Map<Integer, List<String>> m = new TreeMap<>();
        g.entrySet().stream()
                .forEach(e -> {
                    Integer smr = Integer.valueOf(e.getValue().get(0));
                    if (m.get(smr) == null) {
                        m.put(smr, new ArrayList<>());
                    }
                    m.get(smr).add(e.getKey());
                });

        //カプラン・マイヤー法
        Map<Integer, Double> fail = new TreeMap<>();
        Map<Integer, Integer> count = new TreeMap<>();
        Double before = 1d;
        for (Integer smr : m.keySet()) {
            if (fail.get(smr) == null) {
                fail.put(smr, 0d);
            }
            
            Double dead = Double.valueOf(m.get(smr).stream()
                            .filter(sid -> g.get(sid).get(1).equals("1"))
                            .count());
            
            Double total = Double.valueOf(m.entrySet().stream()
                                .filter(e -> e.getKey() >= smr)
                                .mapToInt(e -> e.getValue().size())
                                .sum());
            
            Double surv = before * (total - dead) / total;
            before = surv;
            
            fail.put(smr, 1d - surv);
            count.put(smr, total.intValue());
        }
        
        //故障率データ出力
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS(PATH+gkey+"_FR.csv")){
            //ヘッダ
            pw.println(X+",COUNT,RATE");
            fail.entrySet().stream()
                    .map(df -> df.getKey()+","+count.get(df.getKey())+","+df.getValue()).forEach(pw::println);
        }
        
        //スコアリング
        Map result = scoring(g, fail);
        
        return result;
    }
    
    private static Map scoring(Map<String, List<String>> g, Map<Integer, Double> fail){
        Map<String, List<String>> result = new HashMap<>();
        
        int gidx_smr = 0;
        int gidx_fstat = 1;
        
        g.entrySet().stream().forEach(e ->{
            String sid = e.getKey();
            String fstat = e.getValue().get(gidx_fstat);
            Integer smr = Integer.valueOf(e.getValue().get(gidx_smr));
            List<String> list = new ArrayList<>(e.getValue());
            
            //スコアリング
            if(fstat.equals("0")){
                list.add("3");
            }else{
                Double surv = fail.get(smr);
                if(surv < 0.5d)
                    list.add("1");
                else
                    list.add("2");
            }
            
            result.put(sid, list);
        });
        
        return result;
    }
}
