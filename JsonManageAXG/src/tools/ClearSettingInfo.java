/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 *
 * @author kaeru
 */
public class ClearSettingInfo {
    public static void main(String[] args) {
        try (MongoClient client = MongoClients.create()) {
            infoDB(client);
            extractioDB(client);
            definitionDB(client);
        }
    }
    
    private static void infoDB(MongoClient client){
        client.getDatabase("info").drop();
    }
    
    private static void extractioDB(MongoClient client){
        client.getDatabase("extraction").drop();
    }
    
    private static void definitionDB(MongoClient client){
        client.getDatabase("definition").drop();
    }
}
