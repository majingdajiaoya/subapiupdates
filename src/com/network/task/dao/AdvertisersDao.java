package com.network.task.dao;

import java.util.List;

import com.network.task.bean.Advertisers;

public class AdvertisersDao extends TuringBaseDao {

	private static final String TBLPREFIX = "advertisers";

	public static String table() {

		return TBLPREFIX;
	}

	/**
	 * 获取所有有效状态的Advertisers
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Advertisers> getAllValidAdvertisers() {

		List<Advertisers> list = query("select * " + " from " + table() + " where status=0", null, null, new TuringCommonRowMapper(Advertisers.class));

		return list;
	}

	public static void main(String[] args) {
		String sql = "select * " + " from " + table() + " where status=0";
		System.out.println(sql);
	}

}
