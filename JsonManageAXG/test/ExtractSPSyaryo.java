
import analizer.MSyaryoAnalizer;
import exception.AISTProcessException;
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.stream.Collectors;
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
public class ExtractSPSyaryo {
    private static MongoDBPOJOData shDB;
    
    public static void main(String[] args) throws AISTProcessException {
        shDB = MongoDBPOJOData.create();
        shDB.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);
        
        MSyaryoAnalizer.initialize(shDB.getHeader(), shDB.getObjMap());
        
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS("syaryoanalizer_print.csv")) {
            pw.println(String.join(",", MSyaryoAnalizer.getHeader()));
            shDB.getKeyList().stream().map(sid -> new MSyaryoAnalizer(shDB.getObj(sid)))
                    .map(s -> s.toStringMap())
                    .map(m -> MSyaryoAnalizer.getHeader().stream().map(h -> m.get(h)).collect(Collectors.joining(",")))
                    .forEach(pw::println);
            
        }
    }
}
