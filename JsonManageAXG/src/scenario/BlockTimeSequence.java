/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import time.TimeSeriesObject;

/**
 *
 * @author ZZ17807
 */
public class BlockTimeSequence {

    static Integer DELTA;
    static Integer TERM;
    public ScenarioBlock block;
    public Map<String, Integer[]> timeSeq;

    public BlockTimeSequence(ScenarioBlock block) {
        this.block = block;
        System.out.println(block.item + " 時系列での解析を実行");
        //Map<String, TimeSeriesObject> times = block.getBlockTimeSequence();
        //timeSeq = toTimeSequece(times);

        timeSeq = parseBlock(block);
        //times.entrySet().stream().map(tb -> tb.getKey()+":"+tb.getValue().series).forEach(System.out::println);   
        //timeSeq.entrySet().stream().map(tb -> tb.getKey()+":"+Arrays.asList(tb.getValue())).forEach(System.out::println);
    }

    int nest = 0;
    List<String> timeOR = new ArrayList<>();

    private List parseALL(List<ScenarioBlock> list, ScenarioBlock block) {
        if (block != null) {
            list.add(block);
            if (block.getAND() != null) {
                parseALL(list, block.getAND());
            }
            if (block.getOR() != null) {
                parseALL(list, block.getOR());
            }
        }
        return list;
    }

    private List parseAND(List<ScenarioBlock> list, ScenarioBlock block) {
        if (block != null) {
            if (block.getAND() != null) {
                parseAND(list, block.getAND());
            }

            list.add(block);
        }
        return list;
    }

    //シナリオブロックをパースして1系列にまとめる
    private Map<String, Integer[]> parseBlock(ScenarioBlock block) {
        List<ScenarioBlock> list = parseALL(new ArrayList<>(), block);

        //OR-AND解析
        Map<ScenarioBlock, List<ScenarioBlock>> or = list.stream()
                .filter(b -> b.getOR() != null)
                .collect(Collectors.toMap(b -> b, b -> parseAND(new ArrayList(), b.getOR())));

        //テスト出力
        /*or.entrySet().stream()
                .map(ob -> ob.getKey().item+
                        ":"+ob.getValue().stream().map(oab -> oab.item).collect(Collectors.toList()))
                .forEach(System.out::println);
         */
        //ORブロック部を優先計算
        Queue<ScenarioBlock> priority = new LinkedBlockingQueue<>(or.keySet());
        Map<ScenarioBlock, Map<String, Integer[]>> orTimeAnalize = new HashMap<>();
        while (priority.peek() != null) {
            ScenarioBlock orBlock = priority.poll();
            List<ScenarioBlock> orList = or.get(orBlock);
            System.out.println(orBlock.item);
            //演算の優先度を確認
            Optional<ScenarioBlock> check = orList.stream().filter(b -> priority.contains(b)).findFirst();
            if (check.isPresent()) {
                priority.offer(orBlock);
                continue;
            }

            Map<String, Integer[]> result = toTimeSequece(orBlock.getBlockTimeSequence());
            if (!orList.isEmpty()) {
                //時系列から数字配列に変換
                List<Map<String, Integer[]>> seqList = orList.stream()
                        .map(o -> orTimeAnalize.get(o) == null
                        ? toTimeSequece(o.getBlockTimeSequence())
                        : orTimeAnalize.get(o))
                        .collect(Collectors.toList());

                //演算
                Map<String, Integer[]> andResult = seqList.stream()
                        .reduce((a, b) -> calcTimeSequence("AND", a, b))
                        .orElse(null);

                result = calcTimeSequence("OR", result, andResult);

            }
            orTimeAnalize.put(orBlock, result);
        }
        
        
        list = parseAND(new ArrayList<>(), block);
        System.out.println(list.stream().map(li -> li.item).collect(Collectors.toList()));
        if (list.size() > 1) {
            return list.stream()
                    .map(lb -> orTimeAnalize.get(lb) != null ? orTimeAnalize.get(lb) : toTimeSequece(lb.getBlockTimeSequence()))
                    .reduce((b1, b2) -> calcTimeSequence("AND", b1, b2)).orElse(null);
        }else{
            return toTimeSequece(list.get(0).getBlockTimeSequence());
        }
        //計算結果
        /*orTimeAnalize.entrySet().stream()
                .map(e -> e.getKey().item+"\n"+e.getValue().entrySet().stream()
                                .map(ei -> "  "+ei.getKey()+Arrays.toString(ei.getValue())).collect(Collectors.joining("\n")))
                .forEach(System.out::println);
         */
        //演算のテスト出力
        //calcTestPrint(list);
    }

    private Map<String, Integer[]> calcTimeSequence(String op, Map<String, Integer[]> b1, Map<String, Integer[]> b2) {
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

    private Map<String, Integer[]> toTimeSequece(Map<String, TimeSeriesObject> times) {
        return times.entrySet().stream()
                .collect(Collectors.toMap(
                        t -> t.getKey(),
                        t -> {
                            int n = TERM / DELTA;
                            Integer[] seq = new Integer[n];
                            Arrays.fill(seq, 0);

                            t.getValue().series.stream()
                                    .filter(ti -> ti < TERM && ti > -1)
                                    .map(ti -> ti / DELTA)
                                    .forEach(tidx -> seq[tidx] += 1);
                            return seq;
                        }
                ));
    }

    private Integer[] zero() {
        Integer[] z = new Integer[TERM / DELTA];
        Arrays.fill(z, 0);
        return z;
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

    private void calcTestPrint(ScenarioBlock orblock, List<ScenarioBlock> list) {
        List<String> items = list.stream().map(li -> li.item).collect(Collectors.toList());
        List<Map<String, Integer[]>> bl = list.stream()
                .map(l -> toTimeSequece(l.getBlockTimeSequence()))
                .collect(Collectors.toList());

        System.out.println(bl);

        //演算テスト
        Map<String, Integer[]> bans = bl.size() == 1 ? bl.get(0) : bl.stream()
                .reduce((b1, b2) -> calcTimeSequence("AND", b1, b2)).orElse(null);

        Map<String, Integer[]> ormap = toTimeSequece(orblock.getBlockTimeSequence());
        Map<String, Integer[]> results = calcTimeSequence("OR", ormap, bans);

        System.out.println(String.join("|", items));

        results.entrySet().stream()
                .map(b -> {
                    Integer[] bansarr = bans.get(b.getKey());

                    StringBuilder s = new StringBuilder();
                    s.append(b.getKey());
                    IntStream.range(0, items.size())
                            .forEach(i -> {
                                Integer[] barr = bl.get(i).get(b.getKey());
                                String it = items.get(i).split(":")[0];
                                if (barr != null) {
                                    s.append("\n  " + it + ":" + Arrays.toString(barr));
                                } else {
                                    s.append("\n  " + it + ":None");
                                }
                            });

                    s.append("\n  and  :" + Arrays.toString(bansarr));
                    s.append("\n  " + orblock.item.split(":")[0] + ":" + Arrays.toString(ormap.get(b.getKey())));
                    s.append("\n       :" + Arrays.toString(b.getValue()));

                    return s.toString();
                })
                .forEach(System.out::println);
    }
}
