/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import scenario.valid.ValidateCalculateBlock;
import time.TimeSeriesObject;

/**
 * AND-ORで接続されたシナリオブロックを１つにまとめる |ブロック1| － |ブロック2| -> |ブロック123| L |ブロック3|
 *
 * @author ZZ17807
 */
public class BlockTimeSequence {

    public ScenarioBlock block;
    public ScenarioBlock pBlock;
    private ValidateCalculateBlock valid;

    public BlockTimeSequence(ScenarioBlock block) {
        this.block = block;
        System.out.println(block.item + " の解析を実行");
        this.valid = new ValidateCalculateBlock();
        this.pBlock = parseBlock(block);
        this.pBlock = reject0(pBlock);
        
        this.valid.setStrBlock("Fin", pBlock);
        this.valid.toFile(block.item.replace(":", "") + ".csv");
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

    //シナリオブロックをパースしてまとめる
    private ScenarioBlock parseBlock(ScenarioBlock block) {
        List<ScenarioBlock> list = parseALL(new ArrayList<>(), block);
        
        //ORブロックにANDで接続されたブロックの集約
        Map<ScenarioBlock, List<ScenarioBlock>> or = list.stream()
                .peek(b -> valid.setBlock(b))
                .filter(b -> b.getOR() != null)
                .collect(Collectors.toMap(b -> b, b -> parseAND(new ArrayList(), b.getOR())));

        //上記で集約したブロックを計算
        Queue<ScenarioBlock> priority = new LinkedBlockingQueue<>(or.keySet());
        Map<ScenarioBlock, ScenarioBlock> orTimeAnalize = new HashMap<>();
        while (priority.peek() != null) {
            ScenarioBlock orBlock = priority.poll();
            List<ScenarioBlock> orList = or.get(orBlock);

            //演算の優先度を確認
            Optional<ScenarioBlock> check = orList.stream().filter(b -> priority.contains(b)).findFirst();
            if (check.isPresent()) {
                priority.offer(orBlock);
                continue;
            }

            ScenarioBlock result = orBlock;

            if (!orList.isEmpty()) {
                //計算の終わったシナリオブロックを更新
                List<ScenarioBlock> seqList = orList.parallelStream()
                        .map(o -> orTimeAnalize.get(o) == null
                        ? o
                        : orTimeAnalize.get(o))
                        .collect(Collectors.toList());

                //演算
                ScenarioBlock andResult = seqList.stream()
                        .reduce((a, b) -> calcBlock("AND", a, b))
                        .orElse(null);

                result = calcBlock("OR", result, andResult);
            }
            orTimeAnalize.put(orBlock, result);
        }

        list = parseAND(new ArrayList<>(), block);
        ScenarioBlock parseResult;
        if (list.size() > 1) {
            parseResult = list.stream()
                    .map(lb -> orTimeAnalize.get(lb) != null ? orTimeAnalize.get(lb) : lb)
                    .reduce((b1, b2) -> calcBlock("AND", b1, b2)).orElse(null);
        } else {
            parseResult = orTimeAnalize.get(list.get(0)) != null ? orTimeAnalize.get(list.get(0)) : list.get(0);
        }

        return parseResult;
    }

    private ScenarioBlock calcBlock(String op, ScenarioBlock b1, ScenarioBlock b2) {
        if (b1 == null || b2 == null) {
            return null;
        }

        //2ブロックの登録されているSIDを集約
        List<String> sids = new ArrayList<>();
        sids.addAll(b1.getBlockSeq().keySet());
        sids.addAll(b2.getBlockSeq().keySet());
        sids = sids.stream().distinct().collect(Collectors.toList());

        Map<String, TimeSeriesObject> result = sids.stream().collect(Collectors.toMap(sid -> sid, sid -> {
            TimeSeriesObject b1t = b1.getBlock1Seq(sid);
            TimeSeriesObject b2t = b2.getBlock1Seq(sid);

            //演算
            if (op.equals("AND")) {
                return b1t.and(b2t);
            } else {
                return b1t.or(b2t);
            }
        }));

        ScenarioBlock calcBlock = new ScenarioBlock(b1.item + op + b2.item, result);

        valid.setBlock(b1);
        valid.setBlock(b2);
        valid.setBlock(calcBlock);

        return calcBlock;
    }

    private ScenarioBlock reject0(ScenarioBlock p) {
        System.out.println("before:"+p.blockSeq.size());
        List<String> rejectIDs = p.blockSeq.entrySet().stream()
                                    .filter(pb -> !Arrays.stream(pb.getValue().arrSeries).filter(pbi -> pbi > 0).findFirst().isPresent())
                                    .map(pb -> pb.getKey())
                                    .collect(Collectors.toList());
        System.out.println(rejectIDs);
        rejectIDs.stream().forEach(p.blockSeq::remove);
        System.out.println("after:"+p.blockSeq.size());
        return p;
    }

    public void print() {
        System.out.println(block.item);
        pBlock.getBlockSeq().entrySet().stream()
                .map(tb -> tb.getKey() + ":" + Arrays.asList(tb.getValue().arrSeries))
                .forEach(System.out::println);
    }
}
