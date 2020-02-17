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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import testmain.ScenarioCreateTest;

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
        ScenarioBlock root = ScenarioCreateTest.stest();
        
        ScenarioAnalize scenario = new ScenarioAnalize(score, "project\\KM_PC200_DB\\out");
        scenario.analize(root);
    }

    public ScenarioAnalize(Map<String, String[]> score, String outPath) {
        this.score = score;
        this.path = outPath;
    }

    public void analize(ScenarioBlock root) throws AISTProcessException {
        errCheck(root);

        try {
            scenarioMap = new LinkedHashMap<>();
            scenarioMap.put("適合シナリオ", new ArrayList<>());

            //ブロックの出力
            System.out.println("登録されたシナリオ：");
            getBlock("", root);
            System.out.println("");

            //時系列作成 
            List<BlockTimeSequence> times = timesSequece(root);
            /*Map<String, List<Integer>> timeDelays = new HashMap<>();
            for(int i=0; i < times.size()-1; i++){
                timeDelays = timeSequenceDelay(times.get(i), times.get(i+1));
            }

            //スコアの適合シナリオ件数更新
            //適合シナリオ件数
            int scidx = Arrays.asList(score.get("#HEADER")).indexOf("シナリオ");
            timeDelays.entrySet().stream()
                    .forEach(td -> {
                        String[] sc = score.get(td.getKey());
                        sc[scidx] = String.valueOf(td.getValue().size());
                        score.put(td.getKey(), sc);
                        
                        td.getValue().stream()
                                .forEach(tdi -> scenarioMap.get("適合シナリオ").add(td.getKey() + "," + tdi));
                    });
             */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Timeで接続された時系列を作成
    private List<BlockTimeSequence> timesSequece(ScenarioBlock block) {
        List<BlockTimeSequence> times = new ArrayList<>();
        ScenarioBlock b = block;
        do {
            times.add(new BlockTimeSequence(b));
        } while ((b = b.getNEXT()) != null);

        return times;
    }

    /*
    private Map<String, List<Integer>> timeSequenceDelay(BlockTimeSequence start, BlockTimeSequence stop) {
        System.out.println(start.block.item + "->" + stop.block.item + " 時間遅れを解析");
        String timeTitle = start.block.item + "->" + stop.block.item;
        Map<String, List<Integer>> delays = new HashMap<>();

        for (String sid : stop.timeSeq.keySet()) {
            Integer[] st = start.timeSeq.get(sid);
            if (st == null) {
                if (scenarioMap.get(timeTitle + "シナリオ不適合") == null) {
                    scenarioMap.put(timeTitle + "シナリオ不適合", new ArrayList<>());
                }
                scenarioMap.get(timeTitle + "シナリオ不適合").add(sid);
                continue;
            } else {
                if (scenarioMap.get(timeTitle + " シナリオ適合") == null) {
                    scenarioMap.put(timeTitle + "シナリオ適合", new ArrayList<>());
                }
                scenarioMap.get(timeTitle + "シナリオ適合").add(sid);
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
            for (int j : area) {
                Optional<Integer> d = IntStream.range(i, j + 1).boxed()
                        .filter(t -> 0 < st[t])
                        .findFirst();
                if (d.isPresent()) {
                    //System.out.println(i+"-"+j+":"+d.get());
                    delay.add(j - d.get());

                    //適合時系列
                    fitStart.add(d.get());
                    fitStop.add(j);
                }

                i = j + 1;
            }

            //不適合時系列の削除
            start.reject(sid, fitStart);
            stop.reject(sid, fitStop);

            delays.put(sid, delay);
        }

        //テスト出力
        //System.out.println(testTimePrint(timeTitle + "シナリオ適合", scenarioMap.get(timeTitle + "シナリオ適合"), delays, start, stop));
        //System.out.println(testTimePrint(timeTitle + "シナリオ不適合", scenarioMap.get(timeTitle + "シナリオ不適合"), delays, start, stop));

        return delays;
    }

    private String testTimePrint(String s, Collection<String> sids, Map m1, BlockTimeSequence t1, BlockTimeSequence t2) {
        StringBuilder sb = new StringBuilder();
        sb.append(s);

        if (sids == null) {
            sb.append("\n None");
        } else {
            sids.stream()
                    .map(sid -> "\n" + sid + ":" + m1.get(sid)
                    + "\n  " + (t1.timeSeq.get(sid) != null ? Arrays.toString(t1.timeSeq.get(sid)) : " None")
                    + "\n  " + (t2.timeSeq.get(sid) != null ? Arrays.toString(t2.timeSeq.get(sid)) : " None"))
                    .forEach(sb::append);
        }
        return sb.toString();
    }*/
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
