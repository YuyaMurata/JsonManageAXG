/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import exception.AISTProcessException;
import file.CSVFileReadWrite;
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

/**
 *
 * @author kaeru
 */
public class ScenarioAnalize {

    private Map<String, List<String>> scenarioMap;
    private Map<String, String[]> score;
    private String path;

    public ScenarioAnalize(Map<String, String[]> score, String outPath) {
        this.score = score;
        this.path = outPath;
    }

    public void analize(ScenarioBlock root) {
        scenarioMap = new LinkedHashMap<>();
        scenarioMap.put("適合シナリオ", new ArrayList<>());
        getBlock("", root);
        
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

    public void similar(List<String> syaryoList) throws AISTProcessException {

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

    public static void time(ScenarioBlock start) {
        //車両ID + シナリオ(各部品のリスト)
        Map<String, List<List<String>>> sidTimes = extract(start);
    }

    private static Map<String, List<List<String>>> extract(ScenarioBlock block) {
        Map<String, List<List<String>>> map = new HashMap<>();
        while ((block = block.getNEXT()) != null) {
            Map<String, List<String>> dmap = block.data.parallelStream()
                .collect(Collectors.groupingBy(d -> d.split(",")[0]));
            dmap.entrySet().stream().forEach(d -> {
                if (map.get(d.getKey()) == null) {
                    map.put(d.getKey(), new ArrayList<>());
                }
                map.get(d.getKey()).add(d.getValue());
            });
        }

        map.entrySet().stream().forEach(m -> {
            System.out.println(m.getKey());
        });

        return null;
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
