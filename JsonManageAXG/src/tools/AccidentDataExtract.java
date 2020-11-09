/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import analizer.MSyaryoAnalizer;
import exception.AISTProcessException;
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;
import static tools.AttachedSyaryoInfo.db;

/**
 *
 * @author KM65486
 */
public class AccidentDataExtract {

    public static void main(String[] args) throws AISTProcessException {
        //事故車両推定用に一通りのデータを抽出
        db = MongoDBPOJOData.create();
        db.set("json", "KM_PC200_DB_P_Form", MSyaryoObject.class);

        MHeaderObject h = db.getHeader();

        try (PrintWriter pw = CSVFileReadWrite.writerSJIS("PC200_ServiceData_Accident_202003.csv")) {
            pw.println("車両ID,作番,請求金額,概要,使用部品");
            db.getKeyList().stream()
                    .map(sid -> (MSyaryoObject) db.getObj(sid))
                    .filter(s -> s.getData("受注") != null)
                    .flatMap(s -> extractService(h, s).stream())
                    .forEach(pw::println);
        }
    }

    private static List<String> extractService(MHeaderObject h, MSyaryoObject s) {
        MSyaryoAnalizer a = new MSyaryoAnalizer(s);
        List<String> list = a.get("受注").entrySet().stream()
                .map(e -> {
                    List<String> order = e.getValue();
                    Map<String, List<String>> parts = a.getSBNParts(e.getKey());
                    List<String> pnoPName = parts.values().stream()
                            .map(pv -> pv.get(h.getHeaderIdx("部品", "品番")) + " " + pv.get(h.getHeaderIdx("部品", "部品名称")))
                            .collect(Collectors.toList());

                    //データ作成
                    //車両ID、作番、請求金額、概要(テキスト)、品番-品名...
                    List<String> data = new ArrayList<>();
                    data.add(a.get().getName());
                    data.add(e.getKey());
                    data.add(e.getValue().get(h.getHeaderIdx("受注", "請求金額")));
                    data.add(e.getValue().get(h.getHeaderIdx("受注", "概要１")) + " " + e.getValue().get(h.getHeaderIdx("受注", "概要２")));
                    data.add(String.join("_", pnoPName));

                    return String.join(",", data);
                }).collect(Collectors.toList());
        return list;
    }
}
