/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eval.analizer;

import file.CSVFileReadWrite;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.Random;
import mongodb.MongoDBPOJOData;
import obj.MHeaderObject;
import obj.MSyaryoObject;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * 車両オブジェクト用の分析器
 *
 * @author ZZ17390
 */
public class MSyaryoAnalizer {

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
    public Integer maxSMR = -1;
    public Integer[] cluster = new Integer[3];
    public TreeMap<String, Map.Entry<Integer, Integer>> ageSMR = new TreeMap<>();
    public TreeMap<Integer, Integer> smrDate = new TreeMap<>();
    private static int D_SMR = 10;
    private static int R = 10;
    public static String LEAST_DATE = "20170501";
    private List<String[]> termAllSupport;

    public static Boolean DISP_COUNT = true;

    //初期設定
    private static MHeaderObject header;

    private static int CNT;

    public static void initialize(String db, String collection) {
        CNT = 0;
        MongoDBPOJOData mdb = MongoDBPOJOData.create();
        mdb.set(db, collection, MSyaryoObject.class);
        header = mdb.getHeader();

        //受注情報の最大日付
        int idx = header.getHeaderIdx("受注", "受注.作業完了日");
        LEAST_DATE = String.valueOf(
                mdb.getKeyList().parallelStream()
                    .map(s -> mdb.getObj(s))
                    .filter(s -> s.getData("KOMTRAX_SMR") != null)
                    .map(s -> s.getData("受注"))
                    .filter(odr -> odr != null)
                    .flatMap(odr -> odr.values().parallelStream().map(d -> d.get(idx)))
                    .mapToInt(date -> Integer.valueOf(date)).max().getAsInt());
    }

    public MSyaryoAnalizer(MSyaryoObject obj) {
        CNT++;
        this.syaryo = obj;

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
        this.type = syaryo.getName().split("-")[1] + syaryo.getName().split("-")[2].replace(" ", "");
        this.no = syaryo.getName().split("-")[3];

        //SMR
        setSMRDateDict();

        //Status
        setSpecStatus();
        setOwnerStatus();
        setServiceStatus();
        setLifeStatus();
    }

    private void setSpecStatus() {
        //車両マスタのKOMTRAX列は利用しない
        if (get("KOMTRAX_SMR") != null) {
            komtrax = true;
        }

        //KUECでの中古売買のみ
        if (get("中古") != null) {
            used = true;
            usedlife = new ArrayList<>(get("中古").keySet());
        }

        //オールサポート
        if (get("オールサポート") != null) {
            allsupport = true;
            termAllSupport = get("オールサポート").entrySet().stream()
                    .map(e -> new String[]{e.getKey(), e.getValue().get(header.getHeaderIdx("オールサポート", "オールサポート.契約満了日"))})
                    .collect(Collectors.toList());
        }
    }

    private void setLifeStatus() {
        //納入日
        lifestart = get("新車").keySet().stream().findFirst().get().split("#")[0];

        //最終確認日
        if (get("KOMTRAX_SMR") != null) {
            lifestop = get("KOMTRAX_SMR").keySet().stream().reduce((a, b) -> b).orElse(null);
        } else if (get("受注") != null) {
            lifestop = getValue("受注", "受注.作業完了日", true).get(0);
        } else {
            lifestop = "-1";
        }

        //最大SMR
        maxSMR = getDateToSMR(LEAST_DATE);

        //lifedead
        if (get("廃車") != null) {
            lifedead = get("廃車").keySet().stream().findFirst().get().split("#")[0];
        } else {
            lifedead = "-1";
        }
    }

    private void setServiceStatus() {
        if (get("受注") != null) {
            //受注情報
            int idx_date = header.getHeaderIdx("受注", "受注.作業完了日");
            numOrders = get("受注").size();
            acmLCC = getValue("受注", "受注.請求金額", false).parallelStream().mapToInt(p -> Integer.valueOf(p)).sum();
            sbnDate = get("受注").entrySet().parallelStream()
                    .collect(Collectors.toMap(
                            e -> e.getKey(),
                            e -> e.getValue().get(idx_date),
                            (e1, e2) -> e1,
                            TreeMap::new)
                    );

            //作業情報
            if (get("作業") != null) {
                numWorks = get("作業").size();
            }

            //部品情報
            if (get("部品") != null) {
                numParts = get("部品").size();
            }
        }
    }

    private void setOwnerStatus() {
        if (get("顧客") != null) {
            numOwners = get("顧客").size();

            //主要な代理店
            mcompany = getValue("顧客", "顧客.会社コード", false).stream().filter(c -> c.length() > 1).findFirst().get();
        }

    }

