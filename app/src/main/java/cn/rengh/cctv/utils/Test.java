
package cn.rengh.cctv.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;

import com.r.library.common.util.ThreadManager;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Test {
    public void ams(Context context){
        ActivityManager activityManage = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }
    public void test() {
        final Semaphore semaphore = new Semaphore(5, true);
        FutureTask<Integer> ft = new FutureTask(new Callable() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        });
        ft.run();

        Thread thread = new Thread(ft);
        thread.getState();

        AtomicInteger atomicInteger;

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4,
                10,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(10),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

                    }
                });

        ThreadManager.getInstance();

        ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
        threadLocal.set(3);
    }
}
