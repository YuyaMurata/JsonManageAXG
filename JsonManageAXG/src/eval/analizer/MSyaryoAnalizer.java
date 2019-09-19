/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.analizer;

import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.AbstractMap;
import java.util.Comparator;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17390
 */
public class MSyaryoAnalizer{
    public MSyaryoObject syaryo;
    public String kind = "";
    public String type = "";
    public String no = "";
    public String mcompany = "";
    public Boolean used = false;
    public Boolean komtrax = false;
    public Boolean allsupport = false;
    public String lifedead = "";
    public String lifestart = "";
    public String lifestop = "";
    public List<String> usedlife = null;
    public Integer numOwners = -1;
    public Integer numOrders = -1;
    public Integer numParts = -1;
    public Integer numWorks = -1;
    public Integer acmLCC = -1;
    public Integer[] cluster = new Integer[3];
    public TreeMap<String, Map.Entry<Integer, Integer>> ageSMR = new TreeMap<>();
    public TreeMap<Integer, String> smrDate = new TreeMap<>();
    private int D_DATE = 365;
    private int D_SMR = 10;
    private List<String[]> termAllSupport;

    public static Boolean DISP_COUNT = true;
    
    //初期設定
    private static MongoDBPOJOData db;
    private static MHeaderObject header;

    private static int CNT = 0;
    
    public static void initialize(MongoDBPOJOData mdb){
        db = mdb;
        header = db.getHeader();
    }
    
    public MSyaryoAnalizer(String name) {
        CNT++;
        this.syaryo = db.getObj(name);

        //設定
        settings();

        if (CNT % 1000 == 0 && DISP_COUNT) {
            System.out.println(CNT + " Trans SyaryoAnalizer");
        }
    }

    public MSyaryoObject get() {
        MSyaryoObject s = syaryo;
        return s;
    }

    public Map<String, List<String>> get(String key) {
        return syaryo.getData(key);
    }

    private void settings() {
        //Name
        this.kind = syaryo.getName().split("-")[0];
        this.type = syaryo.getName().split("-")[1]+syaryo.getName().split("-")[2].replace(" ", "");
        this.no = syaryo.getName().split("-")[3];
        
        //Status
        setLifeStatus();
        setSpecStatus();
        setOwnerStatus();
        setServiceStatus();
    }
    
    private void setSpecStatus(){
        //車両マスタのKOMTRAX列は利用しない
        if(get("KOMTRAX_SMR") != null)
            komtrax = true;
        
        //KUECでの中古売買のみ
        if(get("中古") != null){
            used = true;
            usedlife = new ArrayList<>(get("中古").keySet());
        }
        
        //オールサポート
        if(get("オールサポート") != null){
            allsupport = true;
            termAllSupport = get("オールサポート").entrySet().stream()
                                .map(e -> new String[]{e.getKey(), e.getValue().get(header.getHeaderIdx("オールサポート", "オールサポート.契約満了日"))})
                                .collect(Collectors.toList());
        }
    }
    
    private void setLifeStatus(){
        //納入日
        lifestart = get("新車").keySet().stream().findFirst().get();
        
        //最終確認日
        if(get("KOMTRAX_SMR") != null)
            lifestop = get("KOMTRAX_SMR").keySet().stream()
                            .sorted(Comparator.comparing(d -> Integer.valueOf(d), Comparator.reverseOrder()))
                            .findFirst().get();
        else if(get("受注") != null)
            lifestop = getValue("受注", "受注.作業完了日", true).get(0);
        else
            lifestop = "-1";
        
        //lifedead
        if(get("廃車") != null)
            lifedead = get("廃車").keySet().stream().findFirst().get();
        else
            lifedead = "-1";
    }
    
