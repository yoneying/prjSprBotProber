package org.ayakaji.probe;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import io.itit.itf.okhttp.FastHttpClient;
import io.itit.itf.okhttp.Response;

public class IOPProber {

	public static String khhx(String jsonData) {
		if (jsonData == null || jsonData.equals(""))
			return "{\"ERROR\" : \"The message is empty!\"}";
		JSONObject jsonObj = null;
		try {
			jsonObj = JSONObject.parseObject(jsonData);
			if (jsonObj == null)
				return "{\"ERROR\" : \"The message format is incorrect!\"}";
		} catch (JSONException e) {
			return Toolkit.getJSONString("The message format is incorrect!", e);
		}
		if (!jsonObj.containsKey("HOST") || !jsonObj.containsKey("PORT") || !jsonObj.containsKey("USERID")) {
			return "{\"ERROR\" : \"The parameters include at least HOST (host name or IP), PORT (port number) and USERID (user ID)!\"}";
		}
		try {
			Response resp = FastHttpClient.post().addHeader("Content-Type", "text")
					.body("<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/"
							+ "soap-envelope\" xmlns:att=\"http://attrquery.exintf.campaign.customization.huawei.com\" xmlns:xsd=\"http:"
							+ "//attrquery.exintf.campaign.customization.huawei.com/xsd\" xmlns:xsd1=\"http://domain.util.common.campaig"
							+ "n.crm.huawei.com/xsd\"><soap:Header/><soap:Body><att:queryUserAttrsService><att:args0><xsd:channelType>bs"
							+ "acHal</xsd:channelType><xsd:field></xsd:field><xsd:msisdn>" + jsonObj.getString("USERID")
							+ "</xsd:msisdn"
							+ "><xsd:requestHeader><xsd1:accessChannel>bsacHal</xsd1:accessChannel><xsd1:beId>101</xsd1:beId><xsd1:langu"
							+ "age>2</xsd1:language><xsd1:operator>Campaign</xsd1:operator><xsd1:password>" + jsonObj.getString("PASSWD") + "</xsd1"
							+ ":password><xsd1:transactionId>1</xsd1:transactionId><xsd1:version></xsd1:version></xsd:requestHeader><xsd"
							+ ":type>M</xsd:type></att:args0></att:queryUserAttrsService></soap:Body></soap:Envelope>")
					.url("http://" + jsonObj.getString("HOST") + ":" + jsonObj.getString("PORT")
							+ "/pc/services/CUserAttrQueryService")
					.build().execute();
			String respTxt = resp.string();
			if (respTxt.indexOf("<xsd:resultMessage>查询成功.</xsd:resultMessage>") != -1) {
				return "{\"STATUS\" : \"SUCCESS\", \"DETAIL\" : \"" + respTxt + "\"}";
			} else {
				return "{\"STATUS\" : \"FAILURE\", \"DETAIL\" : \"" + respTxt + "\"}";
			}
		} catch (Exception e) {
			return Toolkit.getJSONString("ERROR", e);
		}
	}
}
