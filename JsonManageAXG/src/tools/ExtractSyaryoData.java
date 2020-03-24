/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import exception.AISTProcessException;
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class ExtractSyaryoData {

    public static MongoDBPOJOData db;

    public static void main(String[] args) {
        db = MongoDBPOJOData.create();
        db.set("json", "KM_PC200_DB_P_Form", MSyaryoObject.class);

        String dataKey = "KR車両";
        String[] header = {};

        extract(dataKey, header);
    }

    private static void extract(String k, String[] h) {
        //show Header
        MHeaderObject hobj = db.getHeader();
        System.out.println(k + ":" + hobj.getHeader(k));

        try (PrintWriter pw = CSVFileReadWrite.writerSJIS(k + ".csv")) {
            if (h.length < 1) {
                db.getKeyList().parallelStream()
                        .map(sid -> (MSyaryoObject)db.getObj(sid))
                        .filter(s -> s.getData(k) != null)
                        .flatMap(s -> 
                            s.getData(k).entrySet().stream().map(d -> s.getName()+","+d.getKey()+","+String.join(",", d.getValue()))
                        )
                        .forEach(pw::println);
            }else{
                db.getKeyList().stream()
                        .map(sid -> (MSyaryoObject)db.getObj(sid))
                        .filter(s -> s.getData(k) != null)
                        .flatMap(s -> 
                            s.getData(k).entrySet().stream().map(d -> s.getName()+","+d.getKey()+","+
                                    Arrays.stream(h).map(hi -> hobj.getHeaderIdx(k, hi)).map(idx -> d.getValue().get(idx)).collect(Collectors.joining(",")))
                        )
                        .forEach(pw::println);
            }
        } catch (AISTProcessException ex) {
            ex.printStackTrace();
        }
    }
}
