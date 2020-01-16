/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import testmain.JsonManageAXGTestMain;
import exception.AISTProcessException;
import extract.SyaryoObjectExtract;
import file.CSVFileReadWrite;
import file.MapToJSON;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import score.time.TimeSeriesObject;

/**
 *
 * @author kaeru
 */
public class ScenarioAnalize {

    private Map<String, List<String>> scenarioMap;
    private Map<String, String[]> score;
    private String path;
    private int delta = 100;
    
    //Test用
    public static void main(String[] args) throws AISTProcessException {
        Map<String, String[]> score = ((Map<String, List<String>>)MapToJSON.toMapSJIS("scenario_valid.json")).entrySet().stream()
                                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toArray(new String[e.getValue().size()])));
        //抽出処理
        SyaryoObjectExtract objex = JsonManageAXGTestMain.extract();
        JsonManageAXGTestMain.scenario(score, objex);
    }
    
    public ScenarioAnalize(Map<String, String[]> score, String outPath) {
        this.score = score;
        this.path = outPath;
    }

    public void analize(ScenarioBlock root) {
        scenarioMap = new LinkedHashMap<>();
        scenarioMap.put("適合シナリオ", new ArrayList<>());
        getBlock("", root);
        
        //時系列作成
        timeSequece(root);
        
        //テスト用
        Random rand = new Random();
        List<String> h = Arrays.asList(score.get("#HEADER"));
        int lastN = scenarioMap.values().stream().map(v -> v.size()).reduce((e1, e2) -> e2).orElse(null);
        for (String sid : score.keySet()) {
            if (sid.equals("#HEADER") || (rand.nextInt(score.size())< lastN)) {
                continue;
            }

            String[] s = score.get(sid);
            Integer num = rand.nextInt(10);
            IntStream.range(0, num).forEach(i
                -> scenarioMap.get("適合シナリオ").add(sid + "," + i + "XX,OOO")
            );
            s[h.indexOf("シナリオ")] = String.valueOf(num);
        }
    }
    
    //車両ごとの時系列を作成
    private Map<String, Integer[]> timeSequece(ScenarioBlock block){
        System.out.println("時系列での解析を実行");
        Map<String, TimeSeriesObject> times = block.getBlockTimeSequence();
        times.entrySet().stream().map(t -> t.getKey()+":"+t.getValue().series).forEach(System.out::println);
        
        return null;
    }

    public void similar(List<String> syaryoList, String syaryo) throws AISTProcessException {

        //テスト用
        Random rand = new Random();
        List<String> h = Arrays.asList(score.get("#HEADER"));
        for (String sid : score.keySet()) {
            if (sid.equals("#HEADER")) {
                continue;
            }

            String[] s = score.get(sid);
            s[h.indexOf("類似度")] = String.valueOf(Math.abs(rand.nextDouble()));
        }
        
        //CSV出力
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS(path+"\\simular_search_results.csv")){
            score.entrySet().stream().map(e -> e.getKey()+","+String.join(",", e.getValue())).forEach(pw::println);
        }
    }

    public Map<String, List<String>> getScenarioResults() {
        return scenarioMap;
    }

    public Map<String, String[]> getSearchResults() {
        return score;
    }

    private void getBlock(String s, ScenarioBlock block) {
        if (block != null) {
            scenarioMap.put(block.item, block.data);
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
