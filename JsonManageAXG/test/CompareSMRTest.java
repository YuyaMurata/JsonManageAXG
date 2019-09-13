
import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ZZ17807
 */
public class CompareSMRTest {
    public static void main(String[] args) {
        MongoDBPOJOData formDB = MongoDBPOJOData.create();
        formDB.set("json", "komatsuDB_PC200_Form", MSyaryoObject.class);
    }
}
