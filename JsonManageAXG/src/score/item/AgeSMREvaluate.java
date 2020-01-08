/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package score.item;

import axg.shuffle.form.util.FormalizeUtils;
import analizer.MSyaryoAnalizer;
import score.obj.ESyaryoObject;
import score.time.TimeSeriesObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import obj.MSyaryoObject;
import py.PythonCommand;

/**
 *
 * @author ZZ17807
 */
public class AgeSMREvaluate extends EvaluateTemplate {

    private Map<String, String> AGE_SMR_PARTS;
    private Map<String, String> AGE_SMR_SETTING;
    private Map<String, List<String>> PARTS_DEF;

    public AgeSMREvaluate(Map<String, String> settings, Map<String, List<String>> def) {
        super.enable = settings.get("#EVALUATE").equals("ENABLE");
        
        super.setHeader("経年/SMR", Arrays.asList(new String[]{"ADMIT_D", "FOLD_D", "X", "FSTAT"}));
        AGE_SMR_SETTING = settings;
        //AGE_SMR_SETTING.keySet().stream().forEach(settings::remove);
        AGE_SMR_PARTS = settings.entrySet().stream()
                .filter(e -> e.getKey().charAt(0) != '#')
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        super._settings = AGE_SMR_SETTING;

        PARTS_DEF = def;
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
        Integer div = Integer.valueOf(AGE_SMR_SETTING.get("#DIVIDE_X"));

        //時系列情報の取得
        AGE_SMR_PARTS.keySet().stream().forEach(k -> {
            TimeSeriesObject t = new TimeSeriesObject(s.a, super.dateSeq(s.a, sv.get(k)));

            //生存解析のデータ作成
            if (!t.series.isEmpty()) {
                t.series.stream().forEach(ti -> {
                    String k2 = FormalizeUtils.dup(k, data);
                    data.put(k2, new ArrayList<>());
                    
                    //納入年月
                    data.get(k2).add(s.a.lifestart);

                    //最初のサービス実績
                    String firstDate = s.a.getSMRToDate(ti).toString();//s.a.getSMRToDate(t.first()).toString();
                    data.get(k2).add(firstDate);

                    //経年
                    if (visual.equals("AGE")) {
                        Integer y = s.a.age(firstDate) / div;
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
                if(data.get(k) == null){
                    data.put(k, new ArrayList<>());
                    
                    //納入年月
                    data.get(k).add(s.a.lifestart);
                }
                    
                data.get(k).add(s.date);
                //経年
                if (visual.equals("AGE")) {
                    Integer y = s.a.age(s.date) / div;
                    data.get(k).add(y.toString());
                } else {
                    //SMR
                    Integer smr = s.smr / div;
                    data.get(k).add(smr.toString());
                }
                data.get(k).add("0");
            }

        });

        return data;
    }

    @Override
    public Map<String, Double> normalize(ESyaryoObject s, Map<String, List<String>> data) {
        List<String> mid = data.values().stream()
                .sorted(Comparator.comparing(v -> v.get(1), Comparator.naturalOrder()))
                .limit(1)
                .flatMap(l -> l.stream().map(li -> li.length()==0?"-1":li))
                .collect(Collectors.toList());
        
        Map norm = _header.get("経年/SMR").stream()
                .collect(Collectors.toMap(
                        h -> h,
                        h -> Double.valueOf(mid.get(_header.get("経年/SMR").indexOf(h))))
                );
        
        return norm;
    }
    
    //画像生成
    public static void printImage(String deirectory){
        System.out.println("経年/SMRの画像を生成．");
        PythonCommand.py("py\\agesmr_visualize.py", new String[]{deirectory});
    }
    
    @Override
    public void scoring() {
        //評価適用　無効
        if(!super.enable) return ;
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void testPrint(Map<String, List<String>> data, Map<String, Double> norm, ESyaryoObject s){
        //集約データのテスト出力
        System.out.println(s.a.get().getName());
        data.entrySet().stream().map(d -> "  " + d.getKey() + ":" + d.getValue()).forEach(System.out::println);
        
        //正規化データのテスト出力
        norm.entrySet().stream().map(d -> "  " + d.getKey() + ":" + d.getValue().intValue()).forEach(System.out::println);
    }

    @Override
    public Boolean check(ESyaryoObject s) {
        //すべての車両を評価
        return false;
    }
}
