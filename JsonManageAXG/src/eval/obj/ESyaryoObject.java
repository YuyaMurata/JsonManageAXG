/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.obj;

import eval.analizer.MSyaryoAnalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import obj.MSyaryoObject;
import org.apache.commons.math3.ml.clustering.Clusterable;

/**
 * 評価用車両オブジェクト
 * @author ZZ17807
 */
public class ESyaryoObject implements Clusterable{
    public MSyaryoAnalizer a;
    
    double[] points;
    public Integer cid = -1;
    public Integer score = 0;
    public Map<String, List<String>> sv;
    public Map<String, List<String>> data;
    public Map<String, Double> norm;
    public String date;
    public Integer smr;
    private Boolean errflg;
    
    
    public ESyaryoObject(MSyaryoObject syaryo) {
        this.a = new MSyaryoAnalizer(syaryo);
        date = a.LEAST_DATE;
        smr = a.maxSMR;
    }
    
    public void setData(Map<String, List<String>> sv, Map<String, List<String>> data, Map<String, Double> norm){
        this.sv = sv;
        this.data = data;
        this.norm = norm;
        this.points = norm.values().stream().mapToDouble(v -> v).toArray();
        
        //データ欠損確認フラグ
        this.errflg = !norm.values().stream().filter(v -> v > 0).findFirst().isPresent();
        if(errflg)
            this.cid = 0;
    }
    
    public Boolean none(){
        return errflg;
    }
    
    public void setID(Integer id){
        this.cid = id;
    }
    
    public void setDateSMR(String d, Integer v){
        if(!d.equals("-1"))
            this.date = d;
        this.smr = v;
    }
    
    public String check(){
        String p = Arrays.toString(getPoint()).replace("[", "").replace("]", "").replace(" ", "");
        String avg = String.valueOf(Arrays.stream(p.split(",")).mapToDouble(s -> Double.valueOf(s)).average().getAsDouble());
        
        return a.syaryo.getName()+","+date+","+a.age(date)+","+smr+","+p+","+avg+","+cid+","+score;
    }

    @Override
    public double[] getPoint() {
        return points;
    }
}