    private void setServiceStatus(){
        if(get("受注") != null){
            //受注情報
            numOrders = get("受注").size();
            acmLCC = getValue("受注", "受注.請求金額", false).parallelStream().mapToInt(p -> Integer.valueOf(p)).sum();
            sbnDate = get("受注").entrySet().parallelStream()
                    .collect(Collectors.toMap(
                            e -> e.getKey(),
                            e -> e.getValue().get(header.getHeaderIdx("受注", "受注.作業完了日")),
                            (e1, e2) -> e1,
                            TreeMap::new)
                    );
            
            //作業情報
            if(get("作業") != null)
                numWorks = get("作業").size();
            
            //部品情報
            if(get("部品") != null)
                numParts = get("部品").size();
        }
    }
    
    private void setOwnerStatus(){
        if(get("顧客") != null){
            numOwners = get("顧客").size();
            
            //主要な代理店
            mcompany = getValue("顧客", "顧客.会社コード", false).stream().filter(c -> c.length() > 1).findFirst().get();
        }
        
    }
    
    private void setDateSMRDict(){
        //KOMTRAXが存在する場合の辞書構成
    }

    /*
    private void setAgeSMR(Map<String, List<String>> act_smr) {
        //初期値
        ageSMR.put("0", new AbstractMap.SimpleEntry<>(0, 0));
        smrDate.put(0, lifestart);

        // d刻みでSMRをsで丸める
        for (String date : act_smr.keySet()) {
            Integer t = age(date) / D_DATE;
            Integer smr = (Double.valueOf(act_smr.get(date).get(0)).intValue() / D_SMR) * D_SMR;  //ACT_SMRの構成が変わるとエラー
            if (maxSMR[4] <= smr) {
                ageSMR.put(date, new AbstractMap.SimpleEntry<>(t, smr));

                if (smrDate.get(smr) == null) {
                    smrDate.put(smr, date);
                }

                maxSMR[4] = smr;
            }
        }

        //取得できていない箇所を手入力サービスメータから取得
        if (get("SMR") == null) {
            return;
        }

        List<String> l = new ArrayList<>(ageSMR.keySet());
        Integer last = Integer.valueOf(l.get(l.size() - 1));
        List<String> svsmr = get("SMR").keySet().stream()
                .filter(date -> last < Integer.valueOf(date.split("#")[0]))
                .collect(Collectors.toList());

        for (String date : svsmr) {
            Integer t = age(date) / D_DATE;
            Integer smr = (Double.valueOf(get("SMR").get(date).get(2)).intValue() / D_SMR) * D_SMR;  //SMRの構成が変わるとエラー

            if (maxSMR[4] <= smr) {
                ageSMR.put(date, new AbstractMap.SimpleEntry<>(t, smr));

                if (smrDate.get(smr) == null) {
                    smrDate.put(smr, date);
                }

                maxSMR[4] = smr;
            }
        }
    }*/

    public String getSMRToDate(Integer smr) {
        try {
            return smrDate.ceilingEntry(smr).getValue();
        } catch (NullPointerException ne) {
            return null;
        }
    }

    public Map.Entry<Integer, Integer> getDateToSMR(String date) {
        if (ageSMR.get(date) != null) {
            return ageSMR.get(date);
        } else {
            return forecast(date);
        }
    }

    //周辺2点から予測 精度低
    public Map.Entry<Integer, Integer> forecast(String date) {
        Integer t = age(date) / D_DATE;
        Integer smr = 0;
        Map.Entry<String, Map.Entry<Integer, Integer>> a1 = ageSMR.floorEntry(date);
        try {
            //区間点を予測
            Map.Entry<String, Map.Entry<Integer, Integer>> a2 = ageSMR.higherEntry(date);
            if (!a1.getKey().equals("0")) {
                Double a = (a2.getValue().getValue() - a1.getValue().getValue()) / time(a2.getKey(), a1.getKey()).doubleValue();
                smr = ((Double) (a1.getValue().getValue().doubleValue() + a * time(date, a1.getKey()))).intValue() / D_SMR * D_SMR;
            }
        } catch (NullPointerException ne) {
            try {
                //最終2点から未来を予測
                Map.Entry<String, Map.Entry<Integer, Integer>> a0 = ageSMR.lowerEntry(ageSMR.floorKey(date));
                Double a = (a1.getValue().getValue() - a0.getValue().getValue()) / time(a1.getKey(), a0.getKey()).doubleValue();
                smr = ((Double) (a1.getValue().getValue().doubleValue() + a * time(date, a1.getKey()))).intValue() / D_SMR * D_SMR;
            } catch (NullPointerException ne2) {
                System.out.println(syaryo.getName());
                System.out.println("a0=" + ageSMR.lowerEntry(ageSMR.floorKey(date)) + " , a1=" + a1);
                ageSMR.entrySet().stream().map(a -> a.getKey() + "," + a.getValue()).forEach(System.out::println);
                ne2.printStackTrace();
                System.exit(0);
            }
        }

        return new AbstractMap.SimpleEntry<>(t, smr);
    }

