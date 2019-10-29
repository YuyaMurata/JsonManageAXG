/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.item;

import eval.obj.ESyaryoObject;
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17390
 */
public class UseEvaluate extends EvaluateTemplate {

    private Map<String, List<String>> USE_DATAKEYS;
    private MHeaderObject HEADER_OBJ;
    
    private static Map<String, List<String>> loadDim2Rawh = new HashMap(){{
        put("LOADMAP_実エンジン回転VSエンジントルク", Arrays.asList(new String[]{"_1100","_1300","_1500","_1700","_1800","_1900","_2000","_2100","_2200","_2300","_2400","_2400_"}));
        put("LOADMAP_エンジン水温VS作動油温", Arrays.asList(new String[]{"_50","_75","_85","_95","_100","_105","_120","_120_"}));
        put("LOADMAP_ポンプ斜板(R)", Arrays.asList(new String[]{"_100","_200","_300","_300_"}));
        put("LOADMAP_ポンプ斜板(F)", Arrays.asList(new String[]{"_100","_200","_300","_300_"}));
    }};
    
    public UseEvaluate(Map<String, List<String>> settings, MHeaderObject h) {
        USE_DATAKEYS = settings;
        HEADER_OBJ = h;

        settings.entrySet().forEach(e -> {
            List<String> hlist = e.getValue().stream()
                    .flatMap(eh -> loadDim2Rawh.get(eh) == null ? 
                                h.getHeader(eh).stream() : 
                                loadDim2Rawh.get(eh).stream().flatMap(hraw -> h.getHeader(eh).stream().map(hcol -> hcol.split("\\.")[0]+"."+hraw+"."+hcol.split("\\.")[1])))
                    .collect(Collectors.toList());
            super.setHeader(e.getKey(), hlist);
        });
    }

    @Override
    public ESyaryoObject trans(MSyaryoObject syaryo) {
        ESyaryoObject s = new ESyaryoObject(syaryo);

        //評価対象データキーの抽出
        Map<String, List<String>> sv = extract(s);

        //評価対象データを集約
        Map<String, List<String>> data = aggregate(s, sv);

        //評価対象データの正規化
        Map<String, Double> norm = normalize(s, data);

        //各データを検証にセット
        s.setData(sv, data, norm);

        return s;
    }

    //対象データキーの抽出
    @Override
    public Map<String, List<String>> extract(ESyaryoObject s) {
        Map<String, List<String>> map = USE_DATAKEYS.entrySet().stream()
                .collect(Collectors.toMap(
                        ld -> ld.getKey(),
                        ld -> ld.getValue().stream()
                                .map(dkey -> s.a.get(dkey) != null ? dkey : "")
                                .collect(Collectors.toList())
                ));
        
        return map;
    }

