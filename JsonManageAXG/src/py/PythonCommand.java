/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author zz17390
 */
public class PythonCommand {

    public static void py(String pyFile, String[] args) {
        System.out.println(pyFile+" "+String.join(",", args));
        
        String pyc = "python,"+pyFile+","+String.join(",", args);
        
        String[] out;
        try {
            out = execCommand(pyc.split(","));
            //Python出力
            System.out.println(out[0]);
            System.out.println(out[1]);
            System.out.println(out[2]);
        } catch (IOException ex) {
        } catch (InterruptedException ex) {
        }
    }

    /**
     * 外部コマンドを実行します。 またリタンーン値で、標準出力、エラー出力 、リターンコードを取得します。 例：
     * execCommand("notepad.exe");
     *
     * @see execCommand(String[] cmds) ※実行するコマンドに引数（パラメータ）がある場合は、 以下を使用してください。
     * @param cmd 実行するコマンド
     * @return コマンド実行結果情報を保持するString配列 配列[0] ⇒ 標準出力 配列[1] ⇒ エラー出力 配列[2] ⇒
     * リターンコード
     * @throws IOException 入出力エラーが発生した場合
     */
    public static String[] execCommand(String cmd) throws IOException,
        InterruptedException {
        return execCommand(new String[]{cmd});
    }

    /**
     * 外部コマンドを引数（パラメータ）を指定して実行します。 またリタンーン値で、標準出力、エラー出力 、リターンコードを取得します。 例：
     * execCommand(new String[]{"notepad.exe","C:\test.txt"});
     *
     * Process.waitFor()を実行していますので、外部コマンドの実行が 終了するまでこのメソッドは待機します。
     *
     * @see execCommand(String cmd) ※実行するコマンドに引数がない場合は簡易的にこちらを 使用してください。
     * @param cmds 実行するコマンドと引数を含む配列
     * @return コマンド実行結果情報を保持するString配列 配列[0] ⇒ 標準出力 配列[1] ⇒ エラー出力 配列[2] ⇒
     * リターンコード
     * @throws IOException 入出力エラーが発生した場合
     */
    public static String[] execCommand(String[] cmds) throws IOException,
        InterruptedException {
        String[] returns = new String[3];
        String LINE_SEPA = System.getProperty("line.separator");
        Runtime r = Runtime.getRuntime();
        Process p = r.exec(cmds);
        InputStream in = null;
        BufferedReader br = null;
        try {
            in = p.getInputStream();
            StringBuffer out = new StringBuffer();
            br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line + LINE_SEPA);
            }
            returns[0] = out.toString();
            br.close();
            in.close();
            in = p.getErrorStream();
            StringBuffer err = new StringBuffer();
            br = new BufferedReader(new InputStreamReader(in));
            while ((line = br.readLine()) != null) {
                err.append(line + LINE_SEPA);
            }
            returns[1] = err.toString();
            returns[2] = Integer.toString(p.waitFor());
            return returns;
        } finally {
            if (br != null) {
                br.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }
}
