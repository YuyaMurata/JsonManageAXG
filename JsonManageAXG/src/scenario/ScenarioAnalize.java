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
import scenario.valid.ValidateCalculateBlock;
import testmain.ScenarioCreateTest;
import time.TimeSeriesObject;

/**
 *
 * @author kaeru
 */
public class ScenarioAnalize {

    private Map<String, List<String>> scenarioMap;
    private Map<String, String[]> score;
    private String path;

    //Test用
    public static void main(String[] args) throws AISTProcessException {
        Map<String, String[]> score = ((Map<String, List<String>>) MapToJSON.toMapSJIS("project\\KM_PC200_DB\\out\\scoring_results.json")).entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toArray(new String[e.getValue().size()])));
        //抽出処理
        SyaryoObjectExtract objex = JsonManageAXGTestMain.extract();

        //シナリオの解析
        ScenarioBlock.setSyaryoObjectExtract(objex);
        ScenarioBlock root = ScenarioCreateTest.s01();

        ScenarioAnalize scenario = new ScenarioAnalize(score, "project\\KM_PC200_DB\\out");
        scenario.analize(root);
    }

    public ScenarioAnalize(Map<String, String[]> score, String outPath) {
        this.score = score;
        this.path = outPath;
    }

    ValidateCalculateBlock valid;

    public void analize(ScenarioBlock root) throws AISTProcessException {
        errCheck(root);

        try {
            valid = new ValidateCalculateBlock();
            scenarioMap = new LinkedHashMap<>();
            scenarioMap.put("適合シナリオ", new ArrayList<>());

            //ブロックの出力
            System.out.println("登録されたシナリオ：");
            getBlock("", root);
            System.out.println("");

            //時系列作成 
            List<BlockTimeSequence> blockList = timesSequece(root);
            Map<String, Map<String, List<Integer>>> timeMap = new LinkedHashMap<>();
            List<String> fit = new ArrayList<>(blockList.get(0).pBlock.blockSeq.keySet());
            for (int i = 0; i < blockList.size() - 1; i++) {
                BlockTimeSequence start = blockList.get(i);
                BlockTimeSequence stop = blockList.get(i + 1);
                Map<String, List<Integer>> delay = diffTimeSequenceDelay(start, stop);
                timeMap.put(start.block.item + "->" + stop.block.item, delay);
                
                //適合チェック
                fit = fit.stream().filter(sid -> delay.get(sid) != null).collect(Collectors.toList());
            }
            
            //全系列で共通する車両IDを抽出

            //時系列評価
            Map<String, List<Integer>> eval = new HashMap();
            fit.stream().forEach(sid ->{
                timeMap.values().stream().map(t -> t.get(sid))
                        .forEach(v ->{
                            if(eval.get(sid) == null)
                                eval.put(sid, new ArrayList<>());
                            eval.get(sid).addAll(v);
                        }); 
            });
            
            System.out.println("評価対象:"+eval.size());
            
            blockList.stream().forEach(b -> valid.setBlock(b.pBlock));
            valid.setDelay("Fin.Scenario", eval);
            valid.filter(eval.keySet());
            valid.toFile("Scenario.csv");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //各時系列ブロックのAND OR解析
    private List<BlockTimeSequence> timesSequece(ScenarioBlock block) {
        List<BlockTimeSequence> times = new ArrayList<>();
        ScenarioBlock b = block;
        do {
            times.add(new BlockTimeSequence(b));
        } while ((b = b.getNEXT()) != null);

        return times;
    }

    private Map<String, List<Integer>> diffTimeSequenceDelay(BlockTimeSequence start, BlockTimeSequence stop) {
        System.out.println(start.block.item + "->" + stop.block.item + " 時間遅れを解析");
        String timeTitle = start.block.item + "->" + stop.block.item;
        Map<String, List<Integer>> delays = new HashMap<>();
        
        //2ブロックに共通して存在する車両リスト
        List<String> fitSIDs = start.pBlock.blockSeq.keySet().stream()
                    .filter(sid -> stop.pBlock.blockSeq.get(sid) != null)
                    .collect(Collectors.toList());
            

        for (String sid : fitSIDs) {
            TimeSeriesObject stobj = start.pBlock.getBlock1Seq(sid);
            TimeSeriesObject spobj = stop.pBlock.getBlock1Seq(sid);

            //Diveide Time Area
            List<Integer> area = IntStream.range(0, spobj.arrSeries.length).boxed()
                    .filter(i -> 0 < spobj.arrSeries[i])
                    .collect(Collectors.toList());

            //Time Area Delay
            List<Integer> fitStart = new ArrayList();
            List<Integer> fitStop = new ArrayList();
            List<Integer> delay = new ArrayList();
            int i = 0;
            for (int j : area) {
                Optional<Integer> d = IntStream.range(i, j + 1).boxed()
                        .filter(t -> 0 < stobj.arrSeries[t])
                        .findFirst();
                if (d.isPresent()) {
                    delay.add(j - d.get());

                    //適合時系列
                    fitStart.add(d.get());
                    fitStop.add(j);
                }

                i = j + 1;
            }

            //選択されなかった時系列の削除
            stobj.delete(fitStart);
            spobj.delete(fitStop);

            //valid.setStrBlock("Del", start.pBlock);
            //valid.setStrBlock("Del", stop.pBlock);
            
            if(!delay.isEmpty())
                delays.put(sid, delay);
        }

        //valid.setDelay(timeTitle, delays);

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
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS(path + "\\simular_search_results.csv")) {
            score.entrySet().stream().map(e -> e.getKey() + "," + String.join(",", e.getValue())).forEach(pw::println);
        }
    }

    public Map<String, List<String>> getScenarioResults() {
        return scenarioMap;
    }

    public Map<String, String[]> getSearchResults() {
        return score;
    }

    int nest = 0;

    private void getBlock(String s, ScenarioBlock block) {
        if (block != null) {
            scenarioMap.put(block.item, block.data.toList());
            if (s.equals("-")) {
                System.out.print(s + block.item);
            } else if (s.equals("|")) {
                String indent = IntStream.range(0, nest - 4).boxed().map(i -> " ").collect(Collectors.joining());
                System.out.println("");
                System.out.print("|" + indent + s + block.item);
            } else {
                System.out.print(s + block.item);
            }

            nest += block.item.getBytes().length;
            getBlock("-", block.getAND());
            nest -= block.item.getBytes().length;
            getBlock("|", block.getOR());
            getBlock("\n", block.getNEXT());
        }
    }

    private void errCheck(ScenarioBlock root) throws AISTProcessException {
        if (root == null) {
            System.err.println("シナリオブロックの中身がNullです");
            throw new AISTProcessException("シナリオブロックエラー");
        }
        if (!root.getErrCheck().isEmpty()) {
            System.err.println(root.getErrCheck());
            throw new AISTProcessException("シナリオブロックエラー");
        }
    }
}
