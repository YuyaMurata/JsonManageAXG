/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extract;

import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class SyaryoObjectExtract {
    public static MongoDBPOJOData db;
    
    public SyaryoObjectExtract(String dbn, String collection) {
        db = MongoDBPOJOData.create();
        db.set(dbn, collection, MSyaryoObject.class);
    }
    
    
}
