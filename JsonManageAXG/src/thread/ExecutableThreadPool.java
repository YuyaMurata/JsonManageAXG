/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package thread;

import java.util.concurrent.ForkJoinPool;

/**
 *
 * @author kaeru
 */
public class ExecutableThreadPool {
    private static ExecutableThreadPool instance = new ExecutableThreadPool();
    private ForkJoinPool pool;
    
    private ExecutableThreadPool(){
        pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    }
    
    public void setN(int n){
        pool = new ForkJoinPool(n);
    }
    
    public static ExecutableThreadPool getInstance(){
        return instance;
    }
    
    public ForkJoinPool getPool(){
        return pool;
    }
}
