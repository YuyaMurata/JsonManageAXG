/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package score.time;

import analizer.MSyaryoAnalizer;
import java.util.List;
import java.util.stream.Collectors;

/**
 * サービス実績をSMR時系列データに変換するオブジェクト
 * @author ZZ17807
 */
public class TimeSeriesObject {
    private MSyaryoAnalizer s;
    public List<Integer> series;

    public TimeSeriesObject(MSyaryoAnalizer syaryo, List<String> datesq) {
        this.s = syaryo;
        this.series = toSeries(datesq);
    }

    //サービス実績の時系列を取得
    private List<Integer> toSeries(List<String> svdates) {
        //重複除去 同じ日に何度も壊れることを想定しない
        List<String> sequence = svdates.stream().distinct().collect(Collectors.toList());
        
        //日付系列をSMR系列に変換する
        List<Integer> t = sequence.stream()
                            .map(d -> s.getDateToSMR(d.split("#")[0]))
                            .collect(Collectors.toList());
        
        return t;
    }
    
    public Integer first(){
        return series.get(0);
    }
}
