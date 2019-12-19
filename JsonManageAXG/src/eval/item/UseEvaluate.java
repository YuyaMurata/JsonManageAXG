/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.item;

import eval.analizer.MSyaryoAnalizer;
import eval.cluster.DataVector;
import eval.obj.ESyaryoObject;
import eval.util.CalculateBearingLife;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import obj.MHeaderObject;

/**
 *
 * @author ZZ17390
 */
public class UseEvaluate extends EvaluateTemplate {

    private Map<String, Map<String, Map<String, String>>> USE_DATAKEYS;
    private MHeaderObject HEADER_OBJ;
    private String TARGET = "";

    public UseEvaluate(Map settings, MHeaderObject h) {
        super.enable = ((Map<String, String>) settings).get("#EVALUATE").equals("ENABLE");
        TARGET = ((Map<String, String>) settings).get("#SCORE_TARGET");
        settings.remove("#EVALUATE");
        settings.remove("#SCORE_TARGET");

        USE_DATAKEYS = (Map<String, Map<String, Map<String, String>>>) settings;
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

        //LOADMAPデータが存在しない場合は評価しない
        if (sa.get("LOADMAP_DATE_SMR") == null) {
            s.setData();
            return s;
        }

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
        int smridx = 1; //LOADMAP_DATE_SMR Value
        String date = s.a.get("LOADMAP_DATE_SMR") != null ? s.a.get("LOADMAP_DATE_SMR").keySet().stream().findFirst().get() : "-1";
        Double smr = Double.valueOf(s.a.get("LOADMAP_DATE_SMR") != null ? s.a.get("LOADMAP_DATE_SMR").values().stream().map(v -> v.get(smridx)).findFirst().get() : "-1");

        s.setDateSMR(date, smr.intValue());

        Map<String, Double> norm = new LinkedHashMap<>();
        data.entrySet().stream().forEach(e -> {
            _header.get(e.getKey()).stream().forEach(h -> {
                int i = _header.get(e.getKey()).indexOf(h);
                //System.out.println("  "+h+"["+i+"]:"+e.getValue().get(i));

                if (data.get(e.getKey()).isEmpty()) {
                    norm.put(e.getKey() + "." + h, -1d);
                } else {
                    norm.put(e.getKey() + "." + h, Double.valueOf(e.getValue().get(i)) / smr); // /smr
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
            if (e.none()) {
                e.score = 0;
            } else {
                if (cids.get(e.cid) == null) {
                    cids.put(e.cid, new ArrayList<>());
                }

                cids.get(e.cid).add(e);
            }
        });

        //cidごとの分割値の差分
        List<DataVector> cidavg = cidScore(cids);

        //スコアリング用にデータを3分割
        /*List<CentroidCluster<DataVector>> splitor = ClusteringESyaryo.splitor(cidavg);
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
        });*/
    }

    private List<DataVector> cidScore(Map<Integer, List<ESyaryoObject>> cids) {
        String item = TARGET.split("\\.")[0];
        String dkey = TARGET.split("\\.")[1];

        //評価方法の判定
        cids.entrySet().stream()
                .map(cid
                        -> new DataVector(cid.getKey(),
                        cid.getValue().stream()
                                .mapToDouble(e -> {
                                    int s = e.norm.size();
                                    //double l = 0;//IntStream.range(0, s/2).mapToDouble(i -> e.getPoint()[i]).sum();
                                    //double r = IntStream.range(3 * s / 4, s).mapToDouble(i -> e.getPoint()[i]).sum();
                                    return e.getPoint()[0];
                                })
                                .average().getAsDouble()))
                .collect(Collectors.toList());

        return null;
    }
}
