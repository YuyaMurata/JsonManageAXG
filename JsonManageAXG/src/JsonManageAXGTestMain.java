
import axg.cleansing.MSyaryoObjectCleansing;
import axg.shuffle.MSyaryoObjectShuffle;
import exception.AISTProcessException;
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
    static String db = "json";
    static String col = "komatsuDB_PC200";
    
    public static void main(String[] args) throws AISTProcessException {
        //cleansing();
        shuffle();
    }
    
    private static void cleansing() throws AISTProcessException{
        //テンプレート生成
        String template = MSyaryoObjectCleansing.createTemplate(db, col, "project\\"+col+"\\config");
        System.out.println("テンプレートファイル:"+template);
        
        //クレンジング処理
        MSyaryoObjectCleansing clean = new MSyaryoObjectCleansing(db, col);
        //clean.clean(template);
        clean.clean("project\\"+col+"\\config\\cleansing_settings.json");
        
        //クレンジングログ出力
        clean.logPrint("project\\"+col+"\\log");
        
        //サマリの出力
        System.out.println(clean.getSummary());
    }
    
    private static void shuffle() throws AISTProcessException{
        String[] templates = MSyaryoObjectShuffle.createTemplate(db, col, "project\\"+col+"\\config");
        System.out.println("テンプレートファイル:"+Arrays.toString(templates));
        
        //シャッフリング処理
        MSyaryoObjectShuffle shuffle = new MSyaryoObjectShuffle(db, col);
        //shuffle.shuffle(templates[0], templates[1]);
        shuffle.shuffle("project\\"+col+"\\config\\shuffle_settings.json", "project\\"+col+"\\config\\layout_settings.json");
    }
}
