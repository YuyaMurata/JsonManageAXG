
import java.util.Arrays;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ZZ17807
 */
public class ArrayGetTets {
    public static void main(String[] args) {
        String s = "1,2,3,,,,,";
        Arrays.stream(s.split(",")).forEach(System.out::println);
    }
}
