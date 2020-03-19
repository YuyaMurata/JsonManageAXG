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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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
    private Map<String, String[]> scoreShowResult;
    private String path;
    private Map<String, List<Integer>> eval;

    //ブロックの計算検証用
    private ValidateCalculateBlock valid;

    //Test用
    public static void main(String[] args) throws AISTProcessException {
        Map<String, String[]> score = JsonManageAXGTestMain.getScoring();
        //抽出処理
        SyaryoObjectExtract objex = JsonManageAXGTestMain.extract();

        //シナリオの解析
        ScenarioBlock.setSyaryoObjectExtract(objex);
        ScenarioBlock root = ScenarioCreateTest.s0();

        ScenarioAnalize scenario = new ScenarioAnalize(score, "project\\KM_PC200_DB\\out");
        scenario.analize(root);
        scenario.getScenarioResults().entrySet().stream().map(re -> re.getKey()+":"+re.getValue().size()).forEach(System.out::println);
    }

    public ScenarioAnalize(Map<String, String[]> score, String outPath) {
        this.score = score;
        this.path = outPath;
        ValidateCalculateBlock.OUTPATH = outPath;
    }

    //シナリオ解析
    public void analize(ScenarioBlock root) throws AISTProcessException {
        errCheck(root);

        try {
            valid = new ValidateCalculateBlock();
            scenarioMap = new LinkedHashMap<>();

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

            //時系列評価
            eval = new TreeMap<>();
            fit.stream().forEach(sid -> {
                timeMap.values().stream().map(t -> t.get(sid))
                        .forEach(v -> {
                            if (eval.get(sid) == null) {
                                eval.put(sid, new ArrayList<>());
                            }
                            eval.get(sid).addAll(v);
                        });
            });
            System.out.println("評価対象車両:" + eval.size() + " 台");

            //適合シナリオの登録
            List<String> evalN = eval.entrySet().stream()
                    .flatMap(e -> e.getValue().stream().map(ei -> e.getKey() + "," + ei))
                    .collect(Collectors.toList());
            scenarioMap.put("適合シナリオ", new ArrayList<>(evalN));

            //シナリオ適合結果
            int scIdx = Arrays.asList(score.get("#HEADER")).indexOf("シナリオ");
            eval.entrySet().stream().forEach(e -> {
                //シナリオ解析が ブロックA->B->C の評価で A->B,B->Cが行われるため
                int bn = blockList.size() > 1 ? blockList.size() - 1 : 1;
                try{
                score.get(e.getKey())[scIdx] = String.valueOf(e.getValue().size() / bn);
                }catch(Exception e1){
                    System.err.println(e.getKey()+":"+score.get(e.getKey()));
                    e1.printStackTrace();
                }
            });
            
            //スコアの解析結果並び替える
            scoreShowResult = score.entrySet().stream()
                                    .filter(sc -> sc.getKey().charAt(0) != '#')
                                    .filter(sc -> Integer.valueOf(sc.getValue()[scIdx]) > 0)  //評価されていない車両は除外
                                    .sorted(Comparator.comparing(sc -> Integer.valueOf(sc.getValue()[scIdx]), Comparator.reverseOrder()))
                                    .collect(Collectors.toMap(sc -> sc.getKey(), sc -> sc.getValue(), (a,b) -> b, LinkedHashMap::new));
            
            
            blockList.stream().forEach(b -> valid.setBlock(b.pBlock));
            valid.setDelay("Fin.Scenario", eval);
            valid.filter(eval.keySet());
            valid.toFile("Result.csv");

        } catch (Exception e) {
            e.printStackTrace();
            throw new AISTProcessException("シナリオ解析エラー");
        }
    }

    ////シナリオブロックを横(AND・OR)方向に解析
    private List<BlockTimeSequence> timesSequece(ScenarioBlock block) {
        List<BlockTimeSequence> times = new ArrayList<>();
        ScenarioBlock b = block;
        do {
            times.add(new BlockTimeSequence(b));
        } while ((b = b.getNEXT()) != null);

        return times;
    }

    //シナリオブロックを縦(時間)方向に解析
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
            if (!delay.isEmpty()) {
                delays.put(sid, delay);
            }
        }

        //valid.setDelay(timeTitle, delays);
        return delays;
    }

    //類似検索
    public void similar(Collection<String> syaryoList, String target) throws AISTProcessException {
        System.out.println("Target:"+target);
        target = target.split(":")[0]; //車両IDに余計ない情報が付加されている場合の除去
        
        if(target.length() < 5){
            int scIdx = Arrays.asList(score.get("#HEADER")).indexOf("シナリオ");
            //スコアの解析結果並び替える
            scoreShowResult = score.entrySet().stream()
                                    .filter(sc -> sc.getKey().charAt(0) != '#')
                                    .filter(sc -> Integer.valueOf(sc.getValue()[scIdx]) > 0)  //評価されていない車両は除外
                                    .sorted(Comparator.comparing(sc -> Integer.valueOf(sc.getValue()[scIdx]), Comparator.reverseOrder()))
                                    .collect(Collectors.toMap(sc -> sc.getKey(), sc -> sc.getValue(), (a,b) -> b, LinkedHashMap::new));
            return;
        }
          
        //車両リスト中およびターゲットの確認
        errCheck(target);
        
        int scIdx = Arrays.asList(score.get("#HEADER")).indexOf("類似度");
        
        //Jaccard係数の算出
        List<Integer> evalTarget = eval.get(target);
        eval.entrySet().stream()
                .filter(e -> syaryoList.contains(e.getKey()))
                .forEach(e ->{
                    List<Integer> set = new ArrayList<>();
                    set.addAll(e.getValue());
                    set.addAll(evalTarget);
                    Long u = set.stream().distinct().count();
                    Long x = evalTarget.stream()
                                    .filter(ti -> e.getValue().contains(ti))
                                    .distinct().count();
                    score.get(e.getKey())[scIdx] = String.valueOf(x.doubleValue() / u.doubleValue());
                });
        
        //スコアの解析結果並び替える
        scoreShowResult = score.entrySet().stream()
                                .filter(sc -> sc.getKey().charAt(0) != '#')
                                .filter(sc -> !sc.getValue()[scIdx].equals("0"))
                                .sorted(Comparator.comparing(sc -> Double.valueOf(sc.getValue()[scIdx]), Comparator.reverseOrder()))
                                .collect(Collectors.toMap(sc -> sc.getKey(), sc -> sc.getValue(), (a,b) -> b, LinkedHashMap::new));
            
        
        //CSV出力
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS(path + "\\simular_search_results.csv")) {
            //header
            pw.println(String.join(",", score.get("#HEADER")));
            
            //スコア
            score.entrySet().stream()
                    .filter(e -> e.getKey().charAt(0) != '#')
                    .map(e -> String.join(",", e.getValue()))
                    .forEach(pw::println);
        }
    }

    private void errCheck(String target) throws AISTProcessException {
        if (eval == null) {
            System.err.println("シナリオ評価がされていません");
            throw new AISTProcessException("類似検索エラー");
        } else {
            if (eval.get(target) == null) {
                System.err.println("選択車両はシナリオ評価対象外です");
                throw new AISTProcessException("類似検索エラー");
            }
        }
    }

    public Map<String, List<String>> getScenarioResults() {
        return scenarioMap;
    }

    public Map<String, String[]> getSearchResults() throws AISTProcessException {
        if(scoreShowResult.isEmpty())
            throw new AISTProcessException("シナリオ適合車両が存在しません");
        return scoreShowResult;
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
