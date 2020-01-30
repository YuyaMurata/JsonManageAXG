package testmain;


import axg.cleansing.MSyaryoObjectCleansing;
import axg.shuffle.MSyaryoObjectShuffle;
import axg.shuffle.form.MSyaryoObjectFormatting;
import exception.AISTProcessException;
import extract.SyaryoObjectExtract;
import file.MapToJSON;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import scenario.ScenarioAnalize;
import scenario.ScenarioBlock;
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
    static String col = "komatsuDB_PC200";
    
    public static void main(String[] args) throws AISTProcessException {
        //cleansing();
        //shuffle();
        //MSyaryoObjectFormatting.form(db, col);
        SyaryoObjectExtract objex = extract();
        Map<String, String[]> score = scoring(objex);
        //scenario(score, objex);
    }
    
    public static void cleansing() throws AISTProcessException{
        MSyaryoObjectCleansing clean = new MSyaryoObjectCleansing(db, col);

        //テンプレート生成
        //String template = clean.createTemplate("project\\"+col+"\\config");
        //System.out.println("テンプレートファイル:"+template);
        
        //クレンジング処理
        //clean.clean(template);
        clean.clean("project\\"+col+"\\config\\cleansing_settings.json");
        
        //クレンジングログ出力
        //clean.logPrint("project\\"+col+"\\log");
        
        //サマリの出力
        //System.out.println(clean.getSummary());
    }
    
    public static void shuffle() throws AISTProcessException{
        MSyaryoObjectShuffle shuffle = new MSyaryoObjectShuffle(db, col);
        
        //テンプレート生成
        //String[] templates = shuffle.createTemplate("project\\"+col+"\\config");
        //System.out.println("テンプレートファイル:"+Arrays.toString(templates));
        
        //シャッフリング処理
        //shuffle.shuffle(templates[0], templates[1]);
        shuffle.shuffle("project\\"+col+"\\config\\shuffle_settings.json", "project\\"+col+"\\config\\layout_settings.json");
    }
    
    public static SyaryoObjectExtract extract() throws AISTProcessException{
        SyaryoObjectExtract objex = new SyaryoObjectExtract(db, col);
        
        //ユーザー定義ファイルの設定
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
    
    public static Map<String, String[]> scoring(SyaryoObjectExtract objex) throws AISTProcessException{
        //スコアリングのテンプレート生成
        String[] templates = ScoringSettingsTemplate.createTemplate(db, col, "project\\"+col+"\\config");
        System.out.println(Arrays.toString(templates));
        //Map mainte = MapToJSON.toMapSJIS(templates[0]);
        //Map use = MapToJSON.toMapSJIS(templates[1]);
        //Map agesmr = MapToJSON.toMapSJIS(templates[2]);
        
        
        //設定ファイルの読み込み
        Map mainte = MapToJSON.toMapSJIS("project\\"+col+"\\config\\score_maintenance_settings.json");
        Map use = MapToJSON.toMapSJIS("project\\"+col+"\\config\\score_use_settings.json");
        Map agesmr = MapToJSON.toMapSJIS("project\\"+col+"\\config\\score_agesmr_settings.json");
        
        //スコアリング
        SyaryoObjectEvaluation eval = new SyaryoObjectEvaluation(objex);
        Map<String, String[]> results = eval.scoring(mainte, use, agesmr, "project\\"+col+"\\out");
        
        //スコアリングの生成物の確認
        //生成画像ファイル一覧の取得
        System.out.println(eval.imageMap());
        //グループリスト
        System.out.println(eval.groupMap());
        
        //比較
        String compFile = eval.compare(new String[]{"project\\"+col+"\\out", "1,1", "2,1", "3,1"});
        System.out.println(compFile);
        
        return results;
    }
    
    public static void scenario(Map<String, String[]> score, SyaryoObjectExtract objex) throws AISTProcessException{
        //シナリオの作成
        ScenarioBlock startBlock = createScenarioBlock(objex);
        
        //シナリオの解析
        ScenarioAnalize scenario = new ScenarioAnalize(score, "project\\"+col+"\\out");
        scenario.analize(startBlock);
        
        //各項目の件数とシナリオ件数
        scenario.getScenarioResults().entrySet().stream().map(re -> re.getKey()+":"+re.getValue().size()).forEach(System.out::println);
        
        //↓フォームに表示される項目
        System.out.println(scenario.getSearchResults());
        
        //類似検索
        List<String> syaryoList = new ArrayList();   //選択した車両リスト
        scenario.similar(syaryoList, "");
        System.out.println(scenario.getSearchResults());
    }
    
    public static ScenarioBlock createScenarioBlock(SyaryoObjectExtract objex) throws AISTProcessException{
        ScenarioBlock.setSyaryoObjectExtract(objex);
        
        ScenarioBlock start = new ScenarioBlock("エンジンオイル");
        ScenarioBlock sc2 = new ScenarioBlock("エンジンオイルフィルタ");
        
        start.setNEXT(sc2);
        
        return start;
    }
}
