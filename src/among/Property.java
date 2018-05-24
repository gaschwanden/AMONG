package among;

import java.util.Random;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * Class to encapsulate a Property with various state variables. 
 * Belongs to a Household and can be traded between Households via the PropertyMarket in Auctions.
 * @author Gideon Aschwanden && Friedrich Burkhard von der Osten
 */
public class Property implements Comparable<Property>{

	private Universe global;
	public int ID;
	public int transactions = 0;


	private double value_initial;
	private double value_previous;
	private double value_projected;
	private double value;
	private double value_transaction;
	private double value_previous_transaction;
	public Integer timeOnMarket = 0;

	private double rent;
	private double cost;
	
	public double annualAppreciation = -999;

	private Mortgage mortgage;

	public int time_since_transaction;
	private int time_since_transaction_previous = 0;

	public Property(Universe u){
		global = u;
		ID = CONST.property_ID;
		CONST.property_ID++;
		value = global.property_values.Sample1(global.rnd);

		value_initial = value;
		value_previous = value;
		value_projected = value;
		value_transaction = value;
		value_previous_transaction = value;
		
		cost = value * CONST.maintenance;
		rent = value * CONST.rentReturn;

		// TODO (optionally randomize initial ownership) time_since_transaction = global.rnd.nextInt((int)CONST.investmentHorizon)*(int)CONST.year_ticks;
		//		time_since_transaction = (int)(52*Math.random());
	}
	public Property(Universe u, int time_since_transaction){
		global = u;
		ID = CONST.property_ID;
		CONST.property_ID++;
		value = global.property_values.Sample1(global.rnd);
		value_initial = value;
		value_previous = value;
		value_projected = value;
		value_transaction = value;
		value_previous_transaction = value;

		cost = value * CONST.maintenance;
		rent = value * CONST.rentReturn;

		// TODO (optionally randomize initial ownership) time_since_transaction = global.rnd.nextInt((int)CONST.investmentHorizon)*(int)CONST.year_ticks;
		//		time_since_transaction = (int)(52*Math.random());
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 1000)
	public void increaseTimeSinceTransaction(){
		time_since_transaction++;
		value *= (1 - (CONST.propertyDevaluation/CONST.year_ticks));
		if(mortgage != null){
			mortgage.decreaseRemainingMonths();
		}
	}

	public void resetTimeSinceTransaction(){
		time_since_transaction_previous = time_since_transaction;
		time_since_transaction = 0;
		timeOnMarket = 0;
	}
	
	public void increaseTimeOnMarket(){
		timeOnMarket++;
	}

	public void setValue(double v){
		annualAppreciation = Math.pow(v/value, 1/(time_since_transaction/CONST.year_ticks))-1;
		value_previous = value;
		value = v;
		value_previous_transaction = value_transaction;
		value_transaction = v;
		cost = value * CONST.maintenance;
		rent = value * CONST.rentReturn;
	}

	public void setProjectedValue(double v){
		value_projected = v;
	}

	public void setMortgage(Mortgage m){
		//System.out.println("Mortgage set to "+m);
		mortgage = m;
	}

	public int getID(){
		return ID;
	}

	public double getValueInitial(){
		return value_initial;
	}

	public double getValuePrevious(){
		return value_previous;
	}

	public double getValueProjected(){
		return value_projected;
	}

	/**
	 * Getter
	 * @return The current value of the property
	 */
	public double getValue(){
		return value;
	}
	
	public double getTransationValue(){
		return value_transaction;
	}
	
	public double getpreviousTransationValue(){
		return value_previous_transaction;
	}

	public double getRent(){
		return rent;
	}

	public double getCost(){
		return cost;
	}

	public int getTimeSinceTransaction(){
		//		if(time_since_transaction>RunEnvironment.getInstance().getCurrentSchedule().getTickCount())time_since_transaction=(int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		return time_since_transaction;
	}

	public int getPreviousTimeSinceTransaction(){
		return time_since_transaction_previous;
	}

	public Mortgage getMortgage(){
		return mortgage;
	}

	@Override
	public int compareTo(Property p) {
		return Double.compare(p.getValue(), this.getValue()); // highest to lowest
	}
}
