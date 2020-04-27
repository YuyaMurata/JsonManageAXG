/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import axg.shuffle.form.item.FormKomtrax;
import axg.shuffle.form.util.FormalizeUtils;
import compress.SnappyMap;
import exception.AISTProcessException;
import file.CSVFileReadWrite;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import obj.MHeaderObject;
import obj.MSyaryoObject;
import thread.ExecutableThreadPool;

/**
 *
 * @author kaeru
 */
public class MergeActSMRData {

    static Map<String, String> test = new HashMap<>();

    public static void main(String[] args) {
        //exec();
        check();
    }
    
    private static void check(){
        //KOMTRAX_SERVICE_METER 仮ヘッダ
        String[] smr = {"機種", "機番", "SMR_TIME", "SMR_VALUE"};
        //KOMTRAX_ACT 仮ヘッダ
        String[] merge = {"機種", "機番", "ACT_DATE", "DB", "ACT_COUNT", "DAILY_UNIT"};
        
        String smrFile = "E:\\data\\out\\axg\\SMALL_SMR.csv";
        String actFile = "E:\\data\\out\\axg\\SMALL_ACT.csv";
        String mergeFile = "E:\\data\\out\\axg\\SMALL_MERGE_ACT_SMR.csv";
        
        //String id = checkout(smrFile, "Check_SMR_{id}_.csv", null);
        checkout(actFile, "Check_ACT_{id}_.csv", "PC200-454629");
    }
    
    private static String checkout(String inf, String outf, String id){
        Map<String, List<String[]>> g = null;
        try (BufferedReader br = CSVFileReadWrite.readerUTF8(inf)) {
            br.readLine();
            g = br.lines()
                    .map(s -> s.split(","))
                    .collect(Collectors.groupingBy(s -> s[0] + "-" + s[1]));
        }catch(Exception e){
            e.printStackTrace();
        }
        
        if(id == null)
            id = g.keySet().stream().findAny().get();
        
        //出力
        try(PrintWriter pw = CSVFileReadWrite.writerSJIS(outf.replace("{id}", id))){
            g.get(id).stream().map(s -> String.join(",", s)).forEach(pw::println);
        } catch (AISTProcessException ex) {
        }
        
        return id;
    }
    
    private static void exec(){
        //KOMTRAX_SERVICE_METER 仮ヘッダ
        String[] smr = {"機種", "機番", "SMR_TIME", "SMR_VALUE"};
        //KOMTRAX_ACT 仮ヘッダ
        String[] act = {"機種", "機番", "ACT_DATE", "ACT_COUNT", "DAILY_UNIT"};

        String smrFile = "E:\\data\\out\\axg\\SMALL_SMR.csv";
        String actFile = "E:\\data\\out\\axg\\SMALL_ACT.csv";

        MHeaderObject h = setHeader();

        System.out.println(h.map);

        //Map<String, byte[]> map = new HashMap<>();
        Map<String, byte[]> smrmap = aggregate(smrFile, "KOMTRAX_SMR");
        Map<String, byte[]> actmap = aggregate(actFile, "KOMTRAX_ACT");
        Map<String, byte[]> map = merge(smrmap, actmap);

        System.out.println(map);

        System.out.println("Finished Data load");
        smrmap = null;
        actmap = null;

        try {
            //Test
            /*map.values().stream().limit(1).map(s -> (MSyaryoObject) SnappyMap.toObject(s)).forEach(s -> {
            String[] id = s.getName().split("-");
            System.out.println(s.getName());
            s.getData("KOMTRAX_SMR").entrySet().stream()
            .map(d -> "    " + d.getKey() + ":" + d.getValue())
            .forEach(System.out::println);
            });*/

            ExecutableThreadPool.getInstance().getPool().submit(() -> {
                map.keySet().parallelStream().forEach(key -> {
                    MSyaryoObject s = (MSyaryoObject) SnappyMap.toObject(map.get(key));
                    FormKomtrax.form(s, h);
                    map.put(key, SnappyMap.toSnappy(s));
                });
            }).get();
        } catch (InterruptedException ex) {
        } catch (ExecutionException ex) {
        }
        System.out.println("Finished Formalize");

        //マージデータ出力
        try (PrintWriter pw = CSVFileReadWrite.writerUTF8("E:\\data\\out\\axg\\SMALL_MERGE_ACT_SMR.csv")) {
            //header
            pw.println("機種,機番,SMR_TIME,DB,SMR_VALUE,DAILY_UNIT");

            map.values().stream().map(s -> (MSyaryoObject) SnappyMap.toObject(s)).forEach(s -> {
                String[] id = s.getName().split("-");
                s.getData("KOMTRAX_SMR").entrySet().stream()
                        .map(d -> id[0] + "," + id[1] + "," + d.getKey() + "," + String.join(",", d.getValue()))
                        .forEach(pw::println);
            });
        } catch (AISTProcessException ex) {
            ex.printStackTrace();
        }
        //System.out.println(test);*/
    }

