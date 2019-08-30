/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg;

import axg.cleansing.MSyaryoObjectCleansing;
import axg.shuffle.MSyaryoObjectShuffle;
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
        //Map header = new MapToJSON().toMap("axg\\mongoobj_syaryo_src.json");
        //MSyaryoObjectCleansing.clean("json", "komatsuDB_PC200", Arrays.asList(new String[]{"8", "8N1", "10"}), header);
        
        //MongoDB Shuffling
        Map sheader = new MapToJSON().toMap("axg\\shuffle_mongo_syaryo.json");
        Map slayout = new MapToJSON().toMap("axg\\layout_mongo_syaryo.json");
        MSyaryoObjectShuffle.shuffle("json", "komatsuDB_PC200", sheader, slayout);
        
        //MongoDB Formalize
        
    }
}
