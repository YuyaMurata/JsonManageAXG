/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package file;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author ZZ17807
 */
public class FileMD5 {
    public static String hash(String file){

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream input = new DigestInputStream(new FileInputStream(file), md);
            
            // ファイルの読み込み
            while (input.read() != -1) {
            }
            
            // ハッシュ値の計算
            byte[] digest = md.digest();
            
            input.close();
            
            return DatatypeConverter.printHexBinary(digest).toUpperCase();
        } catch (NoSuchAlgorithmException | IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
