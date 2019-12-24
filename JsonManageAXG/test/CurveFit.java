
import score.analizer.MSyaryoAnalizer;
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
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

        MSyaryoAnalizer.initialize(db.getHeader(), db.getObjMap());
        MSyaryoAnalizer sa = new MSyaryoAnalizer(db.getObj("PC200-8- -305679")); //test:"PC200-8-N1-314804"
        System.out.println(sa.toString());

        final WeightedObservedPoints obs = new WeightedObservedPoints();

        String start = sa.get("KOMTRAX_SMR").keySet().stream().findFirst().get();
        String stop = sa.get("KOMTRAX_SMR").keySet().stream().sorted(Comparator.reverseOrder()).findFirst().get();

        sa.get("KOMTRAX_SMR").entrySet().stream().forEach(s -> {
            obs.add(time(start, s.getKey()), Double.valueOf(s.getValue().get(1)));
        });

        //SMR追加
        /*sa.get("SMR").entrySet().stream()
                .filter(s -> Integer.valueOf(s.getKey()) > Integer.valueOf(stop))
                .forEach(s -> obs.add(time(start, s.getKey()), Double.valueOf(s.getValue().get(1))));
*/
        final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(15);
        final double[] coeff = fitter.fit(obs.toList());

        //日付の始端と終端
        String sv = sa.get("SMR").keySet().stream().sorted(Comparator.comparing(d -> Integer.valueOf(d), Comparator.reverseOrder())).findFirst().get();
        String least = Integer.valueOf(stop) < Integer.valueOf(sv) ? sv : stop;

        /*try(PrintWriter pw = CSVFileReadWrite.writerSJIS("test_smr.csv")){
            pw.println("date,smr,fit");
            sa.get("KOMTRAX_SMR").entrySet().stream().map(s -> format(s.getKey())+","+s.getValue().get(1)+","+calc(time(start, s.getKey()), coeff)).forEach(pw::println);
            sa.get("SMR").entrySet().stream()
                     .filter(s -> Integer.valueOf(s.getKey()) > Integer.valueOf(stop))
                     .map(s -> format(s.getKey())+","+s.getValue().get(1)+","+calc(time(start, s.getKey()), coeff)).forEach(pw::println);
        }*/
        System.out.println("");

        try (PrintWriter pw = CSVFileReadWrite.writerSJIS("test_smr.csv")) {
            pw.println("date,smr,fit");

            IntStream.range(0, time(start, least).intValue()).forEach(i -> {
                List<String> s = new ArrayList();
                String d = addtime(start, i);
                s.add(format(d));
                
                List<String> v = null;
                if (sa.get("SMR").get(d) != null) {
                    v = sa.get("SMR").get(d);
                }
                if (sa.get("KOMTRAX_SMR").get(d) != null) {
                    v = sa.get("KOMTRAX_SMR").get(d);
                }
                
                if(v != null)
                    s.add(v.get(1));
                else
                    s.add("");
                
                s.add(calc(time(start, addtime(start, i)), coeff));

                pw.println(String.join(",", s));
            });
        }
    }

    private static String calc(Double x, double[] coeff) {
        Double c = IntStream.range(0, coeff.length).mapToDouble(i -> Math.pow(x, i) * coeff[i]).sum();

        return String.valueOf(c < 0d ? 0 : c.intValue());
    }

    private static String format(String d) {
        return d.substring(0, 4) + "/" + d.substring(4, 6) + "/" + d.substring(6, 8);
    }

    public static Double time(String start, String stop) {
        LocalDate st = LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate sp = LocalDate.parse(stop, DateTimeFormatter.ofPattern("yyyyMMdd"));

        Long age = ChronoUnit.DAYS.between(st, sp);

        return age.doubleValue();
    }

    public static String addtime(String start, Integer i) {
        LocalDate st = LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate sp = st.plusDays(i);
        
        return sp.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
