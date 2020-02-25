/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import analizer.MSyaryoAnalizer;
import exception.AISTProcessException;
import extract.CompressExtractionDefineFile;
import extract.SyaryoObjectExtract;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import time.TimeSeriesObject;

/**
 * シナリオブロック 時系列解析の最小単位
 *
 * @author kaeru
 */
public class ScenarioBlock {

    private static SyaryoObjectExtract exObj;
    private static Boolean enable = true;
    private static List<String> exception;
    public static Integer TERM = 10000;
    public static Integer DELTA = 1000;
    public Map<String, TimeSeriesObject> blockSeq;

    //車両抽出オブジェクトの取得
    public static void setSyaryoObjectExtract(SyaryoObjectExtract ex) {
        exception = new ArrayList<>();
        exObj = ex;

        //シナリオ設定が存在する時の処理
        if (exObj.getDefine("#SCENARIO_TERM") != null) {
            TERM = Integer.valueOf(exObj.getDefine("#SCENARIO_TERM").toList().get(0));
            DELTA = Integer.valueOf(exObj.getDefine("#SCENARIO_DELTA").toList().get(0));
        }
    }
    
    //計算用のシナリオブロック
    public ScenarioBlock(String item, Map<String, TimeSeriesObject> blockSeq){
        this.item = item;
        this.blockSeq = blockSeq;
    }
    
    public String item;
    public CompressExtractionDefineFile data;

    public ScenarioBlock(String item) throws AISTProcessException {
        System.out.println("生成ブロック:"+item);
        try {
            check(item);
            this.item = item;
            this.data = exObj.getDefine(item);
            //System.out.println("      "+this.data.toList());
            this.blockSeq = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //シナリオ解析が可能か確認
    private void check(String item) throws AISTProcessException {
        if (!enable || exObj == null) {
            throw new AISTProcessException("抽出処理適用後のオブジェクトセットされていません．");
        }
        if (exObj.getDefine(item) == null) {
            exception.add(item);
            throw new AISTProcessException("定義にない項目が選択されています：" + item);
        }
    }
    
    private void setBlockSeq(){
        Map<String, List<String>> aggregate = data.toList().stream().collect(Collectors.groupingBy(d -> d.split(",")[0]));
        this.blockSeq = aggregate.entrySet().parallelStream()
                .filter(e -> exObj.getAnalize(e.getKey()) != null)
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> {
                            MSyaryoAnalizer s = exObj.getAnalize(e.getKey()).toObj();
                            List<String> dateSeq = e.getValue().stream()
                                    .map(d -> d.split(",")[1])
                                    .map(d -> (d.split("\\.")[0].equals("受注") || d.split("\\.")[0].equals("部品") || d.split("\\.")[0].equals("作業"))
                                    ? s.getSBNToDate(d.split("\\.")[1], true)
                                    : d.split("\\.")[1])
                                    .filter(d -> d != null)
                                    .collect(Collectors.toList());
                            return new TimeSeriesObject(s, dateSeq, TERM, DELTA);
                        })
                );
    }
    
    public Map<String, TimeSeriesObject> getBlockSeq() {
        if (this.blockSeq == null) {
            setBlockSeq();
        }
        
        return this.blockSeq;
    }
    
    public TimeSeriesObject getBlock1Seq(String sid) {
        if (this.blockSeq == null) {
            setBlockSeq();
        }
        
        TimeSeriesObject tobj = this.blockSeq.get(sid);
        if(tobj == null){
            tobj = TimeSeriesObject.getZeroObject(item, TERM, DELTA);
        }
        
        return tobj;
    }

    //シナリオブロックの作成
    private ScenarioBlock and;

    public void setAND(ScenarioBlock block) {
        this.and = block;
    }

    public ScenarioBlock getAND() {
        return this.and;
    }

    private ScenarioBlock or;

    public void setOR(ScenarioBlock block) {
        this.or = block;
    }

    public ScenarioBlock getOR() {
        return this.or;
    }

    private ScenarioBlock next;

    public void setNEXT(ScenarioBlock block) {
        this.next = block;
    }

    public ScenarioBlock getNEXT() {
        return this.next;
    }

    public Integer getN() {
        return this.data.toList().size();
    }

    public List<String> getErrCheck() {
        return exception;
    }
}
