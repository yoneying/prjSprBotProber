package com.gmcc.boss.unicall;

import java.io.IOException;
import java.util.Hashtable;

import com.ibm.ctg.client.ECIRequest;
import com.ibm.ctg.client.JavaGateway;

public class CicsGW {
	public static final String GW_URL_LOCAL = "tcp://localhost:2006/";
	static int m_ctg_weight_all = 0;
	static boolean use_ctg_list = false;
	static String[] m_ctg_list;
	static Hashtable<String, String> m_ctg_item_list = new Hashtable<String, String>();
	GwNode m_hdr = null;
	static int m_NodeCount = 0;
	static int m_NodeCurrent = 0;
	static CicsGW cicsGW = new CicsGW();
	public static String m_ctg_addr = ""; // 新特性：CTG服务地址，若与新地址不同，则需要重新初始化
	
	public static void reset() {
		String ctgSrv = IUniCallFactory.ctgSrv.get();
		if (!m_ctg_addr.equals(ctgSrv)) {
			try {
				cicsGW.finalize(); // 销毁
			} catch (Throwable e) {
				e.printStackTrace();
			}
			cicsGW = null;
			cicsGW = new CicsGW();
		}
	}

	public CicsGW() {
		try {
			String ctgSrv = IUniCallFactory.ctgSrv.get();
			m_ctg_addr = ctgSrv; // 保存CTG服务地址
			String str_ctg_list = "tcp://" + ctgSrv + "/,tcp://" + ctgSrv + "/";
			String str_ctg_weight_list = "6,4";
			String[] ctg_list = str_ctg_list.split(",");
			String[] ctg_weight_list = str_ctg_weight_list.split(",");
			if ((ctg_list == null) || (ctg_weight_list == null) || (ctg_list.length != ctg_weight_list.length)
					|| (ctg_list.length == 0)) {
				System.out.println(
						"The length of ctg list does not match the length of ctg weight list,use local style only!");
			} else {
				int[] m_ctg_weight_list = new int[ctg_weight_list.length];
				for (int i = 0; i < ctg_weight_list.length; i++) {
					m_ctg_weight_all += Integer.parseInt(ctg_weight_list[i]);
					m_ctg_weight_list[i] = Integer.parseInt(ctg_weight_list[i]);
				}
				m_ctg_list = new String[m_ctg_weight_all];
				int count = 0;
				for (int j = 0; j < m_ctg_weight_list.length; j++) {
					for (int k = 0; k < m_ctg_weight_list[j]; k++) {
						m_ctg_list[(count++)] = ctg_list[j];
						System.out.println("m_ctg_list[" + (count - 1) + "]=" + ctg_list[j]);
					}
				}
				use_ctg_list = true;
				System.out.println("CTG profile loaded ok!");
			}
			return;
		} catch (Exception ex) {
			System.out.println("Load ctg list error:" + ex.getMessage());
		}
	}

	public static int length() {
		return m_NodeCount;
	}

	public static int getCurrent() {
		return m_NodeCurrent;
	}

	public static String[] getCtgList() {
		return m_ctg_list;
	}

	public static Hashtable<String, String> getCtgItemList() {
		return m_ctg_item_list;
	}

	public static void flow(ECIRequest eci) throws Exception {
		GwNode node = null;
		node = cicsGW.getNode();
		if (node == null) {
			System.out.println("Ctg Get Node Error!");
			return;
		}
		try {
			if (!node.gateway.isOpen()) {
				node.gateway.open();
			}
			node.gateway.flow(eci);
			node.gateway.close();
		} catch (Exception e) {
			node.gateway.close();
			throw e;
		} finally {
			cicsGW.freeNode(node);
		}
	}

	synchronized GwNode getNode() throws IOException {
		if (this.m_hdr == null) {
			this.m_hdr = newGwNode();
			this.m_hdr.Next = null;
		}
		GwNode node = this.m_hdr;
		this.m_hdr = this.m_hdr.Next;

		m_NodeCurrent += 1;

		return node;
	}

	synchronized void freeNode(GwNode node) {
		if (node != null) {
			node.Next = this.m_hdr;
			this.m_hdr = node;
			m_NodeCurrent -= 1;
		}
	}

	GwNode newGwNode() throws IOException {
		GwNode node = new GwNode();
		node.gateway = new JavaGateway();
		String gwUrl = getNextUrl();
		node.gateway.setURL(gwUrl);
		m_ctg_item_list.put(String.valueOf(m_NodeCount++), gwUrl);

		return node;
	}

	synchronized String getNextUrl() {
		String gw_url = "";
		if (!use_ctg_list) {
			gw_url = "tcp://localhost:2006/";
		} else {
			gw_url = m_ctg_list[(m_NodeCount % m_ctg_weight_all)];
		}
		return gw_url;
	}
}
