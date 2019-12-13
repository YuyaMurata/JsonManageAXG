/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.item;

import eval.analizer.MSyaryoAnalizer;
import eval.cluster.ClusteringESyaryo;
import eval.cluster.DataVector;
import eval.obj.ESyaryoObject;
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
import org.apache.commons.math3.ml.clustering.CentroidCluster;

/**
 *
 * @author ZZ17390
 */
public class UseEvaluate extends EvaluateTemplate {

    private Map<String, Map<String, Map<String, String>>> USE_DATAKEYS;
    private MHeaderObject HEADER_OBJ;

    public UseEvaluate(Map settings, MHeaderObject h) {
        super.enable = ((Map<String, String>)settings).get("#EVALUATE").equals("ENABLE");
        settings.remove("#EVALUATE");
        
        USE_DATAKEYS = (Map<String, Map<String, Map<String, String>>>)settings;
        HEADER_OBJ = h;

        //設定ファイルのヘッダ変換　データ項目.行.列
        USE_DATAKEYS.entrySet().forEach(e -> {
            List<String> hlist = e.getValue().entrySet().stream()
                    .flatMap(d -> {
                        if (!d.getValue().containsKey("SUM")) {
                            return d.getValue().entrySet().stream()
                                    .filter(di -> !di.getKey().equals("HEADER"))
                                    .flatMap(di -> Arrays.stream(d.getValue().get("HEADER").split(",")).map(dj -> d.getKey() + "." + di.getKey() + "." + dj));
                        } else {
                            if (d.getValue().get("SUM").equals("COLUMN")) {
                                return Arrays.stream(d.getValue().get("HEADER").split(","))
                                        .map(dj -> d.getKey() + ".SUM("
                                        + d.getValue().keySet().stream()
                                                .filter(di -> !di.equals("HEADER") && !di.equals("SUM"))
                                                .collect(Collectors.joining(".")) + ")."
                                        + dj);
                            } else {
                                return d.getValue().keySet().stream()
                                        .filter(di -> !di.equals("HEADER") && !di.equals("SUM"))
                                        .map(di -> d.getKey() + "."
                                        + di + ".SUM("
                                        + Arrays.stream(d.getValue().get("HEADER").split(","))
                                                .collect(Collectors.joining(".")) + ")"
                                        );
                            }
                        }
                    })
                    .collect(Collectors.toList());
            super.setHeader(e.getKey(), hlist);
        }
        );
    }

