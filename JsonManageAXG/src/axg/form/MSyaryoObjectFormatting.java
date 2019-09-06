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
import obj.MHeaderObject;
import obj.MSyaryoObject;
import file.MapToJSON;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import mongodb.MongoDBPOJOData;

/**
 *
 * @author zz17390
 */
public class MSyaryoObjectFormatting {
    //本社コード
    private static Map<String, String> honsyIndex = new MapToJSON().toMap("settings\\index\\honsya_index.json");
    //生産日情報
    private static Map<String, String> productIndex = new MapToJSON().toMap("settings\\index\\product_index.json");
    
    private static DecimalFormat df = new DecimalFormat("00");

    //public static List<String> w = new ArrayList<>();
    //public static List<String> p = new ArrayList<>();

    //public static String currentKey = "";
    
    public static void main(String[] args) {
        form("json", "komatsuDB_PC200");
    }
    
    public static void form(String db, String collection){
        Long start = System.currentTimeMillis();
        
        MongoDBPOJOData shuffleDB = MongoDBPOJOData.create();
        shuffleDB.set(db, collection+"_Shuffle", MSyaryoObject.class);
        MongoDBPOJOData formDB = MongoDBPOJOData.create();
        formDB.set(db, collection+"_Form", MSyaryoObject.class);
        formDB.clear();
        
        MHeaderObject header = shuffleDB.getHeader();
        formDB.coll.insertOne(header);
        
        shuffleDB.getKeyList().parallelStream()
                .map(sid -> formOne(header, shuffleDB.getObj(sid)))
                .forEach(formDB.coll::insertOne);
        
        long stop = System.currentTimeMillis();
        System.out.println("FormattingTime="+(stop-start)+"ms");

        shuffleDB.close();
        formDB.close();
    }

    private static MSyaryoObject formOne(MHeaderObject header, MSyaryoObject obj) {
        //整形時のデータ削除ルールを設定
        DataRejectRule rule = new DataRejectRule();

        //キーの整形
        Long start = System.currentTimeMillis();
        formKey(obj);
        Long keyst = System.currentTimeMillis();
        
        //生産の整形
        obj.setData("生産", FormProduct.form(obj.getData("生産"), productIndex, obj.getName()));
        Long pst = System.currentTimeMillis();
        
        //出荷情報の整形
        obj.setData("出荷", FormDeploy.form(obj.getData("出荷"), obj.getDataKeyOne("生産"), obj.getName()));
        Long depst = System.currentTimeMillis();
        
        //顧客の整形  経歴の利用方法の確認
        obj.setData("顧客", FormOwner.form(obj.getData("顧客"), header.getHeader("顧客"), honsyIndex, rule));
        Long owst = System.currentTimeMillis();
        
        //新車の整形
        obj.setData("新車", FormNew.form(obj.getData("新車"), obj.getData("生産"), obj.getData("出荷"), header.getHeader("新車")));
        rule.addNew(obj.getDataKeyOne("新車"));
        Long newst = System.currentTimeMillis();
        
        //中古車の整形  // U Nが残っているためそれを利用した処理に変更
        obj.setData("中古車", FormUsed.form(obj.getData("中古車"), header.getHeader("中古車"), rule.getNew()));
        Long usst = System.currentTimeMillis();
        
        //受注
        obj.setData("受注", FormOrder.form(obj.getData("受注"), header.getHeader("受注"), rule));
        Long odst = System.currentTimeMillis();
        
        List sbnList = null;
        if (obj.getData("受注") != null) {
            sbnList = new ArrayList(obj.getData("受注").keySet());
        }
        
        //廃車
        obj.setData("廃車", FormDead.form(obj.getData("廃車"), rule.currentDate, header.getHeader("廃車")));
        Long deadst = System.currentTimeMillis();
        
        //作業
        obj.setData("作業", FormWork.form(obj.getData("作業"), sbnList, header.getHeader("作業"), rule.getWORKID()));
        Long wst = System.currentTimeMillis();
        
        //部品
        obj.setData("部品", FormParts.form(obj.getData("部品"), sbnList, header.getHeader("部品"), rule.getPARTSID()));
        Long ptst = System.currentTimeMillis();
        
        //SMR
        obj.setData("SMR", FormSMR.form(obj.getData("SMR"), header.getHeader("SMR"), obj.getName().split("-")[3]));
        Long smrst = System.currentTimeMillis();
        
        //AS 解約、満了情報が残っているため修正
        obj.setData("オールサポート", FormAllSurpport.form(obj.getData("オールサポート"), header.getHeader("オールサポート")));
        Long asst = System.currentTimeMillis();
        
        //Komtrax 紐づいていないことを考慮する
        FormKomtrax.form(obj);
        Long kmst = System.currentTimeMillis();
        
        //空データは削除
        removeEmptyObject(obj);
        Long empst = System.currentTimeMillis();
        
        obj.recalc();
        
        Long stop = System.currentTimeMillis();
        System.out.println(obj.getName()+":"+(stop-start)+"ms["+(keyst-start)+","+(pst-keyst)+","+(depst-pst)+","+(owst-depst)
                +","+(newst-owst)+","+(usst-newst)+","+(odst-usst)+","+(deadst-odst)+","+(wst-deadst)+","+(ptst-wst)+","+(smrst-ptst)+","+(asst-smrst)+","+(kmst-asst)+","+(empst-kmst)+"]");
        
        return obj;
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
