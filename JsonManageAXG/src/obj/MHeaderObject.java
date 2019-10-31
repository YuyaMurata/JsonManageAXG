/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package obj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;

/**
 *
 * @author ZZ17807
 */
public class MHeaderObject {
    private ObjectId id;
    private List<String> header;
    private Boolean isCompleted;
    private transient Map<String, List<String>> map;
    
    public MHeaderObject(){
    }
    
    public MHeaderObject(List<String> header) {
        this.header = header;
        this.isCompleted = true;
    }
    
    public void setHeaderMap(){
        //header Map
        map = new HashMap<>();
        for(String s : header){
            if(s.equals("id "))
                continue;

            String k = s.split("\\.")[0];
            
            if(map.get(k) == null){
                map.put(k, new ArrayList<>());
            }
            
            map.get(k).add(s);
        }
        
        //車両オブジェクトのヘッダの調整
        map.values().stream().forEach(l -> l.remove(0));
    }
    
    public ObjectId getId() {
        return id;
    }

    public void setId(final ObjectId id) {
        this.id = id;
    }
    
    public List<String> getHeader(){
        return header;
    }
    
    public List<String> getHeader(String key){
        return this.map.get(key);
    }
    
    public Integer getHeaderIdx(String key, String idxKey){
        String item = idxKey;
        if(!idxKey.contains("."))
            item = key+"."+idxKey;
        return this.map.get(key).indexOf(item);
    }
    
    public void setHeader(final List header){
        this.header = header;
    }
    
    public Boolean getIsCompleted(){
        return isCompleted;
    }
    
    public void setIsCompleted(final Boolean isCompleted){
        this.isCompleted = isCompleted;
    }
    
    public void print(){
        map.entrySet().stream().map(e -> e.getKey()+":"+e.getValue()).forEach(System.out::println);
    }
}
