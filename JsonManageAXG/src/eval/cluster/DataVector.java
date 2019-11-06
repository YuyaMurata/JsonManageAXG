/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.cluster;

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
    
    @Override
    public double[] getPoint() {
        return points;
    }
}