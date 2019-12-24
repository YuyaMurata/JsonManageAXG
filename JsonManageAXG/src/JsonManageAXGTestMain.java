
import axg.cleansing.MSyaryoObjectCleansing;
import axg.shuffle.MSyaryoObjectShuffle;
import java.util.Arrays;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ZZ17807
 */
public class JsonManageAXGTestMain {
    public static void main(String[] args) {
        //cleansing();
        shuffle();
    }
    
    private static void cleansing(){
        //テンプレート生成
        String template = MSyaryoObjectCleansing.createTemplate("json", "komatsuDB_PC200", "project\\komatsuDB_PC200\\config");
        System.out.println("テンプレートファイル:"+template);
        
        //クレンジング処理
        MSyaryoObjectCleansing clean = new MSyaryoObjectCleansing("json", "komatsuDB_PC200");
        //clean.clean(template);
        clean.clean("project\\komatsuDB_PC200\\config\\cleansing_settings.json");
        
        //クレンジングログ出力
        clean.logPrint("project\\komatsuDB_PC200\\log");
        
        //サマリの出力
        clean.getSummary();
    }
    
    private static void shuffle(){
        String[] templates = MSyaryoObjectShuffle.createTemplate("json", "komatsuDB_PC200", "project\\komatsuDB_PC200\\config");
        System.out.println("テンプレートファイル:"+Arrays.toString(templates));
        
        //シャッフリング処理
        MSyaryoObjectShuffle shuffle = new MSyaryoObjectShuffle("json", "komatsuDB_PC200");
        shuffle.shuffle(templates[0], templates[1]);
        //shuffle.shuffle("project\\komatsuDB_PC200\\config\\shuffle_settings.json", "project\\komatsuDB_PC200\\config\\layout_settings.json");
    }
}
