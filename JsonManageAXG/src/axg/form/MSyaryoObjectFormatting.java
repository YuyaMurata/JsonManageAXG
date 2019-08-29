/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.form;

import axg.form.item.FormAllSurpport;
import axg.form.item.FormDead;
import axg.form.item.FormDeploy;
import axg.form.item.FormKomtrax;
import axg.form.item.FormNew;
import axg.form.item.FormOrder;
import axg.form.item.FormOwner;
import axg.form.item.FormParts;
import axg.form.item.FormProduct;
import axg.form.item.FormSMR;
import axg.form.item.FormUsed;
import axg.form.item.FormWork;
import axg.form.rule.DataRejectRule;
import axg.obj.MHeaderObject;
import axg.obj.MSyaryoObject;
import file.MapToJSON;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import mongodb.MongoDBCleansingData;

/**
 *
 * @author zz17390
 */
public class MSyaryoObjectFormatting {
    //本社コード
    private static Map<String, String> honsyIndex = new MapToJSON().toMap("settings\\generator\\index\\honsya_index.json");
    //生産日情報
    private static Map<String, String> productIndex = new MapToJSON().toMap("settings\\generator\\index\\product_index.json");
    
    private static DecimalFormat df = new DecimalFormat("00");

    //public static List<String> w = new ArrayList<>();
    //public static List<String> p = new ArrayList<>();

    //public static String currentKey = "";
    
    public static void form(String db, String collection){
        Long start = System.currentTimeMillis();
        
        MongoDBCleansingData shuffleDB = MongoDBCleansingData.create();
        shuffleDB.set(db, collection+"_Clean", MSyaryoObject.class);
        MongoDBCleansingData formDB = MongoDBCleansingData.create();
        formDB.set(db, collection+"_Form", MSyaryoObject.class);
        
        MHeaderObject header = shuffleDB.getHeader();
        formDB.coll.insertOne(header);
        
        shuffleDB.getKeyList().stream().forEach(sid ->{
            MSyaryoObject obj = formDB.getObj(sid);
            formOne(header, obj);
            obj.recalc();
            formDB.coll.insertOne(obj);
        });
        
        long stop = System.currentTimeMillis();
        System.out.println("FormattingTime="+(stop-start)+"ms");

        shuffleDB.close();
        formDB.close();
    }

    private static void formOne(MHeaderObject header, MSyaryoObject syaryo) {
        //整形時のデータ削除ルールを設定
        DataRejectRule rule = new DataRejectRule();

        //キーの整形
        formKey(syaryo);

        //生産の整形
        syaryo.setData("生産", FormProduct.form(syaryo.getData("生産"), productIndex, syaryo.getName()));

        //出荷情報の整形
        syaryo.setData("出荷", FormDeploy.form(syaryo.getData("出荷"), syaryo.getDataKeyOne("生産"), syaryo.getName()));

        //顧客の整形  経歴の利用方法の確認
        syaryo.setData("顧客", FormOwner.form(syaryo.getData("顧客"), header.getIndex("顧客"), honsyIndex, rule));

        //新車の整形
        syaryo.setData("新車", FormNew.form(syaryo.getData("新車"), syaryo.getData("生産"), syaryo.getData("出荷"), header.getIndex("新車")));
        rule.addNew(syaryo.getDataKeyOne("新車"));
        
        //中古車の整形  // U Nが残っているためそれを利用した処理に変更
        syaryo.setData("中古車", FormUsed.form(syaryo.getData("中古車"), header.getIndex("中古車"), rule.getNew()));
        
        //受注
        syaryo.setData("受注", FormOrder.form(syaryo.getData("受注"), header.getIndex("受注"), rule));

        List sbnList = null;
        if (syaryo.getData("受注") != null) {
            sbnList = new ArrayList(syaryo.getData("受注").keySet());
        }
        
        //廃車
        syaryo.setData("廃車", FormDead.form(syaryo.getData("廃車"), rule.currentDate, header.getIndex("廃車")));
        
        //作業
        syaryo.setData("作業", FormWork.form(syaryo.getData("作業"), sbnList, header.getIndex("作業"), rule.getWORKID()));

        //部品
        syaryo.setData("部品", FormParts.form(syaryo.getData("部品"), sbnList, header.getIndex("部品"), rule.getPARTSID()));

        //SMR
        syaryo.setData("SMR", FormSMR.form(syaryo.getData("SMR"), header.getIndex("SMR"), syaryo.getName().split("-")[3]));
        
        //AS 解約、満了情報が残っているため修正
        syaryo.setData("オールサポート", FormAllSurpport.form(syaryo.getData("オールサポート"), header.getIndex("オールサポート")));
        
        //Komtrax 紐づいていないことを考慮する
        FormKomtrax.form(syaryo);
       
        removeEmptyObject(syaryo);
    }

    //キーをまとめて整形
    private static void formKey(MSyaryoObject syaryo) {
        Map<String, Map<String, List<String>>> map = syaryo.getMap();
        map.keySet().stream().forEach(k -> {
            Map m = map.get(k).entrySet().stream()
                    .filter(e -> !e.getKey().replace(" ", "").equals(""))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            
            syaryo.setData(k, m);
        });
        
        //日付修正
        Map<String, Map<String, List<String>>> dmap = syaryo.getMap();
        dmap.keySet().stream().forEach(k -> {
            Map m = new TreeMap();
            
            dmap.get(k).entrySet().stream().forEach(d ->{
                m.put(dup(dateFormalize(d.getKey()), m), d.getValue());
            });
            
            syaryo.setData(k, m);
        });
    }

    //重複して並んでいるIDを重複除去
    public static List exSeqDuplicate(List<String> dupList) {
        List list = new ArrayList();
        String tmp = "";
        for (String el : dupList) {
            if (tmp.equals(el)) {
                continue;
            }
            tmp = el;
            list.add(tmp);
        }

        return list;
    }

    //日付をまとめて整形　2018/08/09 -> 20180809
    public static String dateFormalize(String date) {
        //日付の場合は整形
        if(date.contains("/"))
            date = date.split("#")[0].split(" ")[0].replace("/", "").substring(0, 8);
        
        return date;    
    }

    //空の情報を削除
    public static void removeEmptyObject(MSyaryoObject syaryo) {
        Map map = syaryo.getMap().entrySet().stream()
                            .filter(e -> !e.getValue().isEmpty())
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        syaryo.setMap(map);
    }

    //日付が重複する場合に連番を付与　20180809が2つ登場したとき-> 20180809#0001
    public static String dup(String key, Map map) {
        int cnt = 0;
        String k = key;
        while (map.get(k) != null) {
            k = key + "#" + df.format(++cnt);
        }
        return k;
    }
}