    @Override
    public ESyaryoObject trans(MSyaryoAnalizer sa) {
        ESyaryoObject s = new ESyaryoObject(sa);

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
                        ld -> ld.getValue().keySet().stream()
                                .map(dkey -> s.a.get(dkey) != null ? dkey : "")
                                .collect(Collectors.toList())
                ));

        return map;
    }

    @Override
    public Map<String, List<String>> aggregate(ESyaryoObject s, Map<String, List<String>> sv) {
        Map<String, List<String>> data = sv.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(), //評価項目
                        e -> e.getValue().stream().filter(d -> !d.isEmpty()).flatMap(d -> { //データ項目
                            Map<String, String> setting = USE_DATAKEYS.get(e.getKey()).get(d);
                            List<String> h = HEADER_OBJ.getHeader(d);
                            
                            if (!setting.containsKey("SUM")) {
                                return s.a.get(d).entrySet().stream()
                                        .filter(di -> setting.get(di.getKey()) != null ? true : setting.get("INDEX") != null)
                                        .flatMap(di -> h.stream()
                                            .map(hi -> {
                                                String[] set = setting.get("INDEX")!= null ? setting.get("INDEX").split(",") : setting.get(di.getKey()).split(",");
                                                int idx = HEADER_OBJ.getHeaderIdx(d, hi);
                                                return mask(set[idx], di.getValue().get(idx));
                                            })
                                        );
                            }else{
                                return sum(setting, h, s.a.get(d)).stream();
                            }
                        }).collect(Collectors.toList())
                ));        
        return data;
    }

    private String mask(String m, String ij) {
        return String.valueOf(Double.valueOf(m) * Double.valueOf(ij));
    }

    private List<String> sum(Map<String, String> setting, List<String> h, Map<String, List<String>> d) {   
        if(setting.get("SUM").equals("COLUMN")){
            return h.stream()
                    .map(hj -> h.indexOf(hj))
                    .map(hj -> d.entrySet().stream()
                                    .map(di -> mask(setting.get(di.getKey()).split(",")[hj], di.getValue().get(hj)))
                                    .mapToDouble(di -> Double.valueOf(di)).sum())
                                    .map(sum -> String.valueOf(sum))
                    .collect(Collectors.toList());
        }else{
           return setting.keySet().stream().filter(hi -> !hi.equals("HEADER") && !hi.equals("SUM"))
                    .map(hi -> h.stream().map(hj -> h.indexOf(hj))
                                            .map(hj -> mask(setting.get(hi).split(",")[hj], d.get(hi).get(hj)))
                                            .mapToDouble(dj -> Double.valueOf(dj)).sum())
                                    .map(sum -> String.valueOf(sum))
                    .collect(Collectors.toList()); 
        }
    }

    @Override
    public Map<String, Double> normalize(ESyaryoObject s, Map<String, List<String>> data) {
        int smridx = 1; //LOADMAP_DATE_SMR Value
        String date = s.a.get("LOADMAP_DATE_SMR") != null ? s.a.get("LOADMAP_DATE_SMR").keySet().stream().findFirst().get() : "-1";
        Double smr = Double.valueOf(s.a.get("LOADMAP_DATE_SMR") != null ? s.a.get("LOADMAP_DATE_SMR").values().stream().map(v -> v.get(smridx)).findFirst().get() : "-1");
        
        s.setDateSMR(date, smr.intValue());
        
        Map<String, Double> norm = new LinkedHashMap<>();
        data.entrySet().stream().forEach(e -> {
            _header.get(e.getKey()).stream().forEach(h ->{
                int i= _header.get(e.getKey()).indexOf(h);
                //System.out.println("  "+h+"["+i+"]:"+e.getValue().get(i));
                
                if(data.get(e.getKey()).isEmpty())
                    norm.put(e.getKey()+"."+h, -1d);
                else
                    norm.put(e.getKey()+"."+h, Double.valueOf(e.getValue().get(i))/smr);
            });
        });

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

    @Override
    public void scoring() {
        //評価適用　無効
        if(!super.enable) return ;
        
        Map<Integer, List<ESyaryoObject>> cids = new LinkedHashMap<>();
        
        //CIDで集計
        super._eval.values().stream().forEach(e -> {
            if (!e.norm.values().stream().filter(ed -> ed > 0d).findFirst().isPresent()) {
                e.score = 0;
            } else {
                if (cids.get(e.cid) == null) {
                    cids.put(e.cid, new ArrayList<>());
                }

                cids.get(e.cid).add(e);
            }
        });
        
        //cidごとの分割値の差分
        List<DataVector> cidavg = cids.entrySet().stream()
                .map(cid
                        -> new DataVector(cid.getKey(),
                        cid.getValue().stream()
                                .mapToDouble(e -> {
                                        int s = e.norm.size();
                                        double l = 0;//IntStream.range(0, s/2).mapToDouble(i -> e.getPoint()[i]).sum();
                                        double r = IntStream.range(3*s/4, s).mapToDouble(i -> e.getPoint()[i]).sum();
                                        return r-l;
                                    })
                                .average().getAsDouble()))
                .collect(Collectors.toList());
        
        //スコアリング用にデータを3分割
        List<CentroidCluster<DataVector>> splitor = ClusteringESyaryo.splitor(cidavg);
        List<Integer> sort = IntStream.range(0, splitor.size()).boxed()
                .sorted(Comparator.comparing(i -> splitor.get(i).getPoints().stream().mapToDouble(d -> d.p).average().getAsDouble(), Comparator.reverseOrder()))
                .map(i -> i).collect(Collectors.toList());

        //スコアリング
        sort.stream().forEach(i -> {
            splitor.get(i).getPoints().stream()
                    .map(sp -> sp.cid)
                    .forEach(cid -> {
                        cids.get(cid).stream().forEach(e -> {
                            e.score = sort.indexOf(i) + 1;
                        });
                    });
        });
    }
}
