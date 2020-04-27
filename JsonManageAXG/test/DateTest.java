
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author kaeru
 */
public class DateTest {
    public static void main(String[] args) throws ParseException {
        String ymd = "2020-1-1 16:24:35";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdfout = new SimpleDateFormat("yyyyMMdd");
        Date d = sdf.parse(ymd);
        System.out.println(sdfout.format(d));
    }
}
