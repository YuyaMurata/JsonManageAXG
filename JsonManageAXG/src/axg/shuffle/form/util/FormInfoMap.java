/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle.form.util;

import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;

/**
 *
 * @author ZZ17807
 */
public class FormInfoMap {
    private ObjectId id;
    private String name;
    private Map<String, String> map;

    public FormInfoMap() {
    }
    
    public FormInfoMap(String name, Map<String, String> map) {
        this.name = name;
        this.map = map;
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
    
    public Map<String, String> getMap(){
        return map;
    }
    
    public void setMap(final Map map){
        this.map = map;
    }
    
    public String getInfo(String key){
        return map.get(key);
    }
    
    @Override
    public String toString(){
        return name+":"+map;
    }
}
