package org.ayakaji.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.gmcc.boss.common.cbo.global.cbo.common.CTagSet;
import com.gmcc.boss.unicall.CIntPkg;
import com.gmcc.boss.unicall.IntService;

public class TransProber {

	public static void main(String[] args) {
		if (args != null && args.length > 0) {
			System.out.println(probe(args[0]));
		}
//		System.out.println(
//				probe("{ \"SRV_TYP\" : \"TPCloud\", \"SRV_ADDR\" : \"192.195.0.39:7777\", \"CALL_SRV\" : \"BOSSINT\", "
//						+ "\"TELNUM\" : \"15069071151\", \"REGION\" : \"531\" }"));
//		System.out.println(
//				probe("{ \"SRV_TYP\" : \"Cics\", \"SRV_ADDR\" : \"134.80.173.22:2006\", \"CALL_SRV\" : \"BOSSINT\", "
//						+ "\"TELNUM\" : \"15069071152\", \"REGION\" : \"531\" }"));
	}

	@SuppressWarnings("unchecked")
	public static String probe(String jsonData) {
		// 1. Validate in parameter
		if (jsonData == null || jsonData.equals(""))
			return "{\"ERROR\" : \"The message is empty!\"}";
		JSONObject jsonObj = null;
		try {
			System.out.println(jsonData);
			jsonObj = JSONObject.parseObject(jsonData);
			if (jsonObj == null)
				return "{\"ERROR\" : \"The message format is incorrect!\"}";
		} catch (JSONException e) {
			return Toolkit.getJSONString("The message format is incorrect!", e);
		}
		if (!jsonObj.containsKey("SRV_TYP") || !jsonObj.containsKey("SRV_ADDR") || !jsonObj.containsKey("CALL_SRV")
				|| !jsonObj.containsKey("TELNUM") || !jsonObj.containsKey("REGION")) {
			return "{\"ERROR\" : \"The parameters include at least SRV_TYP (TPCloud or CICS), SRV_ADDR (192.195.0.39:7777), "
					+ "CALL_SRV (BOSSINT), TELNUM (15069071152) and REGION (531)!\"}";
		}

		// 2. Execute
		IntService svc = new IntService(jsonObj.getString("SRV_TYP"), jsonObj.getString("SRV_ADDR"),
				jsonObj.getString("CALL_SRV"));
		CIntPkg pkg = new CIntPkg();
		pkg.strIntID = "bsacBoss";
		pkg.strIntCmd = "CscGetInfo";
		pkg.strTelnum = jsonObj.getString("TELNUM");
		pkg.strRegion = jsonObj.getString("REGION");
		pkg.strIntSubCmd = "arIntQryBalByServnum";
		CTagSet tagSet = new CTagSet();
		tagSet.put("ISBALANCEDETAIL", "1");
		tagSet.put("ISQRYBILL", "1");
		tagSet.put("COMMANDID", "arIntQryBalByServnum");
		tagSet.put("ISQRYCUSTBILL", "0");
		tagSet.put("TELNUM", jsonObj.getString("TELNUM"));
		tagSet.put("AccessPoint", "5");
		tagSet.put("UniContactFlag", "2");
		tagSet.put("ACTIVITYCODE", "T3000201");
		tagSet.put("AccessType", "5");
		pkg.m_inParams.add(0, tagSet);
		pkg.SetTagParam("EMG_CRMSTATUS", "0");
		pkg.SetTagParam("MessageId", "1");
		pkg.SetTagParam("EMG_IMSTATUS", "0");
		pkg.SetTagParam("ReqType", "7");
		pkg.SetTagParam("AppName", "HIBOSS");
		pkg.SetTagParam("AccessType", "bascBoss");
		pkg.SetTagParam("Menu", "T3000201");
		pkg.SetTagParam("LogSN", "");
		pkg.SetTagParam("EMG_BOSSSTATUS", "0");
		pkg.strMenuID = "UMMP_REALFEE_QRY1";
		if (svc.Invoke(pkg)) {
			String result = pkg.m_outParams.toString();
			IntService.destroy();
			if (result.indexOf("BALANCE") != -1) {
				return "{\"STATUS\" : \"SUCCESS\", \"DETAIL\" : \"" + pkg.m_outParams + "\"}";
			}
		}
		IntService.destroy();
		return "{\"STATUS\" : \"FAILURE\", \"DETAIL\" : \"" + pkg.m_outParams + "\"}";
	}

	public static String start(String jsonData) {
		Runtime runtime = Runtime.getRuntime();
		try {
			String ret = "";
			Process process = runtime
					.exec(new String[] { "/bin/sh", "-c", "java -jar StandaloneProber-0.0.4.jar \"" + jsonData.replace("\"", "\\\"") + "\"" });
			BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
			BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "GBK"));
			String line;
			while ((line = stdoutReader.readLine()) != null) {
				if (line.indexOf("STATUS") != -1) ret = line;
			}
			while ((line = stderrReader.readLine()) != null) {
				System.out.println(line);
			}
			int exitVal = process.waitFor();
			if (exitVal != 0)
				System.out.println("process exit value is " + exitVal);
			return ret;
		} catch (IOException e) {
			return "IOException";
		} catch (InterruptedException e) {
			return "InterruptedException";
		}
	}
}
