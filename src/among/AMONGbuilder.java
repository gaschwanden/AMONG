package among;

import au.edu.unimelb.eresearch.repast.parameterswrapper.ParametersWrapper;
import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

/**
 * Class to set up a scenario within Repast using runtime parameters. A list of
 * Households owning a number of properties is connected to a PropertyMarket.
 * Households can buy and sell Properties.
 * 
 * @author Gideon Aschwanden && Friedrich Burkhard von der Osten
 */
public class AMONGbuilder implements ContextBuilder<Object> {

	@Override
	public Context build(Context<Object> context) {
		CONST.household_ID = 0;
		CONST.property_ID = 0;
		context.setId("among");
		RunEnvironment.getInstance().endAt(CONST.tick_limit);

		Parameters params = ParametersWrapper.getInstance().getParameters();
		VAR.random_seed = params.getInteger("randomSeed");
		// VAR.initial_households = params.getInteger("households");

		VAR.CGTDiscount = params.getDouble("cgt");
		VAR.negativeGearing = params.getDouble("ng");

		Universe global = new Universe(VAR.random_seed);
		context.add(global);

		AlternativeMarket a = new AlternativeMarket(global);
		global.alternative_market = a;
		context.add(a);

		Bank b = new Bank(global);
		global.bank = b;
		context.add(b);

		Developer d = new Developer(global);
		global.developer = d;
		context.add(d);

		PropertyMarket pm = new PropertyMarket(global);
		global.property_market = pm;
		context.add(pm);

		for (int i = 0; i < VAR.initial_households; i++) {
			Household h = new Household(global);
			global.households.add(h);
			context.add(h);
			int time = (int) (Math.random() * 52);
			Property p = new Property(global, time);
			global.properties.add(p);
			context.add(p);

			h.addProperty(p);
		}
		global.initialize();

		return context;
	}

}
