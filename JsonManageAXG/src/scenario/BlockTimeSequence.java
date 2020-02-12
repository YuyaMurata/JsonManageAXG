/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import time.TimeSeriesObject;

/**
 *
 * @author ZZ17807
 */
public class BlockTimeSequence {

    static Integer DELTA;
    public ScenarioBlock block;
    public Map<String, Integer[]> timeSeq;
    private Deque<String> timeParseSeq;

    public BlockTimeSequence(ScenarioBlock block) {
        this.block = block;
        System.out.println(block.item + " 時系列での解析を実行");
        Map<String, TimeSeriesObject> times = block.getBlockTimeSequence();
        timeSeq = toTimeSequece(times);

        timeParseSeq = new LinkedBlockingDeque<>();
        parseBlock("", block);
        timeParseSeq.stream().forEach(System.out::println);

        //times.entrySet().stream().map(tb -> tb.getKey()+":"+tb.getValue().series).forEach(System.out::println);   
        //timeSeq.entrySet().stream().map(tb -> tb.getKey()+":"+Arrays.asList(tb.getValue())).forEach(System.out::println);
    }

    int nest = 0;
    List<String> timeOR = null;

    private void parseBlock(String s, ScenarioBlock block) {
        if (block != null) {
            if (s.equals("|")) {
                timeOR = new ArrayList<>();
            }

            if (timeOR != null) {
                timeOR.add(block.item);
            } else {
                timeParseSeq.push(block.item);
            }

            nest++;
            parseBlock("&", block.getAND());
            nest--;
            parseBlock("|", block.getOR());

            //System.out.println(" " + block.item);
        } else {
            if (timeOR != null) {
                if (!timeOR.isEmpty()) {
                    int orNest = nest - timeOR.size();
                    String si;
                    List<String> sil = new ArrayList<>();
                    while ((orNest > 0) && (timeParseSeq.peek() != null)) {
                        sil.add(timeParseSeq.pop());
                        orNest--;
                    }
                    System.out.println(String.join("&", timeOR)+"|"+String.join("&", sil));
                    
                    timeOR = null;
                }
            }
        }
    }

    private Map<String, Integer[]> toTimeSequece(Map<String, TimeSeriesObject> times) {
        return timeSeq = times.entrySet().stream()
                .collect(Collectors.toMap(
                        t -> t.getKey(),
                        t -> {
                            int n = 10000 / DELTA;
                            Integer[] seq = new Integer[n];
                            Arrays.fill(seq, 0);

                            t.getValue().series.stream()
                                    .filter(ti -> ti < 10000 && ti > -1)
                                    .map(ti -> ti / DELTA)
                                    .forEach(tidx -> seq[tidx] += 1);
                            return seq;
                        }
                ));
    }

    public void reject(String sid, List<Integer> fit) {
        Integer[] tseq = timeSeq.get(sid);
        IntStream.range(0, tseq.length)
                .filter(t -> !fit.contains(t))
                .forEach(t -> tseq[t] = 0);
        timeSeq.put(sid, tseq);
    }

    public void print() {
        System.out.println(block.item);
        timeSeq.entrySet().stream()
                .map(tb -> tb.getKey() + ":" + Arrays.asList(tb.getValue()))
                .forEach(System.out::println);
    }
}
