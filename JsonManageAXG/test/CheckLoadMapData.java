
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
public class CheckLoadMapData {
    private static MongoDBPOJOData shDB;

    public static void main(String[] args) {
        shDB = MongoDBPOJOData.create();
        shDB.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);
        MHeaderObject h = shDB.getHeader();
        
        export(h);
        //check(h);
        
    }
    
    private static void export(MHeaderObject h){
        String smrKey = "LOADMAP_DATE_SMR";
        
        //2dim
        //String key = "LOADMAP_ポンプ圧(MAX)";
        //String key = "LOADMAP_実エンジン回転VSエンジントルク";
        String key = "LOADMAP_エンジン水温VS作動油温";
        /*shDB.getKeyList().stream()
                    .map(s -> shDB.getObj(s))
                    .filter(s -> s.getData(key) != null)
                    .peek(s -> System.out.println(s.getName()))
                    .forEach(s ->{
                        try(PrintWriter pw = CSVFileReadWrite.writerSJIS("file\\"+s.getName()+"_"+key+".csv")){
                            pw.println(s.getName()+",SMR,"+smr(s.getDataOne(smrKey).get(0)));
                            pw.println(","+h.getHeader(key).stream().map(head -> head.split("\\.")[1]).collect(Collectors.joining(",")));
                            s.getData(key).entrySet().stream().map(d -> d.getKey()+","+d.getValue().stream().map(v -> smr(v).toString()).collect(Collectors.joining(","))).forEach(pw::println);
                        }
                    });
        */
        //水温
        /*shDB.getKeyList().stream()
                    .map(s -> shDB.getObj(s))
                    .filter(s -> s.getData(key) != null)
                    .peek(s -> System.out.println(s.getName()))
                    .forEach(s ->{
                        try(PrintWriter pw = CSVFileReadWrite.writerSJIS("file\\"+s.getName()+"_LOADMAP_エンジン水温.csv")){
                            pw.println(s.getName()+",SMR,"+smr(s.getDataOne(smrKey).get(0)));
                            pw.println(","+h.getHeader(key).stream().map(head -> head.split("\\.")[1]).collect(Collectors.joining(",")));
                            String str = IntStream.range(0, shDB.getObj("PC200-10- -453947").getData(key).keySet().size())
                                                .boxed().map(i -> s.getData(key).entrySet().stream()
                                                                    .sorted(Comparator.comparing(d -> Integer.valueOf(d.getKey().replace("_",""))))
                                                                    .mapToInt(d -> smr(d.getValue().get(i))).sum()).map(sum -> sum.toString()).collect(Collectors.joining(","));
                             pw.println(","+str);
                        }
                    });
        */
        //油温
        shDB.getKeyList().stream()
                    .map(s -> shDB.getObj(s))
                    .filter(s -> s.getData(key) != null)
                    .peek(s -> System.out.println(s.getName()))
                    .forEach(s ->{
                        try(PrintWriter pw = CSVFileReadWrite.writerSJIS("file\\"+s.getName()+"_LOADMAP_作動油温.csv")){
                            pw.println(s.getName()+",SMR,"+smr(s.getDataOne(smrKey).get(0)));
                            pw.println(","+shDB.getObj("PC200-10- -453947").getData(key).keySet().stream().sorted(Comparator.comparing(d -> Integer.valueOf(d.replace("_","")))).collect(Collectors.joining(",")));
                            String str = s.getData(key).entrySet().stream()
                                            .sorted(Comparator.comparing(d -> Integer.valueOf(d.getKey().replace("_",""))))
                                            .map(d -> d.getValue().stream().mapToInt(v -> smr(v)).sum()).map(sum -> sum.toString()).collect(Collectors.joining(","));
                             pw.println(","+str);
                        }
                    });
    }
    
    private static void check(MHeaderObject h){
        String key = "LOADMAP_実エンジン回転VSエンジントルク";
        String smrKey = "LOADMAP_DATE_SMR";
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS("loadmap_engine.csv")){
            //header
            pw.println("sid,smr,"+h.getHeader(key).stream().map(s -> s.split("\\.")[1]).collect(Collectors.joining(","))+","+String.join(",", shDB.getObj("PC200-10- -453947").getData(key).keySet()));
            shDB.getKeyList().stream()
                    .map(s -> shDB.getObj(s))
                    .filter(s -> s.getData(key) != null)
                    .peek(s -> System.out.println(s.getName()))
                    .map(s -> s.getName()+","+(smr(s.getDataOne(smrKey).get(0)))+","+
                                IntStream.range(0, s.getData(key).get("_1100").size())
                                        .boxed().map(i -> s.getData(key).values()
                                                .stream()
                                                .mapToInt(l -> Integer.valueOf(l.get(i))).sum())
                                        .map(sum -> rate(s.getDataOne(smrKey).get(0), sum.toString()).toString()).collect(Collectors.joining(","))
                            +","+
                                s.getData(key).values().stream()
                                        .map(l -> l.stream()
                                                .mapToInt(li -> Integer.valueOf(li)).sum())
                                        .map(sum -> rate(s.getDataOne(smrKey).get(0),sum.toString()).toString())
                                        .collect(Collectors.joining(",")))
                    .forEach(pw::println);
        }
    }
    
    private static Integer smr(String cnt) {
        return Integer.valueOf(cnt)/18000;
    }
    
    private static Double rate(String cntall, String cnt) {
        return Double.valueOf(cnt)/Double.valueOf(cntall);
    }
}
