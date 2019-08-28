/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author ZZ17390
 */
public class ListToCSV {
    public static List<String> toList(String csv){
        List<String> list = new ArrayList<>();
        try(BufferedReader br = CSVFileReadWrite.reader(csv)){
            String line;
            while((line = br.readLine()) != null){
                //コメント除外
                if(line.charAt(0) == '#')
                    continue;
                
                list.add(line);
            }
            
            return list;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public static Map<String, String> toMap(String csv, int k, int v){
        List<String> l = toList(csv);
        
        Map<String, String> map = l.stream()
                                    .map(s -> s.split(","))
                                    .filter(s -> !s[k].equals(""))
                                    .collect(Collectors.toMap(
                                            s -> s[k], 
                                            s -> s[v]
                                    ));
        
        return map;
    }
    
    public static Map<String, String> toKeyMap(String csv, int k, int v){
        List<String> l = toList(csv);
        
        Map<String, String> map = l.stream()
                                    .map(s -> s.split(","))
                                    .map(s -> s[k]+s[v])
                                    .filter(s -> !s.equals(""))
                                    .collect(Collectors.toMap(
                                            s -> s, 
                                            s -> "1"
                                    ));
        
        return map;
    }
    
    public static void toCSV(String csv, List list){
        try(PrintWriter pw = CSVFileReadWrite.writer(csv)){
            list.stream().forEach(pw::println);   
        }
    }
}
