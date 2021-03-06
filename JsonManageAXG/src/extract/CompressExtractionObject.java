/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extract;

import analizer.MSyaryoAnalizer;
import compress.CompressUtil;
import org.bson.types.ObjectId;

/**
 *
 * @author ZZ17807
 */
public class CompressExtractionObject {
    private ObjectId id;
    private String name;
    private byte[] data;

    public CompressExtractionObject() {
    }
    
    public CompressExtractionObject(MSyaryoAnalizer s) {
        this.name = s.get().getName();
        this.data = CompressUtil.compress(s);
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
    
    public MSyaryoAnalizer toObj(){
        return (MSyaryoAnalizer) CompressUtil.decompress(data);
    }
    
    @Override
    public String toString(){
        return name+":"+data;
    }
}
