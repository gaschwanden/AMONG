package among.controller;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.jr.ob.JSON;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import repast.simphony.parameter.Parameters;
import repast.simphony.parameter.Schema;

public final class WebclientParameters implements Parameters {
	private static final String ENDPOINT_PREFIX = "http://localhost:8080/among-model";

	// TODO: Temporary for testing. This should be a list of a new objects that
	// fully maps each parameters metadata
	private final Map<String, Object> parameterMap = new HashMap<>();

	public static WebclientParameters create() {
		final Map<String, Object> initialParameters = new HashMap<>();

		try {
			final String responseString = Unirest.get(ENDPOINT_PREFIX + "/getInitialParam").asString().getBody();

			final Map<String, Object> responseMap = JSON.std.mapFrom(responseString);

			initialParameters.putAll(responseMap);

		} catch (UnirestException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new WebclientParameters(initialParameters);
	}

	private WebclientParameters(final Map<String, Object> initialParameters) {
		parameterMap.putAll(initialParameters);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		// TODO Auto-generated method stub
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public Schema getSchema() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getValue(String paramName) {
		// TODO: Testing code, get rid of it ASAP
		return parameterMap.get(paramName);
	}

	@Override
	public Double getDouble(String paramName) {
		// TODO: Testing code, get rid of it ASAP
		return (Double) parameterMap.get(paramName);
	}

	@Override
	public Integer getInteger(String paramName) {
		// TODO: Testing code, get rid of it ASAP
		return (Integer) parameterMap.get(paramName);
	}

	@Override
	public Boolean getBoolean(String paramName) {
		// TODO: Testing code, get rid of it ASAP
		return (Boolean) parameterMap.get(paramName);
	}

	@Override
	public String getString(String paramName) {
		// TODO: Testing code, get rid of it ASAP
		return (String) parameterMap.get(paramName);
	}

	@Override
	public Long getLong(String paramName) {
		// TODO: Testing code, get rid of it ASAP
		return (Long) parameterMap.get(paramName);
	}

	@Override
	public Float getFloat(String paramName) {
		// TODO: Testing code, get rid of it ASAP
		return (Float) parameterMap.get(paramName);
	}

	@Override
	public String getValueAsString(String paramName) {
		// TODO: Testing code, get rid of it ASAP
		return String.valueOf(parameterMap.get(paramName));
	}

	@Override
	public void setValue(String paramName, Object val) {
		// TODO: Testing code, get rid of it ASAP
		throw new IllegalStateException("setValue not implemented yet; stop calling me!");
	}

	@Override
	public boolean isReadOnly(String paramName) {
		// TODO: Testing code, get rid of it ASAP
		throw new IllegalStateException("isReadOnly not implemented yet; stop calling me!");
	}

	@Override
	public String getDisplayName(String paramName) {
		// TODO: Testing code, get rid of it ASAP
		return paramName;
	}

	@Override
	public Parameters clone() {
		try {
			return (Parameters) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}
}
//
// static final Object NULL = new Object();
//
// static final Map<String, String> paramMap = new HashMap<String, String>();
//
// Webclient webclientObj = new Webclient();
//// obj.sendGet();
//
// public static Object getParameterValue(Parameters PObj, String paramName) {
//
// return PObj.getValue(paramName);
// }
//
// public static Map<String, String> createParamMap(Parameters pObj) {
//
// paramMap.put(pObj.getDisplayName("shockSupply"),
// pObj.getValue("shockSupply").toString());
// paramMap.put(pObj.getDisplayName("policyShock"),
// pObj.getValue("policyShock").toString());
// paramMap.put(pObj.getDisplayName("CGTMagnitude"),
// pObj.getValue("CGTMagnitude").toString());
// paramMap.put(pObj.getDisplayName("NGMagnitude"),
// pObj.getValue("NGMagnitude").toString());
// paramMap.put(pObj.getDisplayName("demandMagnitude"),
// pObj.getValue("demandMagnitude").toString());
// paramMap.put(pObj.getDisplayName("supplyMagnitude"),
// pObj.getValue("supplyMagnitude").toString());
// paramMap.put(pObj.getDisplayName("csv"), pObj.getValue("csv").toString());
// paramMap.put(pObj.getDisplayName("run"), pObj.getValue("run").toString());
// paramMap.put(pObj.getDisplayName("households"),
// pObj.getValue("households").toString());
//
// paramMap.put(pObj.getDisplayName("aggregate"),
// pObj.getValue("aggregate").toString());
// paramMap.put(pObj.getDisplayName("randomSeed"),
// pObj.getValue("randomSeed").toString());
// paramMap.put(pObj.getDisplayName("cgt"), pObj.getValue("cgt").toString());
// paramMap.put(pObj.getDisplayName("shockDemand"),
// pObj.getValue("shockDemand").toString());
// paramMap.put(pObj.getDisplayName("incomeMagnitude"),
// pObj.getValue("incomeMagnitude").toString());
// paramMap.put(pObj.getDisplayName("shockIncome"),
// pObj.getValue("shockIncome").toString());
// paramMap.put(pObj.getDisplayName("ng"), pObj.getValue("ng").toString());
//
// return paramMap;
// }
//
// public static void sendParameterMap(Parameters pObj) throws Exception {
// Map<String,String> paramsMap= createParamMap(pObj);
// webclientObj.postMethod("http://localhost:8080/among-model/sendParamFromEclipse",
// paramsMap);
//
// }
//
//// HashMap getAllParameters(String paramName);
//
