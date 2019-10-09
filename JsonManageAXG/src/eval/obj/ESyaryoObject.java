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
    private Integer cid;
    private Map<String, List<String>> sv;
    private Map<String, List<String>> data;
    private Map<String, Double> norm;
    
    
    public ESyaryoObject(MSyaryoObject syaryo) {
        this.a = new MSyaryoAnalizer(syaryo);
    }
    
    public void setData(Map<String, List<String>> sv, Map<String, List<String>> data, Map<String, Double> norm){
        this.sv = sv;
        this.data = data;
        this.norm = norm;
        this.points = norm.values().stream().mapToDouble(v -> v).toArray();
    }
    
    public void setID(Integer id){
        this.cid = id;
    }
    
    public String check(){
        return a.syaryo.getName()+":"+a.maxSMR+"/"+a.lifestop+":"+data.toString()+"->"+Arrays.toString(getPoint());
    }

    @Override
    public double[] getPoint() {
        return points;
    }
}
