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
        ScenarioBlock sc1 = new ScenarioBlock("冷却水オーバーヒート");
        ScenarioBlock sc11 = new ScenarioBlock("エンジンオイル");
        ScenarioBlock sc12 = new ScenarioBlock("エンジンオイルフィルタ");
        ScenarioBlock sc2 = new ScenarioBlock("エンジンOH(C1-1)");
        sc1.setAND(sc11);
        sc1.setOR(sc12);
        sc1.setNEXT(sc2);
        sc.setNEXT(sc1);
        testPrint(sc);
    }
    
    public static void testPrint(ScenarioBlock sc){
        ScenarioBlock start = sc;
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