    //作番と日付をswで相互変換
    private Map<String, String> sbnDate = new HashMap<>();
    private Map<String, String> dateSBN = new HashMap<>();

    public String getSBNDate(String sbn, Boolean sw) {
        if (sw) {
            //SBN -> Date
            return sbnDate.get(sbn.split("#")[0]);
        } else {
            //Date -> SBN
            return dateSBN.get(sbn.split("#")[0]);
        }
    }

    //指定作番の作業を返す。
    public Map<String, List<String>> getSBNWork(String sbn) {
        return getSBNData("作業", sbn);
    }

    //指定作番の部品を返す。
    public Map<String, List<String>> getSBNParts(String sbn) {
        return getSBNData("部品", sbn);
    }

    //指定作番のデータを返す。
    private Map<String, List<String>> getSBNData(String key, String sbn) {
        if (get(key) == null) {
            return new HashMap<>();
        }

        List<String> sbns = get(key).keySet().stream()
                .filter(s -> s.split("#")[0].equals(sbn))
                .collect(Collectors.toList());

        Map map = new LinkedHashMap();
        for (String ksbn : sbns) {
            map.put(ksbn, get(key).get(ksbn));
        }

        return map;
    }

    //選択
    public Map<String, List<String>> getValue(String key, Integer[] index) {
        //例外処理1
        if (get(key) == null) {
            return null;
        }

        List<Integer> idxs = Arrays.asList(index);
        //例外処理2  Map size < Index size
        if (get(key).values().stream().findFirst().get().size() < idxs.stream().mapToInt(idx -> idx).max().getAsInt()) {
            return null;
        }

        //指定列を抽出したKey-Valueデータを作成
        Map map = get(key).entrySet().stream()
                .collect(Collectors.toMap(s -> s.getKey(), s -> idxs.stream()
                .map(i -> i < 0 ? s.getKey() : s.getValue().get(i))
                .collect(Collectors.toList())
                ));

        return map;
    }

    //列抽出:keyデータのindex列をsortedしてリストで返す
    public List<String> getValue(String key, String index, Boolean sorted) {
        //例外処理1
        if (get(key) == null) {
            return null;
        } else if (get(key).isEmpty()) {
            return null;
        }

        if (index.equals("-1")) {
            List list = get(key).keySet().stream().map(s -> s.split("#")[0]).collect(Collectors.toList());
            return list;
        }

        int idx = header.getHeaderIdx(key, index);
        //例外処理2
        if (idx == -1) {
            return null;
        }

        List list = get(key).values().stream().map(l -> l.get(idx)).collect(Collectors.toList());

        if (sorted) {
            list = (List) list.stream().map(v -> Double.valueOf(v.toString().split("#")[0]).intValue()).sorted().map(v -> v.toString()).collect(Collectors.toList());
        }

        return list;
    }

    public Map export(Map<String, Integer[]> exportHeader) {
        Map<String, Map<String, List<String>>> exportMap = new TreeMap<>();

        //エクスポートヘッダで指定した要素の取得
        exportHeader.entrySet().stream()
                .filter(h -> get(h.getKey()) != null)
                .forEach(h -> exportMap.put(h.getKey(), getValue(h.getKey(), h.getValue())));

        return exportMap;
    }

