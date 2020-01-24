/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package score.cluster;

import java.util.Arrays;
import org.apache.commons.math3.ml.clustering.Clusterable;

/**
 * wrapper class for clustering
 * @author ZZ17807
 */
public class DataVector implements Clusterable{
    public Integer cid;
    double[] points;
    public double p;
    
    public DataVector(Integer cid, double p){
        this.cid = cid;
        this.p = p;
        this.points = new double[]{p};
    }
    
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(cid);
        sb.append(":");
        sb.append(p);
        sb.append("\n");
        return sb.toString();
    }
    
    @Override
    public double[] getPoint() {
        return points;
    }
}