package among;

import java.util.ArrayList;

import repast.simphony.engine.environment.RunEnvironment;

public class Developer {
	
	private Universe global;
	
	public Developer(Universe u){
		global = u;
	}
	
	public ArrayList<Property> buildProperties(){
		ArrayList<Property> new_properties = new ArrayList<Property>();
		int increaseProperty = (int) Math.ceil((VAR.propertyGrowth*CONST.property_ID)/(double)CONST.year_ticks);
		for(int i = 0; i < increaseProperty; i++){
			Property p = new Property(global);
			new_properties.add(p);
		}
		return new_properties;
	}
	
	public ArrayList<Property> delayedSupplyResponse(){
		// TODO replace constants
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		double anticipated_return = global.property_market.getAnticipatedAnnualReturn(10*(int)CONST.year_ticks, 12*(int)CONST.year_ticks);
		anticipated_return /= 2.0;
		
		int increaseProperty = (int) Math.ceil((anticipated_return*CONST.property_ID)/CONST.year_ticks);
		//System.out.println(tick+" Property increase would be "+increaseProperty+" with previous anticipated return "+anticipated_return);
		// TODO without bounds check this can go out of hand real fast
		/*if(increaseProperty > (0.1*CONST.property_ID)/CONST.year_ticks){
			increaseProperty = (int)(0.1*CONST.property_ID)/(int)CONST.year_ticks;
		}
		else if(increaseProperty < 0){
			increaseProperty = 0;
		}*/
		ArrayList<Property> new_properties = new ArrayList<Property>();
		
		for(int i = 0; i < increaseProperty; i++){
			Property p = new Property(global);
			new_properties.add(p);
		}
		//System.out.println("	Properties added: "+new_properties.size());
		return new_properties;
	}
}
