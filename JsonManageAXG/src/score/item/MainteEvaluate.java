/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package score.item;

import analizer.MSyaryoAnalizer;
import extract.SyaryoObjectExtract;
import score.cluster.ClusteringESyaryo;
import score.cluster.DataVector;
import score.obj.ESyaryoObject;
import time.TimeSeriesObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import py.PythonCommand;

/**
 *
 * @author ZZ17390
 */
public class MainteEvaluate extends EvaluateTemplate {

    private Map<String, String> MAINTE_INTERVAL;
    private SyaryoObjectExtract exObj;

    public MainteEvaluate(Map<String, String> settings, SyaryoObjectExtract exObj) {
        super.enable = settings.get("#EVALUATE").equals("ENABLE");
        
        MAINTE_INTERVAL = settings.entrySet().stream()
                                .filter(e -> e.getKey().charAt(0) != '#')
                                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        this.exObj = exObj;
        super.setHeader("メンテナンス", new ArrayList<>(MAINTE_INTERVAL.keySet()));
        
        //取得設定の出力
        infoPrint("メンテナンス設定", MAINTE_INTERVAL.keySet());
    }
    
    //対象サービスの抽出
    @Override
    public Map<String, List<String>> extract(MSyaryoAnalizer s) {
        Map<String, List<String>> map = MAINTE_INTERVAL.keySet().stream()
                .collect(Collectors.toMap(iv -> iv,
                        iv -> exObj.getDefine(iv).toList().stream()
                                .filter(sv -> sv.split(",")[0].equals(s.get().getName()))
                                .collect(Collectors.toList())
                )
                );
        return map;
    }

    //時系列のメンテナンスデータ取得
    @Override
    public Map<String, List<String>> aggregate(MSyaryoAnalizer s, Map<String, List<String>> sv) {
        Map<String, List<String>> data = new HashMap();

        //時系列情報の取得
        MAINTE_INTERVAL.entrySet().stream().forEach(e -> {
            TimeSeriesObject t = new TimeSeriesObject(s, super.dateSeq(s, sv.get(e.getKey())));

            //最大SMRからSMR系列を取得
            Integer len = s.maxSMR / Integer.valueOf(e.getValue());

            //len == 0 SMRがインターバル時間に届いていない場合、無条件で1と評価
            List<String> series = len != 0 ? IntStream.range(0, len).boxed().map(i -> "0").collect(Collectors.toList()) : Arrays.asList(new String[]{"1"});

            t.series.stream()
                    .map(v -> v == 0 ? 1 : v) //0h交換での例外処理
                    .map(v -> (v % Integer.valueOf(e.getValue())) == 0 ? v - 1 : v) //インターバル時間で割り切れる場合の例外処理
                    .forEach(v -> {
                        int i = v < Integer.valueOf(e.getValue()) ? 0 : v / Integer.valueOf(e.getValue());

                        if (i < len) {
                            series.set(i, v.toString());
                        }
                    });

            data.put(e.getKey(), series);
        });

        return data;
    }

    //正規化
    @Override
    public Map<String, Double> normalize(MSyaryoAnalizer s, Map<String, List<String>> data) {
        Map norm = MAINTE_INTERVAL.keySet().stream()
                .collect(Collectors.toMap(
                        iv -> iv,
                        iv -> data.get(iv).stream()
                                .map(ti -> Integer.valueOf(ti) > 0 ? 1 : 0)
                                .mapToDouble(ti -> ti.doubleValue())
                                .average().getAsDouble(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                )
                );

        return norm;
    }

    @Override
    public void scoring() {
        System.out.println("メンテナンスのスコアリング");
        //評価適用　無効
        if(!super.enable) return ;
        
        Map<Integer, List<ESyaryoObject>> cids = new LinkedHashMap<>();

        //CIDで集計
        super._eval.values().stream().forEach(e -> {
            if (!e.none()) {
                if (cids.get(e.cid) == null) {
                    cids.put(e.cid, new ArrayList<>());
                }

                cids.get(e.cid).add(e);
            }
        });

        //cidごとの平均充足率
        List<DataVector> cidavg = cids.entrySet().stream()
                .map(cid
                        -> new DataVector(cid.getKey(),
                            cid.getValue().stream()
                                .mapToDouble(e -> e.norm.values().stream().mapToDouble(m -> m).average().getAsDouble())
                                .average().getAsDouble()))
                                //.max().getAsDouble()))
                                //.sorted().boxed().limit(cid.getValue().size()/2).reduce((a, b) -> b).orElse(null)))
                .collect(Collectors.toList());

        //スコアリング用にデータを3分割
        List<CentroidCluster<DataVector>> splitor = ClusteringESyaryo.splitor(cidavg);
        if(splitor==null) return ;
        
        List<Integer> sort = IntStream.range(0, splitor.size()).boxed()
                .sorted(Comparator.comparing(i -> splitor.get(i).getPoints().stream().mapToDouble(d -> d.p).average().getAsDouble(), Comparator.naturalOrder()))
                .map(i -> i).collect(Collectors.toList());

        //スコアリング
        sort.stream().forEach(i -> {
            splitor.get(i).getPoints().stream()
                    .map(sp -> sp.cid)
                    .forEach(cid -> {
                        cids.get(cid).stream().forEach(e -> {
                            e.score = sort.indexOf(i) + 1;
                        });
                    });
        });
    }
    
    public static void printImage(String file, String x, String y, String c){
        PythonCommand.py("py\\mainte_visualize.py", new String[]{file});
    }

    @Override
    public Boolean check(MSyaryoAnalizer s) {
        return s.get("受注") == null;
    }
}
