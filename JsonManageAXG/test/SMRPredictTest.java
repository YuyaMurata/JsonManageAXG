
import analizer.MSyaryoAnalizer;
import exception.AISTProcessException;
import extract.SyaryoObjectExtract;
import java.util.ArrayList;
import java.util.List;
import obj.MHeaderObject;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author kaeru
 */
public class SMRPredictTest {
    public static void main(String[] args) throws AISTProcessException {
        //データ取得
        SyaryoObjectExtract objex = new SyaryoObjectExtract("json", "KM_PC200_DB_P");
        
        //ユーザー定義ファイルの設定
        objex.setUserDefine("KM_PC200_DB_P\\config\\user_define_車両除外無し.json");
        objex.getSummary();
        
        MHeaderObject h = objex.getHeader();
        
        MSyaryoAnalizer a = objex.getAnalize("PC200-8--300206");
        System.out.println(a.getDateToSMR("20170115"));
        
        List<String> list = new ArrayList(a.smrDate.values());
        a.smrDate.entrySet().stream()
                .map(e -> list.indexOf(e.getValue())+" - "+e.getValue()+":"+e.getKey())
                .forEach(System.out::println);
        
    }
}
