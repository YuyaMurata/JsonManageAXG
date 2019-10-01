
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
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
public class KomtraxIrregulerCheck {

    private static MongoDBPOJOData shDB;

    public static void main(String[] args) {
        shDB = MongoDBPOJOData.create();
        kmerrCheck();
    }

    private static void kmerrCheck() {
        shDB.set("json", "komatsuDB_PC200_Clean", MSyaryoObject.class);
        MHeaderObject header = shDB.getHeader();

        //時間が異なる車両を抽出
        String[] t = new String[]{"KOMTRAX_ERROR.DWH_INSERT_TIME", "KOMTRAX_ERROR.DWH_UPDATE_TIME", "KOMTRAX_ERROR.DWH_SCRAP_TIME", "KOMTRAX_ERROR.ERROR_TIME", "KOMTRAX_ERROR.S_TIME", "KOMTRAX_ERROR.T_TIME"};

        try (PrintWriter pw = CSVFileReadWrite.writerSJIS("kmerr_異常チェック.csv")) {
            pw.println("SID," + header.getHeader("KOMTRAX_ERROR").stream().map(h -> h.split("\\.")[1]).collect(Collectors.joining(",")));

            shDB.getKeyList().stream()
                    .map(s -> shDB.getObj(s))
                    .filter(s -> s.getData("KOMTRAX_ERROR") != null)
                    .peek(s -> System.out.println(s.getName()))
                    .forEach(s -> {
                        s.getData("KOMTRAX_ERROR").values().stream()
                                .filter(v -> Math.abs(Arrays.stream(t)
                                                    .filter(ti -> !v.get(header.getHeaderIdx("KOMTRAX_ERROR", ti)).equals(" "))
                                                    .mapToInt(ti -> Integer.valueOf(v.get(header.getHeaderIdx("KOMTRAX_ERROR", ti)).split(" ")[0].replace("/", "")))
                                                    .sum() 
                                                    - Integer.valueOf(v.get(0).split(" ")[0].replace("/", "")) * (int) Arrays.stream(t).filter(ti -> !v.get(header.getHeaderIdx("KOMTRAX_ERROR", ti)).equals(" ")).count()) > 7)
                                .map(v -> s.getName()+","+String.join(",", v))
                                .forEach(pw::println);
                    });
        }
    }
}
