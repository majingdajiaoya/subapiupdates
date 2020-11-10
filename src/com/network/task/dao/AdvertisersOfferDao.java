package com.network.task.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;

import com.network.task.bean.AdIcon;
import com.network.task.bean.AdvertiserOfferSize;
import com.network.task.bean.AdvertisersOffer;
import com.network.task.bean.AdvertisersOfferCompare;
import com.network.task.bean.AdvertisersOfferForUpdate;
import com.network.task.bean.AdvertisersOfferUpdate;
import com.network.task.bean.AdvertisersOfferWaitPushBean;
import com.network.task.mail.JavaMailWithAttachment;
import com.network.task.util.DateUtil;
import com.network.task.util.ReadProperties;
import common.Logger;

public class AdvertisersOfferDao extends TuringBaseDao {

	protected static Logger logger = Logger
			.getLogger(AdvertisersOfferDao.class);

	/**
	 * 批量插入网盟广告临时表
	 * 
	 * 首先将临时表中网盟广告都删除再进行批量插入操作
	 */
	public void batchInsertAdvertisersOfferIntoTempTable(
			final List<AdvertisersOffer> advertisersOfferList) {

		if (advertisersOfferList == null || advertisersOfferList.size() == 0) {

			return;
		}

		String sql = "insert into temp_advertisers_offer ("
				+ " temp_adv_offer_id,"
				+ " temp_name,"
				+ " temp_cost_type,"
				+ " temp_offer_type,"
				+ " temp_advertisers_id,"
				+ " temp_pkg,"
				+ " temp_pkg_size,"
				+ " temp_main_icon,"
				+ " temp_preview_url,"
				+ " temp_click_url,"
				+ " temp_country,"
				+ " temp_ecountry,"
				+ " temp_daily_cap,"
				+ " temp_send_cap,"
				+ " temp_payout,"
				+ " temp_expiration,"
				+ " temp_creatives,"
				+ " temp_conversion_flow,"
				+ " temp_supported_carriers,"
				+ " temp_description,"
				+ " temp_os,"
				+ " temp_os_version,"
				+ " temp_device_type,"
				+ " temp_send_type,"
				+ " temp_incent_type,"
				+ " temp_smartlink_type,"
				+ " temp_impress_url,"
				+ " temp_status"
				+ " ) "
				+ " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		super.getJdbcTemplate().batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						ps.setString(1, advertisersOfferList.get(i)
								.getAdv_offer_id());
						ps.setString(2, advertisersOfferList.get(i).getName());

						if (advertisersOfferList.get(i).getCost_type() == null) {

							ps.setNull(3, Types.INTEGER);
						} else {

							ps.setInt(3, advertisersOfferList.get(i)
									.getCost_type());
						}
						if (advertisersOfferList.get(i).getOffer_type() == null) {

							ps.setNull(4, Types.INTEGER);
						} else {

							ps.setInt(4, advertisersOfferList.get(i)
									.getOffer_type());
						}

						ps.setLong(5, advertisersOfferList.get(i)
								.getAdvertisers_id());
						ps.setString(6, advertisersOfferList.get(i).getPkg());
						if (advertisersOfferList.get(i).getPkg_size() == null) {

							ps.setNull(7, Types.FLOAT);
						} else {

							ps.setFloat(7, advertisersOfferList.get(i)
									.getPkg_size());
						}
						ps.setString(8, advertisersOfferList.get(i)
								.getMain_icon());
						ps.setString(9, advertisersOfferList.get(i)
								.getPreview_url());
						ps.setString(10, advertisersOfferList.get(i)
								.getClick_url());
						ps.setString(11, advertisersOfferList.get(i)
								.getCountry());
						ps.setString(12, advertisersOfferList.get(i)
								.getEcountry());
						if (advertisersOfferList.get(i).getDaily_cap() == null) {

							ps.setNull(13, Types.INTEGER);
						} else {

							ps.setInt(13, advertisersOfferList.get(i)
									.getDaily_cap());
						}
						if (advertisersOfferList.get(i).getSend_cap() == null) {

							ps.setNull(14, Types.INTEGER);
						} else {

							ps.setInt(14, advertisersOfferList.get(i)
									.getSend_cap());
						}
						ps.setFloat(15, advertisersOfferList.get(i).getPayout());

