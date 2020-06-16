package com.gmcc.boss.unicall;

import org.apache.log4j.Logger;

import com.gmcc.boss.waf.service.resource.OurResources;

public abstract class IUniCallFactory {
	private static Logger log = Logger.getLogger(IUniCallFactory.class);
	protected static final OurResources RESOURCES = OurResources.getOurResources(IUniCallFactory.class);
	public static InheritableThreadLocal<String> ctgSrv = new InheritableThreadLocal<String>();

	public abstract IUniCall getUniCall();

	static IUniCallFactory getUniCallFactory(String srvTyp, String srvAddr, String callSrv) {
		IUniCallFactory callFactory = null;

		String CSrvCall = srvTyp;
		if ("Cics".equals(CSrvCall)) {
			callFactory = new CicsUniCallFactory();
			ctgSrv.set(srvAddr); // 将ctgd服务地址保存到线程本地变量，交由CicsGW读取
			CicsGW.reset();// 检查CicsGW是否为第一次调用，若不是需要重新初始化
			try {
				PublicService.start();
			} catch (Exception e) {
				log.error("Refreshing CICS route table failed.", e);
			}
		} else if ("Tux".equals(CSrvCall)) {
			callFactory = new TuxUniCallFactory();
		} else if ("TPCloud".equals(CSrvCall)) {
			callFactory = new TPCloudUniCallFactory(srvAddr, callSrv);
		} else if ("CicsCloud".equals(CSrvCall)) {
			callFactory = new CicsCloudUniCallFactory();
		} else if ("TuxCloud".equals(CSrvCall)) {
			callFactory = new TuxCloudUniCallFactory();
//		} else if ("Hessian".equals(CSrvCall)) {
//			String CSrvCallClazz = GlobalConfig.getOriginalConfig("CSrvCall.Clazz",
//					"com.gmcc.boss.unicall.hessian.HessianUniCallFactory");
//			try {
//				callFactory = (IUniCallFactory) Class.forName(CSrvCallClazz).newInstance();
//			} catch (Exception e) {
//				String errMsg = "IUnicallFactory: Create " + CSrvCall + "'s instance failed: " + e.getMessage();
//				log.error(errMsg, e);
//				throw new RuntimeException(errMsg);
//			}
//		} else if ("HttpClient".equals(CSrvCall)) {
//			String CSrvCallClazz = GlobalConfig.getOriginalConfig("CSrvCall.Clazz",
//					"com.gmcc.boss.unicall.httpclient.HttpClientUniCallFactory");
//			try {
//				callFactory = (IUniCallFactory) Class.forName(CSrvCallClazz).newInstance();
//			} catch (Exception e) {
//				String errMsg = "IUnicallFactory: Create " + CSrvCall + "'s instance failed: " + e.getMessage();
//				log.error(errMsg, e);
//				throw new RuntimeException(errMsg);
//			}
		} else if ("TuxJolt".equals(CSrvCall)) {
			callFactory = new TuxJoltUniCallFactory();
		} else if ("TuxJni".equals(CSrvCall)) {
			callFactory = new TuxJniUniCallFactory();
		} else if ("Socket".equals(CSrvCall)) {
			callFactory = new SocketUniCallFactory();
		} else if (!"Other".equals(CSrvCall)) {
			if (null != CSrvCall) {
				Class<?> clsFactory = null;
				try {
					clsFactory = Class.forName(CSrvCall);
				} catch (Exception e) {
					String errMsg = "IUnicallFactory: Class.forName('" + CSrvCall + "') failed.";
					log.error(errMsg, e);
					throw new RuntimeException(errMsg);
				}
				try {
					callFactory = (IUniCallFactory) clsFactory.newInstance();
				} catch (Exception e) {
					String errMsg = "IUnicallFactory: Create " + CSrvCall + "'s instance failed: " + e.getMessage();
					log.error(errMsg, e);
					throw new RuntimeException(errMsg);
				}
			} else {
				String message = RESOURCES.getMessage("IUniCallFactory.message");
				message = message + "[CSrvCall=null]";

				log.error("IUnicallFactory:" + message);
				throw new RuntimeException(message);
			}
		}
		return callFactory;
	}
}
