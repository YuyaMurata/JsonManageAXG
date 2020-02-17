package testmain;


import axg.cleansing.MSyaryoObjectCleansing;
import axg.shuffle.MSyaryoObjectShuffle;
import exception.AISTProcessException;
import extract.SyaryoObjectExtract;
import file.MapToJSON;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
    static String col = "KM_PC200_DB";
    //static String col = "SMALLTEST_DB";
    
    public static void main(String[] args) throws AISTProcessException {
        //cleansing();
        //shuffle();
        //MSyaryoObjectFormatting.form(db, col);
        SyaryoObjectExtract objex = extract();
        //車両の確認
        System.out.println("PC200-8-N1-315119"+":"+(objex.getAnalize("PC200-8-N1-315119")!=null));
        System.out.println("PC200-10- -452030"+":"+(objex.getAnalize("PC200-10--452030")!=null));
        System.out.println("PC200-8-N1-310933"+":"+(objex.getAnalize("PC200-8-N1-310933")!=null));
        System.out.println("PC200-8-N1-313776"+":"+(objex.getAnalize("PC200-8-N1-313776")!=null));
        System.out.println("PC200-8- -305351"+":"+(objex.getAnalize("PC200-8--305351")!=null));
        
        //Map<String, String[]> score = scoring(objex);
        //scenario(score, objex);
    }
    
    //クレンジング
    public static void cleansing() throws AISTProcessException{
        try{
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
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    //整形
    public static void shuffle() throws AISTProcessException{
        MSyaryoObjectShuffle shuffle = new MSyaryoObjectShuffle(db, col);
        
        //テンプレート生成
        //String[] templates = shuffle.createTemplate("project\\"+col+"\\config");
        //System.out.println("テンプレートファイル:"+Arrays.toString(templates));
        
        //シャッフリング処理
        //shuffle.shuffle(templates[0], templates[1]);
        shuffle.shuffle("project\\"+col+"\\config\\shuffle_settings.json", "project\\"+col+"\\config\\layout_settings.json");
    }
    
    //抽出
    public static SyaryoObjectExtract extract() throws AISTProcessException{
        SyaryoObjectExtract objex = new SyaryoObjectExtract(db, col);
        
        //ユーザー定義ファイルの設定
        objex.setUserDefine("project\\"+col+"\\config\\user_define.json");
        
        //データリスト
        //System.out.println(objex.getDataList());
        
        //オブジェクトリスト
        //System.out.println(objex.getObjectList());
        
        //サマリー
        String summary = objex.getSummary();
        System.out.println(summary);
        
        //シナリオ解析の項目
        //System.out.println(objex.getDefineItem());

        return objex;
    }
    
    //スコアリング
    public static Map<String, String[]> scoring(SyaryoObjectExtract objex) throws AISTProcessException{
        //スコアリングのテンプレート生成
        String[] templates = ScoringSettingsTemplate.createTemplate(db, col, "project\\"+col+"\\config");
        //System.out.println(Arrays.toString(templates));
        Map mainte = MapToJSON.toMapSJIS(templates[0]);
        Map use = MapToJSON.toMapSJIS(templates[1]);
        Map agesmr = MapToJSON.toMapSJIS(templates[2]);
        
        
        //設定ファイルの読み込み
        //Map mainte = MapToJSON.toMapSJIS("project\\"+col+"\\config\\score_maintenance_settings.json");
        //Map use = MapToJSON.toMapSJIS("project\\"+col+"\\config\\score_use_settings.json");
        //Map agesmr = MapToJSON.toMapSJIS("project\\"+col+"\\config\\score_agesmr_settings.json");
        
        //スコアリング
        SyaryoObjectEvaluation eval = new SyaryoObjectEvaluation(objex);
        Map<String, String[]> results = eval.scoring(mainte, use, agesmr, "project\\"+col+"\\out");
        
        //スコアリングの生成物の確認
        //生成画像ファイル一覧の取得
        //System.out.println(eval.imageMap());
        //グループリスト
        //System.out.println(eval.groupMap());
        
        //比較
        //String compFile = eval.compare(new String[]{"project\\"+col+"\\out", "1,1", "2,1", "3,1"});
        //System.out.println(compFile);
        
        return results;
    }
    
    //シナリオ
    public static void scenario(Map<String, String[]> score, SyaryoObjectExtract objex) throws AISTProcessException{
        //シナリオの作成
        ScenarioBlock startBlock = createScenarioBlock(objex);
        getBlock("", startBlock);
        System.out.println("");
        
        //シナリオの解析
        /*ScenarioAnalize scenario = new ScenarioAnalize(score, "project\\"+col+"\\out");
        scenario.analize(startBlock);
        
        //各項目の件数とシナリオ件数
        scenario.getScenarioResults().entrySet().stream().map(re -> re.getKey()+":"+re.getValue().size()).forEach(System.out::println);
        
        //↓フォームに表示される項目
        System.out.println(scenario.getSearchResults());
        
        //類似検索
        List<String> syaryoList = new ArrayList();   //選択した車両リスト
        scenario.similar(syaryoList, "");
        System.out.println(scenario.getSearchResults());*/
    }
    
    //テスト用
    public static ScenarioBlock createScenarioBlock(SyaryoObjectExtract objex) throws AISTProcessException{
        ScenarioBlock.setSyaryoObjectExtract(objex);
        
        ScenarioBlock start = new ScenarioBlock("部品1-1");
        ScenarioBlock sc12 = new ScenarioBlock("部品1-2");
        ScenarioBlock sc13 = new ScenarioBlock("部品1-3");
        ScenarioBlock sc14 = new ScenarioBlock("部品1-4");
        ScenarioBlock sc15 = new ScenarioBlock("部品1-5");
        ScenarioBlock sc16 = new ScenarioBlock("部品1-6");
        ScenarioBlock sc161 = new ScenarioBlock("部品1-6");
        ScenarioBlock sc21 = new ScenarioBlock("部品2-1");
        ScenarioBlock sc22 = new ScenarioBlock("部品2-2");
        ScenarioBlock sc31 = new ScenarioBlock("部品3-1");
        ScenarioBlock sc32 = new ScenarioBlock("部品3-2");
        ScenarioBlock sc33 = new ScenarioBlock("部品3-3");
        
        start.setNEXT(sc21);
        start.setAND(sc14);
        start.setOR(sc12);
        
        sc12.setAND(sc13);
        sc13.setOR(sc14);
        sc14.setAND(sc15);
        sc15.setOR(sc16);
        
        sc21.setAND(sc31);
        sc21.setOR(sc22);
        
        sc31.setOR(sc32);
        sc32.setAND(sc33);
        
        return start;
    }
    
    //シナリオブロックの表示テスト
    static int nest = 0;
    public static void getBlock(String s, ScenarioBlock block){
        if (block != null) {
            if (s.equals("-")) {
                System.out.print(s + block.item);
            } else if (s.equals("|")) {
                String indent = IntStream.range(0, nest-4).boxed().map(i -> " ").collect(Collectors.joining());
                System.out.println("");
                System.out.print("|" + indent + s + block.item);
            } else {
                System.out.print(s + block.item);
            }

            nest+=block.item.getBytes().length;
            getBlock("-", block.getAND());
            nest-=block.item.getBytes().length;
            getBlock("|", block.getOR());
            getBlock("\n", block.getNEXT());
        }
    }
}
