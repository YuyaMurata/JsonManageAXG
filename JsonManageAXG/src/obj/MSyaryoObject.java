/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package obj;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;

/**
 *
 * @author ZZ17807
 */
public class MSyaryoObject {
    private ObjectId id;
    private String name;
    private Map<String, Map<String, List<String>>> map;
    private Map<String, Integer> count;

    public MSyaryoObject() {
    }
    
    public MSyaryoObject(Map map) {
        this.name = (String) map.get("name");
        this.map = (Map<String, Map<String, List<String>>>) map.get("map");
        this.count = (Map<String, Integer>) map.get("count");
    }
    
    public ObjectId getId() {
        return id;
    }

    public void setId(final ObjectId id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
    
    public Map<String, Map<String, List<String>>> getMap(){
        return map;
    }
    
    public void setMap(final Map map){
        this.map = map;
    }
    
    public Map getCount(){
        return count;
    }
    
    public Integer getCount(String key){
        return count.get(key);
    }
    
    public void setCount(final Map count){
        this.count = count;
    }
    
    public Map<String, List<String>> getData(String key){
        return this.map.get(key);
    }
    
    public void setData(String key, Map<String, List<String>> data){
        if(data != null)
            this.map.put(key, data);
        else
            this.map.remove(key);
    }
    
    public List<String> getDataOne(String key){
        if(this.map.get(key) != null){
            return this.map.get(key).values().stream().findFirst().get();
        }else{
            return null;
        }
    }
    
    public String getDataKeyOne(String key){
        if(this.map.get(key) != null)
            return this.map.get(key).keySet().stream().findFirst().get();
        else{
            return null;
        }
    }
    
    public void removeAll(String key, List<String> subKeys){
        if(this.map.get(key) == null)
            return ;
            
        subKeys.stream().forEach(sk -> this.map.get(key).remove(sk));
        if(this.map.get(key).isEmpty())
            this.map.remove(key);
    }
    
    public void remove(String key){
        if(this.map.get(key) == null)
            return ;
            
        if(this.map.get(key).isEmpty())
            this.map.remove(key);
    }
    
    public void recalc(){
        this.count = this.map.entrySet().stream()
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().size()));
    }
    
    public void print(){
        System.out.println(name);
        map.entrySet().stream().map(e -> " "+e.getKey()+":"+count.get(e.getKey())+":"+e.getValue()).forEach(System.out::println);
    }
}
