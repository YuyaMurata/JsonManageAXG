/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package time;

import analizer.MSyaryoAnalizer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * サービス実績をSMR時系列データに変換するオブジェクト
 *
 * @author ZZ17807
 */
public class TimeSeriesObject {
    public String name;
    public List<Integer> series;  //SMR系列
    public Integer[] arrSeries;  //SMRをインターバルで区切った系列
    
    public TimeSeriesObject(String name, Integer[] arrSeries){
        this.name = name;
        this.arrSeries = arrSeries;
    }
    
    public TimeSeriesObject(MSyaryoAnalizer s, List<String> datesq) {
        try {
            this.name = s.get().getName();
            this.series = toSeries(s, datesq);
        } catch (Exception e) {
            System.err.println(s.get().getName() + ":" + datesq);
            System.exit(0);
        }
    }
    
    public TimeSeriesObject(MSyaryoAnalizer s, List<String> datesq, Integer term, Integer delta) {
        try {
            this.name = s.get().getName();
            this.series = toSeries(s, datesq);
            if(term != null)
                this.arrSeries = toArrSeries(term, delta);
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
    
    //SMRからDELTA時間に区切った系列を作成  e.g. term=10000, delta=1000  1万時間を千時間に区切った系列となる
    private Integer[] toArrSeries(Integer term, Integer delta) {
        int n = term / delta;
        Integer[] seq = new Integer[n];
        Arrays.fill(seq, 0);

        series.stream()
                .filter(ti -> ti < term && ti > -1)
                .map(ti -> ti / delta)
                .forEach(tidx -> seq[tidx] += 1);
        
        return seq;
    }
    
    //ゼロ系列を作成
    public static TimeSeriesObject getZeroObject(String name, Integer term, Integer delta){
        int n = term / delta;
        Integer[] seq = new Integer[n];
        Arrays.fill(seq, 0);
        return new TimeSeriesObject(name, seq);
    }
    
    //適合しなかった系列の削除
    public void delete(List<Integer> fit){
        Integer[] arr = IntStream.range(0, arrSeries.length).boxed()
                                .map(i -> fit.contains(i)?arrSeries[i]:0)
                                .toArray(Integer[]::new);
        arrSeries = arr;
    }
    
    public TimeSeriesObject and(TimeSeriesObject t){
        Integer[] tarr =  IntStream.range(0, this.arrSeries.length).boxed()
                        .map(i -> Math.min(this.arrSeries[i], t.arrSeries[i]))
                        .toArray(Integer[]::new);
        return new TimeSeriesObject(this.name+"&"+t.name, tarr);
    }
    
    public TimeSeriesObject or(TimeSeriesObject t){
        Integer[] tarr =  IntStream.range(0, this.arrSeries.length).boxed()
                        .map(i -> Math.max(this.arrSeries[i], t.arrSeries[i]))
                        .toArray(Integer[]::new);
        return new TimeSeriesObject(this.name+"|"+t.name, tarr);
        
    }

    public Integer first() {
        return series.get(0);
    }
}
