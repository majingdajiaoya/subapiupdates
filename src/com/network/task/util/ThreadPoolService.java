package com.network.task.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolService {
	
	private final int DEFAULT_POOL_SIZE = 10;
	private AtomicInteger threadCounter;
	private ExecutorService executorService;
	
	public static ThreadPoolService getInstance(){
		return ThreadPoolServiceHolder.instance;
	}
	
	private static class ThreadPoolServiceHolder{
		static ThreadPoolService instance = new ThreadPoolService();
	}
	
	private ThreadPoolService(){
		this.threadCounter = new AtomicInteger();
		this.executorService = Executors.newFixedThreadPool(DEFAULT_POOL_SIZE, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				return new Thread(r, "ThreadPoolService-"
						+ ThreadPoolService.this.threadCounter.incrementAndGet());
			}
		});
	}
	
	public void start(){
		
	}
	
	public <T> Future<T> submitTask(Callable<T> task){
		return executorService.submit(task);
	}
	
	public void execute(Runnable task){
		this.executorService.execute(task);
	}
	
	public void stop(){
		this.executorService.shutdown();
	}
	

}
