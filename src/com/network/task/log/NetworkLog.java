package com.network.task.log;


import org.apache.log4j.Logger;

public class NetworkLog
{
   
    protected static final Logger RUNERROR_LOG = Logger.getLogger("runerror"); // 运行错误
    
    protected static final Logger EXCEPTION_LOG = Logger.getLogger("exception"); // 运行错误
    
    public static void log(Exception e){
    	
    	RUNERROR_LOG.error("error", e);
    }
    		
    public static void errorLog(Exception e) {
    	
    	RUNERROR_LOG.error("error", e);
	}
    
    public static void exceptionLog(Exception e) {
    	
    	EXCEPTION_LOG.error("error", e);
	}

}
