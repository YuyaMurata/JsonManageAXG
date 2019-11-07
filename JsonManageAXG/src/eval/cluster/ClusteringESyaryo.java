/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.cluster;

import eval.obj.ESyaryoObject;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

/**
 *
 * @author ZZ17807
 */
public class ClusteringESyaryo {
    static int C = 10;
    static int N = 10000;
    static KMeansPlusPlusClusterer<ESyaryoObject> cluster = new KMeansPlusPlusClusterer(C, N);
    //static DBSCANClusterer<ESyaryoObject> cluster = new DBSCANClusterer(0.1, 1, new EuclideanDistance());
    
    public static void cluster(Collection<ESyaryoObject> data){    
        long start = System.currentTimeMillis();
        
        List<CentroidCluster<ESyaryoObject>> results = cluster.cluster(data);
        //List<Cluster<ESyaryoObject>> results = cluster.cluster(data);
        
        for (int i=0; i<results.size(); i++) {
            for (ESyaryoObject s : results.get(i).getPoints())
                    s.setID(i);
        }
        
        long stop = System.currentTimeMillis();
        System.out.println("clustering time = "+(stop-start)+"ms");
    }
    
    static KMeansPlusPlusClusterer<DataVector> spcluster = new KMeansPlusPlusClusterer(3, 100);
    public static List<CentroidCluster<DataVector>> splitor(Collection<DataVector> data){    
        long start = System.currentTimeMillis();
        
        List<CentroidCluster<DataVector>> results = spcluster.cluster(data);
        
        long stop = System.currentTimeMillis();
        System.out.println("3 spliting time = "+(stop-start)+"ms");
        
        return results;
    }
}
