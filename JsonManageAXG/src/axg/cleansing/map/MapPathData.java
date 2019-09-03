/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.cleansing.map;

import com.lynden.gmapsfx.javascript.object.LatLong;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/**
 *
 * @author ZZ17390
 */
public class MapPathData {
    private static Random rand = new Random();
    public final String name;
    public TreeMap<Integer, Integer> gps;
    private List<LatLong> gpsData;
    public final String color;
    
    public MapPathData(String name) {
        this.name = name;
        this.gps = new TreeMap<>();
        this.gpsData = new ArrayList<>();
        this.color = setColor();
    }
    
    private String setColor(){
        //Color
        Integer r = rand.nextInt(256);
        Integer g = rand.nextInt(256);
        Integer b = rand.nextInt(256);
        //return "#" + Integer.toHexString(r) + Integer.toHexString(g) + Integer.toHexString(b);
        
        return "#000000";
    }
    
    public void addData(Integer date, List<String> latlong){
        Double lat = compValue(latlong.get(0));
        Double lon = compValue(latlong.get(1));
        LatLong ll = new LatLong(lat, lon);
        System.out.println(ll.getLatitude());
        gpsData.add(new LatLong(lat, lon));
        gps.put(date, gpsData.size()-1); 
    }
    
    public Integer first(){
        return gps.firstKey();
    }
    
    public Integer last(){
        return gps.lastKey();
    }
    
    public static double compValue(String str) {
        
        //変換
        str = str.replace("N", "").replace("S", "-").replace("E", "").replace("W", "-");

        String[] s = str.split("\\.");

        BigDecimal b1 = new BigDecimal(s[0]);
        BigDecimal b2 = new BigDecimal(s[1]).divide(new BigDecimal("60"), 7, RoundingMode.HALF_UP);
        BigDecimal b3 = new BigDecimal(s[2]).divide(new BigDecimal("3600"), 7, RoundingMode.HALF_UP);
        BigDecimal b4 = new BigDecimal(s[3]).divide(new BigDecimal("21600"), 7, RoundingMode.HALF_UP);

        BigDecimal result = b1.add(b2).add(b3).add(b4);

        return result.doubleValue();
    }
    
    public List<LatLong> getPath(Integer date){
        Integer index = gps.floorEntry(date).getValue();
        return gpsData.subList(0, index);
    }
}
