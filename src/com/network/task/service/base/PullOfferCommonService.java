package com.network.task.service.base;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.network.task.bean.Advertisers;
import com.network.task.bean.AdvertisersOffer;
import com.network.task.bean.AdvertisersOfferCompare;
import com.network.task.bean.AdvertisersOfferForUpdate;
import com.network.task.bean.AdvertisersOfferUpdate;
import com.network.task.bean.AdvertisersOfferWaitPushBean;
import com.network.task.bean.Affiliates;
import com.network.task.bean.AffiliatesOfferPull;
import com.network.task.dao.AdvertisersDistributeDao;
import com.network.task.dao.AdvertisersOfferDao;
import com.network.task.dao.AffiliatesOfferDao;
import com.network.task.schedule.PullOfferJob;
import com.network.task.util.APILimitUtil;
import com.network.task.util.DataUtils;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringNumberUtil;
import com.network.task.util.TuringSpringHelper;
import com.network.task.util.TuringStringUtil;
import common.Logger;

public class PullOfferCommonService {
	public static Map<String, String> blacklist_pkg_map = new HashMap<String, String>();
	protected static Logger logger = Logger
			.getLogger(PullOfferCommonService.class);

	/**
	 * 入网盟广告表处理
	 * 
	 * 根据不同网盟做单子筛选及数量限制，防止网盟放大量单子吸量
	 * 
	 * @param advertisers
	 * @param waitPullAdvertisersOfferListBeforeFilter
	 */
	public void doPullOffer(Advertisers advertisers,
			List<AdvertisersOffer> waitPullAdvertisersOfferListBeforeFilter) {
		if (advertisers == null) {
			return;
		}
		Long advertisersId = advertisers.getId();
		String name = advertisers.getName();
		if (advertisersId == null
				|| waitPullAdvertisersOfferListBeforeFilter == null
				|| waitPullAdvertisersOfferListBeforeFilter.size() == 0) {

			return;
		}

		List<AdvertisersOffer> waitPullAdvertisersOfferList = new ArrayList<AdvertisersOffer>();
		// 过滤pkg黑名单中的广告
		for (int i = 0; i < waitPullAdvertisersOfferListBeforeFilter.size(); i++) {
			AdvertisersOffer advertisersOffer = waitPullAdvertisersOfferListBeforeFilter
					.get(i);
			if (DataUtils.blacklist_pkg_map != null
					&& !DataUtils.blacklist_pkg_map.isEmpty()) {
				if (!OverseaStringUtil.isBlank(DataUtils.blacklist_pkg_map
						.get(advertisersOffer.getPkg()))) {
					logger.info("filter pkg := " + advertisersOffer.getPkg());
					continue;
				}
			}

			boolean apifFlag = APILimitUtil.checkAPILimit(advertisersOffer,
					advertisers);
			if (!apifFlag) {
				continue;
			}

			String ctry = advertisersOffer.getCountry();
			if (ctry == null) {
				logger.info("ctry is null" + advertisersOffer.getAdv_offer_id());
				continue;
			}
			if (ctry.contains("US")) {
				Float payout = advertisersOffer.getPayout();
				if (payout < 0.1) {
					if ("US".equals(ctry)) {
						continue;
					} else {
						ctry = ctry.replace("US:", "");
						ctry = ctry.replace("US", "");
					}
				}

			}

			waitPullAdvertisersOfferList.add(advertisersOffer);
		}

		logger.info("waitPull-AdvertisersOfferList size := "
				+ waitPullAdvertisersOfferList.size() + " advertisersId is:"
				+ advertisersId);

		// 入临时网盟广告表
		if (waitPullAdvertisersOfferList.size() > 0) {
			AdvertisersOfferDao advertisersOfferDao = (AdvertisersOfferDao) TuringSpringHelper
					.getBean("advertisersOfferDao");
			// 删除临时表上次的网盟广告
			logger.info("step-1: deleteTempAdvertisersOfferByAdvertisersId["
					+ advertisersId + "]");

			advertisersOfferDao
					.deleteTempAdvertisersOfferByAdvertisersId(advertisersId);
			// 导入临时网盟广告表
			logger.info("step-2: batchInsertAdvertisersOfferIntoTempTable"
					+ " advertisersId is:" + advertisersId);
			advertisersOfferDao
					.batchInsertAdvertisersOfferIntoTempTable(waitPullAdvertisersOfferList);
		}

		// 入正式网盟广告表
		logger.info("step-3: doPullOffer2FormalTable  " + " advertisersId is:"
				+ advertisersId);
		doPullOffer2FormalTable(advertisersId, name,
				waitPullAdvertisersOfferList.size());
	}

