
import file.FileMD5;
import javax.xml.bind.DatatypeConverter;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ZZ17807
 */
public class HashTest {
    public static void main(String[] args) {
        String a = FileMD5.hash("project\\komatsuDB_PC200\\config\\user_define.json");
        String b = FileMD5.hash("project\\komatsuDB_PC200\\config\\user_define.json");
        
        if(a.equals(b))
            System.out.println("a("+String.valueOf(a)+") = b("+String.valueOf(b));
        else
            System.err.println("a("+String.valueOf(a)+") != b("+String.valueOf(b));
    }
}
