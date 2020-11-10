package com.network.task.dao;

import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;


import org.springframework.jdbc.core.support.JdbcDaoSupport;

public class BaseDAO extends JdbcDaoSupport {

	@Resource
	public void setDb(DataSource dataSource) {
		super.setDataSource(dataSource);
	}

	protected <T> List<T> queryForList(String sql, Object[] parameters, Class<T> result) {
		return super.getJdbcTemplate().query(sql, parameters, new CommonRowMapper<T>(result));
	}

	protected <T> List<T> queryForList(String sql, Class<T> result) {
		return super.getJdbcTemplate().query(sql, new CommonRowMapper<T>(result));
	}

	protected <T> T queryForObject(String sql, Object[] parameters, Class<T> result) {
		List<T> list = getJdbcTemplate().query(sql, parameters, new CommonRowMapper<T>(result));
		if(list==null || list.isEmpty()) return null;
		return list.get(0);
	}
	
	protected <T> T queryForObject(String sql, Class<T> result) {
		List<T> list = getJdbcTemplate().query(sql, new CommonRowMapper<T>(result));
		if(list==null || list.isEmpty()) return null;
		return list.get(0);
	}
	

}
