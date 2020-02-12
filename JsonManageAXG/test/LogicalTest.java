/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ZZ17807
 */
public class LogicalTest {

    public static void main(String[] args) {
        byte C11 = (byte) 0b0111_1000;
        byte C12 = (byte) 0b0110_1000;
        byte C13 = (byte) 0b0111_0110;
        byte C14 = (byte) 0b0011_1111;
        byte C15 = (byte) 0b0111_1011;
        byte C16 = (byte) 0b0101_1010;
        byte C17 = (byte) 0b0111_1011;

        byte A = (byte) ((byte) C11 & C12 & C13 & C14 & (C15 | C16 & C17));
        System.out.println(toBinaryString(A));
        
        byte B = (byte) ((byte) C11 & C12 & C13 & C14 & C15 | (C16 & C17));
        System.out.println(toBinaryString(B));
    }

    private static String toBinaryString(byte b) {
        return String.format("%8s", Integer.toBinaryString(b & 0xff)).replace(" ", "0");
    }

}
