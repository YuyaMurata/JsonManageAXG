/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package score.item;

import analizer.MSyaryoAnalizer;
import score.obj.ESyaryoObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17807
 */
public abstract class EvaluateTemplate {
    public String itemName;
    public Map<String, List<String>> _header;
    public Map<String, ESyaryoObject> _eval;
    public Map<String, String> _settings;
    public Boolean enable;

    public EvaluateTemplate() {
        _eval = new ConcurrentHashMap();
        _header = new HashMap<>();
    }
    
    public void setHeader(String key, List<String> header) {
        this.itemName = key;
        _header.put(key, header);
    }

    //ヘッダーの上位キー
    public Set<String> getKeys() {
        return _header.keySet();
    }

    //ヘッダ取得
    public List<String> header(String key) {
        return _header.get(key);
    }

    //getPoint用ヘッダ取得
    public List<String> headers() {
        return _header.entrySet().stream()
                .flatMap(h -> h.getValue().stream().map(hi -> h.getKey() + "." + hi))
                .collect(Collectors.toList());
    }

    public void add(MSyaryoAnalizer a) {
        _eval.put(a.syaryo.getName(), trans(a));
    }

    ;
    
    public List<String> dateSeq(MSyaryoAnalizer a, List<String> sv) {
        return sv.stream()
                .map(d -> checkSV(a, d))
                .filter(d -> d != null) //古いデータが混入により登録作番が変わる可能性があるため
                .collect(Collectors.toList());
    }

    private String checkSV(MSyaryoAnalizer a, String sv) {
        if (sv.contains("部品") || sv.contains("受注") || sv.contains("作業")) {
            return a.getSBNToDate(sv.split(",")[1].split("\\.")[1], true);
        } else {
            return sv.split(",")[1].split("\\.")[1];
        }
    }

    public ESyaryoObject trans(MSyaryoAnalizer a) {
        //評価用オブジェクト
        ESyaryoObject s = new ESyaryoObject(a);

        if (check(a) || !enable) {
            s.setData(headers());
            return s;
        }
        try {
            //評価対象データの抽出
            Map<String, List<String>> sv = extract(a);

            //評価対象データをSMRで集約
            Map<String, List<String>> data = aggregate(a, sv);

            //評価対象データの正規化
            Map<String, Double> norm = normalize(a, data);

            //各データを検証にセット
            s.setData(headers(), sv, data, norm);
        } catch (Exception e) {
            System.err.println(a.get().getName()+"-データ異常による除外");
            s.setData(headers());
        }

        return s;
    }

    ;
    
    public abstract Boolean check(MSyaryoAnalizer s);

    public abstract Map<String, List<String>> extract(MSyaryoAnalizer s);

    public abstract Map<String, List<String>> aggregate(MSyaryoAnalizer s, Map<String, List<String>> sv);

    public abstract Map<String, Double> normalize(MSyaryoAnalizer s, Map<String, List<String>> data);

    public abstract void scoring();

    public void infoPrint(String info, Set<String> settings) {
        System.out.println(info);
        settings.stream().forEach(System.out::println);
    }
}
