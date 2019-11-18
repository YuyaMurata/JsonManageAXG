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
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public abstract class EvaluateTemplate {
    public Map<String, List<String>> _header;
    public Map<String, ESyaryoObject> _eval; 
    public Map<String, String> _settings; 

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
    
    public void add(MSyaryoObject s){
        _eval.put(s.getName(), trans(s));
    };
    
    public List<String> dateSeq(MSyaryoAnalizer a, int idx, List<String> sv){
        return sv.stream()
                .map(d -> a.getSBNToDate(d.split(",")[idx], true))
                .collect(Collectors.toList());
    }
    
    public abstract ESyaryoObject trans(MSyaryoObject s);
    
    public abstract Map<String, List<String>> extract(ESyaryoObject s);
    
    public abstract Map<String, List<String>> aggregate(ESyaryoObject s, Map<String, List<String>> sv);
    
    public abstract Map<String, Double> normalize(ESyaryoObject s, Map<String, List<String>> data);
    
    public abstract void scoring();
}
