package com.gmcc.boss.unicall;

import java.util.Hashtable;

import com.gmcc.boss.common.base.CObArchive;
import com.gmcc.boss.common.utils.StringUtil;
import com.gmcc.boss.unicall.driver.TPCloud;

public class TPCloudUniCallFactory extends IUniCallFactory {
//	private static final String TPCLOUD_SERV_ADDR_LIST = "TPCLOUD_SERV_ADDR_LIST";

	TPCloudUniCallFactory(String srvAddr, String callSrv) {
//		String servfile = GlobalConfig.getOriginalConfig("TPCLOUD_SERV_PROFILE");
//		if (StringUtil.isEmpty(servfile)) {
//			throw new RuntimeException("profile item [TPCLOUD_SERV_PROFILE] not exists, profile : config.propties");
//		}
//		File fp = new File(servfile);
//		if (!fp.exists()) {
//			throw new RuntimeException("tpcloudserv profile not exists, profile : " + servfile);
//		}
//		FileInputStream fis = null;
//		String serAddr = "";
//		try {
//			fis = new FileInputStream(fp);
//			Properties prop = new Properties();
//			prop.load(fis);
//			serAddr = prop.getProperty("TPCLOUD_SERV_ADDR_LIST");
//			try {
//				if (fis != null) {
//					fis.close();
//				}
//			} catch (IOException e) {
//				throw new RuntimeException(e.getMessage());
//			}
//			TPCloud.setServerAddr(serAddr);
//		} catch (Exception e) {
//			throw new RuntimeException("Load tpcloudserv profile error, " + e.getMessage());
//		} finally {
//			try {
//				if (fis != null) {
//					fis.close();
//				}
//			} catch (IOException e) {
//				throw new RuntimeException(e.getMessage());
//			}
//		}

//		String server = GlobalConfig.getOriginalConfig("CALL_SERVER");
//		if (StringUtil.isEmpty(server)) {
//			throw new RuntimeException("Not config parameter: CALL_SERVER");
//		}
//		PublicService.CALL_SERVER = server;

		TPCloud.setServerAddr(srvAddr);
		PublicService.CALL_SERVER = callSrv;

//		String region_router = GlobalConfig.getOriginalConfig("REGION_ROUTER", "");
//		String[] region_routers = new String[0];
//		if (StringUtil.isNotEmpty(region_router)) {
//			region_routers = region_router.split(";");
//			PublicService.tux_Region_RouList = getRegionRouteList(region_routers);
//		}
	}

	synchronized Hashtable<String, String> getRegionRouteList(String[] region_routers) {
		Hashtable<String, String> region_ser = new Hashtable<String, String>();
		if ((region_routers != null) && (region_routers.length > 0)) {
			for (int i = 0; i < region_routers.length; i++) {
				if (region_routers[i].split(":").length > 1) {
					String tuxServer = region_routers[i].split(":")[0];
					String tuxRegion = region_routers[i].split(":")[1];
					String[] tuxRegions = tuxRegion.split(",");
					for (int j = 0; j < tuxRegions.length; j++) {
						region_ser.put(tuxRegions[j], tuxServer);
					}
				}
			}
		}
		return region_ser;
	}

	class TPCloudUniCall extends IUniCall {
		public static final int DEFAULT_TIMEOUT = 150;

		TPCloudUniCall() {
		}

		public void beginTransaction(int timeout) throws EUniCall {
		}

		public void commit() throws EUniCall {
		}

		public void rollback() throws EUniCall {
		}

		public void invoke(CIntPkg pkg) throws Exception {
			invoke(pkg, 150);
		}

		public void invoke(CIntPkg pkg, int timeout) throws Exception {
			CObArchive is = new CObArchive();
			pkg.Save(is);

			byte[] inBuf = is.buffer();
			int inLen = is.buflen();
			is = null;
			byte[] data = null;

			String callServer = (String) PublicService.tux_Region_RouList.get(pkg.strRegion);
			try {
				if (StringUtil.isNotEmpty(callServer)) {
					data = TPCloud.tpcall(callServer, inBuf, inLen, timeout * 1000);
				} else {
					data = TPCloud.tpcall(PublicService.CALL_SERVER, inBuf, inLen, timeout * 1000);
				}
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			CObArchive os = new CObArchive(data, data.length);
			pkg.Load(os);
			os = null;
		}
	}

	public IUniCall getUniCall() {
		return new TPCloudUniCall();
	}
}
