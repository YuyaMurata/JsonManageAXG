/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.cluster;

import java.util.List;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

/**
 *
 * @author ZZ17807
 */
public class ClusteringDataVec {
    int C = 3;
    int N = 10000;
    
    public void cluster(List<DataVector> data){
        KMeansPlusPlusClusterer<DataVector> cluster = new KMeansPlusPlusClusterer(C, N);
        List<CentroidCluster<DataVector>> results = cluster.cluster(data);
        
        for (int i=0; i<results.size(); i++) {
            System.out.println("Cluster " + i);
                for (DataVector dv : results.get(i).getPoints())
                    System.out.println(dv.name);
            System.out.println();
        }
    }
}
