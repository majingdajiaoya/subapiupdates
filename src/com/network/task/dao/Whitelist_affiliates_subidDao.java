package com.network.task.dao;

import java.util.List;

import com.network.task.bean.BlackListSubAffiliates;

public class Whitelist_affiliates_subidDao extends TuringBaseDao{
	
    private static final String TBLPREFIX  = "whitelist_affiliates_subid";

    public static String table(){
    	
        return TBLPREFIX;
    }
    
    @SuppressWarnings("unchecked")
    public List<BlackListSubAffiliates> getAllInfoByAdvertisers_id(String id){
    	  String sql="SELECT *FROM blacklist_affiliates_subid WHERE advertisers_id="+id;
          List<BlackListSubAffiliates> list = query(sql,
                    null, null,
                    new TuringCommonRowMapper(BlackListSubAffiliates.class));
          
          return list;
    }
	public void add(Long advertisersId, String pkg, String affiliates_sub_id) {
		super.getJdbcTemplate().update("insert into blacklist_affiliates_subid "
    			+ " ( "
				+ " advertisers_id,"
				+ " affiliates_sub_id,"
				+ " pkg,"
    			+ " status"
    			+ " ) "
    			+ " values(?,?,?,?)",
				new Object[] {
    					advertisersId,
    					affiliates_sub_id,
    				pkg,
    				0});
		
	}
}
