package com.network.task.dao;

import java.sql.Types;
import java.util.List;
import com.network.task.bean.Affiliates;
import common.Logger;

/**
 * 网盟分发表：网盟offer分发渠道对应
 * 
 * @author sundu
 *
 */
public class AdvertisersDistributeDao extends TuringBaseDao{
	
	protected static Logger logger = Logger.getLogger(AdvertisersDistributeDao.class);
	
    /**
     * 查询网盟分发的渠道信息
     * 
     * @param advertisersId
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Affiliates> getAdvertisersDistributeAffiliates(Long advertisersId){
    	
		List<Affiliates> list = query("SELECT t1.* ,addpayoutrate,click,cvr,country,ecountry,limit_os "
    		  	+ " FROM advertisers_distribute t "
    		  	+ " LEFT JOIN affiliates t1 ON t.affiliates_id=t1.id"
                + " WHERE t.advertisers_id=? and t.is_top=0 AND t1.status = 0 ",
                new Object[] { advertisersId }, new int[] { Types.BIGINT },
                new TuringCommonRowMapper(Affiliates.class));
      
	    if(list != null
	    		  && list.size() > 0){
	    	  
	    	  return list;
	    }
	      
	    return null;
    }
    public static void main(String[] args) {
		String sql="SELECT t1.* ,addpayoutrate,click,cvr,country,ecountry "
    		  	+ " FROM advertisers_distribute t "
    		  	+ " LEFT JOIN affiliates t1 ON t.affiliates_id=t1.id"
                + " WHERE t.advertisers_id=? and t.is_top=0 AND t1.status = 0";
		System.out.println(sql);
	}
}
