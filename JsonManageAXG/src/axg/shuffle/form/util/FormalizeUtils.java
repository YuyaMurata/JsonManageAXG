/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.shuffle.form.util;

import static com.mongodb.client.model.Filters.eq;
import exception.AISTProcessException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import mongodb.MongoDBPOJOData;

/**
 *
 * @author ZZ17807
 */
public class FormalizeUtils {

    //startからstopまでの経過日数計算
    public static Integer dsub(String start, String stop) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            Date st = sdf.parse(start);
            Date sp = sdf.parse(stop);
            Long age = (sp.getTime() - st.getTime()) / (1000 * 60 * 60 * 24);

            if (age == 0L) {
                age = 1L;
            }

            return age.intValue();
        } catch (ParseException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    //日付が重複する場合に連番を付与　20180809が2つ登場したとき-> 20180809#0001
    public static String dup(String key, Map map) {
        DecimalFormat df = new DecimalFormat("0000");

        int cnt = 0;
        String k = key;
        while (map.get(k) != null) {
            k = key + "#" + df.format(++cnt);
        }
        return k;
    }

    //重複して並んでいるIDを重複除去 S001,S002,S002,S001 -> S001,S002,S001
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

    //日付を整形　2018/08/09 00:00:00#0 -> 20180809
    public static String dateFormalize(String date) {
        //日付の場合は整形
        SimpleDateFormat sdfout = new SimpleDateFormat("yyyyMMdd");
        try {
            if(date.isEmpty()){}
            else if (date.contains("/")) {
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/dd");
                Date d = sdf1.parse(date.split("#")[0]);
                date = sdfout.format(d);
            } else if (date.contains("-")) {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
                Date d = sdf2.parse(date.split("#")[0]);
                date = sdfout.format(d);
            }
        } catch (ParseException ex) {
            System.err.println("整形エラー：日付変換エラー");
        }
        return date;
    }

    public static void createFormInfo(FormInfoMap info) {
        String dbn = "info";
        String col = "DB_FormInfo";

        MongoDBPOJOData db = MongoDBPOJOData.create();
        db.set(dbn, col, FormInfoMap.class);

        db.coll.deleteOne(eq("name", info.getName()));
        db.coll.insertOne(info);
    }

    public static FormInfoMap getFormInfo(String dbcol) throws AISTProcessException {
        String dbn = "info";
        String col = "DB_FormInfo";

        MongoDBPOJOData db = MongoDBPOJOData.create();
        db.set(dbn, col, FormInfoMap.class);

        return (FormInfoMap) db.coll.find(eq("name", dbcol)).first();
    }

    public static Boolean checkDateKey(String headerName) {
        if (headerName.contains("日") || headerName.contains("時")) {
            return true;
        } else if (headerName.contains("DATE") || headerName.contains("TIME")) {
            return true;
        } else {
            return false;
        }
    }
}
