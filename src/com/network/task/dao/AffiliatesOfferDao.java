package com.network.task.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;

import com.network.task.bean.AdvertisersOfferCompare;
import com.network.task.bean.AdvertisersOfferForUpdate;
import com.network.task.bean.AffiliatesOffer;
import com.network.task.bean.AffiliatesOfferPull;

public class AffiliatesOfferDao extends TuringBaseDao {

	/**
	 * 批量插入渠道广告
	 * 
	 * @param affiliatesOfferPullList
	 */
	public void batchInsertAffiliatesOffer(
			final List<AffiliatesOfferPull> affiliatesOfferPullList) {

		String sql = "insert into affiliates_offer (" + " tracking_id, "
				+ " advertisers_offer_id," + " advertisers_id,"
				+ " affiliates_id," + " affiliates_sub_id,"
				+ " affiliates_click_url," + " cap," + " payout,"
				+ " hack_convert_rate," + " send_type," + " status,"
				+ " max_click," + " cvr," + " addpayoutrate" + " ) "
				+ " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		super.getJdbcTemplate().batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						ps.setString(1, affiliatesOfferPullList.get(i)
								.getTracking_id());
						ps.setLong(2, affiliatesOfferPullList.get(i)
								.getAdvertisers_offer_id());
						ps.setLong(3, affiliatesOfferPullList.get(i)
								.getAdvertisers_id());
						ps.setLong(4, affiliatesOfferPullList.get(i)
								.getAffiliates_id());
						ps.setString(5, affiliatesOfferPullList.get(i)
								.getAffiliates_sub_id());
						ps.setString(6, affiliatesOfferPullList.get(i)
								.getAffiliates_click_url());
						ps.setInt(7, affiliatesOfferPullList.get(i).getCap());
						ps.setFloat(8, affiliatesOfferPullList.get(i)
								.getPayout());
						ps.setFloat(9, affiliatesOfferPullList.get(i)
								.getHack_convert_rate());
						ps.setInt(10, affiliatesOfferPullList.get(i)
								.getSend_type());
						ps.setInt(11, affiliatesOfferPullList.get(i)
								.getStatus());
						ps.setInt(12, affiliatesOfferPullList.get(i)
								.getMax_click());
						ps.setFloat(13, affiliatesOfferPullList.get(i).getCvr());
						ps.setInt(14, affiliatesOfferPullList.get(i)
								.getAddpayoutrate());
					}

					public int getBatchSize() {
						return affiliatesOfferPullList.size();
					}
				});

	}

	/**
	 * 批量下线网盟分配的渠道广告
	 * 
	 * @param offlineAdvOfferIdList
	 */
	public void batchOfflineAffiliatesOffers(
			final List<Long> offlineAdvertisersOfferIdList) {

		if (offlineAdvertisersOfferIdList == null
				|| offlineAdvertisersOfferIdList.size() == 0) {

			return;
		}
		Date currentTime = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		String sql = "update affiliates_offer set " + " status = -2 ,utime="
				+ "\"" + formatter.format(currentTime) + "\"" // 系统下线
				+ " WHERE status = 0 and advertisers_offer_id =?";

		super.getJdbcTemplate().batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						ps.setLong(1, offlineAdvertisersOfferIdList.get(i));
					}

					public int getBatchSize() {
						return offlineAdvertisersOfferIdList.size();
					}
				});
	}

	/**
	 * 批量更新下发给渠道的网盟广告 - 如果单价更新了，则渠道广告涉及到单价、cap、利润率更新 -
	 * 单价调整为网盟最新单价，cap设置为网盟广告最新cap，利润率更新为渠道固定默认利润率(30%)
	 * 
	 * @param advertisersOfferForUpdateList
	 */
	public void batchUpdateFormalAffiliatesOfferTable(
			final List<AdvertisersOfferForUpdate> advertisersOfferForUpdateList) {

		if (advertisersOfferForUpdateList == null
				|| advertisersOfferForUpdateList.size() == 0) {

			return;
		}

		// 筛选单价变化的广告
		final List<AdvertisersOfferForUpdate> advertisersOfferForUpdateByPriceList = new ArrayList<AdvertisersOfferForUpdate>();

		// 比较单价变化的广告进行更新
		for (AdvertisersOfferForUpdate advertisersOfferForUpdate : advertisersOfferForUpdateList) {
			// 如果单价不变
			if (advertisersOfferForUpdate.getPayout().equals(
					advertisersOfferForUpdate.getOld_payout())
					&& (advertisersOfferForUpdate.getDaily_cap().intValue() == advertisersOfferForUpdate
							.getOld_daily_cap().intValue())) {
				continue;
			} else {
				advertisersOfferForUpdateByPriceList
						.add(advertisersOfferForUpdate);
			}
		}

		if (advertisersOfferForUpdateByPriceList == null
				|| advertisersOfferForUpdateByPriceList.size() == 0) {

			return;
		}

		// 提价后价格改变

		// 批量更新
		String sql = "UPDATE affiliates_offer SET "
				+ " payout=? "
				+ " WHERE advertisers_offer_id=? and status=0 and addpayoutrate=0";

		super.getJdbcTemplate().batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						if (advertisersOfferForUpdateByPriceList.get(i)
								.getPayout() == null) {

							ps.setNull(1, Types.FLOAT);
						} else {
							ps.setFloat(1, advertisersOfferForUpdateByPriceList
									.get(i).getPayout());
						}
						if (advertisersOfferForUpdateByPriceList.get(i).getId() != null) {
							ps.setLong(2, advertisersOfferForUpdateByPriceList
									.get(i).getId());
						} else {
							ps.setLong(2, Long.valueOf("0"));
						}

					}

					public int getBatchSize() {

						return advertisersOfferForUpdateByPriceList.size();
					}
				});

	}

	/**
	 * 批量更新下发给渠道的网盟广告 - 如果单价更新了，则渠道广告涉及到单价、cap、利润率更新 -
	 * 单价调整为网盟最新单价，cap设置为网盟广告最新cap，利润率更新为渠道固定默认利润率(30%)
	 * 
	 * @param advertisersOfferForUpdateList
	 */
	public void batchUpdateOpenFormalAffiliatesOfferTable(
			final List<AdvertisersOfferCompare> advertisersOfferForUpdateList) {

		if (advertisersOfferForUpdateList == null
				|| advertisersOfferForUpdateList.size() == 0) {

			return;
		}

		// 批量更新
		String sql = "UPDATE affiliates_offer SET  status=0"
				+ " WHERE advertisers_offer_id=?  and status=-2  ";

		super.getJdbcTemplate().batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						ps.setLong(1, advertisersOfferForUpdateList.get(i)
								.getId());
					}

					public int getBatchSize() {

						return advertisersOfferForUpdateList.size();
					}
				});

	}

	@SuppressWarnings("unchecked")
	public void batchUpdateOpenFormalAffiliatesOfferTableForAuto(
			final List<AdvertisersOfferCompare> advertisersOfferForUpdateList,
			Long advertisersId) {

		if (advertisersOfferForUpdateList == null
				|| advertisersOfferForUpdateList.size() == 0) {

			return;
		}
		final Map<Long, AdvertisersOfferCompare> allmapAdvertisersOfferCompare = new HashMap<Long, AdvertisersOfferCompare>();
		String listadvofferid = "";
		for (int i = 0; i < advertisersOfferForUpdateList.size(); i++) {
			AdvertisersOfferCompare temp = advertisersOfferForUpdateList.get(i);
			Long id = temp.getId();
			allmapAdvertisersOfferCompare.put(id, temp);
			if (listadvofferid.length() == 0) {
				listadvofferid = listadvofferid + id;
			} else {
				listadvofferid = listadvofferid + "," + id;
			}
		}
		String sqls = "SELECT tt.* from ((SELECT * from affiliates_offer where status=-2  and advertisers_id="
				+ advertisersId
				+ " and advertisers_offer_id in ( "
				+ listadvofferid
				+ " )  ) tt "
				+ "inner join advertisers_distribute ad on tt.advertisers_id=ad.advertisers_id and tt.affiliates_id=ad.affiliates_id  )";
		logger.info("sqls_=" + sqls);
		final List<AffiliatesOffer> needupdatlist = query(sqls, null, null,
				new TuringCommonRowMapper(AffiliatesOffer.class));
		if (needupdatlist != null && needupdatlist.size() > 0) {
			System.out.println("needupdatlist size " + needupdatlist.size()
					+ " advertisersId:" + advertisersId);
			System.out.println("needupdatlist 0 "
					+ needupdatlist.get(0).getId());
			// 批量更新
			String sql = "UPDATE affiliates_offer SET " + " status=0"
					+ " WHERE  status='-2' and  id=?";

			super.getJdbcTemplate().batchUpdate(sql,
					new BatchPreparedStatementSetter() {
						public void setValues(PreparedStatement ps, int i)
								throws SQLException {
							AffiliatesOffer tempAffiliatesOffer = needupdatlist
									.get(i);
							ps.setLong(1, tempAffiliatesOffer.getId());
						}

						public int getBatchSize() {

							return needupdatlist.size();
						}
					});
		}

	}

	public void updateStatus(final Long advId, final int status) {
		String sql = "UPDATE affiliates_offer SET  status='-2'"
				+ " WHERE advertisers_id=? and status=0";
		super.getJdbcTemplate().update(sql, new Object[] { advId });

	}

	public static void main(String[] args) {
		String s1,s2,s3 = "abc", s4 ="abc" ;
		s1 = new String("abc");
		s2 = new String("abc");
	}
}