    //納車されてからstopまでの経過日数
    public Integer age(String stop) {
        String fstop = stop.split("#")[0];
        return time(lifestart, fstop);
    }

    //納車されてからstopまでの経過日数
    public Map.Entry getAgeSMR(String current) {
        return ageSMR.floorEntry(current);
    }

    //startからstopまでの経過日数計算
    public static Integer time(String start, String stop) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        try {
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

    //オールサポート対象期間のサービスを返す
    public Map<String, List<String>> allsupportService() {
        if (!allsupport) {
            return null;
        }

        int idx = header.getHeaderIdx("受注", "ODDAY");
        Map<String, List<String>> map = new LinkedHashMap<>();
        Map<String, List<String>> services = get("受注");
        for (String sbn : services.keySet()) {
            String date = services.get(sbn).get(idx);
            if (!checkAS(date)) {
                continue;
            }
            map.put(sbn, services.get(sbn));
        }

        if (map.isEmpty()) {
            return null;
        }
        return map;
    }

    //オールサポート対象期間か判定
    private Boolean checkAS(String d) {
        LocalDate date = LocalDate.parse(d, DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (String[] term : termAllSupport) {
            LocalDate ts = LocalDate.parse(term[0]);
            LocalDate tf = LocalDate.parse(term[1]);
            if (!(ts.isAfter(date) || tf.isBefore(date))) {
                return true;
            }
        }
        return false;
    }

    public List<String> getItems(String key, int idx) {
        if (get(key) == null) {
            return new ArrayList<>();
        }

        return get(key).values().stream()
                .map(l -> l.get(idx))
                .distinct().collect(Collectors.toList());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("syaryo:" + syaryo.getName() + " Analize:\n");
        sb.append(" kind = " + kind + "\n");
        sb.append(" type = " + type + "\n");
        sb.append(" no = " + no + "\n");
        sb.append(" used = " + used + "\n");
        sb.append(" komtrax = " + komtrax + "\n");
        sb.append(" allsupport = " + allsupport + "\n");
        sb.append(" allsupport_term = " + (termAllSupport != null ? termAllSupport.stream().map(s -> Arrays.asList(s).toString()).collect(Collectors.joining(",")) : "[]") + "\n");
        sb.append(" lifestart = " + lifestart + "\n");
        sb.append(" lifestop = " + lifestop + "\n");
        sb.append(" usedlife = " + (usedlife != null ? Arrays.asList() : "[]") + "\n");
        sb.append(" numOwners = " + numOwners + "\n");
        sb.append(" numOrders = " + numOrders + "\n");
        sb.append(" numParts = " + numParts + "\n");
        sb.append(" numWorks = " + numWorks + "\n");
        return sb.toString();
    }

    public static String getHeader() {
        //基本情報
        String header = "機種,型/小変形,機番,会社,KOMTRAX,中古,廃車,オールサポート,レンタル,SMR最終更新日,SMR,KOMTRAX_SMR最終更新日,KOMTRAX_SMR,経過日,納入日,最終更新日,廃車日,中古日,";

        //保証情報
        header += "AS期間,";

        //事故情報
        header += "事故,事故受注費計,";

        //受注情報
        header += "顧客数,受注数,作業発注数,部品発注数,ライフサイクルコスト,受注情報1,受注情報2,";

        //評価情報
        header += "使われ方,経年/SMR,メンテナンス";

        return header;
    }

    public static void main(String[] args) {
        MongoDBPOJOData db = MongoDBPOJOData.create();
        db.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);
        
        MSyaryoAnalizer.initialize(db);
        MSyaryoAnalizer sa = new MSyaryoAnalizer("PC200-8-N1-313240"); //test:"PC200-8-N1-314804"
        System.out.println(sa.toString());
    }
}
