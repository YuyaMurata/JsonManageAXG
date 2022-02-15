package tools;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
        // String price = a.get("受注").get("A013Y0445").get(h.getHeaderIdx("受注",
        // "請求金額"));
        // System.out.println(price);
        //System.out.println(h.getHeader("受注"));
        // dataCheck(objex, h);
        // refailure(objex, h);
        lcc(objex, h);

    }

    private static void sbnPrint(MSyaryoAnalizer a, MHeaderObject h) throws AISTProcessException {
        try (PrintWriter csv = CSVFileReadWrite.writerSJIS("test.csv")) {
            csv.println("Key," + String.join(",", h.getHeader("受注")));
            a.get("受注").entrySet().stream()
                    .map(odr -> odr.getKey() + "," + String.join(",", odr.getValue()))
                    .forEach(csv::println);

            csv.println("Key," + String.join(",", h.getHeader("作業")));
            a.getSBNWork("D015Z0038").entrySet().stream()
                    .map(odr -> odr.getKey() + "," + String.join(",", odr.getValue()))
                    .forEach(csv::println);

            csv.println("Key," + String.join(",", h.getHeader("部品")));
            a.getSBNParts("D015Z0038").entrySet().stream()
                    .map(odr -> odr.getKey() + "," + String.join(",", odr.getValue()))
                    .forEach(csv::println);
        }
    }

    private static void dataCheck(SyaryoObjectExtract objex, MHeaderObject h) {
        // String sid = "PC200-8-N1-357147";
        objex.getDefine("C1-1:エンジンOH").toList().stream()
                .map(d -> {
                    String[] d_arr = d.split(",");
                    MSyaryoAnalizer a = objex.getAnalize(d_arr[0]);

                    String sbn = d_arr[1].split("\\.")[1].split("#")[0];
                    return d + "," + a.get("受注").get(sbn).get(h.getHeaderIdx("受注", "請求金額"));
                })
                .forEach(System.out::println);
    }

    private static void refailure(SyaryoObjectExtract objex, MHeaderObject h) throws AISTProcessException {
        // category - id - [(SMR,品番), ()]
        Map<String, Map<String, TreeMap<Integer, String[]>>> refailure = new HashMap<>();

        objex.getDefineItem().stream()
                .filter(cat -> cat.charAt(0) == 'C')
                .forEach(cat -> {
                    Map<String, TreeMap<Integer, String[]>> catMap = new HashMap<>();
                    System.out.println(cat);
                    objex.getDefine(cat).toList().stream().forEach(data -> {
                        String[] d_arr = data.split(",");
                        if (catMap.get(d_arr[0]) == null) {
                            catMap.put(d_arr[0], new TreeMap());
                        }

                        MSyaryoAnalizer a = objex.getAnalize(d_arr[0]);
                        // System.out.println(data);
                        String sbn = d_arr[1].split("\\.")[1].split("#")[0];
                        String date = sbn;
                        Integer smr = -1;
                        String price = "-1";
                        if (a != null) {
                            date = a.getSBNToDate(sbn, true);
                            smr = ((Integer) (a.getDateToSMR(date) / 100)) * 100;
                            price = a.get("受注").get(sbn).get(h.getHeaderIdx("受注", "請求金額"));
                        }
                        // System.out.println(a.get().getName());
                        String[] cat_arr = { sbn, "\"" + d_arr[2] + "\"", "\"" + d_arr[3] + "\"", price };
                        catMap.get(d_arr[0]).put(smr, cat_arr);
                    });
                    refailure.put(cat, catMap);
                });

        // System.out.println(refailure.get("C2-10:旋回モータ"));
        // CSV
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS("工数解析用データ.csv")) {
            refailure.entrySet().stream().forEach(re -> {
                re.getValue().entrySet().stream()
                        .forEach(re_i -> {
                            String smr_odr = re_i.getValue().entrySet().stream()
                                    .map(re_ij -> re_ij.getKey() + "," + String.join(",", re_ij.getValue()))
                                    .collect(Collectors.joining(","));
                            pw.println(re_i.getKey() + "," + re.getKey() + "," + smr_odr);
                        });
            });
        }

    }

    private static void lcc(SyaryoObjectExtract objex, MHeaderObject h) throws AISTProcessException {
        MSyaryoAnalizer a = objex.getAnalize("PC200-10--450972");

        try (PrintWriter pw = CSVFileReadWrite.writerSJIS("LCC解析用データ.csv")) {
            a.get("受注").entrySet().stream()
                    .map(odr -> {
                        String sg_cd = odr.getValue().get(h.getHeaderIdx("受注", "作業形態名称"));
                        String date = odr.getValue().get(h.getHeaderIdx("受注", "受注日"));
                        Integer smr = a.getDateToSMR(date);
                        Integer age = a.age(date);
                        String price = odr.getValue().get(h.getHeaderIdx("受注", "請求金額"));

                        return odr.getKey() + "," + sg_cd + "," + date + "," + age + "," + smr + "," + price;
                    }).forEach(pw::println);
        }
    }
}
