/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package time;

import analizer.MSyaryoAnalizer;
import java.util.List;
import java.util.stream.Collectors;

/**
 * サービス実績をSMR時系列データに変換するオブジェクト
 *
 * @author ZZ17807
 */
public class TimeSeriesObject {

    public String name;
    public List<Integer> series;

    public TimeSeriesObject(MSyaryoAnalizer s, List<String> datesq) {
        try {
            this.name = s.get().getName();
            this.series = toSeries(s, datesq);
        } catch (Exception e) {
            System.err.println(s.get().getName() + ":" + datesq);
            System.exit(0);
        }
    }

    //サービス実績の時系列を取得
    private List<Integer> toSeries(MSyaryoAnalizer s, List<String> svdates) {
        //重複除去 同じ日に何度も壊れることを想定しない
        List<String> sequence = svdates.stream().distinct().collect(Collectors.toList());

        //日付系列をSMR系列に変換する
        List<Integer> t = sequence.stream()
                .map(d -> s.getDateToSMR(d.split("#")[0]))
                .filter(smr -> smr != null)
                .collect(Collectors.toList());

        return t;
    }

    public Integer first() {
        return series.get(0);
    }
}
