/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.item;

import eval.analizer.MSyaryoAnalizer;
import eval.obj.ESyaryoObject;
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
    public Map<String, List<String>> _header;
    public Map<String, ESyaryoObject> _eval; 
    public Map<String, String> _settings; 
    public Boolean enable;

    public EvaluateTemplate() {
        _eval = new ConcurrentHashMap();
        _header = new HashMap<>();
    }
    
    public void setHeader(String key, List<String> header){
        _header.put(key, header);
    }
    
    //ヘッダーの上位キー
    public Set<String> getKeys(){
        return _header.keySet();
    }
    
    //ヘッダ取得
    public List<String> header(String key) {
        return _header.get(key);
    }
    
    public void add(MSyaryoAnalizer a){
        _eval.put(a.syaryo.getName(), trans(a));
    };
    
    public List<String> dateSeq(MSyaryoAnalizer a, List<String> sv){
        return sv.stream()
                .map(d -> checkSV(a, d))
                .collect(Collectors.toList());
    }
    
    private String checkSV(MSyaryoAnalizer a, String sv){
        if(sv.contains("部品") || sv.contains("受注") || sv.contains("作業"))
            return a.getSBNToDate(sv.split(",")[1].split("\\.")[1], true);
        else
            return sv.split(",")[1].split("\\.")[1];
    }
    
    public abstract ESyaryoObject trans(MSyaryoAnalizer a);
    
    public abstract Map<String, List<String>> extract(ESyaryoObject s);
    
    public abstract Map<String, List<String>> aggregate(ESyaryoObject s, Map<String, List<String>> sv);
    
    public abstract Map<String, Double> normalize(ESyaryoObject s, Map<String, List<String>> data);
    
    public abstract void scoring();
}
