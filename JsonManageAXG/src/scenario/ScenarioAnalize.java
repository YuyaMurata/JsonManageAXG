/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import extract.SyaryoObjectExtract;

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
        ScenarioBlock sc1 = new ScenarioBlock("ベルト(C1-10)");
        ScenarioBlock sc2 = new ScenarioBlock("冷却水オーバーヒート");
        ScenarioBlock sc3 = new ScenarioBlock("エンジンOH(C1-1)");
        sc.setNEXT(sc1);
        sc1.setNEXT(sc2);
        sc2.setNEXT(sc3);
        testPrint(sc);
    }
    
    public void time(ScenarioBlock start){
        
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
