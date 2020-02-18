/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario.valid;

import exception.AISTProcessException;
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import scenario.ScenarioBlock;
import time.TimeSeriesObject;

/**
 *
 * @author ZZ17807
 */
public class ValidateCalculateBlock {
    Map<String, Map<String, Integer[]>> map;
    
    public ValidateCalculateBlock() {
        map = new HashMap<>();
    }
    
    public void setBlock(ScenarioBlock b){
        b.getBlockSeq().entrySet().stream().forEach(bi ->{
            if(map.get(bi.getKey())==null){
                map.put(bi.getKey(), new LinkedHashMap<>());
            }
            
            TimeSeriesObject obj = bi.getValue();
            map.get(bi.getKey()).put(b.item, obj.arrSeries);
        });
    }
    
    public void setStrBlock(String str, ScenarioBlock b){
        b.getBlockSeq().entrySet().stream().forEach(bi ->{
            if(map.get(bi.getKey())==null){
                map.put(bi.getKey(), new LinkedHashMap<>());
            }
            
            TimeSeriesObject obj = bi.getValue();
            map.get(bi.getKey()).put(str+"."+b.item, obj.arrSeries);
        });
        map = map.entrySet().stream()
                .filter(m -> b.blockSeq.containsKey(m.getKey()))
                .collect(Collectors.toMap(m -> m.getKey(), m -> m.getValue()));
    }
    
    public void setDelay(String title, Map<String, List<Integer>> del){
        del.entrySet().stream().forEach(di ->{
            if(map.get(di.getKey())==null){
                map.put(di.getKey(), new LinkedHashMap<>());
            }
            
            map.get(di.getKey()).put(title, di.getValue().toArray(new Integer[di.getValue().size()]));
        });
    }
    
    public void filter(Collection keys){
        map = map.entrySet().stream().filter(m -> keys.contains(m.getKey())).collect(Collectors.toMap(m -> m.getKey(), m -> m.getValue()));
    }
    
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("ブロックの計算検証:\n");
        map.entrySet().stream()
                .map(e -> e.getKey()+",\n  "+e.getValue().entrySet().stream()
                            .map(ei -> ","+ei.getKey()+","+Arrays.stream(ei.getValue()).map(eij -> eij.toString()).collect(Collectors.joining(",")))
                            .collect(Collectors.joining("\n"))+"\n")
                .forEach(sb::append);
        return sb.toString();
    }
    
    public void toFile(String filename){
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS(filename)){
            pw.println(toString());
        } catch (AISTProcessException ex) {
            ex.printStackTrace();
        }
    }
}
