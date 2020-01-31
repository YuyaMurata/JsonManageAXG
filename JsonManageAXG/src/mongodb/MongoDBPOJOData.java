/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mongodb;

import com.mongodb.BasicDBObject;
import obj.MHeaderObject;
import obj.MSyaryoObject;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.*;
import com.mongodb.client.model.IndexOptions;
import exception.AISTProcessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
    private transient Map<String, MSyaryoObject> map;
    private List keys;
    private MHeaderObject header;

    private MongoDBPOJOData() {
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
        this.keys = new ArrayList();
        this.header = null;
    }

    public void check() throws AISTProcessException {
        if (this.coll.countDocuments() == 0) {
            throw new AISTProcessException("参照するDB.コレクションに誤りがあります：" + this.db.getName() + "." + this.coll.getNamespace().getCollectionName());
        }
    }

    public MHeaderObject getHeader() {
        if (this.header == null) {
            MongoCollection hcoll = this.db.getCollection(this.coll.getNamespace().getCollectionName(), MHeaderObject.class);
            this.header = (MHeaderObject) hcoll.find(exists("header")).first();
            this.header.setHeaderMap();
        }

        return this.header;
    }

    public List<String> getKeyList() {

        if (this.keys.isEmpty()) {

            long start = System.currentTimeMillis();
            System.out.println("get key start!");

            MongoDBData cl = MongoDBData.create();

            System.out.println("mongo connect : " + this.db.getName() + "." + this.coll.getNamespace().getCollectionName());

            cl.set(this.db.getName(), this.coll.getNamespace().getCollectionName());
            this.keys = cl.getKeyList();
            cl.close();

            long stop = System.currentTimeMillis();
            System.out.println("get key time=" + (stop - start) + "ms");
        }

        return this.keys;
    }

    public Object getObj(String name) {
        return this.coll.find(eq("name", name)).first();
    }

    public Map<String, MSyaryoObject> getObjMap() {
        if (map == null) {
            map = getKeyList().parallelStream().map(sid -> (MSyaryoObject) getObj(sid))
                    .collect(Collectors.toMap(s -> s.getName(), s -> s));
        }

        return map;
    }

    public void createIndexes() {
        this.coll.createIndex(new BasicDBObject().append("name", 1), new IndexOptions().unique(true));
    }

    public void clear() {
        System.err.println("Drop Collection " + this.coll.getNamespace().getCollectionName());
        this.coll.drop();
    }

    public void close() {
        client.close();
    }
}
