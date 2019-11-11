/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.survive;

import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author ZZ17807
 */
public class SurvivalESyaryo {
    public Map<String, List<String>> results = new HashMap<>();
    
    public void analize(Map<String, List<String>> data) {
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
    
    private Map<String, List<String>> km(String gkey, Map<String, List<String>> g) {
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
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS(gkey+"_test.csv")){
            fail.entrySet().stream().map(df -> df.getKey()+","+count.get(df.getKey())+","+df.getValue()).forEach(pw::println);
        }
        
        //スコアリング
        Map result = scoring(g, fail);
        
        return result;
    }
    
    private Map scoring(Map<String, List<String>> g, Map<Integer, Double> fail){
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
