/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package score;

import exception.AISTProcessException;
import score.cluster.ClusteringESyaryo;
import score.item.AgeSMREvaluate;
import score.item.EvaluateTemplate;
import score.item.MainteEvaluate;
import score.item.UseEvaluate;
import score.survive.SurvivalESyaryo;
import extract.SyaryoObjectExtract;
import file.CSVFileReadWrite;
import file.MapToJSON;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import py.PythonCommand;
import thread.ExecutableThreadPool;

/**
 *
 * @author ZZ17807
 */
public class SyaryoObjectEvaluation {

    private SyaryoObjectExtract exObj;

    public SyaryoObjectEvaluation(SyaryoObjectExtract exObj) {
        this.exObj = exObj;
    }

    public Map<String, String[]> scoring(Map mainteSettings, Map useSettings, Map agesmrSettings, String outPath) throws AISTProcessException {
        try {
            System.out.println("スコアリング開始");
            //メンテナンス分析
            EvaluateTemplate evalMainte = new MainteEvaluate(mainteSettings, exObj);

            //使われ方分析
            EvaluateTemplate evalUse = new UseEvaluate(useSettings, exObj.getHeader());

            //経年/SMR分析
            EvaluateTemplate evalAgeSMR = new AgeSMREvaluate(agesmrSettings, exObj);

            /**
             * スコアリング終了 メンテ　:　86230 使われ方　:　5569 経年　:　151826
             */
            long start = System.currentTimeMillis();
            Map sidsCounts = new ConcurrentHashMap();
            int tenP = (exObj.keySet().size() / 10) == 0 ? 1 : (exObj.keySet().size() / 10);
            ExecutableThreadPool.getInstance().threadPool.submit(()
                    -> exObj.keySet().parallelStream()
                            .peek(sid -> {
                                sidsCounts.put(sid, 0);
                                if (sidsCounts.size() % tenP == 0) {
                                    System.out.println("Scoring process :" + (10 * (sidsCounts.size() / tenP)) + "%");
                                }
                            })
                            .map(sid -> exObj.getAnalize(sid).toObj())
                            .forEach(s -> {
                                evalMainte.add(s);
                                evalUse.add(s);
                                evalAgeSMR.add(s);
                            })).get();
            long stop = System.currentTimeMillis();

            //クラスタリング
            ClusteringESyaryo.cluster(evalMainte._eval.values());
            ClusteringESyaryo.cluster(evalUse._eval.values());

            //スコアリング
            try {
                evalMainte.scoring();
                evalUse.scoring();
                evalAgeSMR.scoring();
            } catch (Exception e) {
                e.printStackTrace();
            }

            //故障解析
            System.out.println("故障解析");
            SurvivalESyaryo.acmfailure(evalMainte, evalUse, evalAgeSMR, outPath);

            //データ出力
            System.out.println("データ出力");
            print(evalMainte, outPath + "\\mainte_score.csv");
            MainteEvaluate.printImage(outPath + "\\mainte_score.csv", "AGE", "AVG", "SCORE");
            AgeSMREvaluate.printImage(outPath);
            print(evalUse, outPath + "\\use_score.csv");
            print(evalAgeSMR, outPath + "\\agesmr_score.csv");

            //スコアの集約
            Map<String, String[]> results = new LinkedHashMap();
            results.put("#HEADER", new String[]{"SID", "シナリオ", "類似度", "メンテナンス", "使われ方", "経年/SMR"});
            evalMainte._eval.keySet().stream().forEach(s -> {
                String[] d = new String[6];
                d[0] = s;
                d[1] = "0";
                d[2] = "0";
                d[3] = evalMainte._eval.get(s).score.toString();
                d[4] = evalUse._eval.get(s).score.toString();
                d[5] = evalAgeSMR._eval.get(s).score.toString();

                results.put(s, d);
            });

            //スコアリング結果の出力
            MapToJSON.toJSON(outPath + "\\scoring_results.json", results);

            System.out.println("スコアリング終了");
            System.out.println("　スコアリング　　:　" + (stop - start));

            return results;
        } catch (AISTProcessException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new AISTProcessException("スコアリングエラー");
        }
    }

    public Map<String, String> imageMap() {
        return SurvivalESyaryo.fileList;
    }

    public Map<String, List<String>> groupMap() {
        return SurvivalESyaryo.groupList;
    }

    public String compare(String[] pathScore) {
        IntStream.range(1, pathScore.length).forEach(i -> pathScore[i] = pathScore[i].replace(",", "_"));
        PythonCommand.py("py\\compare_score.py", pathScore);
        return pathScore[0] + "\\compare_score.png";
    }

    private static void print(EvaluateTemplate eval, String file) throws AISTProcessException {
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS(file)) {
            pw.println("SID,DATE,AGE,SMR," + eval._header.entrySet().stream()
                    .flatMap(h -> h.getValue().stream()
                    .map(hv -> h.getKey() + "_" + hv))
                    .collect(Collectors.joining(",")) + ",AVG,CID,SCORE");
            eval._eval.values().stream()
                    .map(s -> s.check())
                    .forEach(pw::println);
        }
    }

    //メンテナンスのみ
    /*private static void print(EvaluateTemplate evtemp, ESyaryoObject eval, MHeaderObject header) throws AISTProcessException {
        String file = "file\\test_print_eval_" + eval.name + ".csv";
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS(file)) {
            //評価結果
            pw.println("評価結果");
            pw.println("SID,DATE,AGE,SMR," + evtemp._header.entrySet().stream()
                    .flatMap(h -> h.getValue().stream()
                    .map(hv -> h.getKey() + "_" + hv))
                    .collect(Collectors.joining(",")) + ",CID");
            pw.println(eval.check());
            pw.println();

            //変換データ
            pw.println("評価SMR系列");
            pw.println("評価対象,インターバル");
            eval.data.entrySet().stream().map(e -> e.getKey() + "," + String.join(",", e.getValue())).forEach(pw::println);
            pw.println();

            //評価利用サービス
            pw.println("評価対象となったサービス群");
            pw.println("評価対象,SID,作番," + String.join(",", header.getHeader("部品")) + ",日付,SMR");
            eval.sv.entrySet().stream()
                    .flatMap(e -> e.getValue().stream()
                    .map(d -> e.getKey() + "," + d + "," + eval.a.getSBNToDate(d.split(",")[1], true) + "," + eval.a.getDateToSMR(eval.a.getSBNToDate(d.split(",")[1], true))))
                    .forEach(pw::println);
            pw.println();

            //評価に利用されなかったサービス
            pw.println("評価に利用されなかったサービス");
            pw.println("作番," + String.join(",", header.getHeader("部品")) + ",日付,SMR");
            if (eval.a.syaryo.getData("部品") != null) {
                eval.a.syaryo.getData("部品").entrySet().stream()
                        .filter(e -> !eval.sv.values().stream()
                        .flatMap(d -> d.stream().map(di -> di.split(",")[1]))
                        .filter(d -> d.equals(e.getKey())).findFirst().isPresent())
                        .map(e -> e.getKey() + "," + String.join(",", e.getValue()) + "," + eval.a.getSBNToDate(e.getKey(), true) + "," + eval.a.getDateToSMR(eval.a.getSBNToDate(e.getKey(), true)))
                        .forEach(pw::println);
            }
        }

        try {
            //excelファイルの生成
            DataConvertionUtil.csvToEXCEL(file, file.replace("csv", "xls"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }*/
}
