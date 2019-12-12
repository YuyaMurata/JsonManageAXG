/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import extract.SyaryoObjectExtract;
import java.util.List;
import java.util.Map;

/**
 * シナリオブロック
 * 時系列解析の最小単位
 * @author kaeru
 */
public class ScenarioBlock {
    public static SyaryoObjectExtract extract;
    public static void setSyaryoObjectExtract(SyaryoObjectExtract ex){
        extract = ex;
    }
    
    public String item;
    public List<String> data;
    public ScenarioBlock(String item) {
        this.item = item;
        if(!item.equals("root")){
            this.data = extract.getDefine().get(item);
        }
    }
    
    private ScenarioBlock and;
    public void setAND(ScenarioBlock block){
        this.and = block;
    }
    
    public ScenarioBlock getAND(){
        return this.and;
    }
    
    private ScenarioBlock or;
    public void setOR(ScenarioBlock block){
        this.or = block;
    }
    
    public ScenarioBlock getOR(){
        return this.or;
    }
    
    private ScenarioBlock next;
    public void setNEXT(ScenarioBlock block){
        this.next = block;
    }
    
    public ScenarioBlock getNEXT(){
        return this.next;
    }
    
    public Integer getN(){
        return this.data.size();
    }
}
