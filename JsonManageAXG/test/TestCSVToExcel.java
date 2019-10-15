
import file.DataConvertionUtil;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ZZ17807
 */
public class TestCSVToExcel {
    private static String PATH= "C:\\Users\\zz17807\\OneDrive - Komatsu Ltd\\共同研究\\メンテ検証\\raw\\";
    public static void main(String[] args) throws Exception {
        
        DataConvertionUtil.csvToEXCEL(PATH+"test_print_eval_PC200-8- -300208_.csv", "test_print_eval_PC200-8- -300208_.xls");
    }
}
