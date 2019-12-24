/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package score.item;

import score.analizer.MSyaryoAnalizer;
import score.cluster.ClusteringESyaryo;
import score.cluster.DataVector;
import score.obj.ESyaryoObject;
import score.util.CalculateBearingLife;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import obj.MHeaderObject;
import org.apache.commons.math3.ml.clustering.CentroidCluster;

/**
 *
 * @author ZZ17390
 */
public class UseEvaluate extends EvaluateTemplate {

    private Map<String, Map<String, Map<String, String>>> USE_DATAKEYS;
    private MHeaderObject HEADER_OBJ;
    private List<String> SCORE_TARGET;

    public UseEvaluate(Map settings, MHeaderObject h) {
        super.enable = ((Map<String, String>) settings).get("#EVALUATE").equals("ENABLE");
        settings.remove("#EVALUATE");

        //スコア基準値の取得
        SCORE_TARGET = new ArrayList();

        USE_DATAKEYS = (Map<String, Map<String, Map<String, String>>>) settings;
        HEADER_OBJ = h;

        //設定ファイルのヘッダ変換　データ項目.行.列
        USE_DATAKEYS.entrySet().forEach(e -> {
            List<String> hlist = e.getValue().entrySet().stream()
                    .flatMap(d -> createHeader(e.getKey(), d).stream())
                    .collect(Collectors.toList());
            super.setHeader(e.getKey(), hlist);
        }
        );
    }

    //設定ファイルからヘッダを作成
    private List<String> createHeader(String item, Map.Entry<String, Map<String, String>> d) {
        List itemHeader;
        if (!d.getValue().containsKey("SUM")) {
            //通常のヘッダ処理
            itemHeader = d.getValue().entrySet().stream()
                    .filter(di -> di.getKey().charAt(0) != '#') //設定情報を読み込まない
                    .filter(di -> !di.getKey().equals("HEADER"))
                    .flatMap(di -> Arrays.stream(d.getValue().get("HEADER").split(",")).map(dj -> d.getKey() + "." + di.getKey() + "." + dj))
                    .collect(Collectors.toList());
        } else {
            //行和を計算する場合のヘッダ処理
            if (d.getValue().get("SUM").equals("COLUMN")) {
                itemHeader = Arrays.stream(d.getValue().get("HEADER").split(","))
                        .map(dj -> d.getKey() + ".SUM("
                        + d.getValue().keySet().stream()
                                .filter(di -> di.charAt(0) != '#') //設定情報を読み込まない
                                .filter(di -> !di.equals("HEADER") && !di.equals("SUM"))
                                .collect(Collectors.joining(".")) + ")."
                        + dj).collect(Collectors.toList());
            } else {
                //列和を計算する場合のヘッダ処理
                itemHeader = d.getValue().keySet().stream()
                        .filter(di -> di.charAt(0) != '#') //設定情報を読み込まない
                        .filter(di -> !di.equals("HEADER") && !di.equals("SUM"))
                        .map(di -> d.getKey() + "."
                        + di + ".SUM("
                        + Arrays.stream(d.getValue().get("HEADER").split(","))
                                .collect(Collectors.joining(".")) + ")"
                        ).collect(Collectors.toList());
            }
        }

        //スコアのターゲットデータ用処理
        if (d.getValue().containsKey("#SCORE")) {
            itemHeader.stream().forEach(h -> {
                SCORE_TARGET.add(item + "." + h);
            });
        }

        return itemHeader;
    }

