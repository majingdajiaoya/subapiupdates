package com.network.task.schedule;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.network.task.bean.Advertisers;
import com.network.task.bean.AdvertisersOffer;
import com.network.task.bean.BlacklistPKG;
import com.network.task.dao.AdvertisersDao;
import com.network.task.dao.AdvertisersOfferDao;
import com.network.task.dao.BlacklistPKGDao;
import com.network.task.log.NetworkLog;
import com.network.task.mail.JavaMailWithAttachment;
import com.network.task.service.base.PullOfferServiceTask;
import com.network.task.util.DataUtils;
import com.network.task.util.NetworkSpringHelper;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.ReadProperties;
import com.network.task.util.TuringSpringHelper;

public class PullOfferJob {
	public static String mailtosend = "";

	public static void main(String[] args) {
		// 单独debug
		ApplicationContext ctx = null;
		ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
		NetworkSpringHelper networkSpringHelper = new NetworkSpringHelper();
		networkSpringHelper.setApplicationContext(ctx);
		doPullOffer();
	}

	protected static final Logger logger = Logger.getLogger("runerror");

	public static void doPullOffer() {
		mailtosend = "";
		try {
			logger.info("Do pullOffer tasks begin...");

			/**
			 * 根据网盟表中配置信息创建拉取offer任务
			 */
			AdvertisersDao advertisersDao = (AdvertisersDao) TuringSpringHelper.getBean("advertisersDao");

			List<Advertisers> advertisersList = advertisersDao.getAllValidAdvertisers();

			if (advertisersList == null || advertisersList.size() == 0) {

				logger.info("advertisersList is empty");

				return;
			}

			// 加载pkg黑名单
			// 查下PKG黑名单，进行过滤黑名单中的广告
			BlacklistPKGDao blacklistPKGDao = (BlacklistPKGDao) TuringSpringHelper.getBean("blacklistPKGDao");

			List<BlacklistPKG> blacklistPKGList = blacklistPKGDao.getAllValidAdvertisers();

			if (blacklistPKGList != null && blacklistPKGList.size() > 0) {

				if (DataUtils.blacklist_pkg_map != null && !DataUtils.blacklist_pkg_map.isEmpty()) {

					DataUtils.blacklist_pkg_map.clear();
				}

				for (BlacklistPKG blacklistPKG : blacklistPKGList) {

					DataUtils.blacklist_pkg_map.put(blacklistPKG.getPkg(), blacklistPKG.getPkg());
				}
			}

			for (Advertisers advertisers : advertisersList) {

				logger.info("create task for advertisers := " + advertisers.getName() + "-" + advertisers.getId());

				if (OverseaStringUtil.isBlank(advertisers.getPullofferclass())) {

					logger.info("pullofferclass is empty");

					continue;
				}

				Class<?> pullOfferService = Class.forName(advertisers.getPullofferclass());

				if (pullOfferService == null) {

					logger.info("pullOfferService is null := " + advertisers.getPullofferclass());

					continue;
				}

				logger.info("begin service class := " + advertisers.getPullofferclass());

				Constructor<?> constructor = pullOfferService.getDeclaredConstructor(new Class[] { Advertisers.class });

				constructor.setAccessible(true);

				PullOfferServiceTask pullOfferServiceTask = (PullOfferServiceTask) constructor.newInstance(new Object[] { advertisers });

				if (pullOfferServiceTask == null) {

					continue;
				}

				logger.info(advertisers.getName() + " pull offer service task start...");

				pullOfferServiceTask.run();

				// Thread.sleep(5*1000);
			}
		} catch (Exception e) {

			e.printStackTrace();

			NetworkLog.exceptionLog(e);
		}
		// 发送邮件进行
		try {
			Thread.sleep(20 * 1000);
			if (!mailtosend.equals("") && mailtosend.length() > 2) {
				// 发送字符串
				logger.info("mailtosend := " + mailtosend);
				sendMailTwo(mailtosend);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void sendMailTwo(String send) {
		JavaMailWithAttachment se = new JavaMailWithAttachment(false);
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append(send);
		sb.append("</html>");
		String title2 = "联盟offers更新监控(下线|新增超过100)";
		String to = ReadProperties.getValue("to");
		se.doSendHtmlEmail(title2, send, to, null);

	}

}
