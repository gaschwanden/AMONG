package among;

import java.util.ArrayList;

public class Developer {

	private Universe global;

	public Developer(Universe u) {
		global = u;
	}

	public ArrayList<Property> buildProperties() {
		ArrayList<Property> new_properties = new ArrayList<Property>();
		int increaseProperty = (int) Math.ceil((VAR.propertyGrowth * CONST.property_ID) / CONST.year_ticks);
		for (int i = 0; i < increaseProperty; i++) {
			Property p = new Property(global);
			new_properties.add(p);
		}
		return new_properties;
	}

	public ArrayList<Property> delayedSupplyResponse() {
		double anticipated_return = global.property_market.getAverageMarketProspect(12 * (int) CONST.year_ticks);
		anticipated_return /= 2.0;

		int increaseProperty = (int) Math.ceil((anticipated_return * CONST.property_ID) / CONST.year_ticks);
		ArrayList<Property> new_properties = new ArrayList<Property>();

		for (int i = 0; i < increaseProperty; i++) {
			Property p = new Property(global);
			new_properties.add(p);
		}
		return new_properties;
	}
}
