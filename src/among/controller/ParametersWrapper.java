package among.controller;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

public final class ParametersWrapper {
	private static final boolean USE_WEBCLIENT = true;

	private static ParametersWrapper instance;

	private ParametersWrapper() {
	}

	public static ParametersWrapper getInstance() {
		if (instance == null) {
			instance = new ParametersWrapper();
		}

		return instance;
	}

	public Parameters getParameters() {
		return USE_WEBCLIENT ? WebclientParameters.create() : RunEnvironment.getInstance().getParameters();
	}
}
