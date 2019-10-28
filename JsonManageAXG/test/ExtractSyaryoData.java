
import file.CSVFileReadWrite;
import file.ListToCSV;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
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
public class ExtractSyaryoData {

    private static MongoDBPOJOData shDB;

    public static void main(String[] args) {
        shDB = MongoDBPOJOData.create();
        shDB.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);

        String[] headers = new String[]{"作業"};
        List<String> out = extract(headers);
        //List<String> out = subextract(headers, "指定作業形態によるクレンジング結果.csv");
        
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS("test_work_all.csv")) {
            pw.println("SID,作番," + String.join(",", shDB.getHeader().getHeader(headers[0])));
            out.stream().forEach(pw::println);
        }
    }

    private static List<String> extract(String[] headers) {
        List<String> data = new ArrayList();
        MHeaderObject hobj = shDB.getHeader();
        String key = headers[0].split("\\.")[0];

        if (!headers[0].contains(".")) {
            shDB.getKeyList().stream()
                    .map(s -> shDB.getObj(s))
                    .filter(s -> s.getData(key) != null)
                    .forEach(s -> {
                        s.getData(key).entrySet().stream()
                                .map(d -> s.getName() + "," + d.getKey() + "," + String.join(",", d.getValue()))
                                .forEach(data::add);
                    });
        } else {
            shDB.getKeyList().stream()
                    .map(s -> shDB.getObj(s))
                    .filter(s -> s.getData(key) != null)
                    .forEach(s -> {
                        s.getData(key).values().stream()
                                .map(d -> s.getName() + "," + Arrays.stream(headers)
                                .map(h -> d.get(hobj.getHeaderIdx(key, h)))
                                .collect(Collectors.joining(","))
                                ).forEach(data::add);
                    });
        }

        return data;
    }
    
    private static List<String> subextract(String[] headers, String subfile) {
        //read
        List<String> sub = ListToCSV.toList(subfile);
        Map subMap = new HashMap();
        sub.stream().map(l -> l.split(",")[0]+","+l.split(",")[1]).forEach(str -> subMap.put(str, ""));
        
        List<String> data = new ArrayList();
        String key = headers[0].split("\\.")[0];

        shDB.getKeyList().stream()
                    .map(s -> shDB.getObj(s))
                    .filter(s -> s.getData(key) != null)
                    .forEach(s -> {
                        s.getData(key).entrySet().stream()
                                .filter(d -> subMap.get(s.getName()+","+d.getKey()) == null)
                                .map(d -> s.getName() + "," + d.getKey() + "," + String.join(",", d.getValue()))
                                .forEach(data::add);
                    });
        

        return data;
    }
}