    //対象データキーの抽出
    @Override
    public Map<String, List<String>> extract(ESyaryoObject s) {
        Map<String, List<String>> map = USE_DATAKEYS.entrySet().stream()
                .collect(Collectors.toMap(
                        ld -> ld.getKey(),
                        ld -> ld.getValue().keySet().stream()
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
                            if (h != null) {
                                return inData(setting, h, s, d);
                            } else {
                                return outData(setting, h, s, d);
                            }
                        }).collect(Collectors.toList())
                ));
        return data;
    }

    private Stream<String> inData(Map<String, String> setting, List<String> h, ESyaryoObject s, String d) {
        List<String> setH = Arrays.asList(setting.get("HEADER").split(","));
        List<String> dataH = h.stream().map(hi -> hi.split("\\.")[1]).collect(Collectors.toList());

        if (!setting.containsKey("SUM")) {
            return nosum(setting, setH, dataH, s.a.get(d)).stream();
        } else {
            return sum(setting, setH, dataH, s.a.get(d)).stream();
        }
    }

    private Stream<String> outData(Map<String, String> setting, List<String> h, ESyaryoObject s, String d) {
        List<String> list = new ArrayList<>();
        if (d.equals("ベアリング寿命")) {
            CalculateBearingLife be = new CalculateBearingLife(s, HEADER_OBJ);
            list.add(be.life().toString());
        }
        return list.stream();
    }

    private String mask(String m, String ij) {
        return String.valueOf(Double.valueOf(m) * Double.valueOf(ij));
    }

    private List<String> nosum(Map<String, String> setting, List<String> setH, List<String> dataH, Map<String, List<String>> d) {
        return d.entrySet().stream()
                .filter(di -> setting.get(di.getKey()) != null ? true : setting.get("INDEX") != null)
                .flatMap(di
                        -> setH.stream()
                        .map(hi -> {
                            String[] set = setting.get("INDEX") != null ? setting.get("INDEX").split(",") : setting.get(di.getKey()).split(",");
                            return mask(set[setH.indexOf(hi)], di.getValue().get(dataH.indexOf(hi)));
                        })
                ).collect(Collectors.toList());
    }

    private List<String> sum(Map<String, String> setting, List<String> setH, List<String> dataH, Map<String, List<String>> d) {
        if (setting.get("SUM").equals("COLUMN")) {
            return setH.stream()
                    .map(hj
                            -> d.entrySet().stream()
                            .map(di -> mask(setting.get(di.getKey()).split(",")[setH.indexOf(hj)], di.getValue().get(dataH.indexOf(hj))))
                            .mapToDouble(di -> Double.valueOf(di)).sum())
                    .map(sum -> String.valueOf(sum))
                    .collect(Collectors.toList());
        } else {
            return setting.keySet().stream().filter(hi -> !hi.equals("HEADER") && !hi.equals("SUM"))
                    .map(hi
                            -> setH.stream()
                            .map(hj -> mask(setting.get(hi).split(",")[setH.indexOf(hj)], d.get(hi).get(dataH.indexOf(hj))))
                            .mapToDouble(dj -> Double.valueOf(dj)).sum())
                    .map(sum -> String.valueOf(sum))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Map<String, Double> normalize(ESyaryoObject s, Map<String, List<String>> data) {
        int smridx = 1;//HEADER_OBJ.getHeaderIdx("LOADMAP_DATE_SMR", "SMR"); //LOADMAP_DATE_SMR Value
        String date = s.a.get("LOADMAP_DATE_SMR") != null ? s.a.get("LOADMAP_DATE_SMR").keySet().stream().findFirst().get() : "-1";
        Double smr = Double.valueOf(s.a.get("LOADMAP_DATE_SMR") != null ? s.a.get("LOADMAP_DATE_SMR").values().stream().map(v -> v.get(smridx)).findFirst().get() : "-1");

        s.setDateSMR(date, smr.intValue());

        Map<String, Double> norm = new LinkedHashMap<>();
        data.entrySet().stream().forEach(e -> {
            _header.get(e.getKey()).stream().forEach(h -> {
                int i = _header.get(e.getKey()).indexOf(h);

                if (data.get(e.getKey()).isEmpty()) {
                    norm.put(e.getKey() + "." + h, -1d);
                } else {
                    if (s.a.get(h.split("\\.")[0]) != null) {
                        norm.put(e.getKey() + "." + h, Double.valueOf(e.getValue().get(i)) / smr); // /smr
                    } else {
                        norm.put(e.getKey() + "." + h, Double.valueOf(e.getValue().get(i)));
                    }
                }
            });
        });

        return norm;
    }

    @Override
    public void scoring() {
        //評価適用　無効
        if (!super.enable) {
            return;
        }

        Map<Integer, List<ESyaryoObject>> cids = new LinkedHashMap<>();

        //CIDで集計
        super._eval.values().stream().forEach(e -> {
            if (!e.none()) {
                if (cids.get(e.cid) == null) {
                    cids.put(e.cid, new ArrayList<>());
                }

                cids.get(e.cid).add(e);
            }
        });

        //cidごとの分割値の差分
        List<DataVector> cidavg = cidScore(cids);
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

    private List<DataVector> cidScore(Map<Integer, List<ESyaryoObject>> cids) {
        List<String> h = super.headers();

        //全体平均
        List<Double> avg = SCORE_TARGET.stream()
                .map(t -> cids.values().stream()
                .flatMap(v -> v.stream().map(vi -> vi.getPoint()[h.indexOf(t)]))
                .mapToDouble(cij -> cij).average().getAsDouble())
                .collect(Collectors.toList());
        //System.out.println("AVG:" + avg);

        //各CID平均
        Map<Integer, List<Double>> cidAvg = cids.entrySet().stream()
                .collect(Collectors.toMap(
                        cid -> cid.getKey(),
                        cid -> SCORE_TARGET.stream()
                                .map(t -> cid.getValue().stream()
                                .map(ci -> ci.getPoint()[h.indexOf(t)])
                                .mapToDouble(cij -> cij).average().getAsDouble())
                                .collect(Collectors.toList())
                ));
        //cidAvg.entrySet().stream().forEach(System.out::println);

        //CIDと平均の比率を計算
        List<DataVector> vec = cidAvg.entrySet().stream()
                .map(cid
                        -> new DataVector(cid.getKey(),
                        IntStream.range(0, cid.getValue().size()).boxed()
                                .mapToDouble(i -> cid.getValue().get(i) / avg.get(i))
                                .average().getAsDouble()))
                .collect(Collectors.toList());

        //System.out.println(vec);
        return vec;
    }

    @Override
    public Boolean check(ESyaryoObject s) {
        //LOADMAP_DATE_SMRのデータが存在しないとき評価しない
        return s.a.get("LOADMAP_DATE_SMR") == null;
    }
}
