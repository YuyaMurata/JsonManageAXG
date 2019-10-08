/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.item;

import eval.obj.ESyaryoObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public abstract class EvaluateTemplate {
    private static Map<String, List<String>> _header;
    public List<MSyaryoObject> _master;
    public List<ESyaryoObject> _eval; 

    public EvaluateTemplate() {
        _master = new ArrayList<>();
        _eval = new ArrayList<>();
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
        _master.add(s);
        _eval.add(trans(s));
    };
    
    public abstract ESyaryoObject trans(MSyaryoObject s);
    
    public abstract Map<String, Integer> scoring(Map<String, List> cluster, String key, Map<String, List<Double>> data);  
}
