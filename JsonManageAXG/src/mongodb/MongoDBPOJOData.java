/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mongodb;

import axg.obj.MHeaderObject;
import axg.obj.MSyaryoObject;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

/**
 *
 * @author ZZ17807
 */
public class MongoDBPOJOData {
    private MongoClient client;
    private MongoDatabase db;
    public MongoCollection coll;
    
    private MongoDBPOJOData(){
        Logger.getLogger("org.mongodb.driver").setLevel(Level.SEVERE);
        initialize();
    }
    
    private void initialize() {
        CodecRegistry pojoCodecRegistry = fromRegistries(com.mongodb.MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        MongoClientSettings settings = MongoClientSettings.builder()
        .codecRegistry(pojoCodecRegistry)
        .build();
        client = MongoClients.create(settings);
    }

    public static MongoDBPOJOData create() {
        return new MongoDBPOJOData();
    }

    public void set(String dbn, String col, Class clazz) {
        this.db = client.getDatabase(dbn);
        this.coll = db.getCollection(col, clazz);
    }
    
    public MHeaderObject getHeader(){
        MongoCollection hcoll = this.db.getCollection(this.coll.getNamespace().getCollectionName(), MHeaderObject.class);
        MHeaderObject h = (MHeaderObject) hcoll.find(exists("header")).first();
        h.setHeaderMap();
        return h;
    }
    
    public List<String> getKeyList(){
        long start = System.currentTimeMillis();
        System.out.println("get key start!");
        MongoDBData cl = MongoDBData.create();
        
        System.out.println(this.db.getName()+","+this.coll.getNamespace().getCollectionName());
        
        cl.set(this.db.getName(), this.coll.getNamespace().getCollectionName());
        List keys = cl.getKeyList();
        long stop = System.currentTimeMillis();
        
        System.out.println("get key time="+(stop-start)+"ms");
        cl.close();
        return keys;
    }
    
    public MSyaryoObject getObj(String name){
        return (MSyaryoObject) this.coll.find(eq("name", name)).first();
    }
    
    public void clear(){
        System.err.println("Drop Collection!");
        this.coll.drop();
    }
    
    public void close() {
        client.close();
    }
}
