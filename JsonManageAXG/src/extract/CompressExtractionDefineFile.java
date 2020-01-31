/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extract;

import compress.CompressUtil;
import compress.SnappyMap;
import java.util.List;
import org.bson.types.ObjectId;

/**
 *
 * @author ZZ17807
 */
public class CompressExtractionDefineFile {
    private ObjectId id;
    private String name;
    private byte[] data;

    public CompressExtractionDefineFile() {
    }
    
    public CompressExtractionDefineFile(String item, List<String> data) {
        this.name = item;
        this.data = CompressUtil.compress(data);
    }
    
    public ObjectId getId() {
        return id;
    }

    public void setId(final ObjectId id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
    
    public byte[] getData(){
        return data;
    }
    
    public void setData(final byte[] data){
        this.data = data;
    }
    
    public List<String> toList(){
        return (List<String>) CompressUtil.decompress(data);
    }
    
    @Override
    public String toString(){
        return name+":"+data;
    }
}
