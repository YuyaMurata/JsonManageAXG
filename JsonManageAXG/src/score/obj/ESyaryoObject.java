/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package score.obj;

import analizer.MSyaryoAnalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.math3.ml.clustering.Clusterable;

/**
 * 評価用車両オブジェクト
 *
 * @author ZZ17807
 */
public class ESyaryoObject implements Clusterable {

    //public MSyaryoAnalizer a;
    public String name;
    double[] points;
    public Integer cid = 0;
    public Integer score = 1;
    public Map<String, List<String>> sv;
    public Map<String, List<String>> data;
    public Map<String, Double> norm;
    public String date;
    public Integer age;
    public Integer smr;
    private Boolean errflg;

    public ESyaryoObject(MSyaryoAnalizer s) {
        this.name = s.get().getName();
        date = s.LEAST_DATE;
        smr = s.maxSMR;
        age = s.age(date);
    }

    public void setData(List<String> h, Map<String, List<String>> sv, Map<String, List<String>> data, Map<String, Double> norm) {
        this.sv = sv;
        this.data = data;
        this.norm = norm;
        
        //データ欠損確認フラグ
        this.errflg = !norm.values().stream().filter(v -> v > 0).findFirst().isPresent();
        if (errflg) {
            this.norm = h.stream().collect(Collectors.toMap(hi -> hi, hi -> 0d));
        }
        
        this.points = norm.values().stream().mapToDouble(v -> v).toArray();
    }
    
    public void setData(List<String> h){
        //データ欠損確認フラグ
        this.errflg = true;
        this.norm = h.stream().collect(Collectors.toMap(hi -> hi, hi -> 0d));
        this.points = norm.values().stream().mapToDouble(v -> v).toArray();
    }

    public Boolean none() {
        return errflg;
    }

    public void setID(Integer id) {
        this.cid = id;
    }

    public void setDateSMR(String d, Integer v) {
        if (!d.equals("-1")) {
            this.date = d;
        }
        this.smr = v;
    }

    public String check() {
        String p = Arrays.toString(getPoint()).replace("[", "").replace("]", "").replace(" ", "");
        String avg = String.valueOf(Arrays.stream(p.split(",")).mapToDouble(s -> Double.valueOf(s)).average().getAsDouble());
        
        return name + "," + date + "," + age +","+smr + "," + p + "," + avg + "," + cid + "," + score;
    }

    @Override
    public double[] getPoint() {
        return points;
    }

    //経年/SMR専用のメソッド　複数サービスに対応
    public List<double[]> getPoints() {
        List<double[]> pointList = data.values().stream()
                .map(v -> v.stream().mapToDouble(vi -> vi.length()==0?-1d:Double.valueOf(vi)).toArray())
                .collect(Collectors.toList());
        return pointList;
    }

    //経年/SMR専用のメソッド MTBFの計算
    public Double getMTBF(int xidx, int svidx) {
        Map<String, List<Double>> fail = new HashMap<>();
        data.entrySet().stream()
                .filter(d -> d.getValue().get(svidx).equals("1"))
                .forEach(d -> {
                    String k = d.getKey().split("#")[0];
                    if (fail.get(k) == null) {
                        fail.put(k, new ArrayList<>());
                        fail.get(k).add(0d);
                    }
                    fail.get(k).add(Double.valueOf(d.getValue().get(xidx)));
                });

        Map<String, List<Double>> diffMap = new HashMap<>();
        for (String key : fail.keySet()) {
            //差分系列
            List<Double> failseq = fail.get(key);
            if (!failseq.isEmpty()) {
                Collections.sort(failseq);
                System.out.println(key+failseq);
                List<Double> diffseq = new ArrayList<>();
                IntStream.range(0, failseq.size() - 1).boxed()
                        .map(i -> failseq.get(i+1) - failseq.get(i))
                        .forEach(diffseq::add);
                System.out.println(diffseq);
                diffMap.put(key, diffseq);
            }
        }
        
        OptionalDouble mtbf = diffMap.values().stream()
                .flatMap(v -> v.stream()).mapToDouble(vi -> vi)
                .average();
        
        return mtbf.isPresent()?mtbf.getAsDouble():null;
    }
}
