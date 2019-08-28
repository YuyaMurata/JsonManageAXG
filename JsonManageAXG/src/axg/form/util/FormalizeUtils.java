/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.form.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author ZZ17807
 */
public class FormalizeUtils {
    //startからstopまでの経過日数計算
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    public static Integer dsub(String start, String stop) {
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
}
