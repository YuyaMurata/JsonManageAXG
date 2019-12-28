
import exception.AISTProcessException;
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author ZZ17807
 */
public class CompareSMRTest {

    public static void main(String[] args) throws AISTProcessException {
        MongoDBPOJOData formDB = MongoDBPOJOData.create();
        formDB.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);

        //SMRの下がる車両を抽出
        formDB.getKeyList().stream().filter(sid -> sid.equals("PC200-8-N1-316686"))
                .map(s -> formDB.getObj(s))
                .filter(s -> s.getData("KOMTRAX_SMR") != null)
                .forEach(s -> {
            try {
                //detect(s.getName(), s.getData("KOMTRAX_SMR"));
                output(s.getName(), s.getData("KOMTRAX_SMR"));
            } catch (AISTProcessException ex) {
                Logger.getLogger(CompareSMRTest.class.getName()).log(Level.SEVERE, null, ex);
            }
                });
    }

    private static void detect(String n, Map<String, List<String>> smr) throws AISTProcessException {
        Integer temp = 0;
        List<String> dates = new ArrayList<>();
        for (String date : smr.keySet()) {
            String v = smr.get(date).get(1);
            if (temp > Integer.valueOf(v)) {
                dates.add(date);
            }
            temp = Integer.valueOf(v);
        }

        if (!dates.isEmpty()) {
            System.out.println(n+dates);
            try (PrintWriter pw = CSVFileReadWrite.writerSJIS("smrtest\\" + n + "_smr.csv")) {
                pw.println("date,value,check");
                smr.entrySet().stream()
                        .map(s -> format(s.getKey().split("#")[0]) + "," + s.getValue().get(1) + "," + (dates.contains(s.getKey()) ? "1" : ""))
                        .forEach(pw::println);
            }
        }
    }
    
    private static void output(String n, Map<String, List<String>> smr) throws AISTProcessException {

        try (PrintWriter pw = CSVFileReadWrite.writerSJIS(n + "_smr.csv")) {
            pw.println("date,value");
            smr.entrySet().stream()
                .map(s -> format(s.getKey().split("#")[0]) + "," + s.getValue().get(1))
                .forEach(pw::println);
        }
    }

    private static String format(String d) {
        return d.substring(0, 4) + "/" + d.substring(4, 6) + "/" + d.substring(6, 8);
    }
}
