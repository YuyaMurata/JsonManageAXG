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
    public ForkJoinPool threadPool;
    
    private ExecutableThreadPool(){
        int N = Runtime.getRuntime().availableProcessors();
        threadPool = new ForkJoinPool(N);
    }
    
    public static ExecutableThreadPool getInstance(){
        return instance;
    }
}
