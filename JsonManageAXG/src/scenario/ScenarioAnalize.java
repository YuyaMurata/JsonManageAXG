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
import java.util.Optional;
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
    private int delta = 1000;
    
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
        try{
            System.out.println("ブロックの中身を確認:"+root);
            System.out.println("ブロックの中身を確認:"+root.getNEXT());
            
        scenarioMap = new LinkedHashMap<>();
        scenarioMap.put("適合シナリオ", new ArrayList<>());
        getBlock("", root);
        
        //時系列作成
        BlockTimeSequence.DELTA = delta;
        List<BlockTimeSequence> times = timesSequece(root);
        Map<String, List<Integer>> timeDelays = timeSequenceDelay(times.get(0), times.get(1));
        
        //スコアの適合シナリオ件数更新
        //適合シナリオ件数
        int scidx = Arrays.asList(score.get("#HEADER")).indexOf("シナリオ");
        timeDelays.entrySet().stream()
                .forEach(td -> {
                    String[] sc = score.get(td.getKey());
                    sc[scidx] = String.valueOf(td.getValue().size());
                    score.put(td.getKey(), sc);
                    
                    td.getValue().stream()
                                .forEach(tdi -> scenarioMap.get("適合シナリオ").add(td.getKey()+","+tdi));});
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    //Timeで接続された時系列を作成
    private List<BlockTimeSequence> timesSequece(ScenarioBlock block){
        List<BlockTimeSequence> times = new ArrayList<>();
        ScenarioBlock b = block;
        do{
            times.add(new BlockTimeSequence(b));
        }while((b = b.getNEXT()) != null);
        
        return times;
    }
    
    private Map<String, List<Integer>> timeSequenceDelay(BlockTimeSequence start, BlockTimeSequence stop){
        System.out.println("2ブロックの時間遅れを解析");
        Map<String, List<Integer>> delays = new HashMap<>();
        
        for(String sid : stop.timeSeq.keySet()){
            Integer[] st = start.timeSeq.get(sid);
            if(st == null){
                System.out.println(sid+" シナリオ不適合");
                continue;
            }
            Integer[] sp = stop.timeSeq.get(sid);
            
            //Diveide Time Area
            List<Integer> area = IntStream.range(0, sp.length).boxed()
                                        .filter(i -> 0 < sp[i])
                                        .collect(Collectors.toList());
            
            //Time Area Delay
            List<Integer> fitStart = new ArrayList();
            List<Integer> fitStop = new ArrayList();
            List<Integer> delay = new ArrayList();
            int i = 0;
            for(int j : area){
                Optional<Integer> d = IntStream.range(i, j+1).boxed()
                                            .filter(t -> 0 < st[t])
                                            .findFirst();
                if(d.isPresent()){
                    //System.out.println(i+"-"+j+":"+d.get());
                    delay.add(j-d.get());
                    
                    //適合時系列
                    fitStart.add(d.get());
                    fitStop.add(j);
                }
                
                i = j+1;
            }
            
            //不適合時系列の削除
            start.reject(sid, fitStart);
            stop.reject(sid, fitStop);
            
            delays.put(sid, delay);
        }
        
        delays.entrySet().stream().map(tb -> tb.getKey()+":"+tb.getValue()).forEach(System.out::println);
        
        return delays;
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
