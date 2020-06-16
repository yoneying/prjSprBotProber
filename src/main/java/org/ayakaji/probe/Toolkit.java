package org.ayakaji.probe;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.IllegalBlockingModeException;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.spring.PropertyPreFilters;
import com.alibaba.fastjson.support.spring.PropertyPreFilters.MySimplePropertyPreFilter;

public class Toolkit {
	private static Logger logger = LogManager.getLogger(Toolkit.class);

	/**
	 * IP Regular Expression
	 */
	private static final String ipRegex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
	
	/**
	 * Excluded Properties
	 */
	private static final String[] propExc = new String[] { "inputStream", "outputStream" };
	
	/**
	 * If regular expression match
	 * @param regex
	 * @param data
	 * @return
	 */
	private static final boolean match(String regex, String data) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(data);
		return matcher.matches();
	}

	/**
	 * Validate json & convert to json object.
	 * 
	 * @param json
	 * @param props
	 * @return
	 */
	public static JSONObject validate(String json, String[] props) {
		if (json == null || json.equals("")) {
			logger.error("{\"ERROR\" : \"Json is empty.\"}");
			return null;
		}
		JSONObject jsonObj = null;
		try {
			jsonObj = JSONObject.parseObject(json);
			if (jsonObj == null) {
				logger.error("{\"ERROR\" : \"Json format is incorrect.\"}");
				return null;
			}
			for (String prop : props) {
				if (!jsonObj.containsKey(prop)) { // Check attributes' integrity
					logger.error("{\"ERROR\" : \"Json must contain " + prop + "\"}");
					return null;
				}
				if (prop.equals("HOST") && !match(ipRegex, jsonObj.getString("HOST"))) { // Check attributes' validity
					logger.error("{\"ERROR\" : \"HOST is not a valid IP Address!\"}");
					return null;
				} else if (prop.equals("PORT")
						&& (jsonObj.getIntValue("PORT") > 65535 || jsonObj.getIntValue("PORT") < 0)) {
					logger.error("{\"ERROR\" : \"PORT is not within legal range!\"}");
					return null;
				} else if (prop.equals("USERID") && jsonObj.getString("USERID").length() <= 3) {
					logger.error("{\"ERROR\" : \"USERID's length at least 4!\"}");
					return null;
				}
			}
		} catch (JSONException e) {
			logger.error("{\"ERROR\" : \"Request body convert to JSON object failure.\"}");
			return null;
		}
		return jsonObj;
	}
	
	/**
	 * Including Filter
	 * 
	 * @param props
	 * @return
	 */
	public static final MySimplePropertyPreFilter getIncludeFilter(String[] props) {
		PropertyPreFilters filters = new PropertyPreFilters();
		MySimplePropertyPreFilter includeFilter = filters.addFilter();
		includeFilter.addIncludes(props);
		return includeFilter;
	}
	
	/**
	 * Excluding Filter
	 * 
	 * @param props
	 * @return
	 */
	public static final MySimplePropertyPreFilter getExcludeFilter(String[] props) {
		PropertyPreFilters filters = new PropertyPreFilters();
		MySimplePropertyPreFilter excludeFilter = filters.addFilter();
		excludeFilter.addExcludes(props);
		return excludeFilter;
	}
	

	/**
	 * [将摘要和详细转换为报文]
	 * 
	 * @param summary
	 * @param except
	 * @return
	 */
	public static String getJSONString(String summary, Object detail) {
		JSONObject jsonObj = new JSONObject(new LinkedHashMap<String, Object>());
		jsonObj.put("ABSTRACT", summary);
		jsonObj.put("BRIEF", detail);
		return JSON.toJSONString(jsonObj, getExcludeFilter(propExc), SerializerFeature.PrettyFormat);
	}
	
	/**
	 * [打开套接字封装]
	 * 
	 * @param host
	 * @param port
	 * @return
	 */
	public static Socket openSocket(String host, int port) {
		InetSocketAddress sockAddr = null;
		try {
			sockAddr = new InetSocketAddress(host, port);
		} catch (IllegalArgumentException e) {
			logger.error(getJSONString("The port is outside the valid port range or the host address is empty!", e));
			return null;
		} catch (SecurityException e) {
			logger.error(getJSONString("Security manager refuses to resolve host name!", e));
			return null;
		}
		Socket sock = new Socket();
		try {
			sock.setKeepAlive(false); // Do not send probe packets to each other when the TCP connection is idle
			sock.setReceiveBufferSize(65535); // Receive buffer size, refer to F5 Receive Window settings
			sock.setSendBufferSize(65535); // Send buffer size, refer to F5 Send Buffer setting
			sock.setSoLinger(false, -1); // Disable Linger Time
			sock.setSoTimeout(10000); // Read stream timeout
			sock.setTcpNoDelay(false); // Disable Nagle algorithm
			sock.setTrafficClass(0x04); // High availability
		} catch (SocketException e) {
			logger.error(getJSONString("Socket Exception.", e));
			return null;
		} catch (IllegalArgumentException e) {
			logger.error(getJSONString("Illegal Argument Exception.", e));
		}

		try {
			sock.connect(sockAddr);
			logger.info(getJSONString("Connection established successfully!", sock));
			return sock;
		} catch (IOException e) {
			logger.error(getJSONString("An error occurred during the connection!", e));
			return null;
		} catch (IllegalBlockingModeException e) {
			logger.error(getJSONString("This socket has an associated channel and the channel is in non-blocking mode!", e));
			return null;
		} catch (IllegalArgumentException e) {
			logger.error(getJSONString("The endpoint is empty or this socket is not compatible with the socket address!", e));
			return null;
		}
	}
	
	public static OutputStream getOutputStream(Socket sock) {
		if (sock == null || sock.isClosed()) {
			logger.error("{\"ABSTRACT\" : \"Socket status is incorrect!\"}");
			return null;
		}
		OutputStream os = null;
		try {
			os = sock.getOutputStream();
			return os;
		} catch (IOException e) {
			closeSocket(sock);
			logger.error(getJSONString("I / O exception or socket not connected when creating output stream!", e));
		}
		return null;
	}
	
	public static void shutdownOutput(Socket sock) {
		if (sock == null || sock.isClosed()) {
			logger.error("{\"ABSTRACT\" : \"Socket status is incorrect!\"}");
			return;
		}
		try {
			sock.shutdownOutput();
		} catch (IOException e) {
			logger.error(getJSONString("I/O Exception", e));
		}
	}
	
	public static InputStream getInputStream(Socket sock) {
		if (sock == null || sock.isClosed()) {
			logger.error("{\"ABSTRACT\" : \"Socket status is incorrect!\"}");
			return null;
		}
		InputStream is = null;
		try {
			is = sock.getInputStream();
			return is;
		} catch (IOException e) {
			closeSocket(sock);
			logger.error(getJSONString("I / O exception or socket not connected when creating output stream!", e));
		}
		return null;
	}
	
	public static void close(BufferedInputStream bis) {
		try {
			bis.close();
		} catch (IOException e) {
			logger.error(getJSONString("Exception", e));
		}
	}
	
	/**
	 * Close Socket Package
	 * 
	 * @param sock
	 */
	public static void closeSocket(Socket sock) {
		if (sock == null)
			return;
		if (!sock.isInputShutdown()) { // 关闭InputStream
			InputStream is = null;
			try {
				is = sock.getInputStream();
			} catch (IOException e) {
				logger.error("套接字已关闭、未连接或已使用命令关闭了套接字输入");
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("发生I/O错误");
				} finally {
					is = null;
				}
			}
		}
		if (!sock.isOutputShutdown()) {
			OutputStream os = null;
			try {
				os = sock.getOutputStream();
			} catch (IOException e) {
				logger.error("套接字未连接");
			}
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					logger.error("I/O错误");
				} finally {
					os = null;
				}
			}
		}
		if (!sock.isClosed()) {
			try {
				sock.close();
			} catch (IOException e) {
				logger.error("I/O错误");
			} finally {
				sock = null;
			}
		}
	}
}
