/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg;

import axg.cleansing.MongoDBCleansing;
import file.MapToJSON;
import java.util.Arrays;
import java.util.Map;

/**
 *
 * @author ZZ17807
 */
public class TestMongoExec {
    public static void main(String[] args) {
        //MongoDB Cleansing
        Map header = new MapToJSON().toMap("axg\\mongoobj_syaryo_src.json");
        MongoDBCleansing.clean("json", "komatsuDB_PC200", Arrays.asList(new String[]{"8", "8N1", "10"}), header);
        
        //MongoDB Shuffling
        
        
        //MongoDB Formalize
        
        
    }
}
