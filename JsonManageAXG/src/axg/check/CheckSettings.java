/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.check;

import exception.AISTProcessException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import obj.MHeaderObject;

/**
 *
 * @author kaeru
 */
public class CheckSettings {

    //標準的な設定確認
    public static void check(MHeaderObject h, String setting, Map<String, Map<String, List<String>>> map) throws AISTProcessException {
        //ファイル構成の確認
        try {
            map.values().stream().map(m -> (Map<String, List<String>>) m).mapToInt(m -> m.values().size()).sum();
        } catch (ClassCastException e) {
            throw new AISTProcessException(setting + "設定ファイルの構成に誤りがあります．");
        }

        List<String> exists = new ArrayList<>();
        //データ項目が正しいか確認
        if (!setting.equals("シャッフル")) {
            exists = map.keySet().stream()
                    .filter(d -> h.getHeader(d) == null)
                    .collect(Collectors.toList());
            if (!exists.isEmpty()) {
                System.err.println(setting + "設定ファイルの誤り");
                System.err.println("設定ファイルの下記項目が存在しません．\n" + exists);
                throw new AISTProcessException(setting + "設定ファイルの内容に誤りがあります．");
            }
        }

        //データ列が正しいか確認
        if (setting.equals("クレンジング")) {
            exists = map.values().stream()
                    .flatMap(d -> d.keySet().stream())
                    .filter(d -> d.contains("."))
                    .filter(d -> !h.getHeader().contains(d))
                    .distinct().collect(Collectors.toList());
        } else {
            exists = map.values().stream()
                    .flatMap(d -> d.values().stream().flatMap(di -> di.stream()))
                    .filter(d -> d.contains("."))
                    .filter(d -> !h.getHeader().contains(d))
                    .distinct().collect(Collectors.toList());
        }

        if (!exists.isEmpty()) {
            System.err.println(setting + "設定ファイルの誤り");
            System.err.println("設定ファイルの項目に下記項目が存在しません．\n  " + exists);
            throw new AISTProcessException(setting + "設定ファイルの項目に誤りがあります．");
        }
    }

    //シャッフルとレイアウトの対応確認
    public static void check(Map<String, Map<String, List<String>>> shuffle, Map<String, Map<String, List<String>>> layout) throws AISTProcessException {
        //データ項目が正しいか確認
        Set<String> dkeys = new HashSet<>(shuffle.keySet());
        layout.keySet().stream().forEach(dkeys::add);
        List<String> exists = dkeys.stream()
                .filter(d -> layout.get(d) == null || shuffle.get(d) == null)
                .collect(Collectors.toList());
        if (!exists.isEmpty()) {
            System.err.println("レイアウト設定ファイルの誤り");
            System.err.println("下記項目がレイアウトに存在しません．\n   " + exists);
            throw new AISTProcessException("レイアウト設定ファイルに誤りがあります．");
        }

        //レコード列の確認
        exists = shuffle.keySet().stream()
                .filter(s -> shuffle.get(s).values().stream().findFirst().get().size() != layout.get(s).values().stream().findFirst().get().size())
                .collect(Collectors.toList());
        if (!exists.isEmpty()) {
            System.err.println("レイアウト設定ファイルの誤り");
            System.err.println("下記項目のレコード長が異なります．\n   " + exists);
            throw new AISTProcessException("レイアウト設定ファイルに誤りがあります");
        }
    }
}
