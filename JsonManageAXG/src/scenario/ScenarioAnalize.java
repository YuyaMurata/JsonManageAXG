/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import eval.time.TimeSeriesObject;
import extract.SyaryoObjectExtract;
import java.util.List;
import java.util.Map;

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
    
    public static void time(ScenarioBlock start){
        while((start = start.getNEXT()) != null){
            System.out.println(start.item);
            extract(start);
        }
    }
    
    private static Map<String, List<String>> extract(ScenarioBlock block){
        block.data.forEach(System.out::println);
        return null;
    }
    
    public static void testPrint(ScenarioBlock sc){
        getBlock("", sc);
    }
    
    public static void getBlock(String s, ScenarioBlock block){
        if(block != null){
            if(s.equals("-"))
                System.out.print(s+block.item);
            else if(s.equals("|"))
                System.out.print(s+block.item);
            else
                System.out.println(block.item);
            
            getBlock("|",block.getOR());
            getBlock("-",block.getAND());
            getBlock("", block.getNEXT());
        }
    }
}
