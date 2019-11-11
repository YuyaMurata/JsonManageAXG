/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.item;

import eval.obj.ESyaryoObject;
import eval.time.TimeSeriesObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class AgeSMREvaluate extends EvaluateTemplate {

    private Map<String, String> AGE_SMR_PARTS;
    private Map<String, String> AGE_SMR_SETTING;
    private Map<String, List<String>> PARTS_DEF;

    public AgeSMREvaluate(Map<String, String> setting, Map<String, List<String>> def) {
        super.setHeader("経年/SMR", Arrays.asList(new String[]{"ADMIT_D", "FOLD_D", "X", "FSTAT"}));
        AGE_SMR_SETTING = setting.entrySet().stream()
                .filter(e -> e.getKey().contains("#"))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        AGE_SMR_SETTING.keySet().stream().forEach(setting::remove);
        AGE_SMR_PARTS = setting;

        PARTS_DEF = def;
    }

    @Override
    public ESyaryoObject trans(MSyaryoObject syaryo) {
        ESyaryoObject s = new ESyaryoObject(syaryo);

        //評価対象データの抽出
        Map<String, List<String>> sv = extract(s);

        //評価対象データをSMRで集約
        Map<String, List<String>> data = aggregate(s, sv);

        System.out.println(s.a.get().getName());
        data.entrySet().stream().map(d -> "  " + d.getKey() + ":" + d.getValue()).forEach(System.out::println);

        //評価対象データの正規化
        Map<String, Double> norm = normalize(s, data);
        norm.entrySet().stream().map(d -> "  " + d.getKey() + ":" + d.getValue().intValue()).forEach(System.out::println);

        //各データを検証にセット
        s.setData(sv, data, norm);

        return s;
    }

    @Override
    public Map<String, List<String>> extract(ESyaryoObject s) {
        Map<String, List<String>> map = AGE_SMR_PARTS.keySet().stream()
                .collect(Collectors.toMap(iv -> iv,
                        iv -> PARTS_DEF.get(iv).stream()
                                .filter(sv -> sv.split(",")[0].equals(s.a.get().getName()))
                                .collect(Collectors.toList())
                )
                );
        return map;
    }

    @Override
    public Map<String, List<String>> aggregate(ESyaryoObject s, Map<String, List<String>> sv) {
        Map<String, List<String>> data = new HashMap();
        String visual = AGE_SMR_SETTING.get("#VISUAL_X");
        Integer agediv = Integer.valueOf(AGE_SMR_SETTING.get("#DIVIDE_AGE"));
        Integer smrdiv = Integer.valueOf(AGE_SMR_SETTING.get("#DIVIDE_SMR"));
        
        //時系列情報の取得
        AGE_SMR_PARTS.keySet().stream().forEach(k -> {
            int idx = Arrays.asList(PARTS_DEF.get(k + "#H").get(0).split(",")).indexOf("部品.作番");

            TimeSeriesObject t = new TimeSeriesObject(s.a, super.dateSeq(s.a, idx, sv.get(k)));

            //生存解析のデータ作成
            if (data.get(k) == null) {
                data.put(k, new ArrayList<>());

                //納入年月
                data.get(k).add(s.a.lifestart);

                if (!t.series.isEmpty()) {
                    //最初のサービス実績
                    String firstDate = s.a.getSMRToDate(t.first()).toString();
                    data.get(k).add(firstDate);

                    //経年
                    if (visual.equals("AGE")) {
                        Integer y = s.a.age(firstDate) / agediv;
                        data.get(k).add(y.toString());
                    } else {
                        //SMR
                        Integer smr = t.first() / smrdiv;
                        data.get(k).add(smr.toString());
                    }

                    //サービス発生の有無
                    data.get(k).add("1");
                } else {
                    data.get(k).add(s.date);
                    //経年
                    if (visual.equals("AGE")) {
                        Integer y = s.a.age(s.date) / agediv;
                        data.get(k).add(y.toString());
                    } else {
                        //SMR
                        Integer smr = s.smr / smrdiv;
                        data.get(k).add(smr.toString());
                    }
                    data.get(k).add("0");
                }
            }
        });

        return data;
    }

    @Override
    public Map<String, Double> normalize(ESyaryoObject s, Map<String, List<String>> data) {
        Map<String, List<String>> mid = data.values().stream()
                .sorted(Comparator.comparing(v -> v.get(1), Comparator.naturalOrder()))
                .limit(1)
                .collect(Collectors.toMap(v -> "SUV", v -> v));

        Map norm = _header.get("経年/SMR").stream()
                .collect(Collectors.toMap(
                        h -> h,
                        h -> Double.valueOf(mid.get("SUV").get(_header.get("経年/SMR").indexOf(h))))
                );

        return norm;
    }

    @Override
    public void scoring() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
