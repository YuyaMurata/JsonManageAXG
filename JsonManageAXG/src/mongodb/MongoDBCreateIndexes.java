/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mongodb;

import com.mongodb.client.model.IndexOptions;
import java.util.List;
import org.bson.Document;

/**
 *
 * @author ZZ17807
 */
public class MongoDBCreateIndexes {
    public static void create(String db, String collection) {
        MongoDBData mongo = MongoDBData.create();
        mongo.set(db, collection);
        Document index = new Document("name", 1);
        mongo.coll.createIndex(index, new IndexOptions().unique(true));
        mongo.close();
    }
}
