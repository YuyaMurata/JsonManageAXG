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

    public UseEvaluate(Map<String, List<String>> settings, MHeaderObject h) {
        USE_DATAKEYS = settings;
        HEADER_OBJ = h;

        settings.entrySet().forEach(e -> {
            List<String> hlist = e.getValue().stream()
                    .flatMap(eh -> h.getHeader(eh).stream())
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
                                .flatMap(v -> s.a.get(v).values().stream().flatMap(loadmap -> loadmap.stream()))
                                .collect(Collectors.toList())
                ));
        return data;
    }

    @Override
    public Map<String, Double> normalize(ESyaryoObject s, Map<String, List<String>> data) {
        Double smr = Double.valueOf(s.a.get("LOADMAP_DATE_SMR") != null ? s.a.get("LOADMAP_DATE_SMR").values().stream().map(v -> v.get(0)).findFirst().get() : "-1");
        Map norm = data.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> IntStream.range(0, _header.get(e.getKey()).size()).boxed()
                                .map(i -> !e.getValue().isEmpty() ? e.getValue().get(i) : "0")
                                .peek(System.out::println)
                                .map(loadmap -> Double.valueOf(loadmap) / smr)
                                .collect(Collectors.toList())
                ));

        return norm;
    }

    private List<String> body(Map<String, List<String>> map) {
        return null;
    }

    private List<String> engine(Map<String, List<String>> map) {
        return null;
    }

    private List<String> pump(Map<String, List<String>> map) {
        return null;
    }

    private List<String> travel(Map<String, Double> data, int r) {
        return null;
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