	/**
	 * 入正式网盟广告表
	 * 
	 * 临时网盟广告表数据和正式网盟广告表对比，筛选新增、下线、修改的广告分别进行处理
	 * 
	 * 1. 下线的广告，进行下线操作，并同步将网盟下放给渠道的广告进行关闭 2.
	 * 修改的，根据单价、推广链接、main_icon、预览链接、国家、类型是否改变进行判断，进行更新操作 3.
	 * 新增直接入网盟广告正式表，并同步将网盟新增广告下发给相关已分配此网盟的渠道 4.
	 * 
	 * @param advertisersId
	 *            : 系统中网盟ID
	 */
	public void doPullOffer2FormalTable(Long advertisersId, String name,
			int allsize) {

		if (advertisersId == null) {

			return;
		}

		AdvertisersOfferDao advertisersOfferDao = (AdvertisersOfferDao) TuringSpringHelper
				.getBean("advertisersOfferDao");

		Integer beforeallsize = advertisersOfferDao
				.getadvoffersSize(advertisersId);
		Integer nowallsize = allsize;
		Integer offlinesize = 0;
		Integer updatesize = 0;
		Integer addsize = 0;

		// 获取对比广告，进行下线及修改广告操作
		List<AdvertisersOfferCompare> advertisersOfferCompareList = advertisersOfferDao
				.getAdvertisersOfferCompareListForOutlineAndUpdate(advertisersId);

		if (advertisersOfferCompareList != null
				&& advertisersOfferCompareList.size() > 0) {

			logger.info("step-4: advertisersOfferCompareList := "
					+ advertisersOfferCompareList.size() + " advertisersId is:"
					+ advertisersId);

			/**
			 * (1) 如果比较对象中：tem_adv_offer_id 为空，则adv_offer_id的广告下线 (2)
			 * 对比payout、daily_cap
			 * 、click_url、pkg、preview_url、main_icon、expiration是否和相关temp的字段相同
			 * ，如果不同则是待更新广告
			 */
			List<String> offlineAdvOfferIdList = new ArrayList<String>();// 下线网盟广告ID列表
																			// key为adv_offer_id(网盟自有广告ID)
			List<Long> offlineAdvertisersOfferIdList = new ArrayList<Long>();// 下线的网盟广告ID
																				// key为advertisers_offer_id(我方系统分配的网盟ID)
			List<AdvertisersOfferForUpdate> advertisersOfferForUpdateList = new ArrayList<AdvertisersOfferForUpdate>();// 待更新广告

			for (AdvertisersOfferCompare advertisersOfferCompare : advertisersOfferCompareList) {

				// 如果正式表中adv_offer_id不为空，但临时表中adv_offer_id为空，则是下线广告
				if (!OverseaStringUtil.isBlank(advertisersOfferCompare
						.getAdv_offer_id())
						&& OverseaStringUtil.isBlank(advertisersOfferCompare
								.getTemp_adv_offer_id())) {

					offlineAdvOfferIdList.add(advertisersOfferCompare
							.getAdv_offer_id());

					offlineAdvertisersOfferIdList.add(advertisersOfferCompare
							.getId());

					continue;
				}

				// 如果这些字段有一个不相同，则更新广告
				Float payout = advertisersOfferCompare.getPayout();
				Integer daily_cap = advertisersOfferCompare.getDaily_cap();
				String click_url = advertisersOfferCompare.getClick_url();
				String pkg = advertisersOfferCompare.getPkg();
				String preview_url = advertisersOfferCompare.getPreview_url();
				String ImpressionUrl = advertisersOfferCompare.getImpress_url();
				String main_icon = advertisersOfferCompare.getMain_icon();
				String os = advertisersOfferCompare.getOs();
				String minOs = advertisersOfferCompare.getOs_version();
				if ((ImpressionUrl != null && !ImpressionUrl
						.equals(advertisersOfferCompare.getTemp_impress_url() == null ? ""
								: advertisersOfferCompare.getTemp_impress_url()))
						|| (payout != null && !payout
								.equals(advertisersOfferCompare
										.getTemp_payout() == null ? 0
										: advertisersOfferCompare
												.getTemp_payout()))
						|| (daily_cap != null && !daily_cap
								.equals(advertisersOfferCompare
										.getTemp_daily_cap() == null ? 0
										: advertisersOfferCompare
												.getTemp_daily_cap()))
						|| (click_url != null && !click_url
								.equals(advertisersOfferCompare
										.getTemp_click_url() == null ? ""
										: advertisersOfferCompare
												.getTemp_click_url()))
						|| (pkg != null && !pkg.equals(advertisersOfferCompare
								.getTemp_pkg() == null ? ""
								: advertisersOfferCompare.getTemp_pkg()))
						|| (preview_url != null && !preview_url
								.equals(advertisersOfferCompare
										.getTemp_preview_url() == null ? ""
										: advertisersOfferCompare
												.getTemp_preview_url()))
						|| (main_icon != null && !advertisersOfferCompare
								.getMain_icon()
								.equals(advertisersOfferCompare
										.getTemp_main_icon() == null ? ""
										: advertisersOfferCompare
												.getTemp_main_icon()))
						|| (os != null && !os.equals(advertisersOfferCompare
								.getOs() == null ? "" : advertisersOfferCompare
								.getOs()))
						|| (minOs != null && !minOs
								.equals(advertisersOfferCompare
										.getTemp_os_version() == null ? ""
										: advertisersOfferCompare
												.getTemp_os_version()))) {

					AdvertisersOfferForUpdate advertisersOfferForUpdate = new AdvertisersOfferForUpdate();

					// 更新前网盟cap和价格
					advertisersOfferForUpdate
							.setOld_daily_cap(advertisersOfferCompare
									.getDaily_cap());
					advertisersOfferForUpdate
							.setOld_payout(advertisersOfferCompare.getPayout());

					advertisersOfferForUpdate.setId(advertisersOfferCompare
							.getId());// ID保持为正式库表中的值
					advertisersOfferForUpdate
							.setAdv_offer_id(advertisersOfferCompare
									.getAdv_offer_id());// 网盟自身offer
														// ID
					advertisersOfferForUpdate.setName(advertisersOfferCompare
							.getTemp_name());// 网盟offer名称
					advertisersOfferForUpdate
							.setCost_type(advertisersOfferCompare
									.getTemp_cost_type());// Offer转化类型(0:CPI、1:CPA、2:CPS等)
					advertisersOfferForUpdate
							.setOffer_type(advertisersOfferCompare
									.getTemp_offer_type());// Offer业务分类(0:GP-CPI
															// 3:五类)
					advertisersOfferForUpdate
							.setAdvertisers_id(advertisersOfferCompare
									.getTemp_advertisers_id());// 所属网盟ID
					advertisersOfferForUpdate.setPkg(advertisersOfferCompare
							.getTemp_pkg());// pkg包名
					advertisersOfferForUpdate
							.setPkg_size(advertisersOfferCompare
									.getTemp_pkg_size());// 包大小(M)
					advertisersOfferForUpdate
							.setMain_icon(advertisersOfferCompare
									.getTemp_main_icon());// Icon连接
					advertisersOfferForUpdate
							.setPreview_url(advertisersOfferCompare
									.getTemp_preview_url());// 预览连接
					advertisersOfferForUpdate
							.setClick_url(advertisersOfferCompare
									.getTemp_click_url());// 推广点击连接
					advertisersOfferForUpdate
							.setCountry(advertisersOfferCompare
									.getTemp_country());// 推广国家
					advertisersOfferForUpdate
							.setEcountry(advertisersOfferCompare
									.getTemp_ecountry());// 推广排除国家
					advertisersOfferForUpdate
							.setDaily_cap(advertisersOfferCompare
									.getTemp_daily_cap());// 每日cap限制
					advertisersOfferForUpdate
							.setSend_cap(advertisersOfferCompare
									.getTemp_send_cap());// 下发渠道cap限制
					advertisersOfferForUpdate.setPayout(advertisersOfferCompare
							.getTemp_payout());// 支付价格
					advertisersOfferForUpdate
							.setExpiration(advertisersOfferCompare
									.getTemp_expiration());// 过期日期
					advertisersOfferForUpdate
							.setCreatives(advertisersOfferCompare
									.getTemp_creatives());// 推广素材
					advertisersOfferForUpdate
							.setConversion_flow(advertisersOfferCompare
									.getTemp_conversion_flow());// 转化类型(0:cpi,
					advertisersOfferForUpdate
							.setSupported_carriers(advertisersOfferCompare
									.getTemp_supported_carriers());// 支持运营商(WIFI,
					advertisersOfferForUpdate
							.setDescription(advertisersOfferCompare
									.getTemp_description());// 其他方面描述(限制或者约束等)
					advertisersOfferForUpdate.setOs(advertisersOfferCompare
							.getTemp_os());// 操作系统类型(0:andriod:1:ios:2:pc)
					advertisersOfferForUpdate
							.setOs_version(advertisersOfferCompare
									.getTemp_os_version());// 系统版本要求
					advertisersOfferForUpdate
							.setDevice_type(advertisersOfferCompare
									.getTemp_device_type());// 设备类型(0:mobile,
					advertisersOfferForUpdate
							.setSend_type(advertisersOfferCompare
									.getTemp_send_type());// 下发类型(0:自动下发，1:手动下发)
					advertisersOfferForUpdate
							.setIncent_type(advertisersOfferCompare
									.getTemp_incent_type());// 是否激励(0:非激励
					advertisersOfferForUpdate
							.setSmartlink_type(advertisersOfferCompare
									.getTemp_smartlink_type());// 是否Smartlink类型(0:是,1:否)
					advertisersOfferForUpdate.setStatus(advertisersOfferCompare
							.getTemp_status());//
					advertisersOfferForUpdate
							.setImpressionUrl(advertisersOfferCompare
									.getTemp_impress_url());
					advertisersOfferForUpdate
							.setOs_version(advertisersOfferCompare
									.getTemp_os_version());
					advertisersOfferForUpdateList
							.add(advertisersOfferForUpdate);
				}
			}

			logger.info("step-5: offline-AdvOfferIdList size := "
					+ offlineAdvOfferIdList.size() + " advertisersId is:"
					+ advertisersId);
			logger.info("step-5: advertisersOffer-ForUpdateList size := "
					+ advertisersOfferForUpdateList.size()
					+ " advertisersId is:" + advertisersId);

			/**
			 * 下线已经不存在的广告(设置为系统下线) 1. 下线网盟广告表中的相关广告 2. 下线已经下发给渠道的相关广告 3. 修改次数时+1
			 */
			AffiliatesOfferDao affiliatesOfferDao = (AffiliatesOfferDao) TuringSpringHelper
					.getBean("affiliatesOfferDao");

			if (offlineAdvOfferIdList != null
					&& offlineAdvOfferIdList.size() > 0) {
				offlinesize = offlineAdvOfferIdList.size();
				/**
				 * 下线时updates+1
				 */
				logger.info("step-6: batchOffline-AdvertisersOffers offlineAdvOfferIdList size := "
						+ offlineAdvOfferIdList.size()
						+ " advertisersId is:"
						+ advertisersId);
				advertisersOfferDao
						.batchOfflineAdvertisersOffers(offlineAdvOfferIdList);// 下线网盟广告

				logger.info("step-7: batchOffline-AffiliatesOffers offlineAdvertisersOfferIdList size := "
						+ offlineAdvertisersOfferIdList.size()
						+ " advertisersId is:" + advertisersId);
				affiliatesOfferDao
						.batchOfflineAffiliatesOffers(offlineAdvertisersOfferIdList);// 下线网盟分配的渠道广告
			}

			/**
			 * 批量更新待更新的广告 1. 更新网盟广告表中的相关广告 2. 对分配到渠道表中的网盟广告进行更新操作 -
			 * 如果单价/cap更新了，则渠道广告涉及到单价、cap、利润率更新 -
			 * 单价调整为网盟最新单价，cap设置为网盟广告最新cap，利润率更新为渠道默认利润率
			 */
			if (advertisersOfferForUpdateList != null
					&& advertisersOfferForUpdateList.size() > 0) {
				updatesize = advertisersOfferForUpdateList.size();

				logger.info("step-8: batchUpdate-FormalAdvertisersOfferTable advertisersOfferForUpdateList size := "
						+ advertisersOfferForUpdateList.size()
						+ " advertisersId is:" + advertisersId);
				// 更新正式网盟广告表
				advertisersOfferDao
						.batchUpdateFormalAdvertisersOfferTable(advertisersOfferForUpdateList);

				logger.info("step-9: batchUpdate-FormalAffiliatesOfferTable"
						+ " advertisersId is:" + advertisersId);
				// 更新网盟广告给渠道正式渠道广告表
				affiliatesOfferDao
						.batchUpdateFormalAffiliatesOfferTable(advertisersOfferForUpdateList);
			}
		}

		// 查询新增广告
		List<AdvertisersOfferCompare> advertisersOfferCompareForAddList = advertisersOfferDao
				.getAdvertisersOfferCompareListForAdd(advertisersId);
		if (advertisersOfferCompareForAddList != null
				&& advertisersOfferCompareForAddList.size() > 0) {
			List<AdvertisersOfferCompare> openlists = new ArrayList<AdvertisersOfferCompare>();
			List<AdvertisersOfferCompare> addlists = new ArrayList<AdvertisersOfferCompare>();
			addsize = advertisersOfferCompareForAddList.size();
			for (int i = 0; i < advertisersOfferCompareForAddList.size(); i++) {
				AdvertisersOfferCompare temp = advertisersOfferCompareForAddList
						.get(i);
				if (temp.getId() != null) {
					// 判断 是否是在已经 分配列表中的
					openlists.add(temp);
				} else {
					addlists.add(temp);
				}
			}

			if (addlists != null && addlists.size() > 0) {

				logger.info("step-10: batchInsert-AdvertisersOfferIntoFormalTable advertisersOfferCompareForAddList size := "
						+ addlists.size()
						+ " advertisersId is:"
						+ advertisersId);
				// 新增广告批量入正式网盟广告表
				advertisersOfferDao
						.batchInsertAdvertisersOfferIntoFormalTable(addlists);

				// 新增广告自动分配到网盟分发渠道中
				logger.info("step-11: batchAdd-AdvertisersOffersIntoAffiliatesTable"
						+ " advertisersId is:" + advertisersId);
				batchAddAdvertisersOffers2Affiliates(advertisersId, addlists);
			}
			/**
			 * 如果下线重新上线，updates+1,openlists
			 */
			if (openlists != null && openlists.size() > 0) {
				logger.info("step-13: batchopen-AdvertisersOfferIntoFormalTable openlists size := "
						+ openlists.size()
						+ " advertisersId is:"
						+ advertisersId);
				advertisersOfferDao
						.batchUpdateOpenFormalAdvertisersOfferTable(openlists);
				AffiliatesOfferDao affiliatesOfferDao = (AffiliatesOfferDao) TuringSpringHelper
						.getBean("affiliatesOfferDao");
				// 需要增加对于top广告下发的开启与否？？暂时不需要呢
				// 重新打开的offers 对应的渠道中也直接打开,先更新手动下发的，
				affiliatesOfferDao
						.batchUpdateOpenFormalAffiliatesOfferTable(openlists);
				logger.info("step-14: batchopen-AdvertisersOfferIntoFormalTable "
						+ " advertisersId is:" + advertisersId);
				// 然后更新自动下发的数据，需要判断现在是否还是自动下发的状态，如果不是就不需要更新的
				// SELECT tt.* from ((SELECT * from affiliates_offer where
				// status=-2 and send_type=0 and advertisers_id=15 and
				// advertisers_offer_id in (4043642,4044429) ) tt
				// inner join advertisers_distribute ad on tt.advertisers_id
				// =ad.advertisers_id and tt.affiliates_id=ad.affiliates_id )
				affiliatesOfferDao
						.batchUpdateOpenFormalAffiliatesOfferTableForAuto(
								openlists, advertisersId);
				logger.info("step-15: batchopen-AdvertisersOfferIntoFormalTableAuto "
						+ " advertisersId is:" + advertisersId);

			}

		}

		// 增加到数据库中，
		try {
			if ((offlinesize > 100 || addsize > 100) && beforeallsize > 0) {
				float rate = (float) ((float) offlinesize / (float) beforeallsize) * 100.0f;
				BigDecimal b = new BigDecimal(rate);
				float f3 = b.setScale(0, BigDecimal.ROUND_HALF_UP).floatValue();
				if (f3 > 50.0f) {
					PullOfferJob.mailtosend = PullOfferJob.mailtosend
							+ "联盟ID:[" + advertisersId + "]" + name + ", 更新前:"
							+ beforeallsize + ", 更新后:" + nowallsize + ", 下线:"
							+ offlinesize + ", 新增:" + addsize + ", 变动率:" + f3
							+ "%" + "<br />";
				}

			}
			AdvertisersOfferUpdate addtemp = new AdvertisersOfferUpdate();
			addtemp.setAdvertisers_id(advertisersId);
			addtemp.setBefroreallsize(beforeallsize);
			addtemp.setNowallsize(nowallsize);
			addtemp.setOfflinesize(offlinesize);
			addtemp.setUpdatesize(updatesize);
			addtemp.setAddsize(addsize);
			List<AdvertisersOfferUpdate> ttmp = new ArrayList<AdvertisersOfferUpdate>();
			ttmp.add(addtemp);
			advertisersOfferDao.batchInsertAdvertisersOfferUpdate(ttmp);

		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.info("===End pull offer===");
	}

	/**
	 * 新增网盟广告自动入已分配给相关渠道的渠道广告表
	 * 
	 * 根据：网盟自身offerID(adv_offer_id)+所属网盟ID(advertisers_id)，
	 * 查询advertisers_offer_id，进行入渠道广告表 判断是否超过1000条记录，如果超过则进行分批提交
	 */
	public void batchAddAdvertisersOffers2Affiliates(Long advertisersId,
			List<AdvertisersOfferCompare> advertisersOfferCompareForAddList) {

		List<AdvertisersOfferWaitPushBean> advertisersWaitPushOfferList = new ArrayList<AdvertisersOfferWaitPushBean>();

		// 总页数
		int loopNum = (advertisersOfferCompareForAddList.size() / 1000) + 1;

		int endPageNum = advertisersOfferCompareForAddList.size() % 1000;// 最后一页的数量

		logger.info("\t loopNum := " + loopNum);

		for (int i = 0; i < loopNum; i++) {

			int beginIndex = 0;
			int endIndex = endPageNum;

			if (loopNum == 1) {

				beginIndex = 0;
				endIndex = endPageNum;
			} else {

				if (i == 0) {

					beginIndex = 0;
					endIndex = 1000;
				}
				if (i > 0) {

					beginIndex = i * 1000;
					endIndex = (i + 1) * 1000;

					if (i == loopNum - 1) {

						endIndex = beginIndex + endPageNum;
					}
				}
			}

			logger.info("\t beginIndex := " + beginIndex + " endIndex := "
					+ endIndex);

			List<AdvertisersOfferCompare> subAddList = advertisersOfferCompareForAddList
					.subList(beginIndex, endIndex);

			logger.info("\t subAddList size := " + subAddList.size());

			if (subAddList != null && subAddList.size() > 0) {

				StringBuffer advOfferIdsBuffer = new StringBuffer();

				for (int j = 0; j < subAddList.size(); j++) {

					if (j == subAddList.size() - 1) {

						advOfferIdsBuffer
								.append("'")
								.append(subAddList.get(j)
										.getTemp_adv_offer_id()).append("'");
					} else {

						advOfferIdsBuffer
								.append("'")
								.append(subAddList.get(j)
										.getTemp_adv_offer_id()).append("'")
								.append(",");
					}
				}

				AdvertisersOfferDao advertisersOfferDao = (AdvertisersOfferDao) TuringSpringHelper
						.getBean("advertisersOfferDao");

				// 查询满足条件的advertisersOfferWaitPushBean
				List<AdvertisersOfferWaitPushBean> subAdvertisersOfferIdList = advertisersOfferDao
						.getAdvertisersOfferIdsForAdd(advertisersId,
								advOfferIdsBuffer.toString());

				// 合并到一起
				if (subAdvertisersOfferIdList != null
						&& subAdvertisersOfferIdList.size() > 0) {
					advertisersWaitPushOfferList
							.addAll(subAdvertisersOfferIdList);
				}
			}
		}

		logger.info("step-12: advertisersWaitPushOfferListSize := "
				+ advertisersWaitPushOfferList.size() + " advertisersId is:"
				+ advertisersId);

		// 新增广告入渠道广告表
		if (advertisersWaitPushOfferList != null
				&& advertisersWaitPushOfferList.size() > 0) {

			// 查询满足条件的渠道列表(即分配了此网盟广告的渠道)
			AdvertisersDistributeDao advertisersDistributeDao = (AdvertisersDistributeDao) TuringSpringHelper
					.getBean("advertisersDistributeDao");

			List<Affiliates> distributeAffiliatesList = advertisersDistributeDao
					.getAdvertisersDistributeAffiliates(advertisersId);

			if (distributeAffiliatesList != null
					&& distributeAffiliatesList.size() > 0) {

				// 分发网盟新增广告到渠道广告表，按渠道逐个分发
				for (Affiliates affiliates : distributeAffiliatesList) {

					logger.info("\t  distribute advertisersWaitPushOfferListSize := "
							+ advertisersWaitPushOfferList.size());
					logger.info("\t " + " affiliates := "
							+ affiliates.getName() + "(id: "
							+ affiliates.getId() + ")");

					pushAdvertisersOffer2Affiliates(affiliates,
							advertisersWaitPushOfferList);
				}
			}
		}
	}

	/**
	 * 分发网盟广告到某个渠道
	 * 
	 * @param affiliates
	 * @param advertisersWaitPushOfferList
	 */
	public void pushAdvertisersOffer2Affiliates(Affiliates affiliates,
			List<AdvertisersOfferWaitPushBean> advertisersWaitPushOfferList) {

		if (affiliates == null || affiliates.getId() == null
				|| advertisersWaitPushOfferList == null
				|| advertisersWaitPushOfferList.size() == 0) {

			return;
		}

		// 获取所有渠道信息
		List<AffiliatesOfferPull> affiliatesOfferPullList = new ArrayList<AffiliatesOfferPull>();

		for (int i = 0; i < advertisersWaitPushOfferList.size(); i++) {

			AdvertisersOfferWaitPushBean advertisersOfferWaitPushBean = advertisersWaitPushOfferList
					.get(i);

			if (advertisersOfferWaitPushBean.getAdvertisers_id() == null
					|| advertisersOfferWaitPushBean.getAdvertisers_offer_id() == null) {

				continue;
			}
			Float hackConvertRate = null;
			Float payout = null;
			Long affiliatesId = affiliates.getId();
			Long advertisersId = advertisersOfferWaitPushBean
					.getAdvertisers_id();
			Long advertisersOfferId = advertisersOfferWaitPushBean
					.getAdvertisers_offer_id();
			Integer cap = advertisersOfferWaitPushBean.getDaily_cap();
			Integer max_click = affiliates.getClick();
			Float cvr = affiliates.getCvr();
			String country = affiliates.getCountry();
			String ecountry = affiliates.getEcountry();
			String offer_country = advertisersOfferWaitPushBean.getCountry();
			String offer_os = advertisersOfferWaitPushBean.getOs();
			String limit_os = affiliates.getLimit_os();

			// 限制系统
			if (limit_os.equals(offer_os)) {
				continue;
			}

			// 判断是否按国家下发
			// 允许下发
			Boolean allow = true;
			if (!country.equals("ALL")) {
				if (offer_country.indexOf(":") >= 0) {
					String[] countryList = offer_country.split(":");
					for (int j = 0; j < countryList.length; j++) {
						if (!country.contains(countryList[j])) {
							allow = false;
							break;
						}
					}
				} else {
					if (!country.contains(offer_country)) {
						allow = false;
					}
				}
			}
			// 不允许下发

			if (ecountry != null && ecountry.length() > 0) {
				if (offer_country.indexOf(":") >= 0) {
					String[] countryList = offer_country.split(":");
					for (int j = 0; j < countryList.length; j++) {
						if (ecountry.contains(countryList[j])) {
							allow = false;
							break;
						}
					}
				} else {
					if (ecountry.contains(offer_country)) {
						allow = false;
					}
				}

			}

			if (!allow) {
				continue;
			}

			// 区分提价与不提价的区别
			// 根据advertisers_distribute表中addpayoutrate字段不为0
			if (!affiliates.getAddpayoutrate().equals(0)) {
				payout = getRaisePayout(
						advertisersOfferWaitPushBean.getPayout(),
						affiliates.getAddpayoutrate());
				hackConvertRate = getRaiseHackConvertRate(
						affiliates.getProfit_rate(),
						affiliates.getAddpayoutrate());
			} else {
				hackConvertRate = affiliates.getProfit_rate();
				payout = advertisersOfferWaitPushBean.getPayout();
			}

			AffiliatesOfferPull affiliatesOfferPull = new AffiliatesOfferPull();

			String trackingId = affiliatesId + "" + advertisersId + ""
					+ advertisersOfferId + "" + Long.valueOf(i) + ""
					+ TuringStringUtil.getRandint();

			/**
			 * 生成渠道访问URL：http://sub.gadmobs.com/network/tracking/58a79bdd7e199?
			 * affid=3&ct=1&ot=1 affid:为 affiliatesId ct:为
			 * advertisersOffer的costType ot:为 advertisersOffer的offerType
			 */

			// 获取渠道tracking域名
			String domain = affiliates.getDomain();

			if (domain != null && domain.length() > 0) {

				// String affiliates_click_url = domain + "/" + trackingId +
				// "?aff_id=" + affiliatesId;
				String affiliates_click_url = "";
				affiliates_click_url = domain + trackingId + "&userid="
						+ affiliatesId;
				affiliatesOfferPull.setTracking_id(trackingId);// 我方系统Offer跟踪ID
				affiliatesOfferPull.setAdvertisers_offer_id(advertisersOfferId);// 上游网盟offer
																				// ID
				affiliatesOfferPull.setAdvertisers_id(advertisersId);// 上游网盟ID
				affiliatesOfferPull.setAffiliates_id(affiliatesId);// 下游渠道ID
				affiliatesOfferPull.setAffiliates_sub_id("");// 下游子渠道
				affiliatesOfferPull
						.setAffiliates_click_url(affiliates_click_url);// 下游渠道广告URL
				affiliatesOfferPull.setCap(cap);// cap限制
				affiliatesOfferPull.setPayout(payout);// 价格
				affiliatesOfferPull.setHack_convert_rate(hackConvertRate);// 回调扣除百分比
				affiliatesOfferPull.setSend_type(0);// 下发类型(0:自动下发，1:手动下发)
				affiliatesOfferPull.setStatus(0);// 状态(0 在线 -1 手动下线 -2 系统下线)
				affiliatesOfferPull.setAddpayoutrate(affiliates
						.getAddpayoutrate());
				affiliatesOfferPull.setMax_click(max_click);
				affiliatesOfferPull.setCvr(cvr);
				affiliatesOfferPullList.add(affiliatesOfferPull);
			}
		}

		if (affiliatesOfferPullList != null
				&& affiliatesOfferPullList.size() > 0) {

			AffiliatesOfferDao affiliatesOfferDao = (AffiliatesOfferDao) TuringSpringHelper
					.getBean("affiliatesOfferDao");
			// 执行批量插入渠道广告操作
			affiliatesOfferDao
					.batchInsertAffiliatesOffer(affiliatesOfferPullList);
		}

	}

	/**
	 * 计算提价到指定比率后的价格
	 * 
	 * @param advertisersPayout
	 *            : 网盟给定价格
	 * @param addpayoutrate
	 *            : 我方提价比率(20代表提价20%)
	 * @return
	 */
	public Float getRaisePayout(Float advertisersPayout, Integer addpayoutrate) {

		if (advertisersPayout == null) {

			return null;
		}

		if (addpayoutrate == null || addpayoutrate == 0) {

			return advertisersPayout;
		}

		return TuringNumberUtil.floatDecimalFormat(advertisersPayout
				* ((float) (100 + addpayoutrate) / (float) 100), "0.000");
	}

	/**
	 * 计算提价到指定比率后的扣量比率(利润率)
	 * 
	 * @param
	 *            : 网盟给定价格
	 * @param addpayoutrate
	 *            : 我方提价比率(20代表提价20%)
	 * @return
	 */
	public Float getRaiseHackConvertRate(Float hack_convert_rate,
			Integer addpayoutrate) {

		if (addpayoutrate == null || addpayoutrate == 0) {

			return hack_convert_rate;
		}

		return ((100 + addpayoutrate) * hack_convert_rate - addpayoutrate) / 100;
	}

	public static void main(String[] args) {
		String country = "JP:KR:US:TW";
		String offer_country = "JP:KR:US:CN";
		Boolean allow = true;
		for (int i = 0; i < 10; i++) {
			if (!country.equals("ALL")) {
				if (offer_country.indexOf(":") >= 0) {
					String[] countryList = offer_country.split(":");
					for (int j = 0; j < countryList.length; j++) {
						if (country.contains(countryList[j])) {
						} else {
							allow = false;
							break;
						}
					}
				} else {
					if (!country.contains(offer_country)) {
						allow = false;
					}
				}

			}
			if (!allow) {
				System.out.println("不能下发");
			}
		}

	}

}
