/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import exception.AISTProcessException;
import file.ListToCSV;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author kaeru
 */
public class ErrorCode {
    static String codePath = "toolsettings\\PC200-11_故障コード.csv";
    public static Map<String, String> define;
    
    public static void init(){
        try {
            define = ListToCSV.toList(codePath)
                    .stream()
                    .collect(
                            Collectors.toMap(
                                    l ->l.split(",")[0],
                                    l -> l.replace(l.split(",")[0]+",", ""),
                                    (a,b) -> a));
            System.out.println(define);
        } catch (AISTProcessException ex) {
            ex.printStackTrace();
        }
    }
    
}