    private static MHeaderObject setHeader() {
        List<String> hlist = Arrays.asList(new String[]{"KOMTRAX_SMR.SMR_DATE", "KOMTRAX_SMR.DB", "KOMTRAX_SMR.SMR_VALUE", "KOMTRAX_SMR.DAILY_UNIT"});
        MHeaderObject h = new MHeaderObject(hlist);
        h.setHeaderMap();
        return h;
    }

    private static Map<String, byte[]> aggregate(String file, String dataKey) {
        try (BufferedReader br = CSVFileReadWrite.readerUTF8(file)) {
            br.readLine();
            Map<String, List<String[]>> g = br.lines()
                    .map(s -> s.split(","))
                    .collect(Collectors.groupingBy(s -> s[0] + "-" + s[1]));

            //データの形式の統一
            Map<String, byte[]> form = g.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey(),
                            e -> {
                                Map<String, Map<String, List<String>>> map = new HashMap<>();
                                Map<String, List<String>> data = e.getValue().stream()
                                        .collect(Collectors.toMap(
                                                s -> s[2].split(" ")[0].replace("/", ""),
                                                s -> {
                                                    List<String> v = new ArrayList<>();
                                                    v.add(dataKey);
                                                    v.add(s[3]);
                                                    v.add(s.length == 4 ? "1" : s[4]);
                                                    return v;
                                                },
                                                (v1, v2) -> v2));

                                map.put("KOMTRAX_SMR", data);

                                MSyaryoObject obj = new MSyaryoObject();
                                obj.setName(e.getKey());
                                obj.setMap(map);
                                return SnappyMap.toSnappy(obj);
                            }));

            return form;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static Map<String, byte[]> merge(Map<String, byte[]> m1, Map<String, byte[]> m2) {
        try {
            Map<String, byte[]> map = ExecutableThreadPool.getInstance().getPool().submit(() -> {
                //Key Merge
                List<String> keys = new ArrayList<>();
                keys.addAll(m1.keySet());
                keys.addAll(m2.keySet());
                keys = keys.stream().distinct().collect(Collectors.toList());

                System.out.println(keys);

                //Merge
                return keys.stream()
                        .collect(Collectors.toMap(
                                k -> k,
                                k -> {
                                    MSyaryoObject o1
                                    = m1.get(k) != null ? (MSyaryoObject) SnappyMap.toObject(m1.get(k))
                                    : (MSyaryoObject) SnappyMap.toObject(m2.get(k));

                                    if (o1 != null && m2.get(k) != null) {
                                        MSyaryoObject o2 = (MSyaryoObject) SnappyMap.toObject(m2.get(k));
                                        Map<String, List<String>> data = new TreeMap(o1.getData("KOMTRAX_SMR"));
                                        o2.getData("KOMTRAX_SMR").entrySet().stream()
                                                .forEach(o2e -> {
                                                    String d = FormalizeUtils.dup(o2e.getKey(), data);
                                                    data.put(d, o2e.getValue());
                                                });
                                        o1.setData("KOMTRAX_SMR", data);
                                    }
                                    return SnappyMap.toSnappy(o1);
                                }
                        ));
            }).get();

            return map;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}
