/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import extract.SyaryoObjectExtract;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author kaeru
 */
public class ScenarioAnalize {

    public static void main(String[] args) {
        /* 例として下記を解析
           冷却水OH -> エンジン故障
         */
        SyaryoObjectExtract soe = new SyaryoObjectExtract("json", "komatsuDB_PC200_Form");
        soe.setUserDefine("config\\PC200_user_define.json");

        ScenarioBlock.setSyaryoObjectExtract(soe);
        ScenarioBlock sc = new ScenarioBlock("root");
        ScenarioBlock sc1 = new ScenarioBlock("エンジンオイル");
        ScenarioBlock sc2 = new ScenarioBlock("エンジンオイルフィルタ");
        ScenarioBlock sc3 = new ScenarioBlock("燃料メインフィルタ");
        sc.setNEXT(sc1);
        sc1.setNEXT(sc2);
        sc2.setNEXT(sc3);
        testPrint(sc);

        time(sc);
    }
    
    public void analize(){
        
    }
    
    public void similar(List<String> syaryoList){
        
    }
    
    public Map<String, List<String>> getResults(){
        return null;
    }
    
    public static void time(ScenarioBlock start) {
        //車両ID + シナリオ(各部品のリスト)
        Map<String, List<List<String>>> sidTimes = extract(start);
    }

    private static Map<String, List<List<String>>> extract(ScenarioBlock block) {
        Map<String, List<List<String>>> map = new HashMap<>();
        while ((block = block.getNEXT()) != null) {
            Map<String, List<String>> dmap = block.data.parallelStream()
                                                .collect(Collectors.groupingBy(d -> d.split(",")[0]));
            dmap.entrySet().stream().forEach(d ->{
                if(map.get(d.getKey()) == null)
                    map.put(d.getKey(), new ArrayList<>());
                map.get(d.getKey()).add(d.getValue());
            });
        }
        
        map.entrySet().stream().forEach(m ->{
            System.out.println(m.getKey());
        });
        
        return null;
    }

    public static void testPrint(ScenarioBlock sc) {
        getBlock("", sc);
    }

    public static void getBlock(String s, ScenarioBlock block) {
        if (block != null) {
            if (s.equals("-")) {
                System.out.print(s + block.item);
            } else if (s.equals("|")) {
                System.out.print(s + block.item);
            } else {
                System.out.println(block.item);
            }

            getBlock("|", block.getOR());
            getBlock("-", block.getAND());
            getBlock("", block.getNEXT());
        }
    }
}
