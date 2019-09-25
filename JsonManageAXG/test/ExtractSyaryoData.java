
import file.CSVFileReadWrite;
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
        
        String[] headers = new String[]{"分類.会社コード", "分類.車歴区分"};
        List<String> out = extract(headers);
        
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS("test.csv")) {
            pw.println("SID,"+String.join(",", headers));
            out.stream().forEach(pw::println);
        }
    }

    private static List<String> extract(String[] headers) {
        List<String> data = new ArrayList();
        MHeaderObject hobj = shDB.getHeader();
        String key = headers[0].split("\\.")[0];
        
        shDB.getKeyList().stream()
                .map(s -> shDB.getObj(s))
                .filter(s -> s.getData(key)!=null)
                .forEach(s -> {            
                    s.getData(key).values().stream()
                            .map(d -> s.getName()+","+Arrays.stream(headers)
                                    .map(h -> d.get(hobj.getHeaderIdx(key, h)))
                                    .collect(Collectors.joining(","))
                            ).forEach(data::add);
                });
        
        return data;
    }
}
