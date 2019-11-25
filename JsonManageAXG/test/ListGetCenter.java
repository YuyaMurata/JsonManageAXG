
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ZZ17807
 */
public class ListGetCenter {
    public static void main(String[] args) {
        Random r = new Random();
        List<Integer> a = new ArrayList();
        IntStream.range(0, 10).map(i -> r.nextInt()).forEach(a::add);
        
        System.out.println(a);
        System.out.println(a.stream().sorted().collect(Collectors.toList()));
        System.out.println("c="+a.stream().sorted().limit(a.size()/2).reduce((a1, b1) -> b1).orElse(null));
    }
}
