/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.form.item;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author ZZ17807
 */
public class FormAllSurpport {

    //オールサポートの整形　(解約日を終了日とする)
    public static Map form(Map<String, List<String>> as, List indexList) {
        if (as == null) {
            return null;
        }

        int finDate = indexList.indexOf("オールサポート.M契約満了日");
        int kaiDate = indexList.indexOf("オールサポート.M解約日");

        Map newMap = new TreeMap();
        for (String date : as.keySet()) {
            List<String> aslist = as.get(date);
            String findt = aslist.get(finDate);
            String kykdt = aslist.get(kaiDate);
            if (!(kykdt.equals("0") || kykdt.equals(""))) {
                if (Integer.valueOf(kykdt) < Integer.valueOf(findt)) {
                    aslist.set(finDate, kykdt);
                }
            }
            
            if(Integer.valueOf(aslist.get(finDate)) > Integer.valueOf(date.split("#")[0]))
                newMap.put(date.split("#")[0], aslist);
        }
        
        if (newMap.isEmpty()) {
            return null;
        }

        return newMap;
    }
}
