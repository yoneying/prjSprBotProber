package com.gmcc.boss.unicall;

import com.gmcc.boss.waf.service.resource.OurResources;
import com.ibm.ctg.client.ECIRequest;

public class CicsECI {
	private static final OurResources RESOURCES = OurResources.getOurResources(CicsECI.class);
	protected ECIRequest m_eci = new ECIRequest();
	private boolean m_bFlag = false;
	public static final int stUp = 1;
	public static final int stDown = 2;
	public static final int stUnknown = 0;
	public static final short DEFAULT_ECI_TIMEOUT = 150;

	public CicsECI() {
		reset();
	}

	public CicsECI(String servName) {
		this();
		this.m_eci.Server = servName;
	}

	public void setServer(String server) throws Exception {
		if (server == null) {
			throw new Exception(RESOURCES.getMessage("CicsECI.fwqmc"));
		}
		if ((this.m_eci.Server != null) && (server.equals(this.m_eci.Server))) {
			return;
		}
		if (this.m_bFlag) {
			throw new Exception(RESOURCES.getMessage("CicsECI.jzxg"));
		}
		this.m_eci.Server = server;
	}

	public void setUserInfo(String userId, String passwd) {
		this.m_eci.Userid = userId;
		this.m_eci.Password = passwd;
	}

	public void setTransId(String transId) {
		this.m_eci.Transid = transId;
	}

	public void beginTransaction() throws ECIException {
		beginTransaction(150);
	}

	public void setECITimeout(short timeout) {
		this.m_eci.setECITimeout(timeout);
	}

	public void beginTransaction(int timeout) throws ECIException {
		if (this.m_bFlag) {
			throw new ECIException(RESOURCES.getMessage("CicsECI.shiwu"));
		}
		this.m_eci.Extend_Mode = 1;
		this.m_eci.Luw_Token = 0;
		this.m_eci.setECITimeout((short) timeout);
		this.m_bFlag = true;
	}

	public void commit() throws ECIException {
		if (!this.m_bFlag) {
			throw new ECIException(RESOURCES.getMessage("CicsECI.tjsb"));
		}
		ECIRequest eci = new ECIRequest();
		copyECI(eci, this.m_eci);

		eci.Server = null;
		eci.Extend_Mode = 2;

		reset();
		this.m_bFlag = false;
		ExternalCall(eci);
	}

	public void rollback() throws ECIException {
		if (!this.m_bFlag) {
			throw new ECIException(RESOURCES.getMessage("CicsECI.htsb"));
		}
		ECIRequest eci = new ECIRequest();
		copyECI(eci, this.m_eci);

		eci.Server = null;
		eci.Extend_Mode = 4;

		reset();
		this.m_bFlag = false;
		ExternalCall(eci);
	}

	public void link(String progId, byte[] inBuf, int inLen) throws ECIException {
		ECIRequest eci = new ECIRequest();
		copyECI(eci, this.m_eci);
		eci.Program = progId;
		if ((inBuf == null) || (inLen == 0)) {
			eci.Commarea = null;
			eci.Commarea_Length = 0;
		} else {
			eci.Commarea = inBuf;
			eci.Commarea_Length = inLen;
		}
		ExternalCall(eci);
		if ((this.m_bFlag) && (eci.Luw_Token != 0)) {
			this.m_eci.Luw_Token = eci.Luw_Token;
		}
	}

	public void link(String progId, byte[] inBuf, int inLen, String transId) throws ECIException {
		setTransId(transId);
		link(progId, inBuf, inLen);
	}

	public static void ExternalCall(ECIRequest eci) throws ECIException {
		try {
			CicsGW.flow(eci);
		} catch (Exception e) {
			throw new ECIException(RESOURCES.getMessage("CicsECI.qqsb") + e.getMessage());
		}
		int retcode = eci.getCicsRc();
		if (retcode != 0) {
			throw new ECIException(eci.getCicsRc(), RESOURCES.getMessage("CicsECI.cwfhjg") + eci.getCicsRcString());
		}
	}

	private void copyECI(ECIRequest dst, ECIRequest src) {
		dst.Server = src.Server;
		dst.Userid = src.Userid;
		dst.Password = src.Password;
		dst.Transid = src.Transid;
		dst.Call_Type = src.Call_Type;
		dst.Extend_Mode = src.Extend_Mode;
		dst.Luw_Token = src.Luw_Token;
		dst.setECITimeout(src.getECITimeout());
	}

	private void reset() {
		this.m_eci.Call_Type = 12;
		this.m_eci.Commarea = null;
		this.m_eci.Commarea_Length = 0;
		this.m_eci.Transid = null;
		this.m_eci.Extend_Mode = 0;
		this.m_eci.Luw_Token = 0;
		this.m_eci.setECITimeout((short) 150);
	}

	public int serverStatus() {
		ECIRequest eci = new ECIRequest().getStatus(this.m_eci.Server);
		try {
			ExternalCall(eci);
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
		return eci.CicsServerStatus;
	}

	public static int serverStatus(String servName) {
		ECIRequest eci = new ECIRequest().getStatus(servName);
		try {
			ExternalCall(eci);
		} catch (Exception e) {
			return 0;
		}
		return eci.CicsServerStatus;
	}

	public static void main(String[] argv) {
		if ((argv == null) || (argv.length != 4)) {
			throw new IllegalArgumentException("usage argv: server username password servicename");
		}
		CicsECI cicsECI = new CicsECI();

		int inLen = 31744;
		byte[] inBuf = new byte[inLen];
		try {
			cicsECI.setServer(argv[0]);
			cicsECI.setUserInfo(argv[1], argv[2]);
			cicsECI.beginTransaction();
			cicsECI.link(argv[3], inBuf, inLen);
			cicsECI.commit();
			String str = new String(inBuf);
			System.out.println(str);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Hello world!");
	}
}
