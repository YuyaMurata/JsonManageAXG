package tools;

import java.io.PrintWriter;

import analizer.MSyaryoAnalizer;
import exception.AISTProcessException;
import extract.SyaryoObjectExtract;
import file.CSVFileReadWrite;
import obj.MHeaderObject;

public class ErrorCodeExproler {
    public static void main(String[] args) throws AISTProcessException {
        SyaryoObjectExtract objex = new SyaryoObjectExtract("json", "KM_PC200_DB_P");
        objex.setUserDefine("JsonManageAXG\\project\\KM_PC200_DB_P\\config\\user_define_車両除外無し.json");
        System.out.println(objex.getSummary());
        MHeaderObject h = objex.getHeader();
        
        String sid = objex.keySet().get(0);
        MSyaryoAnalizer a = objex.getAnalize("PC200-10--450670");
        System.out.println(objex.getDefine("C1-1:エンジンOH").toList().get(0));
        

    }

    private static void sbnPrint(MSyaryoAnalizer a, MHeaderObject h) throws AISTProcessException{
        try(PrintWriter csv = CSVFileReadWrite.writerSJIS("test.csv")){
            csv.println("Key,"+String.join(",", h.getHeader("受注")));
            a.get("受注").entrySet().stream()
                .map(odr -> odr.getKey()+","+String.join(",", odr.getValue()))
                .forEach(csv::println);

            csv.println("Key,"+String.join(",", h.getHeader("作業")));
            a.getSBNWork("D015Z0038").entrySet().stream()
                .map(odr -> odr.getKey()+","+String.join(",", odr.getValue()))
                .forEach(csv::println);

            csv.println("Key,"+String.join(",", h.getHeader("部品")));
            a.getSBNParts("D015Z0038").entrySet().stream()
                .map(odr -> odr.getKey()+","+String.join(",", odr.getValue()))
                .forEach(csv::println);
        }
    }
}
