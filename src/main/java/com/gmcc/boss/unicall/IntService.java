package com.gmcc.boss.unicall;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.gmcc.boss.common.base.CEMsgInfo;
import com.gmcc.boss.common.base.CommandContext;
import com.gmcc.boss.common.cbo.global.cbo.common.CTagSet;
import com.gmcc.boss.common.exception.ExceptionUtil;
import com.gmcc.boss.common.exception.OurErrorException;
import com.gmcc.boss.common.web.AppEnv;
import com.gmcc.boss.waf.service.resource.OurResources;
import com.gmcc.boss.waf.utils.LogUtil;
import com.huawei.boss.common.util.FrameWorkUtil;
import com.huawei.boss.eme.util.EmeUtil;
import com.huawei.ngcrm.framework.log.LogTraceInfoManager;

public class IntService {
	private static final OurResources RESOURCES = OurResources.getOurResources(IUniCallFactory.class);
	private static Logger log = Logger.getLogger(IntService.class);
	private static IUniCallFactory uniCallFactory = null;
//	private static IUniCallFactory debugUniCallFactory = null;
	// @SuppressWarnings({"MS_PKGPROTECT"})
	protected static CSrvCallHome intServiceHome;
	public int retCode = 0;
	public int errCode = 0;
	public String retInfo = "";
	private String srvTyp = "";

	// @SuppressWarnings({"NM_METHOD_NAMING_CONVENTION"})
	public boolean Invoke(CIntPkg aIntPkg) {
		aIntPkg.SetTagParam("LogLevel", CommandContext.getLogBackLevel());
		aIntPkg.SetTagParam("REQ_FROM_IP", CommandContext.getHostAddr());
		aIntPkg.SetTagParam("REQ_FROM_APP", CommandContext.getAppName());
		aIntPkg.SetTagParam("REQ_FROM_URL", CommandContext.getCommand());
		aIntPkg.SetTagParam("$$CLINF", CommandContext.getPidInfo());

//		String usertype = GlobalConfig.getOriginalConfig("SESSION.USE_TYPE", "2.0");
		String usertype = "2.0";
		if (usertype.equals("3.0")) {
			String accessAddr = CommandContext.getAccessAddr();
			String accessPoint = CommandContext.getAccessPoint();
			String accessType = CommandContext.getAccessType();
			String accessSubType = CommandContext.getAccessSubType();
			if ((accessAddr != null) || (accessPoint != null) || (accessType != null) || (accessSubType != null)) {
				CTagSet tagSet = new CTagSet();
				tagSet.put("TermAddr", CommandContext.getAccessAddr());
				tagSet.put("AccessPoint", CommandContext.getAccessPoint());
				tagSet.put("AccessType", CommandContext.getAccessType());
				tagSet.put("AccessSubType", CommandContext.getAccessSubType());

				aIntPkg.setTagSetParam("AccessInfo", tagSet);
			}
		}
		EmeUtil.setContextEmgCrmRegionListStr(aIntPkg.strRegion);
		aIntPkg.SetTagParam("EMG_IMSTATUS", CommandContext.getEMG_IMSTATUSE());
		aIntPkg.SetTagParam("EMG_CRMSTATUS", CommandContext.getEMG_CRMSTATUS());
		aIntPkg.SetTagParam("EMG_BOSSSTATUS", CommandContext.getEMG_BOSSSTATUS());

		aIntPkg.SetTagParam("LoginId4A", CommandContext.getLoginId4A());
		aIntPkg.SetTagParam("Ticket4A", CommandContext.getTicket4A());
		aIntPkg.SetTagParam("SessId4A", CommandContext.getSessId4A());

		String loginOperID = CommandContext.getLoginOperID();
		if (null != loginOperID) {
			aIntPkg.SetTagParam("LoginOperID", loginOperID);
		}
		if (StringUtils.isNotEmpty(CommandContext.getMemcacheSessionID())) {
			if (!CommandContext.isWriteOperation()) {
				aIntPkg.SetTagParam("ReadMemcacheRecOid", CommandContext.getMemcacheSessionID());
			} else {
				aIntPkg.SetTagParam("WriteMemcacheRecOid", CommandContext.getMemcacheSessionID());
			}
		}
		if (log.isDebugEnabled()) {
			StringBuffer sb = new StringBuffer();
			sb.append("invokeReq:");
			sb.append(aIntPkg.toString());
			sb.append(",command=" + CommandContext.getCommand());
			sb.append(",menuId=" + CommandContext.getActiveMenuId());
//			sb.append(",backLevel=" + LogUtil.getBackLogLevel());
			log.debug(sb.toString());
		}
		boolean flag = Invoke(aIntPkg, 150);
		if (log.isDebugEnabled()) {
			StringBuffer sb = new StringBuffer();
			sb.append("invokeRes:");
			sb.append(aIntPkg.toString());
			sb.append(",command=" + CommandContext.getCommand());
			sb.append(",menuId=" + CommandContext.getActiveMenuId());
//			sb.append(",backLevel=" + LogUtil.getBackLogLevel());
			log.debug(sb.toString());
		}
		return flag;
	}

//	private static final boolean bStatReqFlag = GlobalConfig.getBooleanConfig("ngcrm.stat.req.log", false)
//			.booleanValue();
	@SuppressWarnings("unused")
	private static final boolean bStatReqFlag = false;

