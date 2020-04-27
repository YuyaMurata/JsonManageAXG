/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle.form;

import axg.shuffle.form.item.FormAllSurpport;
import axg.shuffle.form.item.FormDead;
import axg.shuffle.form.item.FormDeploy;
import axg.shuffle.form.item.FormKomtrax;
import axg.shuffle.form.item.FormLoadMap;
import axg.shuffle.form.item.FormNew;
import axg.shuffle.form.item.FormOrder;
import axg.shuffle.form.item.FormOwner;
import axg.shuffle.form.item.FormParts;
import axg.shuffle.form.item.FormProduct;
import axg.shuffle.form.item.FormSMR;
import axg.shuffle.form.item.FormUsed;
import axg.shuffle.form.item.FormWork;
import axg.shuffle.form.rule.DataRejectRule;
import axg.shuffle.form.util.FormInfoMap;
import thread.ExecutableThreadPool;
import axg.shuffle.form.util.FormalizeUtils;
import exception.AISTProcessException;
import java.util.HashMap;
import obj.MHeaderObject;
import obj.MSyaryoObject;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;

/**
 *
 * @author zz17390
 */
public class MSyaryoObjectFormatting {

    public static void form(String db, String collection) throws AISTProcessException {
        Long start = System.currentTimeMillis();

        //シャッフリング用 Mongoコレクションの読み込み
        MongoDBPOJOData shuffleDB = MongoDBPOJOData.create();
        shuffleDB.set(db, collection + "_Shuffle", MSyaryoObject.class);

        //header
        MHeaderObject header = shuffleDB.getHeader();

        //整形用 Mongoコレクションを生成
        MongoDBPOJOData formDB = MongoDBPOJOData.create();
        formDB.set(db, collection + "_Form", MSyaryoObject.class);
        formDB.clear();
        formDB.coll.insertOne(header);

        try {
            //整形実行
            ExecutableThreadPool.getInstance().getPool().submit(()
                    -> shuffleDB.getKeyList().parallelStream()
                            .map(sid -> formOne(header, (MSyaryoObject) shuffleDB.getObj(sid)))
                            .forEach(formDB.coll::insertOne)).get();
        } catch (InterruptedException | ExecutionException ex) {
            System.err.println("整形エラー");
            ex.printStackTrace();
            throw new AISTProcessException("整形エラー");
        }

        //整形情報の登録
        setFormInfo(formDB, db + "." + collection + "_Form");

        long stop = System.currentTimeMillis();
        System.out.println("FormattingTime=" + (stop - start) + "ms");

        //インデックスの作成
        formDB.createIndexes();
        formDB.close();

        //中間コレクションの削除
        shuffleDB.clear();
        shuffleDB.close();
    }

    //1台の整形
    private static MSyaryoObject formOne(MHeaderObject header, MSyaryoObject obj) {
        //整形時のデータ削除ルールを設定
        DataRejectRule rule = new DataRejectRule();

        //キーの整形
        formKey(obj, header);

        //生産の整形
        obj.setData("生産", FormProduct.form(obj.getData("生産"), obj.getName()));

        //出荷情報の整形
        obj.setData("出荷", FormDeploy.form(obj.getData("出荷"), obj.getDataKeyOne("生産"), obj.getName()));

        //顧客の整形
        obj.setData("顧客", FormOwner.form(obj.getData("顧客"), header.getHeader("顧客"), rule));

        //新車の整形
        obj.setData("新車", FormNew.form(obj.getData("新車"), obj.getData("生産"), obj.getData("出荷"), header.getHeader("新車")));
        rule.addNew(obj.getDataKeyOne("新車"));

        //中古車の整形
        obj.setData("中古車", FormUsed.form(obj.getData("中古車"), header.getHeader("中古車"), rule.getNew()));

        //受注の整形
        obj.setData("受注", FormOrder.form(obj.getData("受注"), header.getHeader("受注"), rule));

        //廃車の整形
        obj.setData("廃車", FormDead.form(obj.getData("廃車"), rule.currentDate, header.getHeader("廃車")));

        //作業の整形
        obj.setData("作業", FormWork.form(obj.getData("作業"), rule.sbnList, header.getHeader("作業")));

        //部品の整形
        obj.setData("部品", FormParts.form(obj.getData("部品"), rule.sbnList, header.getHeader("部品")));

        //SMRの整形
        obj.setData("SMR", FormSMR.form(obj.getData("SMR"), header.getHeader("SMR"), obj.getName().split("-")[3]));

        //ASの整形
        obj.setData("オールサポート", FormAllSurpport.form(obj.getData("オールサポート"), header.getHeader("オールサポート")));

        //Komtraxの整形
        FormKomtrax.form(obj, header);

        //Komtraxの整形
        FormLoadMap.form(obj, header);

        //空データは削除
        removeEmptyObject(obj);

        obj.recalc();

        return obj;
    }

    //キーをまとめて整形
    private static void formKey(MSyaryoObject syaryo, MHeaderObject h) {
        Map<String, Map<String, List<String>>> map = syaryo.getMap();
        map.keySet().stream().forEach(k -> {
            Map m = map.get(k).entrySet().stream()
                    .filter(e -> !e.getKey().replace(" ", "").equals(""))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

            syaryo.setData(k, m);
        });

        //日付修正
        Map<String, Map<String, List<String>>> dmap = syaryo.getMap();
        dmap.keySet().stream().filter(k -> FormalizeUtils.checkDateKey(h.mapSubkey.get(k))).forEach(k -> {
            Map m = new TreeMap();
            
            dmap.get(k).entrySet().stream().forEach(d -> {
                try {
                    m.put(FormalizeUtils.dup(FormalizeUtils.dateFormalize(d.getKey()), m), d.getValue());
                } catch (Exception e) {
                    System.err.println(h.mapSubkey.get(k));
                    System.err.println(k+":"+d);
                    e.printStackTrace();
                    System.exit(0);
                }
            });

            syaryo.setData(k, m);
        });
    }

    //空の情報を削除
    public static void removeEmptyObject(MSyaryoObject syaryo) {
        Map map = syaryo.getMap().entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        syaryo.setMap(map);
    }

    //整形情報の登録
    public static FormInfoMap setFormInfo(MongoDBPOJOData formDB, String dbcoll) throws AISTProcessException {
        try {
            int idx = formDB.getHeader().getHeaderIdx("受注", "作業完了日");
            //整形実行
            Integer maxLeastDate
                    = ExecutableThreadPool.getInstance().getPool().submit(()
                            -> formDB.getKeyList().parallelStream()
                            .map(sid -> (MSyaryoObject) formDB.getObj(sid))
                            .filter(obj -> obj.getData("KOMTRAX_SMR") != null)
                            .filter(obj -> obj.getData("受注") != null)
                            .flatMap(obj -> obj.getData("受注").values().stream())
                            .mapToInt(odr -> Integer.valueOf(odr.get(idx))).max().getAsInt()).get();

            Map infoMap = new HashMap();
            infoMap.put("MAX_LEAST_DATE", maxLeastDate.toString());
            infoMap.put("MACHINE_KIND", formDB.getKeyList().get(0).split("-")[0]);

            FormInfoMap info = new FormInfoMap(dbcoll, infoMap);
            FormalizeUtils.createFormInfo(info);

            return info;
        } catch (InterruptedException | ExecutionException ex) {
            System.err.println("KOMTRAX_SMR、受注情報が紐づいていません．");
            ex.printStackTrace();
            throw new AISTProcessException("整形データからの受注最大日付の取得エラー");
        }
    }

}
