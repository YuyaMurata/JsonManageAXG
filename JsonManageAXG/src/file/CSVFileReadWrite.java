/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package file;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author ZZ17390
 */
public class CSVFileReadWrite {
    public static PrintWriter writer(String filename){
        try {
            return  new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename)));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        
        return null;
    }
    
    public static PrintWriter writerSJIS(String filename){
        try {
            return  new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename), "SJIS"));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.exit(0);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        
        return null;
    }
    
    public static PrintWriter addwriter(String filename){
        try {
            return  new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename, true), "SJIS"));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.exit(0);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        
        return null;
    }
    
    public static BufferedReader reader(String filename){
        try {
            return new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException ex) {
        }
        
        return null;
    }
    
    public static BufferedReader readerSJIS(String filename){
        try {
            return new BufferedReader(new InputStreamReader(new FileInputStream(filename), "SJIS"));
        } catch (UnsupportedEncodingException ex) {
        } catch (FileNotFoundException ex) {
        }
        
        return null;
    }
}
