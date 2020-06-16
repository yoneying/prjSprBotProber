package org.ayakaji.probe;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/probe")
public class Controller {
	private Logger logger = LogManager.getLogger(Controller.class);

	/**
	 * [测试用例]
	 * 
	 * @return
	 */
	@RequestMapping("/test")
	public Map<String, String> test() {
		Map<String, String> a = new HashMap<String, String>();
		a.put("1", "hello");
		a.put("2", "world");
		logger.debug("");
		return a;
	}

	/**
	 * [包月量查询 CBE]
	 * 
	 * @param paraMap
	 * @return
	 */
	@RequestMapping("/byl")
	public String byl(@RequestBody String jsonData) {
		return CBEProber.byl(jsonData);
	}

	/**
	 * [余额查询 CBE]
	 * 
	 * @param json
	 * @return
	 */
	@RequestMapping("/bal")
	public String bal(@RequestBody String json) {
		return CBEProber.bal(json);
	}

	/**
	 * [客户画像查询 IOP]
	 * 
	 * @param jsonData
	 * @return
	 */
	@RequestMapping("/khhx")
	public String khhx(@RequestBody String jsonData) {
		return IOPProber.khhx(jsonData);
	}
	
	/**
	 * Query balance using TPCloud or CICS
	 * @param jsonData
	 * @return
	 */
	@RequestMapping("/balTrans")
	public String balTrans(@RequestBody String jsonData) {
		return TransProber.start(jsonData);
	}
}
