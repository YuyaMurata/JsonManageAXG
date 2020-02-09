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
import score.time.TimeSeriesObject;

/**
 * シナリオブロック 時系列解析の最小単位
 *
 * @author kaeru
 */
public class ScenarioBlock {

    private static SyaryoObjectExtract exObj;
    //private static Map<String, MSyaryoAnalizer> analize;
    private static Boolean enable = true;
    private static List<String> exception;

    //車両抽出オブジェクトの取得
    public static void setSyaryoObjectExtract(SyaryoObjectExtract ex) {

        exception = new ArrayList<>();
        exObj = ex;
        /*analize = extract.getObjMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey(),
                            e -> e.getValue()));*/

    }

    public String item;
    public CompressExtractionDefineFile data;
    public ScenarioBlock(String item) throws AISTProcessException {
        try {
            check(item);
            this.item = item;
            this.data = exObj.getDefine(item);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //シナリオ解析が可能か確認
    private void check(String item) throws AISTProcessException {
        if (!enable) {
            throw new AISTProcessException("抽出処理適用後のオブジェクトセットされていません．");
        }
        if (exObj.getDefine(item) == null) {
            exception.add(item);
            throw new AISTProcessException("定義にない項目が選択されています：" + item);
        }
    }

    public Map<String, TimeSeriesObject> getBlockTimeSequence() {
        Map<String, List<String>> aggregate = data.toList().stream().collect(Collectors.groupingBy(d -> d.split(",")[0]));
        Map<String, TimeSeriesObject> times = aggregate.entrySet().stream()
                .filter(e -> exObj.getAnalize(e.getKey()) != null)
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> {
                            MSyaryoAnalizer s = exObj.getAnalize(e.getKey()).toObj();
                            List<String> dateSeq = e.getValue().stream()
                                    .map(d -> d.split(",")[1])
                                    .map(d -> s.getSBNToDate(d.split("\\.")[1], true) != null ? s.getSBNToDate(d.split("\\.")[1], true) : d.split("\\.")[1])
                                    .filter(d -> d != null)
                                    .collect(Collectors.toList());
                            return new TimeSeriesObject(s, dateSeq);
                        })
                );
        return times;
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
        System.out.println(block.item);
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
