
import file.FileMD5;
import java.util.TreeSet;
import java.util.stream.IntStream;
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
        TreeSet<Integer> tree = new TreeSet<>();
        IntStream.range(0, 10).map(i -> i*2).forEach(tree::add);
        
        System.out.println(tree);
        
        System.out.println(tree.lower(1));
        System.out.println(tree.higher(1));
        System.out.println(tree.lower(0));
        System.out.println(tree.higher(0));
    }
}
