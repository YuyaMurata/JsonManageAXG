
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.poi.ss.formula.functions.MatrixFunction;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author ZZ17807
 */
public class MahalanobisTest {

    public MahalanobisTest(Collection<double[]> data) {
        //変換
        double[][] d = new double[data.size()][data.iterator().next().length];
        data.toArray(d);

        //Matrix
        RealMatrix mat = MatrixUtils.createRealMatrix(d);

        //1
        double[] one = new double[d.length];
        Arrays.fill(one, 1);
        RealVector onemat = MatrixUtils.createRealVector(one);
        RealVector meanv = mat.transpose().operate(onemat).mapDivide(one.length);
        System.out.println(meanv);

        //分散共分散行列
        Covariance cov = new Covariance(mat);
        System.out.println(cov.getN());
        
        //マハラノビス距離
        for(int i=0; i< d.length; i++){
            RealVector difv = mat.getColumnVector(i).subtract(meanv);
        }
    }

    public static void main(String[] args) {
        List<double[]> mat = new ArrayList() {
            {
                add(new double[]{0, 1, 2, 3, 4, 5});
                add(new double[]{6, 1, 12, 3, 6, 7});
                add(new double[]{7, 11, 32, 7, 6, 9});
                add(new double[]{2, 6, 5, 3, 3, 5});
                add(new double[]{3, 0, 0, 2, 5, 0});
                add(new double[]{3, 2, 1, 2, 1, 2});
                add(new double[]{1, 1, 3, 2, 1, 2});
                add(new double[]{0, 4, 4, 1, 1, 1});
                add(new double[]{0, 1, 1, 2, 1, 1});
                add(new double[]{1, 2, 1, 5, 3, 2});
            }
        };

        new MahalanobisTest(mat);
    }
}
