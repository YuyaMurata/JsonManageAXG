/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.survive;

import eval.item.EvaluateTemplate;
import eval.obj.ESyaryoObject;
import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *　生存解析もしくは故障解析を実行するクラス
 * @author ZZ17807
 */
public class SurvivalESyaryo {

    public static Map<String, List<String>> results = new HashMap<>();
    public static String PATH = "";
    public static String X = "";
    public static Integer DELTA = 1;

    //故障解析　外部呼出し時のメソッド　(累積の故障発生確率の計測)
    public static void acmfailure(EvaluateTemplate mainte, EvaluateTemplate use, EvaluateTemplate agesmr, String outPath) {
        //ファイル出力パス
        PATH = outPath;

        //出力ファイル設定
        X = agesmr._settings.get("#VISUAL_X");
        DELTA = Integer.valueOf(agesmr._settings.get("#DIVIDE_X"));

        //グループ分類 初期化
        Map<String, List<ESyaryoObject>> group = IntStream.range(0, 4).boxed()
                                                    .flatMap(i -> IntStream.range(0, 4).boxed().map(j -> i+","+j))
                                                    .collect(Collectors.toMap(gij -> gij, gij -> new ArrayList()));
        
        agesmr._eval.keySet().stream().forEach(s -> {
            String g = mainte._eval.get(s).score.toString()
                    + "," + use._eval.get(s).score.toString();
            
            group.get(g).add(agesmr._eval.get(s));
        });

        //グループごとの故障分析
        group.entrySet().stream().forEach(g -> {
            analize(g.getKey(), g.getValue());
        });
    }

    //故障解析
    public static void analize(String gkey, List<ESyaryoObject> g) {
        TreeMap<Double, Integer> fail = new TreeMap<>();
        TreeMap<Double, Integer> count = new TreeMap<>();

        int xidx = 2;
        int svidx = 3;

        //故障台数
        g.stream().forEach(gs -> {
            gs.getPoints().stream()
                    .filter(d -> {
                        if (fail.get(d[xidx]) == null) {
                            fail.put(d[xidx], 0);
                        }
                        return d[svidx] == 1d;
                    }).map(d -> d[xidx])
                    .forEach(d -> {
                        fail.put(d, fail.get(d) + 1);
                    });
        });

        //故障時の残存台数
        fail.keySet().stream().forEach(ft -> {
            Long cnt = g.stream()
                    .map(s -> X.equals("SMR") ? s.a.maxSMR : s.a.age(s.date))
                    .filter(x -> ft <= x / DELTA)
                    .count();
            count.put(ft, cnt.intValue());
        });

        //故障確率の計算
        Map<Double, Double> prob = new HashMap();
        Double before = 1d;
        for (Double smr : fail.keySet()) {
            Integer failCnt = fail.get(smr);
            Integer remN = count.get(smr);

            Double surv = before * (remN - failCnt) / remN;
            before = surv;

            prob.put(smr, 1d - surv);
        }
        
        //mtbf
        //mtbf(g, xidx, svidx);
        int totalSyaryo = g.size();
        long totalFail = g.stream()
                            .mapToLong(s -> s.getPoints().stream()
                                    .filter(v -> v[svidx] == 1d).count())
                            .sum();

        printCSV(totalSyaryo, totalFail, fail, count, prob, PATH + "\\" + gkey + "_FR.csv");
    }

    //各車両の対象部品群 MTBF
    private static Map<String, Double> mtbf(List<ESyaryoObject> data, int xidx, int svidx) {
        Map<String, Double> mtbfMap = data.stream()
                                .collect(Collectors.toMap(
                                        s -> s.a.syaryo.getName(), 
                                        s -> s.getMTBF(xidx, svidx)));
        return mtbfMap;
    }

    //生存解析 外部呼出し時のメソッド
    public static void survival(EvaluateTemplate mainte, EvaluateTemplate use, EvaluateTemplate agesmr, String outPath) {
        //ファイル出力パス
        PATH = outPath;

        //出力ファイル設定
        X = agesmr._settings.get("#VISUAL_X");

        //生存分析用のデータ作成
        Map<String, List<String>> data = new HashMap<>();
        agesmr._eval.keySet().stream().forEach(s -> {
            List<String> sv = new ArrayList<>();

            //各評価器のスコア取得
            sv.add(mainte._eval.get(s).score.toString());
            sv.add(use._eval.get(s).score.toString());
            sv.add(agesmr._eval.get(s).score.toString());

            //生存解析に必要なデータ取得
            List<String> d = Arrays.stream(agesmr._eval.get(s).getPoint())
                    .boxed().map(v -> String.valueOf(v.intValue()))
                    .collect(Collectors.toList());
            sv.addAll(d);

            //データ作成
            data.put(s, sv);
        });

        analize(data);

        //results.entrySet().stream()
        //                    .map(r -> r.getKey()+":"+r.getValue())
        //                    .forEach(System.out::println);
    }

