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
    String name;
    double[] points;
    
    public DataVector(String n, double[] p){
        this.name = n;
        this.points = p;
    }
    
    @Override
    public double[] getPoint() {
        return points;
    }
}