	// @SuppressWarnings({ "NM_METHOD_NAMING_CONVENTION" })
	public boolean Invoke(CIntPkg aIntPkg, int timeout) {
		if (StringUtils.isEmpty(aIntPkg.strMenuID)) {
			aIntPkg.strMenuID = FrameWorkUtil.getCurrentMenu();
		}
		try {
			LogTraceInfoManager logtrace = new LogTraceInfoManager();
			String tracevalue = "";
			Map<String, String> logmaps = logtrace.getLogTraceInfo();
			if (null != logmaps) {
				if (null != logmaps.get("TRACEVALUE")) {
					tracevalue = (String) logmaps.get("TRACEVALUE");
					String oper = aIntPkg.strOperator;
					String telnum = aIntPkg.strTelnum;
					String opcode = aIntPkg.strIntCmd;
					if ((tracevalue.equals(oper)) || (tracevalue.equals(telnum)) || (tracevalue.equals(opcode))) {
						CTagSet tagSet2 = new CTagSet();
						tagSet2.SetValue("LTRACER_NAME", (String) logmaps.get("LTRACER_NAME"));
						tagSet2.SetValue("TRACER_SERVER", (String) logmaps.get("TRACER_SERVER"));
						if (null != logmaps.get("TRACER_NAME")) {
							tagSet2.SetValue("TRACER_NAME", (String) logmaps.get("TRACER_NAME"));
						}
						CTagSet tagSet1 = new CTagSet();
						tagSet1.setTagSet("OM_TRACER", tagSet2);
						aIntPkg.setTagSetParam("_BSS_CONTEXT", tagSet1);
					}
				}
			}
		} catch (Exception e) {
			log.error("log_traceinfo error:" + e.getMessage());
		}
		String oldMsgId = "";
		boolean needStatLog = (CommandContext.isInBusi()) && (LogUtil.canStatReqLog()) && (LogUtil.isLogReqMenu());
		if (needStatLog) {
			LogUtil.logStatReqStart("4", aIntPkg.strIntID + "." + aIntPkg.strIntCmd);
			oldMsgId = CommandContext.getMessageId();
			aIntPkg.SetTagParam("LogSN", CommandContext.getLogId());
			aIntPkg.SetTagParam("AppName", AppEnv.getAppName());
			aIntPkg.SetTagParam("MessageId", oldMsgId);
			if (LogUtil.canStatSqlLog()) {
				aIntPkg.SetTagParam("SQLLog", "1");
				if (LogUtil.isControlSqlLogWithSqlText()) {
					aIntPkg.SetTagParam("SQLLogPrint", "1");
				}
			}
		}
		aIntPkg.SetTagParam("Menu", CommandContext.getActiveMenuId());
		if (StringUtils.isNotEmpty(CommandContext.getMemcacheSessionID())) {
			if (!CommandContext.isWriteOperation()) {
				aIntPkg.SetTagParam("ReadMemcacheRecOid", CommandContext.getMemcacheSessionID());
			} else {
				aIntPkg.SetTagParam("WriteMemcacheRecOid", CommandContext.getMemcacheSessionID());
			}
		}
		if (StringUtils.isNotEmpty(CommandContext.getCurrentOp())) {
			aIntPkg.SetTagParam("currentOP", CommandContext.getCurrentOp());
		}
		try {
//			if (GlobalConfig.isDevelopMode()) {
//				developDebug(aIntPkg, timeout);
//			} else {
//				String servType = GlobalConfig.getOriginalConfig("CSrvCall");
			if ("Socket".equals(this.srvTyp)) {
				((SocketUniCallFactory) uniCallFactory).getSubCmdByOpcode(aIntPkg);
			}
			uniCallFactory.getUniCall().invoke(aIntPkg, timeout);
//			}
		} catch (Exception ex) {
//			String resultType;
//			String resultCode;
//			String errorDesc;
			StringBuffer errMsg = new StringBuffer();
			errMsg.append("command=" + CommandContext.getCommand());
			errMsg.append(",menu=" + CommandContext.getActiveMenuId());
			errMsg.append(",intId=").append(aIntPkg.strIntID);
			errMsg.append(",intCmd=").append(aIntPkg.strIntCmd);
			errMsg.append(",telnum=").append(aIntPkg.strTelnum);
			errMsg.append(",region=").append(aIntPkg.strRegion);
			int errorCode = aIntPkg.errcode;
			String info = ex.getMessage();
			if ((info == null) || ("".equals(info)) || ("null".equals(info))) {
				info = RESOURCES.getMessage("IntService.Invoke.java");
			}
			errMsg.append(info);
			String errInfo = errMsg.toString();
			log.error(errInfo, ex);
			throw new OurErrorException(errorCode, errInfo, ex);
		} finally {
			if (needStatLog) {
				String resultType = "0";
				String resultCode = "0";

				String errorDesc = "";
				if ((aIntPkg.retcode == 0) || (aIntPkg.retcode == 3)) {
					resultType = "1";
					resultCode = String.valueOf(aIntPkg.errcode);
					errorDesc = aIntPkg.strInfo;
				}
				LogUtil.logStatReqEnd(oldMsgId, resultType, resultCode, errorDesc);
			}
		}
		List<?> list = aIntPkg.getEMsgInfos();
		if ((list != null) && (list.size() > 0)) {
			if (list.size() > 0) {
				StringBuffer cErrors = new StringBuffer();

				Iterator<?> iterator = list.iterator();
				while (iterator.hasNext()) {
					CEMsgInfo eMsgInfo = (CEMsgInfo) iterator.next();
					cErrors.append(eMsgInfo.getMsgInfo());
				}
				OurErrorException e = new OurErrorException(cErrors.toString(), aIntPkg.strIntID, aIntPkg.strIntCmd,
						list, aIntPkg.errcode);

				throw e;
			}
		}
		if ((aIntPkg.retcode != 1) && (aIntPkg.retcode != 100)) {
			return false;
		}
		return true;
	}

//	private void developDebug(CIntPkg aIntPkg, int timeout) throws Exception {
//		Boolean debug = (Boolean) ServletCommandContext.getAttribute("DEBUG_FLAG");
//		debug = Boolean.valueOf(debug == null ? false : debug.booleanValue());
//		String debugType = (String) ServletCommandContext.getAttribute("DEBUG_TYPE");
//		if ((debug.booleanValue()) && ("Socket".equals(debugType))) {
//			((SocketUniCallFactory) debugUniCallFactory).getSubCmdByOpcode(aIntPkg);
//			debugUniCallFactory.getUniCall().invoke(aIntPkg, timeout);
//		} else {
//			String servType = GlobalConfig.getOriginalConfig("CSrvCall");
//			if ("Socket".equals(servType)) {
//				((SocketUniCallFactory) uniCallFactory).getSubCmdByOpcode(aIntPkg);
//			}
//			uniCallFactory.getUniCall().invoke(aIntPkg, timeout);
//		}
//	}

