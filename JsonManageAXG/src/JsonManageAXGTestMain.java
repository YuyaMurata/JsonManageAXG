
import axg.cleansing.MSyaryoObjectCleansing;
import axg.shuffle.MSyaryoObjectShuffle;
import exception.AISTProcessException;
import extract.SyaryoObjectExtract;
import file.MapToJSON;
import java.util.Arrays;
import java.util.Map;
import score.SyaryoObjectEvaluation;
import score.template.ScoringSettingsTemplate;

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
    static String col = "testDB";
    
    public static void main(String[] args) throws AISTProcessException {
        //cleansing();
        //shuffle();
        SyaryoObjectExtract objex = extract();
        scoring(objex);
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
    
    private static SyaryoObjectExtract extract() throws AISTProcessException{
        SyaryoObjectExtract objex = new SyaryoObjectExtract(db, col);
        objex.setUserDefine("project\\"+col+"\\config\\user_define.json");
        
        //シナリオ解析の項目
        System.out.println(objex.getDefineItem());
        
        //データリスト
        System.out.println(objex.getDataList());
        
        //オブジェクトリスト
        System.out.println(objex.getObjectList());
        
        //サマリー
        System.out.println(objex.getSummary());
        
        return objex;
    }
    
    private static void scoring(SyaryoObjectExtract objex) throws AISTProcessException{
        //スコアリングのテンプレート生成
        String[] templates = ScoringSettingsTemplate.createTemplate(db, col, "project\\"+col+"\\config");
        System.out.println(Arrays.toString(templates));
        
        //設定ファイルの読み込み
        Map mainte = MapToJSON.toMapSJIS("project\\"+col+"\\config\\score_maintenance_settings.json");
        Map use = MapToJSON.toMapSJIS("project\\"+col+"\\config\\score_use_settings.json");
        Map agesmr = MapToJSON.toMapSJIS("project\\"+col+"\\config\\score_agesmr_settings.json");
        
        //スコアリング
        SyaryoObjectEvaluation eval = new SyaryoObjectEvaluation(objex);
        eval.scoring(mainte, use, agesmr, "project\\"+col+"\\out");
        
        //比較
        eval.compare(new String[]{"out", "1_1", "2_1", "3_1"});
        
    }
}
