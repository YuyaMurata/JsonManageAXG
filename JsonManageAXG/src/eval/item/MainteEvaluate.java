/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.item;

import eval.obj.ESyaryoObject;
import eval.time.TimeSeriesObject;
import file.MapToJSON;
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

    private Map<String, String> MAINTE_INTERVAL;
    private Map<String, List<String>> PARTS_DEF;

    public MainteEvaluate(Map<String, String> setting, Map<String, List<String>> def) {
        MAINTE_INTERVAL = setting;
        PARTS_DEF = def;
        super.setHeader("メンテナンス", new ArrayList<>(MAINTE_INTERVAL.keySet()));
    }

    @Override
    public ESyaryoObject trans(MSyaryoObject syaryo) {
        ESyaryoObject s = new ESyaryoObject(syaryo);

        //評価対象データの抽出
        Map<String, List<String>> sv = extract(s);

        //評価対象データをSMRで集約
        Map<String, List<String>> data = aggregate(s, sv);

        //評価対象データの正規化
        Map<String, Double> norm = normalize(s, data);

        //各データを検証にセット
        s.setData(sv, data, norm);

        return s;
    }

    //対象サービスの抽出
    @Override
    public Map<String, List<String>> extract(ESyaryoObject s) {
        Map<String, List<String>> map = MAINTE_INTERVAL.keySet().stream()
                .collect(Collectors.toMap(iv -> iv,
                        iv -> PARTS_DEF.get(iv).stream()
                                .filter(sv -> sv.split(",")[0].equals(s.a.get().getName()))
                                .collect(Collectors.toList())
                )
                );
        return map;
    }

    //時系列のメンテナンスデータ取得
    @Override
    public Map<String, List<String>> aggregate(ESyaryoObject s, Map<String, List<String>> sv) {
        Map<String, List<String>> data = new HashMap();

        //時系列情報の取得
        MAINTE_INTERVAL.entrySet().stream().forEach(e -> {
            int idx = Arrays.asList(PARTS_DEF.get(e.getKey() + "#H").get(0).split(",")).indexOf("部品.作番");

            TimeSeriesObject t = new TimeSeriesObject(s.a, super.dateSeq(s.a, idx, sv.get(e.getKey())));

            //最大SMRからSMR系列を取得
            Integer len = s.a.maxSMR / Integer.valueOf(e.getValue());

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
    public Map<String, Double> normalize(ESyaryoObject s, Map<String, List<String>> data) {
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
}
