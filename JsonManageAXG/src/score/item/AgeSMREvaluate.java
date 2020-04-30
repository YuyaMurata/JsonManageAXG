/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package score.item;

import axg.shuffle.form.util.FormalizeUtils;
import analizer.MSyaryoAnalizer;
import extract.SyaryoObjectExtract;
import score.obj.ESyaryoObject;
import time.TimeSeriesObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import py.PythonCommand;
import score.cluster.ClusteringESyaryo;
import score.cluster.DataVector;

/**
 *
 * @author ZZ17807
 */
public class AgeSMREvaluate extends EvaluateTemplate {

    private Map<String, String> AGE_SMR_PARTS;
    private Map<String, String> AGE_SMR_SETTING;
    private SyaryoObjectExtract exObj;
    
    public AgeSMREvaluate(Map<String, String> settings, SyaryoObjectExtract exObj) {
        if(!settings.keySet().stream().filter(key -> key.charAt(0)!='#').findFirst().isPresent())
            super.enable = false;
        else
            super.enable = settings.get("#EVALUATE").equals("ENABLE");

        super.setHeader("経年/SMR", Arrays.asList(new String[]{"ADMIT_D", "FOLD_D", "X", "FSTAT", "N"}));
        AGE_SMR_SETTING = settings;
        //AGE_SMR_SETTING.keySet().stream().forEach(settings::remove);
        AGE_SMR_PARTS = settings.entrySet().stream()
                .filter(e -> e.getKey().charAt(0) != '#')
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        super._settings = AGE_SMR_SETTING;

        //PARTS_DEF = def;
        this.exObj = exObj;

        //取得設定の出力
        infoPrint("経年/SMR設定", AGE_SMR_PARTS.keySet());
    }

    @Override
    public Map<String, List<String>> extract(MSyaryoAnalizer s) {
        Map<String, List<String>> map = AGE_SMR_PARTS.keySet().stream()
                .collect(Collectors.toMap(iv -> iv,
                        iv -> exObj.getDefine(iv).toList().stream()
                                .filter(sv -> sv.split(",")[0].equals(s.get().getName()))
                                .collect(Collectors.toList())
                )
                );
        return map;
    }

    @Override
    public Map<String, List<String>> aggregate(MSyaryoAnalizer s, Map<String, List<String>> sv) {
        Map<String, List<String>> data = new HashMap();
        String visual = AGE_SMR_SETTING.get("#VISUAL_X");
        Integer div = Integer.valueOf(AGE_SMR_SETTING.get("#DELTA_X"));

        //時系列情報の取得
        AGE_SMR_PARTS.keySet().stream().forEach(k -> {
            TimeSeriesObject t = new TimeSeriesObject(s, super.dateSeq(s, sv.get(k)));

            //生存解析のデータ作成
            if (!t.series.isEmpty()) {
                t.series.stream().forEach(ti -> {
                    String k2 = FormalizeUtils.dup(k, data);
                    data.put(k2, new ArrayList<>());

                    //納入年月
                    data.get(k2).add(s.lifestart);

                    //最初のサービス実績
                    String firstDate = s.getSMRToDate(ti).toString();//s.a.getSMRToDate(t.first()).toString();
                    data.get(k2).add(firstDate);

                    //経年
                    if (visual.equals("AGE")) {
                        Integer y = s.age(firstDate) / div;
                        data.get(k2).add(y.toString());
                    } else {
                        //SMR
                        Integer smr = ti / div;
                        data.get(k2).add(smr.toString());
                    }

                    //サービス発生の有無
                    data.get(k2).add("1");
                });
            } else {
                if (data.get(k) == null) {
                    data.put(k, new ArrayList<>());

                    //納入年月
                    data.get(k).add(s.lifestart);
                }

                data.get(k).add(s.LEAST_DATE);
                //経年
                if (visual.equals("AGE")) {
                    Integer y = s.age(s.LEAST_DATE) / div;
                    data.get(k).add(y.toString());
                } else {
                    //SMR
                    Integer smr = s.maxSMR / div;
                    data.get(k).add(smr.toString());
                }
                data.get(k).add("0");
            }

        });

        return data;
    }

    @Override
    public Map<String, Double> normalize(MSyaryoAnalizer s, Map<String, List<String>> data) {
        List<String> mid = data.values().stream()
                .sorted(Comparator.comparing(v -> v.get(1), Comparator.naturalOrder()))
                .limit(1)
                .flatMap(l -> l.stream().map(li -> li.length() == 0 ? "-1" : li))
                .collect(Collectors.toList());
        mid.add(String.valueOf(data.size()));
        
        //System.out.println(_header.get("経年/SMR"));

        Map norm = _header.get("経年/SMR").stream()
                .collect(Collectors.toMap(
                        h -> h,
                        h -> Double.valueOf(mid.get(_header.get("経年/SMR").indexOf(h))))
                );
        
        return norm;
    }

    //画像生成
    public static void printImage(String directory) {
        System.out.println("経年/SMRの画像を生成．");
        PythonCommand.py("py\\agesmr_visualize.py", new String[]{directory});
    }

    @Override
    public void scoring() {
        System.out.println("経年/SMRのスコアリング");
        //評価適用　無効
        if (!super.enable) {
            return;
        }

        //cidごとの平均充足率
        List<String> keys = new ArrayList(super._eval.keySet());
        List<DataVector> cidavg = super._eval.entrySet().stream()
                .map(e -> new DataVector(keys.indexOf(e.getKey()), e.getValue().data == null ? 0d : e.getValue().data.size()))
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
                    .forEach(sid -> {
                        String key = keys.get(sid);
                        super._eval.get(key).score = sort.indexOf(i) + 1;
                    });
        });
    }

    public void testPrint(Map<String, List<String>> data, Map<String, Double> norm, MSyaryoAnalizer s) {
        //集約データのテスト出力
        System.out.println(s.get().getName());
        data.entrySet().stream().map(d -> "  " + d.getKey() + ":" + d.getValue()).forEach(System.out::println);

        //正規化データのテスト出力
        norm.entrySet().stream().map(d -> "  " + d.getKey() + ":" + d.getValue().intValue()).forEach(System.out::println);
    }

    @Override
    public Boolean check(MSyaryoAnalizer s) {
        //すべての車両を評価
        return false;
    }
}
