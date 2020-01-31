/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compress;

import obj.MSyaryoObject;

/**
 *
 * @author ZZ17807
 */
public class CompressUtil {

    /**
     * データ圧縮
     */
    public static byte[] compress(Object obj) {
        return SnappyMap.toSnappy(obj);
    }

    /**
     * データ解凍
     * @param b
     * @return 
     */
    public static Object decompress(byte[] b) {
        if (b == null) {
            return null;
        }
        return SnappyMap.toObject(b);
    }
}