    //生存解析
    private static void analize(Map<String, List<String>> data) {
        int idx_m = 0;
        int idx_u = 1;
        int idx_smr = 5;
        int idx_fstat = 6;

        //グループ分類
        Map<String, Map<String, List<String>>> group = new HashMap<>();
        data.entrySet().stream().forEach(d -> {
            String sid = d.getKey();
            String key = d.getValue().get(idx_m) + "," + d.getValue().get(idx_u);
            String smr = d.getValue().get(idx_smr);
            String fstat = d.getValue().get(idx_fstat);

            if (group.get(key) == null) {
                group.put(key, new HashMap<>());
            }

            group.get(key).put(sid, Arrays.asList(new String[]{smr, fstat}));
        });

        //グループ確認
        //group.entrySet().stream().map(g -> g.getKey() + ":" + g.getValue().size()).forEach(System.out::println);
        //System.out.println("total:" + group.entrySet().stream().mapToInt(g -> g.getValue().size()).sum());
        //グループごとの故障確率
        Map<String, List<String>> map = new TreeMap<>();
        group.entrySet().stream().forEach(g -> map.putAll(km(g.getKey(), g.getValue())));
        map.entrySet().stream().forEach(e -> {
            String s = e.getValue().get(e.getValue().size() - 1);
            List<String> d = new ArrayList(data.get(e.getKey()));
            d.add(s);

            results.put(e.getKey(), d);
        });
    }

    //カプラン・マイヤー法の計算
    private static Map<String, List<String>> km(String gkey, Map<String, List<String>> g) {
        Map<Double, List<String>> m = new TreeMap<>();
        g.entrySet().stream()
                .forEach(e -> {
                    Double smr = Double.valueOf(e.getValue().get(0));
                    if (m.get(smr) == null) {
                        m.put(smr, new ArrayList<>());
                    }
                    m.get(smr).add(e.getKey());
                });

        //カプラン・マイヤー法
        Map<Double, Double> fail = new TreeMap<>();
        Map<Double, Integer> failcnt = new TreeMap<>();
        Map<Double, Integer> count = new TreeMap<>();
        Double before = 1d;
        for (Double smr : m.keySet()) {
            if (fail.get(smr) == null) {
                fail.put(smr, 0d);
            }

            Double dead = Double.valueOf(m.get(smr).stream()
                    .filter(sid -> g.get(sid).get(1).equals("1"))
                    .count());

            Double total = Double.valueOf(m.entrySet().stream()
                    .filter(e -> e.getKey() >= smr)
                    .mapToInt(e -> e.getValue().size())
                    .sum());

            Double surv = before * (total - dead) / total;
            before = surv;

            failcnt.put(smr, dead.intValue());
            fail.put(smr, 1d - surv);
            count.put(smr, total.intValue());
        }

        int totalSyaryo = g.size();
        int totalFail = failcnt.values().stream().mapToInt(v -> v).sum();
        
        //故障率データ出力
        printCSV(totalSyaryo, totalFail, failcnt, count, fail, PATH + gkey + "_FR.csv");
        
        //スコアリング
        Map result = scoring(g, fail);

        return result;
    }

    //解析結果のスコアリング
    private static Map scoring(Map<String, List<String>> g, Map<Double, Double> fail) {
        Map<String, List<String>> result = new HashMap<>();

        int gidx_smr = 0;
        int gidx_fstat = 1;

        g.entrySet().stream().forEach(e -> {
            String sid = e.getKey();
            String fstat = e.getValue().get(gidx_fstat);
            Integer smr = Integer.valueOf(e.getValue().get(gidx_smr));
            List<String> list = new ArrayList<>(e.getValue());

            //スコアリング
            if (fstat.equals("0")) {
                list.add("3");
            } else {
                Double surv = fail.get(smr);
                if (surv < 0.5d) {
                    list.add("1");
                } else {
                    list.add("2");
                }
            }

            result.put(sid, list);
        });

        return result;
    }
    
    //解析結果のCSV出力
    private static void printCSV(int totalSyaryo, long totalFail, Map<Double, Integer> failur, Map<Double, Integer> remain, Map<Double, Double> result, String filename){
        //故障率データ出力
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS(filename)) {
            //分析情報
            pw.println("X,"+X+",dx,"+DELTA);
            pw.println("Total Machines:"+totalSyaryo+",Total Failures:"+totalFail);

            //ヘッダ
            pw.println(X + ",COUNT,FAIL,RATE");
            
            Optional<Integer> st = remain.values().stream().findFirst();
            pw.println("0,"+(st.isPresent()?st.get():0)+",0,0");
            failur.entrySet().stream()
                    .map(df -> (df.getKey()*DELTA) + "," + remain.get(df.getKey()) + "," + df.getValue() + "," + result.get(df.getKey()))
                    .forEach(pw::println);
        }
    }
}
