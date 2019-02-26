package among;

import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * Class to encapsulate a Property with various state variables. Belongs to a
 * Household and can be traded between Households via the PropertyMarket in
 * Auctions.
 * 
 * @author Gideon Aschwanden && Friedrich Burkhard von der Osten
 */
public class Property implements Comparable<Property> {

	private Universe global;
	public int ID;
	public int transactions = 0;

	public double value_initial;
	public double value_previous;
	public double value_projected;
	private double propertyValueTransaction;
	public double value_transaction;
	public double value_market;
	public double value_previous_transaction;
	public Integer timeOnMarket = 0;
	public Integer quality = (int)(10*Math.random());

	private double rent;
	private double cost;

	private Mortgage mortgage;

	public int time_since_transaction;
	private int time_since_transaction_previous = 0;
	private double AAR;
	public boolean OnMarket;
	private double reservePrice;

	public Property(Universe u) {
		global = u;
		ID = CONST.property_ID;
		CONST.property_ID++;
		propertyValueTransaction = global.property_values.Sample1(global.rnd);

		value_initial = propertyValueTransaction;
		value_previous = propertyValueTransaction;
		value_projected = propertyValueTransaction;
		value_transaction = propertyValueTransaction;
		value_market = propertyValueTransaction;
		value_previous_transaction = propertyValueTransaction;
		reservePrice = propertyValueTransaction*1.05;
		AAR = 0;

		cost = propertyValueTransaction * CONST.maintenance;
		rent = propertyValueTransaction * CONST.rentReturn;

	}

	public Property(Universe u, int time_since_transaction) {
		global = u;
		ID = CONST.property_ID;
		CONST.property_ID++;
		propertyValueTransaction = global.property_values.Sample1(global.rnd);
		value_initial = propertyValueTransaction;
		value_previous = propertyValueTransaction;
		value_projected = propertyValueTransaction;
		value_transaction = propertyValueTransaction;
		value_market = propertyValueTransaction;
		value_previous_transaction = propertyValueTransaction;
		reservePrice = propertyValueTransaction*(1 + VAR.propertyGrowth+VAR.inflation);

		cost = propertyValueTransaction * CONST.maintenance;
		rent = propertyValueTransaction * CONST.rentReturn;

	}

	@ScheduledMethod(start = 1, interval = 1, priority = 1000)
	public void increaseTimeSinceTransaction() {
		time_since_transaction++;
		propertyValueTransaction *= (1 - (CONST.propertyDevaluation / CONST.year_ticks)/2);//depreciation affects only the building why /2
		if (mortgage != null) {
			mortgage.decreaseRemainingMonths();
		}
	}

	public void resetTimeSinceTransaction() {
		time_since_transaction_previous = time_since_transaction;
		time_since_transaction = 0;
		timeOnMarket = 0;
	}

	public void increaseTimeOnMarket() {
		timeOnMarket++;
	}

	public void setPropertyValueTransaction(double v) {
		AAR = Math.pow(v / propertyValueTransaction, 1 / (time_since_transaction / CONST.year_ticks)) - 1;
		value_previous = propertyValueTransaction;
		propertyValueTransaction = v;
		setReservePrice(v*(1+VAR.inflation+VAR.propertyGrowth));
		value_market = v;
		value_previous_transaction = value_transaction;
		value_transaction = v;
		cost = value_transaction * CONST.maintenance;
		rent = value_transaction * CONST.rentReturn;
	}

	public void setProjectedValue(double v) {
		value_projected = v;
	}

	public void setMortgage(Mortgage m) {
		// System.out.println("Mortgage set to "+m);
		mortgage = m;
	}

	public int getID() {
		return ID;
	}

	public double getValueInitial() {
		return value_initial;
	}

	public double getValuePrevious() {
		return value_previous;
	}

	public double getValueProjected() {
		return value_projected;
	}

	/**
	 * Getter
	 * 
	 * @return The current value of the property
	 */
	public double getValue() {
		return propertyValueTransaction;
	}

	public double getTransationValue() {
		return value_transaction;
	}

	public double getpreviousTransationValue() {
		return value_previous_transaction;
	}

	public double getRent() {
		return rent;
	}
	public void setRent(double rentSetter){
		rent = rentSetter;
	}

	public double getCost() {
		return cost;
	}

	public int getTimeSinceTransaction() {
		// if(time_since_transaction>RunEnvironment.getInstance().getCurrentSchedule().getTickCount())time_since_transaction=(int)
		// RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		return time_since_transaction;
	}

	public int getPreviousTimeSinceTransaction() {
		return time_since_transaction_previous;
	}

	public Mortgage getMortgage() {
		return mortgage;
	}

	public double getMarketValue() {
		return value_market;
	}

	@Override
	public int compareTo(Property p) {
		return Double.compare(p.getValue(), this.getValue()); // highest to lowest
	}

	public void updateMarketValueProperty(double aar) {
		value_market = value_market * (1 + aar / CONST.year_ticks);
	}

	public double getAAR() {
		return AAR;
	}


	public void updateReservePriceByLocation(Household h) {
		double localAAR = 0;
		for(int i = ID-(int)h.investment_horizon;i<ID+h.investment_horizon;i++){
			// Control for property array range
			int k = i;
			
			if(k<0){
				k=global.properties.size()+i;}
			if(k == ID){
				continue;}
			if(k>=global.properties.size()){k=i-global.properties.size();}

			Property property = global.properties.get(k);
			if(property.getAAR()>0){
				localAAR += property.getAAR();
			}else{
				localAAR += VAR.propertyGrowth;	
			}
		}
		localAAR = localAAR/(2*(int)h.investment_horizon);
		setReservePrice(value_transaction* Math.pow(1+localAAR, time_since_transaction/CONST.year_ticks));
				//value_previous* Math.pow(1+localAAR, time_since_transaction);
		if(global.rnd.nextBoolean()){
			setReservePrice(getReservePrice() * (1-CONST.bidVariation));
		}else{
			setReservePrice(getReservePrice() * (1+CONST.bidVariation));
		}	
	}

	public double getReservePrice() {
		return reservePrice;
	}

	public void setReservePrice(double reservePrice) {
		this.reservePrice = reservePrice;
	}
}
