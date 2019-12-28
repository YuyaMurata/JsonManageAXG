/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scenario;

import analizer.MSyaryoAnalizer;
import exception.AISTProcessException;
import extract.SyaryoObjectExtract;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * シナリオブロック 時系列解析の最小単位
 *
 * @author kaeru
 */
public class ScenarioBlock {

    private static SyaryoObjectExtract extract;
    private static Map<String, MSyaryoAnalizer> analize;
    private static Boolean enable = true;

    public static void setSyaryoObjectExtract(SyaryoObjectExtract ex) throws AISTProcessException {
        try {
            extract = ex;
            analize = extract.getObjMap().entrySet().stream()
                .collect(Collectors.toMap(
                    e -> e.getKey(),
                    e -> e.getValue()));
        } catch (Exception e) {
            enable = false;
            throw new AISTProcessException("抽出処理に問題があります．");
        }
    }

    public String item;
    public List<String> data;

    public ScenarioBlock(String item) throws AISTProcessException {
        check(item);

        this.item = item;
        this.data = extract.getDefine().get(item);
    }

    private void check(String item) throws AISTProcessException {
        if (!enable) {
            throw new AISTProcessException("抽出処理適用後のオブジェクトセットされていません．");
        }
        if (extract.getDefine().get(item) == null) {
            throw new AISTProcessException("定義にない項目が選択されています：" + item);
        }
    }

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
        return this.data.size();
    }
}
