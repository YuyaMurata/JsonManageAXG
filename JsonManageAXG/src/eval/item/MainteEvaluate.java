/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.item;

import eval.obj.ESyaryoObject;
import eval.time.TimeSeriesObject;
import file.CSVFileReadWrite;
import file.MapToJSON;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17390
 */
public class MainteEvaluate extends EvaluateTemplate {

    private static Map<String, String> INTERVAL;
    private static Map<String, List<String>> TARGET;

    public MainteEvaluate(String setting, Map<String, List<String>> def) {
        INTERVAL = MapToJSON.toMap(setting);
        TARGET = def;
        super.setHeader("メンテナンス", new ArrayList<>(INTERVAL.keySet()));
    }

    @Override
    public ESyaryoObject trans(MSyaryoObject syaryo) {
        ESyaryoObject s = new ESyaryoObject(syaryo);
        
        //評価対象データの抽出
        Map<String, List<String>> sv = extract(syaryo.getName());
        
        //評価対象データをSMRで集約
        Map<String, List<String>> data = aggregate(s, sv);
        
        //評価対象データの正規化
        Map<String, Double> norm = normalize(s, data);
        
        //各データを検証にセット
        s.setData(sv, data, norm);
        
        return s;
    }
    
    //対象サービスの抽出
    private Map<String, List<String>> extract(String sid) {
        Map<String, List<String>> map = INTERVAL.keySet().stream()
                                .collect(Collectors.toMap(
                                        iv -> iv, 
                                        iv -> TARGET.get(iv).stream()
                                                    .filter(s -> s.split(",")[0].equals(sid))
                                                    .collect(Collectors.toList())
                                    )
                                );
        return map;
    }
    
    //時系列のメンテナンスデータ取得
    public Map<String, List<String>> aggregate(ESyaryoObject s, Map<String, List<String>> sv) {
        int dateIDX = 2;
        Map<String, List<String>> data = new HashMap();
        
        //時系列情報の取得
        INTERVAL.entrySet().stream().forEach(e -> {
            List<String> svlist = sv.get(e.getKey()).stream()
                                        .map(v -> v.split(",")[dateIDX])
                                        .collect(Collectors.toList());
            TimeSeriesObject t = new TimeSeriesObject(s.a, dateIDX, svlist);
            
            //最大SMRからSMR系列を取得
            Integer len = s.a.maxSMR / Integer.valueOf(e.getValue());
            
            //len == 0 SMRがインターバル時間に届いていない場合、無条件で1と評価
            List<String> series = len != 0 ? IntStream.range(0, len).boxed().map(i -> "0").collect(Collectors.toList()) : Arrays.asList(new String[]{"1"});

            t.series.stream()
                    .map(v -> v == 0 ? 1 : v) //0h交換での例外処理
                    .map(v -> (v % Integer.valueOf(e.getValue())) == 0 ? v - 1 : v) //インターバル時間で割り切れる場合の例外処理
                    .forEach(v -> {
                        int i = v / Integer.valueOf(e.getValue());
                        if (i < len) {
                            //if(series.get(i).equals("0"))
                            series.set(i, v.toString());
                            //else
                            //    series.set(i, series.get(i)+"_"+v.toString());
                        }
                    });
            
            data.put(e.getKey(), series);
        });
        
        return data;
    }

    //正規化
    public Map<String, Double> normalize(ESyaryoObject s, Map<String, List<String>> data) {
        Map norm = INTERVAL.keySet().stream()
                        .collect(Collectors.toMap(
                                iv -> iv,
                                iv -> data.get(iv).stream()
                                    .map(ti -> Integer.valueOf(ti) > 0 ? 1 : 0)
                                    .mapToDouble(ti -> ti.doubleValue())
                                    .average().getAsDouble()
                            )
                        );

        return norm;
    }

    @Override
    public Map<String, Integer> scoring(Map<String, Integer> cluster, String key, Map<String, List<Double>> data) {
        int maxCluster = cluster.values().stream().distinct().mapToInt(c -> c).max().getAsInt();

        //Average
        Map<Integer, Double> avg = IntStream.range(0, maxCluster + 1).boxed()
                .collect(Collectors.toMap(
                        i -> i,
                        i -> cluster.entrySet().stream()
                                .filter(c -> c.getValue().equals(i))
                                .flatMap(c -> data.get(c.getKey()).stream())
                                .mapToDouble(d -> d).average().getAsDouble()
                ));

        //Sort
        List<Integer> sort = avg.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(a -> a.getKey())
                .collect(Collectors.toList());

        //Scoring
        Map score = cluster.entrySet().stream()
                .collect(Collectors.toMap(c -> c.getKey(), c -> sort.indexOf(c.getValue()) + 1));

        return score;
    }

    //Test
    public static void main(String[] args) {
        LOADER.setFile("PC200_loadmap");
        SyaryoAnalizer.rejectSettings(false, false, false);
        SyaryoAnalizer s = new SyaryoAnalizer(LOADER.getSyaryoMap().get("PC200-10-452525"), true);

        MainteEvaluate mainte = new MainteEvaluate();

        /*Map<String, List<String>> data = mainte.getdata(s);
        Map<String, Double> result = mainte.evaluate("メンテナンス", s);

        data.entrySet().stream().forEach(d -> {
            System.out.println(d.getKey());
            System.out.println("  " + d.getValue());
            System.out.println("  " + result.get(d.getKey()));
        });
         */
        //クラスタ用データ
        System.out.println("\n" + mainte.header("メンテナンス"));
        System.out.println(mainte.getClusterData("メンテナンス"));
    }

}
