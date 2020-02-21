
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ZZ17807
 */
public class ArrayGetTets {
    public static void main(String[] args) {
        Integer[] a = new Integer[]{1,2,3,4,5};
        System.out.println(Arrays.toString(a));
        Integer[] b = a.clone();
        b[0] = 5; b[4] = 1;
        System.out.println(Arrays.toString(a));
        System.out.println(Arrays.toString(b));
        
        String[] s = new String[]{"a","b","c"};
        Map<String, String[]> map = new HashMap();
        map.put("s", s);
        System.out.println("s:"+Arrays.asList(map.get("s")));
        map.get("s")[0] = "c";
        System.out.println("s:"+Arrays.asList(map.get("s")));
    }
}
