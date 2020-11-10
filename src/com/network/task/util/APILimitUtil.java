package com.network.task.util;

import com.network.task.bean.Advertisers;
import com.network.task.bean.AdvertisersOffer;
import common.Logger;

public class APILimitUtil {
	protected static Logger logger = Logger.getLogger(APILimitUtil.class);

	public static void main(String[] args) {
		AdvertisersOffer advertiserOffer = new AdvertisersOffer();
		// 单子
		advertiserOffer.setCountry("US:JP");
		advertiserOffer.setPayout(Float.valueOf("1"));
		advertiserOffer.setPkg("com.tiket.gits");
		// 广告主
		Advertisers advertisers = new Advertisers();
		advertisers.setCountryLimit("JP");
		advertisers.setMinpayout(Float.valueOf("0.06"));
		advertisers.setPkg("com.tiket.gits");
		Boolean b = checkAPILimit(advertiserOffer, advertisers);
		System.out.println(b);
		if (!b) {
			System.out.println("过滤");
		}
	}

	/**
	 * 根据库中信息剔除pkg
	 * 
	 * @param list
	 *            白名单黑名单
	 * @param advertiseId
	 * @param pkg
	 * @return
	 */
	public static boolean checkAPILimit(AdvertisersOffer advertiserOffer, Advertisers advertisers) {

		Long advertiseId = advertisers.getId();

		// 剔除标志（true：要剔除,false 不剔除）

		boolean countryFlag = true;
		boolean pkgFlag = true;
		boolean payoutFlag = true;

		Float limitPayout = advertisers.getMinpayout();
		// String whiteCountry=limit.getCountry_white_list();
		String blackCountry = advertisers.getCountryLimit();
		// String whitePkg=limit.getPkg_white_list();
		String blackPkg = advertisers.getPkg();

		String country = advertiserOffer.getCountry();
		if (country == null || country.length() == 0) {
			return false;
		}
		String pkg = advertiserOffer.getPkg();
		if (pkg == null || pkg.length() == 0) {
			return false;
		}
		Float payout = advertiserOffer.getPayout();
		if (payout == null) {
			return false;
		}

		String[] countrys = country.split(":");

		if (blackCountry != null && blackCountry.length() > 0) {
			for (String cu : countrys) {
				if (blackCountry.toUpperCase().indexOf(cu.toUpperCase()) < 0) {
					countryFlag = false;
				} else {
					countryFlag = true;
					break;
				}

			}
		}
		if (blackPkg != null && blackPkg.length() > 0) {
			if (!blackPkg.contains(pkg)) {
				pkgFlag = false;
				logger.info(" black pkg advertise :" + advertiseId + " blackPkg :" + blackPkg + " apipkg :" + pkg + " pkgFlag:" + pkgFlag);
			}
		}

		if (payout.floatValue() < limitPayout.floatValue()) {
			logger.info(" limit payout advertise :" + advertiseId + " limitpayout :" + limitPayout + " apipayout :" + payout);
			payoutFlag = false;
		}

		if (countryFlag && pkgFlag && payoutFlag) {
			return true;
		}
		return false;
	}
}
