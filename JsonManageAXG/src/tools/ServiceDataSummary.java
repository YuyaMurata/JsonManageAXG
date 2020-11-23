/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import exception.AISTProcessException;
import extract.SyaryoObjectExtract;
import file.CSVFileReadWrite;
import file.ListToCSV;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import obj.MHeaderObject;

/**
 *
 * @author kaeru
 */
public class ServiceDataSummary {

    private static List<String> poweline;

    public static void main(String[] args) throws AISTProcessException {
        //パワーライン対象装置
        poweline = ListToCSV.toList("toolsettings\\パワーライン保障_対象装置.csv");
        //System.out.println(poweline);

        //データ取得
        SyaryoObjectExtract objex = new SyaryoObjectExtract("json", "KM_PC200_DB_P");
        MHeaderObject h = objex.getHeader();

        System.out.println(h.getHeader("受注"));

        //ユーザー定義ファイルの設定
        objex.setUserDefine("KM_PC200_DB_P\\config\\user_define_車両除外無し.json");
        objex.getSummary();

        System.out.println(objex.keySet().size());
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS("PC200_サービス_202001.csv")) {
            //pw.println("車両ID,受注日,SMR,納入からの経過日,作業形態,請求金額,オールサポート対象期間,パワーライン対象装置を含むか判定");
            pw.println("車両ID,受注日,SMR,納入からの経過日,作業形態,請求金額,指示工数,請求工数");
            objex.keySet().stream()
                    .map(sid -> objex.getAnalize(sid))
                    .filter(a -> a.get("受注") != null)
                    .forEach(a -> {
                        //サービスデータ
                        a.get("受注").entrySet().stream()
                                .forEach(e -> {
                                    List sb = new ArrayList();

                                    //車両ID
                                    sb.add(a.get().getName());
                                    
                                    String date = e.getValue().get(h.getHeaderIdx("受注", "受注日"));
                                    sb.add(date);
                                    Integer smr = a.getDateToSMR(date);
                                    sb.add(smr.toString());
                                    Integer age = a.age(date);
                                    sb.add(age.toString());
                                    String code = e.getValue().get(h.getHeaderIdx("受注", "作業形態コード"));
                                    sb.add(code);
                                    String price = e.getValue().get(h.getHeaderIdx("受注", "請求金額"));
                                    sb.add(price);
                                    
                                    //サービス概要
                                    String gaiyo = e.getValue().get(h.getHeaderIdx("受注", "概要１"));
                                    sb.add(gaiyo);
                                    
                                    //作業情報取得
                                    Map<String, List<String>> w = a.getSBNWork(e.getKey());
                                    try {
                                        Double si_step = w.values().stream()
                                                .mapToDouble(wv -> Double.valueOf(wv.get(h.getHeaderIdx("作業", "指示工数"))))
                                                .sum();
                                        Double se_step = w.values().stream()
                                                .mapToDouble(wv -> Double.valueOf(wv.get(h.getHeaderIdx("作業", "請求工数"))))
                                                .sum();

                                        sb.add(si_step.toString());
                                        sb.add(se_step.toString());
                                    } catch (NumberFormatException ne) {
                                        sb.add("-1");
                                        sb.add("-1");
                                    }

                                    /*
                                String allsupport = "0";
                                if(a.allsupport)
                                    allsupport = a.checkAS(date) ? "1" : "0";
                                sb.add(allsupport);
                                     */
                                    //パワーライン
                                    //Long pl = checkPL(a.getSBNWork(e.getKey()), h.getHeaderIdx("作業", "作業コード"));
                                    //sb.add(pl.toString());
                                    pw.println(String.join(",", sb));
                                });

                    });
        }
    }

    //パワーライン対象の作業コードのカウント
    private static Long checkPL(Map<String, List<String>> work, int sgcdIdx) {
        Long plcount = work.values().stream()
                .map(v -> v.get(sgcdIdx))
                .map(cd -> {
                    if (cd.length() > 3) {
                        String device4 = cd.substring(0, 4);
                        if (poweline.contains(device4)) {
                            return true;
                        }
                    }

                    if (cd.length() > 1) {
                        String device2 = cd.substring(0, 2);
                        if (poweline.contains(device2)) {
                            return true;
                        }
                    }

                    //System.out.println(cd);
                    return false;
                })
                .filter(pl -> pl).count();

        return plcount;
    }
}
