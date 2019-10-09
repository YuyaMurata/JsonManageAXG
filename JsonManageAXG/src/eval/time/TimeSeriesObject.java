/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.time;

import eval.analizer.MSyaryoAnalizer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
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
        List<Integer> t = svdates.stream()
                            .map(d -> s.getDateToSMR(d.split("#")[0]))
                            .collect(Collectors.toList());
        
        return t;
    }
    
    public Integer first(){
        return series.get(0);
    }
}
