package com.network.task.dao;

import java.util.List;

import com.network.task.bean.BlacklistPKG;

public class BlacklistPKGDao extends TuringBaseDao{
	
    private static final String TBLPREFIX  = "blacklist_pkg";

    public static String table(){
    	
        return TBLPREFIX;
    }
    
    /**
     * 获取所有BlacklistPKG
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<BlacklistPKG> getAllValidAdvertisers(){
    	
          List<BlacklistPKG> list = query("select * "
        		  	+ " from " + table() +" where status=0",
                    null, null,
                    new TuringCommonRowMapper(BlacklistPKG.class));
          
          return list;
    }
    public static void main(String[] args) {
		String sql="select * "
    		  	+ " from " + table() +" where status=0";
		System.out.println(sql);
	}
}
