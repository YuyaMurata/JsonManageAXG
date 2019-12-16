/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.util;

import static eval.SyaryoObjectEvaluation.db;
import eval.analizer.MSyaryoAnalizer;
import eval.obj.ESyaryoObject;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/**
 *
 * @author kaeru
 */
public class CalculateBearingLife {
    Double[][] engine;
    Double[][] temprature;
    
    MHeaderObject header;
    ESyaryoObject syaryo;
    
    public CalculateBearingLife(ESyaryoObject s, MHeaderObject h){
        this.header = h;
        this.syaryo = s;
        //エンジン回転数VSトルク
        engine = toMatrix("LOADMAP_実エンジン回転VSエンジントルク");
        //エンジン水温VS作動
        temprature = toMatrix("LOADMAP_エンジン水温VS作動油温");
        
        //System.out.println(s.a.get("LOADMAP_DATE_SMR"));
    }
    
    private Double[][] toMatrix(String dkey){
        Map<String, List<String>> data = syaryo.a.get(dkey);
        List<String> h = header.getHeader(dkey);
        
        Double[][] d = new Double[data.size()+2][h.size()+2];
        Arrays.stream(d).forEach(di -> Arrays.fill(di, 0d));
        
        List<String> rowID = header.getHeader(dkey).stream()
                            .sorted(Comparator.comparing(hi -> Double.valueOf(hi.split("\\.")[1].replace("_", ""))))
                            .collect(Collectors.toList());
        List<String> colID = data.keySet().stream()
                            .sorted(Comparator.comparing(di -> Double.valueOf(di.replace("_", ""))))
                            .collect(Collectors.toList());
        
        rowID.stream().forEach(row -> d[0][rowID.indexOf(row) + 1] = Double.valueOf(row.split("\\.")[1].replace("_", "")));
        d[0][rowID.size()] += 1d;
        
        data.entrySet().stream().forEach(e -> {
            int col = colID.indexOf(e.getKey()) + 1;
            d[col][0] = Double.valueOf(e.getKey().replace("_", ""));
            h.stream().forEach(hi -> d[col][rowID.indexOf(hi)+1] = Double.valueOf(e.getValue().get(h.indexOf(hi))));
            d[col][h.size()+1] = Arrays.stream(d[col]).skip(1).mapToDouble(dcj -> dcj).sum();
            IntStream.range(1, d[col].length).boxed().forEach(i -> d[d.length-1][i] += d[col][i]);
        });
        d[colID.size()][0] += 1d;
        
        //printMatrix(d);
        
        return d;
    }
    
    public Double life(){
        //平均回転
        Double avgR = IntStream.range(1, engine.length-1).boxed()
                                    .mapToDouble(i -> engine[i][0] * engine[i][engine[0].length-1] / engine[engine.length-1][engine[0].length-1])
                                    .sum();
        //System.out.println(avgR);
        
        //平均1ポンプトルク
        Double avgT = IntStream.range(1, engine[0].length-1).boxed()
                                    .mapToDouble(j -> engine[0][j] * engine[engine.length-1][j] / engine[engine.length-1][engine[0].length-1])
                                    .sum() / 2;
        //System.out.println(avgT);
        
        //ポンプ全ラジアル荷重
        Double radial = Math.PI * avgT / 102 * Math.pow(10, 3);
        //System.out.println(radial);
        
        //ニードルBrg.荷重
        Double needleBrg = 75.5d / (75.5d + 95.5) * radial;
        //System.out.println(needleBrg);
        
        Double lifeBrg = (Math.pow(10, 6) / (60 * avgR)) * Math.pow((4850 / needleBrg), 3.33) * L() * 4.5 * 0.8 * 0.62;
        System.out.println(lifeBrg);
        
        return lifeBrg;
    }
    
    private Double L(){
        //=IF(AD62<80,0,IF(AD62<100,-0.03*AD62+4.5,IF(AD62<110,-0.05*AD62+6.5,IF(AD62<120,-0.076*AD62+9.36,-0.006*AD62+0.96))))
        Double temp = IntStream.range(0, temprature[0].length).boxed()
                                    .filter(j -> temprature[0][j] > 0d)
                                    .filter(j -> temprature[temprature.length-1][j] > 0d)
                .map(j -> temprature[0][j])
                .reduce((a,b) -> b).orElse(null);
        Double lublicator = temp < 80  ? 0 :
                            temp < 100 ? -0.03*temp+4.5 :
                            temp < 110 ? -0.05*temp+6.5 :
                            temp < 120 ? -0.076*temp+9.36:
                            -0.006*temp+0.96;
        
        System.out.println(temp+","+lublicator);
        
        return lublicator;
    }
    
    private void printMatrix(Double[][] d){
        Arrays.stream(d).map(di -> Arrays.asList(di).toString())
                .forEach(System.out::println);
    }
    
    public static void main(String[] args){
        MongoDBPOJOData db = MongoDBPOJOData.create();
        db.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);
        
        List<MSyaryoObject> slist = db.getObjMap().values().stream()
                    .filter(s -> s.getData("LOADMAP_実エンジン回転VSエンジントルク")!=null)
                    .filter(s -> s.getName().equals("PC200-10- -454756"))
                    .collect(Collectors.toList());
        
        MSyaryoAnalizer.initialize(db.getHeader(), db.getObjMap());
        List<ESyaryoObject> elist = slist.stream().map(s -> new ESyaryoObject(new MSyaryoAnalizer(s))).collect(Collectors.toList());
        
        CalculateBearingLife be = new CalculateBearingLife(elist.get(0), db.getHeader());
        be.life();
    }
}