    @Override
    public Map<String, List<String>> aggregate(ESyaryoObject s, Map<String, List<String>> sv) {
        Map<String, List<String>> data = sv.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue().stream().filter(v -> s.a.get(v) != null)
                                .flatMap(v -> loadDim2Rawh.get(v) == null ?
                                        s.a.get(v).values().stream().flatMap(loadmap -> loadmap.stream())
                                        :loadDim2Rawh.get(v).stream().flatMap(ld -> s.a.get(v).get(ld).stream()))
                                .collect(Collectors.toList())
                ));
        
        return data;
    }

    @Override
    public Map<String, Double> normalize(ESyaryoObject s, Map<String, List<String>> data) {
        int smridx = 1; //LOADMAP_DATE_SMR Value
        Double smr = Double.valueOf(s.a.get("LOADMAP_DATE_SMR") != null ? s.a.get("LOADMAP_DATE_SMR").values().stream().map(v -> v.get(smridx)).findFirst().get() : "-1");
        
        Map<String, Double> norm = new LinkedHashMap<>();
        data.entrySet().stream().forEach(e -> {
            
            switch(e.getKey()){
                case "車体":    norm.putAll(body(e.getKey(), smr, e.getValue(), _header.get(e.getKey())));
                                break;
                case "エンジン": norm.putAll(engine(e.getKey(), smr, e.getValue(), _header.get(e.getKey())));
                                break;
                case "油圧機器": norm.putAll(pump(e.getKey(), smr, e.getValue(), _header.get(e.getKey())));
                                break;
                case "走行機器": norm.putAll(travel(e.getKey(), smr, e.getValue(), _header.get(e.getKey())));
                                break;
            }
            /*
            _header.get(e.getKey()).stream().forEach(h ->{
                int i= _header.get(e.getKey()).indexOf(h);
                //System.out.println("  "+h+"["+i+"]:"+e.getValue().get(i));
                
                if(data.get(e.getKey()).isEmpty())
                    norm.put(e.getKey()+"_"+h, -1d);
                else
                    norm.put(e.getKey()+"_"+h, Double.valueOf(e.getValue().get(i))/smr);
            });*/
        });
        
        return norm;
    }

    private Map<String, Double> body(String key, Double smr, List<String> data, List<String> h) {
        return null;
    }

    private Map<String, Double> engine(String key, Double smr, List<String> data, List<String> h) {
        return null;
    }

    private Map<String, Double> pump(String key, Double smr, List<String> data, List<String> h){
        return null;
    }

    private Map<String, Double> travel(String key, Double smr, List<String> data, List<String> h){
        Map<String, Double> norm = new LinkedHashMap<>();
        System.out.println("h"+h);
        System.out.println("d"+data);
        System.out.println("smr"+smr);
        
        List<String> i = Arrays.asList(new String[]{"LOADMAP_作業機操作状況.TRAVEL", "LOADMAP_作業機操作状況.Hi"});
        List<String> head = h.stream().filter(hi -> i.contains(hi)).collect(Collectors.toList());
        
        if(data.isEmpty())
            head.stream().forEach(hi -> norm.put("走行機器_"+hi, -1d));
        else
            head.stream().forEach(hi -> norm.put("走行機器_"+hi, Double.valueOf(data.get(h.indexOf(hi)))/smr));
        
        _header.put(key, head);
        
        return norm;
    }

    /*
    @Override
    public Map<String, Integer> scoring(Map<String, Integer> cluster, String key, Map<String, List<Double>> data) {
        int maxCluster = cluster.values().stream().distinct().mapToInt(c -> c).max().getAsInt();
        //Average
        Map<Integer, Double> avg = IntStream.range(0, maxCluster + 1).boxed()
                .collect(Collectors.toMap(
                        i -> i,
                        i -> cluster.entrySet().stream()
                                .filter(c -> c.getValue().equals(i))
                                .flatMap(c -> data.get(c.getKey()).stream()
                                .filter(d -> d == 1d)
                                .map(d -> data.get(c.getKey()).indexOf(d))
                                ).map(d -> header(key).get(d).split("_"))
                                .mapToDouble(h -> -Double.valueOf(h[0]) * Double.valueOf(h[1])) //ヘッダ情報を利用し右下に行くほど評価値が下がる用に評価
                                .average().getAsDouble()
                ));

        //Sort
        List<Integer> sort = avg.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(a -> a.getKey())
                .collect(Collectors.toList());

        //Scoring
        Map score = cluster.entrySet().stream()
                .collect(Collectors.toMap(c -> c.getKey(), c -> 3 - sort.indexOf(c.getValue())));

        return score;
    }

   
    public static void main(String[] args) {
        LOADER.setFile("PC200_form");
        SyaryoAnalizer.rejectSettings(false, false, false);
        SyaryoAnalizer s = new SyaryoAnalizer(LOADER.getSyaryoMap().get("PC200-8N1-352999"), true);

        UseEvaluate use = new UseEvaluate();

        String testkey = "LOADMAP_実エンジン回転VSエンジントルク";
        //Map<String, List<String>> data = use.getdata(s);
        Map<String, Double> result = use.evaluate(testkey, s);

        //クラスタ用データ
        System.out.println("\n" + use.header(testkey));
        System.out.println(use.getClusterData(testkey));
    }

    private static void evalCSV_P1(SyaryoAnalizer s, String key, PrintWriter pw) {
        UseEvaluate use = new UseEvaluate();
        use.evaluate(key, s);

        try (PrintWriter csv = CSVFileReadWrite.writerSJIS("data\\" + s.name + "_" + key + ".csv")) {
            csv.println(s.name);

            csv.println("元データ");
            String[][] mat1 = matrix(use.header(key), use.getdata(key, s));
            IntStream.range(0, mat1.length).boxed().map(i -> String.join(",", mat1[i])).forEach(csv::println);
            csv.println();

            csv.println("正規化(ランク)");
            String[][] mat2 = matrix(use.header(key), use.getClusterData(key).get(s.name));
            IntStream.range(0, mat2.length).boxed().map(i -> String.join(",", mat2[i])).forEach(csv::println);
            csv.println();

            csv.println("正規化(エンジン回転数集約)");
            List<String> mat3 = IntStream.range(1, mat1[0].length).boxed()
                    .map(i -> IntStream.range(1, mat1.length).boxed().mapToInt(j -> Integer.valueOf(mat1[j][i])).sum())
                    .map(d -> d.toString())
                    .collect(Collectors.toList());
            csv.println(String.join(",", mat1[0]));
            csv.println("," + String.join(",", mat3));
        }
    }

    private static String[][] matrix(List<String> h, List<Double> d) {
        List<String> x = h.stream().map(i -> Integer.valueOf(i.split("_")[0])).distinct().sorted().map(i -> i.toString()).collect(Collectors.toList());
        List<String> y = h.stream().map(i -> Integer.valueOf(i.split("_")[1])).distinct().sorted().map(i -> i.toString()).collect(Collectors.toList());
        String[][] mat = new String[x.size() + 1][y.size() + 1];
        mat[0][0] = "";

        IntStream.range(0, h.size()).forEach(i -> {
            String hi = h.get(i);
            String di = String.valueOf(d.get(i).intValue());

            int xi = x.indexOf(hi.split("_")[0]) + 1;
            int yi = y.indexOf(hi.split("_")[1]) + 1;

            mat[xi][0] = hi.split("_")[0];
            mat[0][yi] = hi.split("_")[1];
            mat[xi][yi] = di;
        });

        return mat;
    }*/
}
