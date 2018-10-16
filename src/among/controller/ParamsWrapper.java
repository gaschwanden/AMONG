package among.controller;
import java.util.HashMap;
import java.util.Map;

import repast.simphony.parameter.Parameters;


public interface ParamsWrapper extends Cloneable {

	static final Object NULL = new Object();
	 
	static final Map<String, String> paramMap = new HashMap<String, String>();

	Webclient webclientObj = new Webclient();
//	obj.sendGet();

	 public static Object getParameterValue(Parameters PObj, String paramName) {
		
		    return PObj.getValue(paramName);
		  }
	 
	 public static Map<String, String> createParamMap(Parameters pObj) {
		 
		 paramMap.put(pObj.getDisplayName("shockSupply"), pObj.getValue("shockSupply").toString());
		 paramMap.put(pObj.getDisplayName("policyShock"), pObj.getValue("policyShock").toString());
		 paramMap.put(pObj.getDisplayName("CGTMagnitude"), pObj.getValue("CGTMagnitude").toString());
		 paramMap.put(pObj.getDisplayName("NGMagnitude"), pObj.getValue("NGMagnitude").toString());
		 paramMap.put(pObj.getDisplayName("demandMagnitude"), pObj.getValue("demandMagnitude").toString());
		 paramMap.put(pObj.getDisplayName("supplyMagnitude"), pObj.getValue("supplyMagnitude").toString());
		 paramMap.put(pObj.getDisplayName("csv"), pObj.getValue("csv").toString());
		 paramMap.put(pObj.getDisplayName("run"), pObj.getValue("run").toString());
		 paramMap.put(pObj.getDisplayName("households"), pObj.getValue("households").toString());
		 
		 paramMap.put(pObj.getDisplayName("aggregate"), pObj.getValue("aggregate").toString());
		 paramMap.put(pObj.getDisplayName("randomSeed"), pObj.getValue("randomSeed").toString());
		 paramMap.put(pObj.getDisplayName("cgt"), pObj.getValue("cgt").toString());
		 paramMap.put(pObj.getDisplayName("shockDemand"), pObj.getValue("shockDemand").toString());
		 paramMap.put(pObj.getDisplayName("incomeMagnitude"), pObj.getValue("incomeMagnitude").toString());
		 paramMap.put(pObj.getDisplayName("shockIncome"), pObj.getValue("shockIncome").toString());
		 paramMap.put(pObj.getDisplayName("ng"), pObj.getValue("ng").toString());
	
		 return paramMap;
	 }
	 
	 public static void sendParameterMap(Parameters pObj) throws Exception {
		 Map<String,String> paramsMap= createParamMap(pObj);
		 webclientObj.postMethod("http://localhost:8080/among-model/sendParamFromEclipse", paramsMap);
		 
	 }
	
//	  HashMap getAllParameters(String paramName);



}
