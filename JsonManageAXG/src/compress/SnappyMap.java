/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import org.xerial.snappy.Snappy;

/**
 *
 * @author ZZ17390
 */
public class SnappyMap {
    
    public static byte[] toSnappy(Object obj){
        try {
            return Snappy.compress(getBytes(obj));
        } catch (Exception ex) {
            System.err.println("Null");
            return null;
        }
    }
    
    public static Object toObject(byte[] bytes){
        try {
            return getObject(Snappy.uncompress(bytes));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    private static byte[] getBytes(Object obj) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            return baos.toByteArray();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static Object getObject(byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        } catch (IOException ex) {
        } catch (ClassNotFoundException ex) {
        }
        return null;
    }
}
