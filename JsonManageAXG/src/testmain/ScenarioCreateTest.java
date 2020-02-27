/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testmain;

import exception.AISTProcessException;
import scenario.ScenarioBlock;

/**
 *
 * @author zz17807
 */
public class ScenarioCreateTest {

    //検証シナリオ テスト
    public static ScenarioBlock stest() throws AISTProcessException {
        ScenarioBlock b1 = new ScenarioBlock("C1-1:エンジンOH");
        ScenarioBlock b2 = new ScenarioBlock("C1-2:DPF");
        ScenarioBlock b3 = new ScenarioBlock("C1-3:冷却装置");
        ScenarioBlock b4 = new ScenarioBlock("C1-4:サプライポンプ");
        ScenarioBlock b5 = new ScenarioBlock("C1-5:ターボチャージャ");
        ScenarioBlock b6 = new ScenarioBlock("C1-6:排気系");
        ScenarioBlock b7 = new ScenarioBlock("C1-7:吸気系");
        ScenarioBlock b8 = new ScenarioBlock("C1-8:オルタネータ");
        ScenarioBlock b9 = new ScenarioBlock("C1-9:ガスケット");
        ScenarioBlock b10 = new ScenarioBlock("C1-10:ファンベルト");
        ScenarioBlock b11 = new ScenarioBlock("C1-11:オイルポンプ");
        ScenarioBlock b12 = new ScenarioBlock("C1-12:ヘッドガスケット");
        ScenarioBlock b13 = new ScenarioBlock("C1-13:フュエルクーラ");
        ScenarioBlock b14 = new ScenarioBlock("C1-14:エンジンオイルクーラ");
        ScenarioBlock b15 = new ScenarioBlock("C1-15:ダンパ");
        
        b1.setNEXT(b15);
        b1.setAND(b2);
        b1.setOR(b8);
        b8.setAND(b9);
        b2.setAND(b3);
        b2.setOR(b10);
        b10.setOR(b11);
        b11.setAND(b12);
        b12.setAND(b13);
        b13.setOR(b14);
        b3.setAND(b4);
        b4.setAND(b5);
        b5.setOR(b6);
        b6.setAND(b7);

        return b1;
    }

    //検証シナリオ0
    public static ScenarioBlock t0() throws AISTProcessException {
        ScenarioBlock b1 = new ScenarioBlock("部品1");
        ScenarioBlock b2 = new ScenarioBlock("部品2");
        ScenarioBlock b3 = new ScenarioBlock("部品3");

        b1.setNEXT(b2);
        b2.setNEXT(b3);

        return b1;
    }
    
    //検証シナリオ0
    public static ScenarioBlock s0() throws AISTProcessException {
        ScenarioBlock b1 = new ScenarioBlock("充電電圧異常低下(AB00KE)");
        ScenarioBlock b11 = new ScenarioBlock("冷却水オーバーヒート(B@BCNS)");
        ScenarioBlock b2 = new ScenarioBlock("C1-10:ファンベルト");
        ScenarioBlock b3 = new ScenarioBlock("C1-1:エンジンOH");
        ScenarioBlock b31 = new ScenarioBlock("C1-12:ヘッドガスケット");

        b1.setAND(b11);
        b1.setNEXT(b2);
        b2.setNEXT(b3);
        b3.setOR(b31);

        return b1;
    }
    
    //検証シナリオ01
    public static ScenarioBlock s01() throws AISTProcessException {
        ScenarioBlock b1 = new ScenarioBlock("充電電圧異常低下(AB00KE)");
        ScenarioBlock b11 = new ScenarioBlock("冷却水オーバーヒート(B@BCNS)");
        ScenarioBlock b12 = new ScenarioBlock("ラジエータ水位異常低下(B@BCZK)");
        ScenarioBlock b3 = new ScenarioBlock("C1-1:エンジンOH");

        b1.setAND(b11);
        b11.setAND(b12);
        b1.setNEXT(b3);

        return b1;
    }
    
    //検証シナリオ1
    public static ScenarioBlock s1() throws AISTProcessException {
        ScenarioBlock b1 = new ScenarioBlock("M1:燃料メインフィルタ");
        ScenarioBlock b11 = new ScenarioBlock("M4:燃料プレフィルタ");
        ScenarioBlock b2 = new ScenarioBlock("燃料無圧送エラー(CA559)");
        ScenarioBlock b3 = new ScenarioBlock("C1-4:噴射系");

        b1.setOR(b11);
        b1.setNEXT(b2);
        b2.setNEXT(b3);

        return b1;
    }
    
    //検証シナリオ1
    public static ScenarioBlock s4() throws AISTProcessException {
        ScenarioBlock b1 = new ScenarioBlock("冷却水オーバーヒート(B@BCNS)");
        ScenarioBlock b2 = new ScenarioBlock("C1-3:冷却装置");
        ScenarioBlock b3 = new ScenarioBlock("C1-1:エンジンOH");
        
        b1.setNEXT(b2);
        b2.setNEXT(b3);

        return b1;
    }
}
