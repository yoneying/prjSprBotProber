package com.gmcc.boss.unicall.driver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Vector;

public class TPClient {
	private Selector m_sel = null;
	private SendEventHandler m_evtHandler = null;
	private ClientHandler m_msgHandler = null;
	private AddrList m_addrs = new AddrList();
	private Vector<WaitCloseTask> m_tasks = new Vector<WaitCloseTask>();

	public TPClient() {
		this.m_evtHandler = new SendEventHandler();
		this.m_msgHandler = new ClientHandler(this);
		try {
			this.m_sel = Selector.open();
			this.m_evtHandler.register(this.m_sel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isConnected() {
		return this.m_msgHandler.isConnected();
	}

	private int init(String strAddrUrl) {
		try {
			this.m_addrs.parse(strAddrUrl);
		} catch (Exception e) {
			throw new TPError(-1, e.getMessage());
		}
		return this.m_addrs.size();
	}

	public boolean connect(String strAddrs) {
		int ret = init(strAddrs);
		if (0 == ret) {
			return false;
		}
		return connect();
	}

	public boolean connect() {
		this.m_addrs.resetIdx();
		int count = this.m_addrs.size();
		for (int i = 0; i < count; i++) {
			InetSocketAddress addr = this.m_addrs.getAddr();
			Logger.info("try to connect server '%s'...", new Object[] { addr.toString() });
			if (this.m_msgHandler.connect(addr)) {
				Logger.info("connect to server '%s' succeed.", new Object[] { addr.toString() });
				return true;
			}
			Logger.warn("connect to server '%s' failed.", new Object[] { addr.toString() });
		}
		return false;
	}

	public void handleEvents(long timeout_ms) {
		prepareHandlers();
		try {
			if (this.m_sel.select(timeout_ms) == 0) {
				return;
			}
		} catch (IOException e) {
			this.m_msgHandler.close();
			return;
		}
		for (SelectionKey key : this.m_sel.selectedKeys()) {
			IMsgHandler hd = (IMsgHandler) key.attachment();
			if ((key.isReadable()) && (!hd.onRead())) {
				hd.close();
			} else if ((key.isWritable()) && (!hd.onWrite())) {
				hd.close();
			}
		}
		this.m_sel.selectedKeys().clear();
	}

	private void prepare(ClientHandler hd) {
		if (!hd.isConnected()) {
			hd.replyNoConnection();
			return;
		}
		hd.tryWrite();
		hd.register(this.m_sel);
	}

	private void prepareHandlers() {
		prepare(this.m_msgHandler);
		if (this.m_tasks.isEmpty()) {
			return;
		}
		Vector<WaitCloseTask> bak = new Vector<WaitCloseTask>();
		bak.addAll(this.m_tasks);
		for (WaitCloseTask task : bak) {
			prepare(task.m_hd);
			task.run();
		}
	}

	public TPRsp tpcall(TPReq req) {
		ClientHandler hd = getValidHandler();
		if (null == hd) {
			throw new TPError(-1, "no active connection.");
		}
		RequestSlotMap reqMap = hd.reqMap();
		TPRsp rsp = null;
		RequestSlot slot = reqMap.acquire(req);
		if (slot == null) {
			throw new TPError(-1, "acquire request slot failed.");
		}
		this.m_evtHandler.triggerWrite();
		rsp = waitForAck(slot);
		reqMap.giveback(slot);
		if (rsp == null) {
			throw new TPError(-1, "wait response timeout.");
		}
		if (rsp.errcode != 0) {
			throw new TPError(rsp.errcode, new String(rsp.data));
		}
		return rsp;
	}

	private ClientHandler getValidHandler() {
		if (this.m_msgHandler.isConnected()) {
			return this.m_msgHandler;
		}
		return null;
	}

	private TPRsp waitForAck(RequestSlot slot) {
		long timeout = slot.request.timeout + 1000;
		if (timeout < 1000L) {
			timeout = 1000L;
		}
		synchronized (slot) {
			if ((slot.response != null) && (slot.request.msg_id == slot.response.msg_id)) {
				return slot.response;
			}
			long cur_time = System.currentTimeMillis();
			long dst_time = cur_time + timeout;
			while (cur_time < dst_time) {
				try {
					slot.wait(timeout);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
				if ((slot.response != null) && (slot.request.msg_id == slot.response.msg_id)) {
					break;
				}
				cur_time = System.currentTimeMillis();
				if (cur_time >= dst_time) {
					break;
				}
				timeout = dst_time - cur_time;
				System.out.println("spurious wakeup, left timeout: " + timeout);
			}
		}
		return slot.response;
	}

	private boolean isValidAddr(InetSocketAddress addr) {
		for (WaitCloseTask task : this.m_tasks) {
			if (task.m_hd.isSameAddr(addr)) {
				return false;
			}
		}
		return true;
	}

	void switchHandler(ClientHandler handler) {
		if (handler != this.m_msgHandler) {
			return;
		}
		WaitCloseTask task = new WaitCloseTask(this, this.m_msgHandler);
		this.m_tasks.add(task);

		this.m_addrs.resetIdx();
		ClientHandler hd = new ClientHandler(this);
		for (int i = 0; i < this.m_addrs.size(); i++) {
			InetSocketAddress addr = this.m_addrs.getAddr();
			if ((isValidAddr(addr)) &&

					(hd.connect(addr))) {
				break;
			}
		}
		this.m_msgHandler = hd;
	}

	void removeTask(WaitCloseTask task) {
		task.m_hd.replyNoConnection();
		task.m_hd.close();
		this.m_tasks.remove(task);
		Logger.info("closing task is completed, remote address: %s", new Object[] { task.m_hd.m_addr.toString() });
		task.m_hd = null;
	}
}