						if (advertisersOfferList.get(i).getExpiration() == null) {

							ps.setNull(16, Types.DATE);
						} else {

							ps.setDate(
									16,
									new java.sql.Date(DateUtil.StringToDate(
											advertisersOfferList.get(i)
													.getExpiration()).getTime()));
						}
						ps.setString(17, advertisersOfferList.get(i)
								.getCreatives());
						if (advertisersOfferList.get(i).getConversion_flow() == null) {

							ps.setNull(18, Types.INTEGER);
						} else {

							ps.setInt(18, advertisersOfferList.get(i)
									.getConversion_flow());
						}
						ps.setString(19, advertisersOfferList.get(i)
								.getSupported_carriers());
						ps.setString(20, advertisersOfferList.get(i)
								.getDescription());
						if (advertisersOfferList.get(i).getOs() == null) {

							ps.setNull(21, Types.VARCHAR);
						} else {

							ps.setString(21, advertisersOfferList.get(i)
									.getOs());
						}

						ps.setString(22, advertisersOfferList.get(i)
								.getOs_version());// 系统版本限制

						if (advertisersOfferList.get(i).getDevice_type() == null) {

							ps.setNull(23, Types.INTEGER);
						} else {

							ps.setInt(23, advertisersOfferList.get(i)
									.getDevice_type());
						}
						if (advertisersOfferList.get(i).getSend_type() == null) {

							ps.setNull(24, Types.INTEGER);
						} else {

							ps.setInt(24, advertisersOfferList.get(i)
									.getSend_type());
						}
						if (advertisersOfferList.get(i).getIncent_type() == null) {

							ps.setNull(25, Types.INTEGER);
						} else {

							ps.setInt(25, advertisersOfferList.get(i)
									.getIncent_type());
						}
						ps.setInt(26, advertisersOfferList.get(i)
								.getSmartlink_type() == null ? 1
								: advertisersOfferList.get(i)
										.getSmartlink_type());
						ps.setString(27, advertisersOfferList.get(i)
								.getImpressionUrl() == null ? ""
								: advertisersOfferList.get(i)
										.getImpressionUrl());
						ps.setInt(
								28,
								advertisersOfferList.get(i).getStatus() == null ? 0
										: advertisersOfferList.get(i)
												.getStatus());
					}

					public int getBatchSize() {
						return advertisersOfferList.size();
					}
				});
	}

	/**
	 * 批量插入网盟广告正式表
	 * 
	 */
	public void batchInsertAdvertisersOfferIntoFormalTable(
			final List<AdvertisersOfferCompare> advertisersOfferCompareForAddList) {

		if (advertisersOfferCompareForAddList == null
				|| advertisersOfferCompareForAddList.size() == 0) {

			return;
		}

		String sql = "insert into advertisers_offer ("
				+ " adv_offer_id,"
				+ " name,"
				+ " cost_type,"
				+ " offer_type,"
				+ " advertisers_id,"
				+ " pkg,"
				+ " pkg_size,"
				+ " main_icon,"
				+ " preview_url,"
				+ " click_url,"
				+ " country,"
				+ " ecountry,"
				+ " daily_cap,"
				+ " send_cap,"
				+ " payout,"
				+ " expiration,"
				+ " creatives,"
				+ " conversion_flow,"
				+ " supported_carriers,"
				+ " description,"
				+ " os,"
				+ " os_version,"
				+ " device_type,"
				+ " send_type,"
				+ " incent_type,"
				+ " smartlink_type,"
				+ " impress_url,"
				+ " status"
				+ " ) "
				+ " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		super.getJdbcTemplate().batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_adv_offer_id() == null) {

							ps.setNull(1, Types.VARCHAR);
						} else {

							ps.setString(1, advertisersOfferCompareForAddList
									.get(i).getTemp_adv_offer_id());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_name() == null) {

							ps.setNull(2, Types.VARCHAR);
						} else {

							ps.setString(2, advertisersOfferCompareForAddList
									.get(i).getTemp_name());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_cost_type() == null) {

							ps.setNull(3, Types.INTEGER);
						} else {

							ps.setInt(3,
									advertisersOfferCompareForAddList.get(i)
											.getTemp_cost_type());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_offer_type() == null) {

							ps.setNull(4, Types.INTEGER);
						} else {

							ps.setInt(4,
									advertisersOfferCompareForAddList.get(i)
											.getTemp_offer_type());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_advertisers_id() == null) {

							ps.setNull(5, Types.BIGINT);
						} else {

							ps.setLong(5, advertisersOfferCompareForAddList
									.get(i).getTemp_advertisers_id());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_pkg() == null) {

							ps.setNull(6, Types.VARCHAR);
						} else {

							ps.setString(6, advertisersOfferCompareForAddList
									.get(i).getTemp_pkg());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_pkg_size() == null) {

							ps.setNull(7, Types.FLOAT);
						} else {

							ps.setFloat(7, advertisersOfferCompareForAddList
									.get(i).getTemp_pkg_size());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_main_icon() == null) {

							ps.setNull(8, Types.VARCHAR);
						} else {
							ps.setString(8, advertisersOfferCompareForAddList
									.get(i).getTemp_main_icon());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_preview_url() == null) {

							ps.setNull(9, Types.VARCHAR);
						} else {

							ps.setString(9, advertisersOfferCompareForAddList
									.get(i).getTemp_preview_url());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_click_url() == null) {

							ps.setNull(10, Types.VARCHAR);
						} else {

							ps.setString(10, advertisersOfferCompareForAddList
									.get(i).getTemp_click_url());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_country() == null) {

							ps.setNull(11, Types.VARCHAR);
						} else {

							ps.setString(11, advertisersOfferCompareForAddList
									.get(i).getTemp_country());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_ecountry() == null) {

							ps.setNull(12, Types.VARCHAR);
						} else {

							ps.setString(12, advertisersOfferCompareForAddList
									.get(i).getTemp_ecountry());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_daily_cap() == null) {

							ps.setNull(13, Types.INTEGER);
						} else {

							ps.setInt(13, advertisersOfferCompareForAddList
									.get(i).getTemp_daily_cap());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_send_cap() == null) {

							ps.setNull(14, Types.INTEGER);
						} else {

							ps.setInt(14, advertisersOfferCompareForAddList
									.get(i).getTemp_send_cap());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_payout() == null) {

							ps.setNull(15, Types.FLOAT);
						} else {

							ps.setFloat(15, advertisersOfferCompareForAddList
									.get(i).getTemp_payout());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_expiration() == null) {

							ps.setNull(16, Types.DATE);
						} else {

							ps.setDate(
									16,
									new java.sql.Date(DateUtil.StringToDate(
											advertisersOfferCompareForAddList
													.get(i)
													.getTemp_expiration())
											.getTime()));
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_creatives() == null) {

							ps.setNull(17, Types.VARCHAR);
						} else {

							ps.setString(17, advertisersOfferCompareForAddList
									.get(i).getTemp_creatives());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_conversion_flow() == null) {

							ps.setNull(18, Types.INTEGER);
						} else {

							ps.setInt(18, advertisersOfferCompareForAddList
									.get(i).getTemp_conversion_flow());
						}
						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_supported_carriers() == null) {

							ps.setNull(19, Types.VARCHAR);
						} else {

							ps.setString(19, advertisersOfferCompareForAddList
									.get(i).getTemp_supported_carriers());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_description() == null) {

							ps.setNull(20, Types.VARCHAR);
						} else {

							ps.setString(20, advertisersOfferCompareForAddList
									.get(i).getTemp_description());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_os() == null) {

							ps.setNull(21, Types.VARCHAR);
						} else {

							ps.setString(21, advertisersOfferCompareForAddList
									.get(i).getTemp_os());
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_os_version() == null) {

							ps.setNull(22, Types.VARCHAR);
						} else {

							ps.setString(22, advertisersOfferCompareForAddList
									.get(i).getTemp_os_version());// 系统版本限制
						}

						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_device_type() == null) {

							ps.setNull(23, Types.INTEGER);
						} else {

							ps.setInt(23, advertisersOfferCompareForAddList
									.get(i).getTemp_device_type());
						}
						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_send_type() == null) {

							ps.setNull(24, Types.INTEGER);
						} else {

							ps.setInt(24, advertisersOfferCompareForAddList
									.get(i).getTemp_send_type());
						}
						if (advertisersOfferCompareForAddList.get(i)
								.getTemp_incent_type() == null) {

							ps.setNull(25, Types.INTEGER);
						} else {

							ps.setInt(25, advertisersOfferCompareForAddList
									.get(i).getTemp_incent_type());
						}
						ps.setInt(26, advertisersOfferCompareForAddList.get(i)
								.getTemp_smartlink_type() == null ? 1
								: advertisersOfferCompareForAddList.get(i)
										.getTemp_smartlink_type());

						ps.setString(27,
								advertisersOfferCompareForAddList.get(i)
										.getTemp_impress_url() == null ? ""
										: advertisersOfferCompareForAddList
												.get(i).getTemp_impress_url());

						ps.setInt(28, advertisersOfferCompareForAddList.get(i)
								.getTemp_status() == null ? 0
								: advertisersOfferCompareForAddList.get(i)
										.getTemp_status());
					}

					public int getBatchSize() {
						return advertisersOfferCompareForAddList.size();
					}
				});

	}

	/**
	 * 根据 advertisersId 删除网盟临时表中的 AdvertisersOffer
	 * 
	 * @param advertisersId
	 * @return
	 */
	public void deleteTempAdvertisersOfferByAdvertisersId(Long advertisersId) {

		if (advertisersId == null) {

			return;
		}

		super.getJdbcTemplate()
				.update("DELETE FROM temp_advertisers_offer where 1=1 and temp_advertisers_id=?",
						new Object[] { advertisersId });
	}

	/**
	 * 查询正式广告表和临时导入广告表中广告对比信息，left join 区分下线及修改过的广告
	 * 
	 * @param advertisersId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<AdvertisersOfferCompare> getAdvertisersOfferCompareListForOutlineAndUpdate(
			Long advertisersId) {
		String sql = "select t.*, temp.* "
				+ " from advertisers_offer t "
				+ " left join temp_advertisers_offer temp on t.adv_offer_id=temp.temp_adv_offer_id  and t.advertisers_id=temp.temp_advertisers_id"
				+ " where t.advertisers_id=?  and t.status = 0  and t.send_type=0";
		List<AdvertisersOfferCompare> list = query(sql,
				new Object[] { advertisersId }, new int[] { Types.BIGINT },
				new TuringCommonRowMapper(AdvertisersOfferCompare.class));

		if (list != null && list.size() > 0) {

			return list;
		}

		return null;
	}

	/**
	 * 查询新增广告，right join 区分
	 * 
	 * 从网盟广告正式表(advertisers_offer)中查询有效的广告进行比对
	 * 
	 * @param advertisersId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<AdvertisersOfferCompare> getAdvertisersOfferCompareListForAdd(
			Long advertisersId) {

		List<AdvertisersOfferCompare> list = query(
				"SELECT tt.*, temp.* "
						+ " FROM (SELECT * FROM advertisers_offer t WHERE id in ( SELECT min(id) from advertisers_offer ts where ts.advertisers_id=? Group By ts.adv_offer_id ) ) tt "
						+ " RIGHT JOIN temp_advertisers_offer temp ON tt.adv_offer_id=temp.temp_adv_offer_id and tt.advertisers_id=temp.temp_advertisers_id "
						+ " WHERE temp.temp_advertisers_id=? AND ( tt.adv_offer_id is NULL or tt.status<0 )",
				new Object[] { advertisersId, advertisersId }, new int[] {
						Types.BIGINT, Types.BIGINT },
				new TuringCommonRowMapper(AdvertisersOfferCompare.class));

		if (list != null && list.size() > 0) {

			return list;
		}

		return null;
	}

	/**
	 * 查询新增广告主键ID List
	 * 
	 * @param advertisersId
	 *            : 网盟ID
	 * @param advOfferIds
	 *            : 以 , 分割的网盟自身广告ID串
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<AdvertisersOfferWaitPushBean> getAdvertisersOfferIdsForAdd(
			Long advertisersId, String advOfferIds) {

		List<AdvertisersOfferWaitPushBean> list = query(
				"select t.id as advertisers_offer_id, t.os , advertisers_id, payout, daily_cap, cost_type, offer_type ,country"
						+ " FROM advertisers_offer t "
						+ " WHERE t.advertisers_id=? AND t.status = 0 AND t.adv_offer_id in("
						+ advOfferIds + ")", new Object[] { advertisersId },
				new int[] { Types.BIGINT }, new TuringCommonRowMapper(
						AdvertisersOfferWaitPushBean.class));

		if (list != null && list.size() > 0) {

			return list;
		}

		return null;
	}

	/**
	 * 批量下线网盟广告
	 * 
	 * @param offlineAdvOfferIdList
	 */
	public void batchOfflineAdvertisersOffers(
			final List<String> offlineAdvOfferIdList) {

		Date currentTime = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		String sql = "update advertisers_offer set "
				+ " status = -2,updates=updates+1,utime=" + "\""
				+ formatter.format(currentTime) + "\"" // 系统下线
				+ " WHERE adv_offer_id =?";

		super.getJdbcTemplate().batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						ps.setString(1, offlineAdvOfferIdList.get(i));
					}

					public int getBatchSize() {
						return offlineAdvOfferIdList.size();
					}
				});
	}

	/**
	 * 批量更新网盟广告
	 * 
	 */
	public void batchUpdateFormalAdvertisersOfferTable(
			final List<AdvertisersOfferForUpdate> advertisersOfferList) {

		if (advertisersOfferList == null || advertisersOfferList.size() == 0) {

			return;
		}
		final List<AdvertisersOfferForUpdate> advertisersOfferForUpdateByPriceList = new ArrayList<AdvertisersOfferForUpdate>();
		// 比较单价变化的广告进行更新
		for (AdvertisersOfferForUpdate advertisersOfferForUpdate : advertisersOfferList) {
			// 如果单价不变
			if (!advertisersOfferForUpdate.getPayout().equals(
					advertisersOfferForUpdate.getOld_payout())) {
				advertisersOfferForUpdateByPriceList
						.add(advertisersOfferForUpdate);
			}
		}

		String sql = "UPDATE advertisers_offer SET " + " adv_offer_id=?,"
				+ " name=?," + " advertisers_id=?," + " pkg=?,"
				+ " pkg_size=?," + " main_icon=?," + " preview_url=?,"
				+ " click_url=?," + " country=?," + " ecountry=?,"
				+ " daily_cap=?," + " send_cap=?," + " payout=?,"
				+ " expiration=?," + " creatives=?," + " conversion_flow=?,"
				+ " supported_carriers=?," + " description=?," + " os=?,"
				+ " os_version=?," + " device_type=?," + " send_type=?,"
				+ " incent_type=?," + " smartlink_type=?," + " impress_url=?,"
				+ " status=?" + " WHERE id=? and status='0'";

		super.getJdbcTemplate().batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						if (advertisersOfferList.get(i).getAdv_offer_id() == null) {

							ps.setNull(1, Types.VARCHAR);
						} else {

							ps.setString(1, advertisersOfferList.get(i)
									.getAdv_offer_id());
						}

						if (advertisersOfferList.get(i).getName() == null) {

							ps.setNull(2, Types.VARCHAR);
						} else {
							ps.setString(2, advertisersOfferList.get(i)
									.getName());
						}

						if (advertisersOfferList.get(i).getAdvertisers_id() == null) {

							logger.info("Advertisers_id is NULL, Id is :="
									+ advertisersOfferList.get(i).getId());

							ps.setNull(3, Types.BIGINT);
						} else {
							ps.setLong(3, advertisersOfferList.get(i)
									.getAdvertisers_id());
						}

						if (advertisersOfferList.get(i).getPkg() == null) {

							ps.setNull(4, Types.VARCHAR);
						} else {
							ps.setString(4, advertisersOfferList.get(i)
									.getPkg());
						}

						if (advertisersOfferList.get(i).getPkg_size() == null) {

							ps.setNull(5, Types.FLOAT);
						} else {

							ps.setFloat(5, advertisersOfferList.get(i)
									.getPkg_size());
						}
						if (advertisersOfferList.get(i).getMain_icon() == null) {

							ps.setNull(6, Types.VARCHAR);
						} else {

							ps.setString(6, advertisersOfferList.get(i)
									.getMain_icon());
						}
						if (advertisersOfferList.get(i).getPreview_url() == null) {

							ps.setNull(7, Types.VARCHAR);
						} else {

							ps.setString(7, advertisersOfferList.get(i)
									.getPreview_url());
						}
						if (advertisersOfferList.get(i).getClick_url() == null) {

							ps.setNull(8, Types.VARCHAR);
						} else {
							ps.setString(8, advertisersOfferList.get(i)
									.getClick_url());
						}
						if (advertisersOfferList.get(i).getCountry() == null) {

							ps.setNull(9, Types.VARCHAR);
						} else {

							ps.setString(9, advertisersOfferList.get(i)
									.getCountry());
						}
						if (advertisersOfferList.get(i).getEcountry() == null) {

							ps.setNull(10, Types.VARCHAR);
						} else {

							ps.setString(10, advertisersOfferList.get(i)
									.getEcountry());
						}

						if (advertisersOfferList.get(i).getDaily_cap() == null) {

							ps.setNull(11, Types.INTEGER);
						} else {

							ps.setInt(11, advertisersOfferList.get(i)
									.getDaily_cap());
						}
						if (advertisersOfferList.get(i).getSend_cap() == null) {

							ps.setNull(12, Types.INTEGER);
						} else {

							ps.setInt(12, advertisersOfferList.get(i)
									.getSend_cap());
						}

						if (advertisersOfferList.get(i).getPayout() == null) {

							ps.setNull(13, Types.FLOAT);
						} else {

							ps.setFloat(13, advertisersOfferList.get(i)
									.getPayout());
						}

						if (advertisersOfferList.get(i).getExpiration() == null) {

							ps.setNull(14, Types.DATE);
						} else {

							ps.setDate(
									14,
									new java.sql.Date(DateUtil.StringToDate(
											advertisersOfferList.get(i)
													.getExpiration()).getTime()));
						}
						if (advertisersOfferList.get(i).getCreatives() == null) {

							ps.setNull(15, Types.VARCHAR);
						} else {

							ps.setString(15, advertisersOfferList.get(i)
									.getCreatives());
						}

						if (advertisersOfferList.get(i).getConversion_flow() == null) {

							ps.setNull(16, Types.INTEGER);
						} else {

							ps.setInt(16, advertisersOfferList.get(i)
									.getConversion_flow());
						}

						if (advertisersOfferList.get(i).getSupported_carriers() == null) {

							ps.setNull(17, Types.VARCHAR);
						} else {

							ps.setString(17, advertisersOfferList.get(i)
									.getSupported_carriers());
						}

						if (advertisersOfferList.get(i).getDescription() == null) {

							ps.setNull(18, Types.VARCHAR);
						} else {

							ps.setString(18, advertisersOfferList.get(i)
									.getDescription());
						}

						if (advertisersOfferList.get(i).getOs() == null) {

							ps.setNull(19, Types.VARCHAR);
						} else {

							ps.setString(19, advertisersOfferList.get(i)
									.getOs());
						}

						if (advertisersOfferList.get(i).getOs_version() == null) {

							ps.setNull(20, Types.VARCHAR);
						} else {

							ps.setString(20, advertisersOfferList.get(i)
									.getOs_version());// 系统版本限制
						}

						if (advertisersOfferList.get(i).getDevice_type() == null) {

							ps.setNull(21, Types.INTEGER);
						} else {

							ps.setInt(21, advertisersOfferList.get(i)
									.getDevice_type());
						}
						if (advertisersOfferList.get(i).getSend_type() == null) {

							ps.setNull(22, Types.INTEGER);
						} else {

							ps.setInt(22, advertisersOfferList.get(i)
									.getSend_type());
						}
						if (advertisersOfferList.get(i).getIncent_type() == null) {

							ps.setNull(23, Types.INTEGER);
						} else {

							ps.setInt(23, advertisersOfferList.get(i)
									.getIncent_type());
						}
						ps.setInt(24, advertisersOfferList.get(i)
								.getSmartlink_type() == null ? 1
								: advertisersOfferList.get(i)
										.getSmartlink_type());
						ps.setString(25, advertisersOfferList.get(i)
								.getImpressionUrl() == null ? ""
								: advertisersOfferList.get(i)
										.getImpressionUrl());
						ps.setInt(
								26,
								advertisersOfferList.get(i).getStatus() == null ? 0
										: advertisersOfferList.get(i)
												.getStatus());
						ps.setLong(27, advertisersOfferList.get(i).getId());
					}

					public int getBatchSize() {

						return advertisersOfferList.size();
					}
				});

	}

	// 操作advoffers 以及 advoffers update 数据表
	public Integer getadvoffersSize(Long advertisersId) {
		if (advertisersId == null) {
			return null;
		}

		// AdvertiserOfferSize
		String mysql = " SELECT count(*) as allsize from  advertisers_offer where `status`=0 and advertisers_id="
				+ advertisersId;

		List<AdvertiserOfferSize> list = query(mysql, null, null,
				new TuringCommonRowMapper(AdvertiserOfferSize.class));

		if (list != null && list.size() > 0) {

			return list.get(0).getAllsize();
		}

		return 0;
	}

	public void batchInsertAdvertisersOfferUpdate(
			final List<AdvertisersOfferUpdate> advertisersOfferList) {

		if (advertisersOfferList == null || advertisersOfferList.size() == 0) {

			return;
		}

		String sql = "insert into advertisers_offerupdate ("
				+ " advertisers_id," + " befroreallsize," + " nowallsize,"
				+ " offlinesize," + " updatesize," + " addsize" + " ) "
				+ " values(?,?,?,?,?,?)";

		super.getJdbcTemplate().batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						ps.setLong(1, advertisersOfferList.get(i)
								.getAdvertisers_id());
						ps.setInt(2, advertisersOfferList.get(i)
								.getBefroreallsize());
						ps.setInt(3, advertisersOfferList.get(i)
								.getNowallsize());
						ps.setInt(4, advertisersOfferList.get(i)
								.getOfflinesize());
						ps.setInt(5, advertisersOfferList.get(i)
								.getUpdatesize());
						ps.setInt(6, advertisersOfferList.get(i).getAddsize());

					}

					public int getBatchSize() {
						return advertisersOfferList.size();
					}
				});

	}

	/**
	 * 批量更新网盟广告
	 * 
	 */
	public void batchUpdateOpenFormalAdvertisersOfferTable(
			final List<AdvertisersOfferCompare> advertisersOfferList) {

		if (advertisersOfferList == null || advertisersOfferList.size() == 0) {

			return;
		}
		// ,utime=" + "\"" + formatter.format(currentTime) + "\""
		String sql = "UPDATE advertisers_offer SET   " + " pkg=?,"
				+ " main_icon=?," + " preview_url=?," + " click_url=?,"
				+ " country=?," + " ecountry=?," + " daily_cap=?,"
				+ " send_cap=?," + " payout=?," + " creatives=?,"
				+ " supported_carriers=?," + " description=?,"
				+ " updates=updates+1," + " status=? ,utime= ?"
				+ "  WHERE id=? ";

		super.getJdbcTemplate().batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						if (advertisersOfferList.get(i).getTemp_pkg() == null) {

							ps.setNull(1, Types.VARCHAR);
						} else {
							ps.setString(1, advertisersOfferList.get(i)
									.getTemp_pkg());
						}

						if (advertisersOfferList.get(i).getTemp_main_icon() == null) {

							ps.setNull(2, Types.VARCHAR);
						} else {

							ps.setString(2, advertisersOfferList.get(i)
									.getTemp_main_icon());
						}
						if (advertisersOfferList.get(i).getTemp_preview_url() == null) {

							ps.setNull(3, Types.VARCHAR);
						} else {

							ps.setString(3, advertisersOfferList.get(i)
									.getTemp_preview_url());
						}
						if (advertisersOfferList.get(i).getTemp_click_url() == null) {

							ps.setNull(4, Types.VARCHAR);
						} else {
							ps.setString(4, advertisersOfferList.get(i)
									.getTemp_click_url());
						}
						if (advertisersOfferList.get(i).getTemp_country() == null) {

							ps.setNull(5, Types.VARCHAR);
						} else {

							ps.setString(5, advertisersOfferList.get(i)
									.getTemp_country());
						}
						if (advertisersOfferList.get(i).getTemp_ecountry() == null) {

							ps.setNull(6, Types.VARCHAR);
						} else {

							ps.setString(6, advertisersOfferList.get(i)
									.getTemp_ecountry());
						}

						if (advertisersOfferList.get(i).getTemp_daily_cap() == null) {

							ps.setNull(7, Types.INTEGER);
						} else {

							ps.setInt(7, advertisersOfferList.get(i)
									.getTemp_daily_cap());
						}
						if (advertisersOfferList.get(i).getTemp_send_cap() == null) {

							ps.setNull(8, Types.INTEGER);
						} else {

							ps.setInt(8, advertisersOfferList.get(i)
									.getTemp_send_cap());
						}

						if (advertisersOfferList.get(i).getTemp_payout() == null) {

							ps.setNull(9, Types.FLOAT);
						} else {

							ps.setFloat(9, advertisersOfferList.get(i)
									.getTemp_payout());
						}

						if (advertisersOfferList.get(i).getTemp_creatives() == null) {

							ps.setNull(10, Types.VARCHAR);
						} else {

							ps.setString(10, advertisersOfferList.get(i)
									.getTemp_creatives());
						}

						if (advertisersOfferList.get(i)
								.getTemp_supported_carriers() == null) {

							ps.setNull(11, Types.VARCHAR);
						} else {

							ps.setString(11, advertisersOfferList.get(i)
									.getTemp_supported_carriers());
						}

						if (advertisersOfferList.get(i).getTemp_description() == null) {

							ps.setNull(12, Types.VARCHAR);
						} else {

							ps.setString(12, advertisersOfferList.get(i)
									.getTemp_description());
						}

						ps.setInt(13, advertisersOfferList.get(i)
								.getTemp_status() == null ? 0
								: advertisersOfferList.get(i).getTemp_status());

						ps.setObject(14, new Date());
						ps.setLong(15, advertisersOfferList.get(i).getId());
					}

					public int getBatchSize() {

						return advertisersOfferList.size();
					}
				});

	}

	public List<AdvertisersOffer> getMainiconNull() {

		String sql = "SELECT * from advertisers_offer where advertisers_id in (48,53,55,56,58,59,67,68,80,81,91,92,93,94,95,96,97,101) and `status`=0 and main_icon is null ";
		List<AdvertisersOffer> list = query(sql, null, null,
				new TuringCommonRowMapper(AdvertisersOffer.class));

		if (list != null && list.size() > 0) {

			return list;
		}
		return null;
	}

	public AdvertisersOffer gethadiconAdvertisersOffer(String pkg) {
		String sql = "SELECT * from advertisers_offer where pkg=? and (main_icon is not null and main_icon!='') limit 1";
		List<AdvertisersOffer> list = query(sql, new Object[] { pkg },
				new int[] { Types.VARCHAR }, new TuringCommonRowMapper(
						AdvertisersOffer.class));
		if (list != null && list.size() > 0) {
			return list.get(0);
		}

		return null;
	}

	public String gethadiconAdvertisersOfferForString(String pkg) {
		String sql = "SELECT * from advertisers_offer where pkg=? and (main_icon is not null and main_icon!='') limit 1";
		List<AdvertisersOffer> list = query(sql, new Object[] { pkg },
				new int[] { Types.VARCHAR }, new TuringCommonRowMapper(
						AdvertisersOffer.class));
		if (list != null && list.size() > 0) {
			return list.get(0).getMain_icon();
		}
		sql = "SELECT * from offers_icon where pkg=? and (icon is not null and icon!='') limit 1";
		List<AdIcon> list2 = query(sql, new Object[] { pkg },
				new int[] { Types.VARCHAR }, new TuringCommonRowMapper(
						AdIcon.class));
		if (list2 != null && list2.size() > 0) {
			return list2.get(0).getIcon();
		}

		return null;
	}

	public void batchUpdateAdvertisersOfferForIcon(
			final List<AdvertisersOffer> advertisersOfferList) {

		if (advertisersOfferList == null || advertisersOfferList.size() == 0) {

			return;
		}

		String sql = "UPDATE advertisers_offer SET " + " main_icon=?"
				+ " WHERE id=?";

		super.getJdbcTemplate().batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						if (advertisersOfferList.get(i).getMain_icon() == null) {

							ps.setNull(1, Types.VARCHAR);
						} else {

							ps.setString(1, advertisersOfferList.get(i)
									.getMain_icon());
						}
						ps.setLong(2, advertisersOfferList.get(i).getId());
					}

					public int getBatchSize() {

						return advertisersOfferList.size();
					}
				});

	}

	public void updateStatus(final Long advId, final int status) {
		String sql = "UPDATE advertisers_offer SET status='-2'"
				+ " WHERE advertisers_id=? and status=0";
		super.getJdbcTemplate().update(sql, new Object[] { advId });

	}

	public void deleteAllTempAdvertisersOffer() {
		super.getJdbcTemplate().update("DELETE FROM temp_advertisers_offer ",
				new Object[] { null });
	}

	public static void main(String[] args) {
		String sql = "select t.*, temp.* "
				+ " from advertisers_offer t "
				+ " left join temp_advertisers_offer temp on t.adv_offer_id=temp.temp_adv_offer_id  and t.advertisers_id=temp.temp_advertisers_id"
				+ " where t.advertisers_id=?  and t.status = 0  and t.send_type=0";
		System.out.println(sql);
	}

}
