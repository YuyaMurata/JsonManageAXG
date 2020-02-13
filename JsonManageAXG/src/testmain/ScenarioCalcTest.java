/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testmain;

import exception.AISTProcessException;
import extract.SyaryoObjectExtract;
import file.MapToJSON;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import scenario.ScenarioBlock;
import time.TimeSeriesObject;

/**
 *
 * @author kaeru
 */
public class ScenarioCalcTest {
    public static void main(String[] args) throws AISTProcessException {
        Map<String, String[]> score = ((Map<String, List<String>>) MapToJSON.toMapSJIS("project\\KM_PC200_DB\\out\\scoring_results.json")).entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toArray(new String[e.getValue().size()])));
        //抽出処理
        SyaryoObjectExtract objex = JsonManageAXGTestMain.extract();

        //シナリオの解析
        ScenarioBlock.setSyaryoObjectExtract(objex);
    }
    
    private static Map<String, Integer[]> calcTimeSequence(String op, Map<String, Integer[]> b1, Map<String, Integer[]> b2) {
        if (b1 == null || b2 == null) {
            return null;
        }
        List<String> sids = new ArrayList<>();
        sids.addAll(b1.keySet());
        sids.addAll(b2.keySet());
        sids = sids.stream().distinct().collect(Collectors.toList());

        return sids.stream().collect(Collectors.toMap(sid -> sid, sid -> {
            Integer[] b1arr = b1.get(sid) != null ? b1.get(sid) : zero();
            Integer[] b2arr = b2.get(sid) != null ? b2.get(sid) : zero();

            //AND演算
            if (op.equals("AND")) {
                return IntStream.range(0, b1arr.length).boxed()
                        .map(i -> Math.min(b1arr[i], b2arr[i]))
                        .toArray(Integer[]::new);
            } else {
                return IntStream.range(0, b1arr.length).boxed()
                        .map(i -> Math.max(b1arr[i], b2arr[i]))
                        .toArray(Integer[]::new);
            }
        }));
    }

    private  static  Map<String, Integer[]> toTimeSequece(Map<String, TimeSeriesObject> times) {
        return times.entrySet().stream()
                .collect(Collectors.toMap(
                        t -> t.getKey(),
                        t -> {
                            int n = 10000 / 1000;
                            Integer[] seq = new Integer[n];
                            Arrays.fill(seq, 0);

                            t.getValue().series.stream()
                                    .filter(ti -> ti < 10000 && ti > -1)
                                    .map(ti -> ti / 1000)
                                    .forEach(tidx -> seq[tidx] += 1);
                            return seq;
                        }
                ));
    }
    
    private static Integer[] zero() {
        Integer[] z = new Integer[10000 / 1000];
        Arrays.fill(z, 0);
        return z;
    }
}