	protected static synchronized CSrvCall getIntService() {
		CSrvCall aIntService = null;
		try {
			aIntService = intServiceHome.create();
		} catch (Exception ex) {
			log.error("create aIntService", ex);
			try {
				InitialContext context = new InitialContext();
				Object home = context.lookup("tuxedo.services.CSrvCallHome");
				intServiceHome = (CSrvCallHome) PortableRemoteObject.narrow(home, CSrvCallHome.class);
				aIntService = intServiceHome.create();
			} catch (Exception e) {
				log.error("create aIntService", e);
			}
		}
		return aIntService;
	}

	private static boolean inited = false;

	public IntService(String srvTyp, String srvAddr, String callSrv) {
		this.srvTyp = srvTyp;
		init(srvTyp, srvAddr, callSrv);
	}

	public static synchronized void init(String srvTyp, String srvAddr, String callSrv) {
		try {
			if (!inited) {
				if (log.isInfoEnabled()) {
					log.info("IntService: init begin ... ");
					System.out.println("IntService: init begin ... ");
				}
				if (uniCallFactory == null) {
					uniCallFactory = IUniCallFactory.getUniCallFactory(srvTyp, srvAddr, callSrv);
				}
//				if (debugUniCallFactory == null) {
//					debugUniCallFactory = new SocketUniCallFactory();
//				}
				inited = true;
				if (log.isInfoEnabled()) {
					log.info("IntService: init end.");
					System.out.println("IntService: init end.");
				}
			}
		} catch (Exception e) {
			System.out.println(ExceptionUtil.stackTrace(e));
			log.error(e);
		}
	}
	
	public static synchronized void destroy() {
		if (inited) inited = false;
		if (uniCallFactory != null) uniCallFactory = null;
		PublicService.stop();
	}
}
