
import eval.analizer.MSyaryoAnalizer;
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.IntStream;
import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ZZ17807
 */
public class CurveFit {
    public static void main(String[] args) {
        MongoDBPOJOData db = MongoDBPOJOData.create();
        db.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);
        
        MSyaryoAnalizer.initialize(db);
        MSyaryoAnalizer sa = new MSyaryoAnalizer("PC200-8- -305679"); //test:"PC200-8-N1-314804"
        System.out.println(sa.toString());
        
        final WeightedObservedPoints obs = new WeightedObservedPoints();
        
        String start = sa.get("KOMTRAX_SMR").keySet().stream().findFirst().get();
        String stop = sa.get("KOMTRAX_SMR").keySet().stream().sorted(Comparator.reverseOrder()).findFirst().get();
        
        sa.get("KOMTRAX_SMR").entrySet().stream().forEach(s ->{
            obs.add(time(start, s.getKey()), Double.valueOf(s.getValue().get(1)));
        });
        
        final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(4);
        final double[] coeff = fitter.fit(obs.toList());
        
        
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS("test_smr.csv")){
            pw.println("date,smr,fit");
            sa.get("KOMTRAX_SMR").entrySet().stream().map(s -> format(s.getKey())+","+s.getValue().get(1)+","+calc(time(start, s.getKey()), coeff)).forEach(pw::println);
             sa.get("SMR").entrySet().stream()
                     .filter(s -> Integer.valueOf(s.getKey()) > Integer.valueOf(stop))
                     .map(s -> format(s.getKey())+","+s.getValue().get(1)+","+calc(time(start, s.getKey()), coeff)).forEach(pw::println);
        }
        
    }
    
    private static String calc(Double x, double[] coeff){
        //Double x = Double.valueOf(d);
        Double c = IntStream.range(0, coeff.length).mapToDouble(i -> Math.pow(x, i)*coeff[i]).sum();
        
        return String.valueOf(c < 0d?0:c.intValue());
    }
    
    private static String format(String d){
        return d.substring(0,4)+"/"+ d.substring(4, 6)+"/"+ d.substring(6, 8);
    }
    
    public static Double time(String start, String stop) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        try {
            Date st = sdf.parse(start);
            Date sp = sdf.parse(stop);
            Long age = (sp.getTime() - st.getTime()) / (1000 * 60 * 60 * 24);

            return age.doubleValue();
        } catch (ParseException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
