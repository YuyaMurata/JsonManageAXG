/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.cluster.distance;

import java.util.stream.IntStream;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

/**
 *
 * @author ZZ17807
 */
public class MaharanobisDistance implements DistanceMeasure{

    @Override
    public double compute(double[] doubles, double[] doubles1) throws DimensionMismatchException {
        Double d = IntStream.range(0, doubles.length)
                            .mapToDouble(i -> Math.pow(doubles[i] - doubles1[i], 2)).sum();
        return Math.sqrt(d);
    }
    
}