    //SMR <-> 日付　辞書の作成
    private void setSMRDateDict() {
        String[] smrKey = new String[]{"KOMTRAX_SMR", "SMR"};

        //SMR Keyからデータを取得
        Arrays.stream(smrKey)
                .map(key -> get(key))
                .filter(d -> d != null)
                .flatMap(d -> d.entrySet().stream())
                .forEach(d -> {
                    Integer date = Integer.valueOf(d.getKey().split("#")[0]);
                    Integer smr = (Integer.valueOf(d.getValue().get(1)) / D_SMR) * D_SMR;
                    if (smrDate.lastEntry() == null) {
                        smrDate.put(smr, date);
                    }

                    if (smrDate.lastEntry().getValue() < date) {
                        if (smrDate.get(smr) == null) {
                            smrDate.put(smr, date);
                        }
                    }
                });

        //日付が前後している情報を削除
        List<Integer> removeSMR = new ArrayList<>();
        Integer tmpDate = 0;
        for (Integer smr : smrDate.keySet()) {
            Integer date = smrDate.get(smr);
            if (tmpDate > date) {
                removeSMR.add(smr);
            }
            tmpDate = date;
        }
        //System.out.println(removeSMR);

        removeSMR.stream().forEach(smrDate::remove);
    }

    //SMR -> 日付
    public Integer getSMRToDate(Integer smr) {
        if (smrDate.get(smr) != null) {
            return smrDate.get(smr);
        }

        Integer date = regression("smr", smr);

        return date;
    }

    //日付　-> SMR
    public Integer getDateToSMR(String date) {
        Integer d = Integer.valueOf(date.split("#")[0]);
        if (smrDate.values().contains(d)) {
            return smrDate.entrySet().stream()
                    .filter(v -> v.getValue().equals(d))
                    .map(v -> v.getKey()).findFirst().get();
        }

        Integer smr = regression("date", Integer.valueOf(date));

        return smr;
    }

    //回帰式の算出
    private Integer regression(String type, Integer p) {
        Queue<Integer> q = new ArrayDeque();
        int cnt = R / 2;
        Collection<Integer> data = smrDate.keySet();
        if (type.equals("date")) {
            data = smrDate.values();
        }

        for (Integer d : data) {
            q.add(d);
            if (R < q.size()) {
                q.poll();
            }

            if (p < d) {
                cnt--;
            }

            if (cnt < 0) {
                break;
            }
        }

        SimpleRegression reg = new SimpleRegression();
        String stdate = type.equals("date") ? q.peek().toString() : getSMRToDate(q.peek()).toString();
        q.stream().forEach(d -> {
            Integer date = type.equals("date") ? d : getSMRToDate(d);
            Integer smr = type.equals("smr") ? d : getDateToSMR(d.toString());
            reg.addData(time(stdate, date.toString()), smr);
        });

        //System.out.println("Q:"+q);
        //System.out.println("R = "+reg.getSlope()+"x+"+reg.getIntercept());
        Double v;
        if (type.equals("smr")) {
            Double x = (p - reg.getIntercept()) / reg.getSlope();
            v = Double.valueOf(time(stdate, x.intValue()));
        } else {
            v = reg.predict(time(stdate, p.toString()));
        }

        return v.intValue();
    }

    //作番と日付をswで相互変換
    private Map<String, String> sbnDate = new HashMap<>();
    private Map<String, String> dateSBN = new HashMap<>();

    public String getSBNToDate(String sbn, Boolean sw) {
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

    //納車されてからcurrentまでの経過日数
    public Map.Entry getAgeSMR(String current) {
        return ageSMR.floorEntry(current);
    }

    //startからstopまでの経過日数計算
    public static Integer time(String start, String stop) {
        LocalDate st = LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate sp = LocalDate.parse(stop, DateTimeFormatter.ofPattern("yyyyMMdd"));

        Long age = ChronoUnit.DAYS.between(st, sp);

        return age.intValue();
    }

    //経過日から日付を計算
    public static Integer time(String start, Integer days) {
        LocalDate st = LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyyMMdd")).plusDays(days);
        //System.out.println("start="+start+" days="+days+" local:"+st);
        return Integer.valueOf(st.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
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

        Random r = new Random();
        String sid = "PC200-10- -454702";//db.getKeyList().get(r.nextInt(db.getKeyList().size()));
        System.out.println(sid);

        MSyaryoAnalizer.initialize("json", "komatsuDB_PC200_Form");
        MSyaryoAnalizer sa = new MSyaryoAnalizer(db.getObj(sid));
        System.out.println(sa.toString());

        smrDictOut(sa);
        //SMRチェック
        System.out.println("2017/12/28:" + sa.getDateToSMR("20171228"));
        System.out.println("7000:" + sa.getSMRToDate(7000));
    }

    private static void smrDictOut(MSyaryoAnalizer sa) {
        //辞書の出力
        try (PrintWriter pw = CSVFileReadWrite.writerSJIS("test_analize_dict.csv")) {
            pw.println("Date,SMR");
            sa.smrDate.entrySet().stream().map(d -> d.getValue() + "," + d.getKey()).forEach(pw::println);
        } catch (Exception e) {

        }
    }
}
