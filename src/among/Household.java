package among;

import java.util.ArrayList;
import java.util.Collections;

import org.geotools.filter.MathExpressionImpl;
import org.omg.CORBA.Environment;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * Class to encapsulate a Household. 
 * Can own a number of Properties. 
 * Can decide to sell or buy Properties and make Bids on Auctions in the PropertyMarket.
 * @author Gideon Aschwanden && Friedrich Burkhard von der Osten
 */
public class Household implements Comparable<Household>{

	public enum State{
		RENTER,OWNER,INVESTOR;
	}

	private Universe global;
	private int ID;

	private double assets_initial;
	private double assets;
	private State state;

	private double in_income;
	private double in_rent; 

	private double out_living;
	private double out_rent;
	private double out_cost;

	private ArrayList<Property> properties;

	private double investment_horizon;
	private double risk;

	private boolean FHO;
	private boolean buy;
	private double buy_affordable_price;
	private double buy_expected_sale_price;
	public boolean disapointedHoushold = false;

	public Household(Universe u){
		// TODO where to apply inflation?
		global = u;
		ID = CONST.household_ID;
		CONST.household_ID++;

		state = State.RENTER;
		in_income = global.incomes_assets.Sample1(global.rnd);
		assets = global.incomes_assets.Sample2(global.rnd);
		assets_initial = assets;

		out_living = in_income * CONST.livingExpense;
		out_rent = in_income * CONST.rentExpense;

		properties = new ArrayList<Property>();
		investment_horizon = (double)global.rnd.nextInt((int)CONST.investmentHorizonRenter)+1;
		risk = global.rnd.nextDouble();
		buy = false;
		FHO = false;
	}

	@ScheduledMethod(start = 1, interval = 52, priority = 999)
	public void taxes(){
		// TODO does negative gearing include owner occupied properties? Does out_cost need to be balanced with in_rent?
		double income = getTotalIn();
		double ng_discount = VAR.negativeGearing * out_cost;
		double original_tax = global.projectedIT(income);
		income -= ng_discount;
		double tax = global.incomeTax(income);
		for(int i = 0; i < CONST.income_deciles.length; i++){
			if(income < CONST.income_deciles[i]){
				CONST.NGLost[i] += (original_tax - tax);
				break;
			}
		}
		assets -= tax;
		in_income += (in_income * (VAR.wageGrowth));
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 998)
	public void cashflow(){
		assets += in_income/CONST.year_ticks;
		assets += in_rent/CONST.year_ticks;

		assets -= out_living/CONST.year_ticks;
		assets -= out_rent/CONST.year_ticks;
		assets -= out_cost/CONST.year_ticks;

		for(Property p : properties){
			Mortgage m = p.getMortgage();
			if(m != null){
				if(m.getRemainingMonths() % CONST.month_ticks == 0){
					//System.out.println("Mortgage exists, pay instalment, remaining months "+m.getRemainingMonths());
					assets -= m.pay();
					if(m.isPaid()){
						//System.out.println("Mortgage has been paid off");
						p.setMortgage(null);
					}
				}
			}
		}

		if(assets < 0){
			assets -= (assets * VAR.irCredit) / CONST.year_ticks;
		}
		else{
			// TODO this is currently based on the individually perceived market prospect, might wanna centralize the alternative market prospects
			assets += (assets * global.alternative_market.getMarketProspect()) / CONST.year_ticks;
		}
	}


	@ScheduledMethod(start = 1, interval = 1, priority = 997)
	public void makeDecision(){
		//System.out.println("\nDecision making HH "+ID+" with investment horizon "+investment_horizon);
		double expectation_property = global.property_market.getMarketProspect((int)investment_horizon * (int)CONST.year_ticks);
		// This is plain wrong! Check every tick don't just look at the the point farthest away.
		double expectation_alternative = global.alternative_market.getMarketProspect();
		double disposable_income = getDisposableIncome();
		//System.out.println("Prospect property market "+expectation_property);
		//System.out.println("Prospect alternative market "+expectation_alternative);
		//System.out.println("Disposable income "+disposable_income);

		double projected_loan = global.bank.affordableLoan(assets, state, out_rent, disposable_income);
		double projected_value = projected_loan / (1 - CONST.downpayment);

		double projected_deposit = projected_value - projected_loan;
		// TODO projected sale prices over long investment horizon given high expectations are not realistic
		double projected_sale_price = projected_value * Math.pow(1.0 + expectation_property, investment_horizon);
		//System.out.println("Resulting deposit "+projected_deposit);
		//System.out.println("Projected sale price "+projected_sale_price);

		if(state == State.RENTER){
			if(assets > CONST.minDeposit && disposable_income > 0){
				// NPV of buying
				double one_off_costs_buy = projected_deposit
						+ global.property_market.transactionCostBuyer(projected_value)
						- CONST.FHOG;

				double projected_sum = 0;
				for(int i = 1; i < investment_horizon; i++){
					projected_sum += ((projected_loan * CONST.stdVariableRate)
							+ CONST.maintenance * projected_value
							+ CONST.councilRates * projected_value
							+ CONST.buildingInsurance * projected_value)
							/ Math.pow(1 + CONST.governmentBondYield, i);
				}
				double ongoing_costs_buy = projected_loan * CONST.stdVariableRate
						+ CONST.maintenance * projected_value
						+ CONST.councilRates * projected_value
						+ CONST.buildingInsurance * projected_value
						+ projected_sum;

				double benefits_buy = (projected_sale_price 
						* Math.pow((1 - CONST.valueDepreciation), investment_horizon) 
						- global.property_market.transactionCostSeller(projected_sale_price)
						- projected_loan)
						/ Math.pow(1 + CONST.governmentBondYield, investment_horizon);	
				//System.out.println("Depreciation in percent "+(1 - Math.pow((1 - CONST.valueDepreciation), investment_horizon) ));
				//System.out.println("Benefits of buying total "+benefits_buy);
				//System.out.println("One of costs to buy "+one_off_costs_buy);
				//System.out.println("Ongoing costs "+ongoing_costs_buy);
				double npv_buy = benefits_buy - one_off_costs_buy - ongoing_costs_buy;

				// NPV of renting
				double one_off_costs_rent = CONST.rentalBond * out_rent;

				double rent_sum = 0;
				for(int i = 1; i < investment_horizon; i++){
					rent_sum += out_rent / Math.pow(1 + CONST.governmentBondYield, i);
				}
				double moving_sum = 0;
				for(int i = (int)CONST.renterMovingPeriod; i < investment_horizon; i += (int)CONST.renterMovingPeriod){
					moving_sum += CONST.movingCost / Math.pow(1 + CONST.governmentBondYield, i);
				}
				double ongoing_costs_rent = out_rent + rent_sum + moving_sum;

				double investment_sum = 0;
				for(int i = 1; i < investment_horizon; i++){
					double sum = 0;
					sum += ((projected_loan * CONST.stdVariableRate)
							+ CONST.maintenance * projected_value
							+ CONST.councilRates * projected_value
							+ CONST.buildingInsurance * projected_value)
							- out_rent;
					if(i % (int)CONST.renterMovingPeriod == 0){
						sum -= CONST.movingCost;
					}
					investment_sum += sum / Math.pow(1 + CONST.governmentBondYield, i);
				}
				double projected_investment = one_off_costs_buy 
						- one_off_costs_rent 
						+ projected_loan * CONST.stdVariableRate
						+ CONST.maintenance * projected_value
						+ CONST.councilRates * projected_value
						+ CONST.buildingInsurance * projected_value
						- out_rent
						+ investment_sum;

				double dividend_sum = 0;
				for(int i =1; i < investment_horizon; i++){
					dividend_sum += ((1 - CONST.marginalTaxRate) * ((1 - CONST.marginalTaxRate) 
							* CONST.dividendYield
							* (VAR.CGTDiscount * risk * (projected_investment + (projected_investment * Math.pow(1 + expectation_alternative, investment_horizon))))))
							/ Math.pow(1 + CONST.governmentBondYield, investment_horizon);
				}
				double cgt = VAR.CGTDiscount 
						* risk 
						* (projected_investment * Math.pow(1 + expectation_alternative, investment_horizon) - projected_investment) 
						* CONST.marginalTaxRate;

				double benefits_rent = ((risk * Math.pow(1 + expectation_alternative, investment_horizon) 
						+ (1 - risk) * Math.pow(1 + (VAR.irSavings * (1 - CONST.marginalTaxRate)), investment_horizon))
						/ Math.pow(1 + CONST.governmentBondYield, investment_horizon)) * projected_investment 
						+ dividend_sum 
						- (cgt / Math.pow(1 + CONST.governmentBondYield, investment_horizon))
						- (one_off_costs_buy - one_off_costs_rent);

				double npv_rent = benefits_rent - one_off_costs_rent - ongoing_costs_rent;
				//System.out.println("NPV buy "+npv_buy);
				//System.out.println("NPV rent "+npv_rent);
				if(npv_buy > npv_rent){
					buy = true;
					buy_affordable_price = projected_value;
					buy_expected_sale_price = projected_sale_price;
				}
			}
		}
		else{
			for(Property p : properties){
				//				System.out.println("anticipatedAnnual return @ "+p.getTimeSinceTransaction()+" = "+global.property_market.getAnticipatedAnnualReturn(p.getTimeSinceTransaction()));
				double current = Math.pow(1+global.property_market.getAnticipatedAnnualReturn(p.getTimeSinceTransaction()), p.getTimeSinceTransaction()/CONST.year_ticks) * p.getValue();


				//				System.out.println("Property current value "+current+" given past value "+p.getValue()+" before "+p.getTimeSinceTransaction()+" ticks AAR "+global.property_market.getAnticipatedAnnualReturn(p.getTimeSinceTransaction()));
				double scale = global.rnd.nextDouble();
				double projected = p.getValueProjected();

				double margin = (projected - current) * scale;
				double timeOnMarketMultiplicator = Math.pow(1-(CONST.reducedReserveOvertime/100),p.timeOnMarket/CONST.year_ticks);
				double reserve_price = current + margin;// add history of how long it is on the market
				reserve_price = reserve_price*timeOnMarketMultiplicator;
//				if(p.timeOnMarket!=0)System.out.println("timeOnMarketDepriciation = "+timeOnMarketMultiplicator);
				if(p.getTimeSinceTransaction() > investment_horizon * CONST.year_ticks 
						|| current - 2*global.property_market.transactionCostBuyer(p.getValue()) == p.getValue()
						|| assets < 0){
					if(assets < 0){
						reserve_price -= reserve_price * CONST.reducedReserve;
					}
					global.property_market.registerPropertyForSale(this, p, reserve_price);
					p.timeOnMarket++;
					return;
				}
			}
			// TODO if only investors can sell due to negative outlook, system is stable -> boring?
			if(expectation_property < -CONST.stableMargin && state == State.INVESTOR){
				int property_index = global.rnd.nextInt(properties.size());
				Property propertyToSell = properties.get(property_index);

				double scale = global.rnd.nextDouble();
				double projected = propertyToSell.getValueProjected();
				double current = Math.pow(1 + global.property_market.getAnticipatedAnnualReturn(propertyToSell.getTimeSinceTransaction()), propertyToSell.getTimeSinceTransaction()/CONST.year_ticks) * propertyToSell.getValue();

				double margin = (projected - current) * scale;
				double reserve_price = current + margin;
				global.property_market.registerPropertyForSale(this, propertyToSell, reserve_price);
				return;
			}

			if(expectation_property > CONST.stableMargin && assets > CONST.minDeposit && disposable_income > 0){
				double holding_cost = (projected_loan * CONST.stdVariableRate 
						+ CONST.maintenance * projected_value
						+ CONST.councilRates * projected_value
						+ CONST.buildingInsurance * projected_value) *  investment_horizon;
				double rental_return = (CONST.rentReturn * projected_value) * investment_horizon;
				double cgt = global.projectedCGT(projected_sale_price - projected_value, (int)(investment_horizon * CONST.year_ticks));
				double valuation_buy = projected_sale_price + rental_return - projected_value - holding_cost - cgt;

				double projected_investment = projected_deposit + holding_cost;
				double investment_return = (CONST.dividendYield * projected_investment) * investment_horizon;
				double projected_stock_sale = projected_investment * Math.pow(1 + expectation_alternative, investment_horizon);
				cgt = global.projectedCGT(projected_stock_sale - projected_investment, (int)(investment_horizon * CONST.year_ticks));
				double valuation_invest = projected_stock_sale + investment_return - projected_investment - cgt;	

				//System.out.println("Valuation buy "+valuation_buy);
				//System.out.println("Valuation invest "+valuation_invest);
				if(valuation_buy > valuation_invest){
					buy = true;
					buy_affordable_price = projected_value;
					buy_expected_sale_price = projected_sale_price;
				}
			}
		}
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 996)
	public void executeDecision(){
		if(buy){
			ArrayList<Auction> auctions = global.property_market.getAuctions();
			for(Auction a : auctions){
//				if(a.property == null)continue;
				if(a.getReservePrice() <= buy_affordable_price){
					double actual_bid = calculateBid(a.property);
					if(actual_bid <= buy_affordable_price){
						a.registerInterestToBuy(this, actual_bid);
					}
					else{
						disapointedHoushold  = true;
					}
				}
			}
		}
		buy = false;
	}

	/**
	 * Bellow are the method on how households are calculating the price for a particular property
	 * @param p
	 * @return the actual bidding price
	 */
	
	public double calculateBid(Property p){
		
		double actual_bid = 0;
		
//		if(Math.random()<0.8){
			actual_bid = calculateBidByTime(p);
//		}else {
//			actual_bid = calculateBidByNeighbors(p);
//		}

		return actual_bid;
	}

	private double calculateBidByNeighbors(Property p) {
		
		double actual_bid = 0;
		double rate = 0;
		int iteratorCounter =0;
		for (int i = p.ID - 10;i<p.ID+10;i++){
			double localRate = -999;
			if(i==p.ID)continue;
			
			// Properties are arranged in a circular manner where the last is adjecant to the first in the list
			if (i<0) i = global.properties.size()-i;
			if (i>=global.properties.size()){
				localRate = global.properties.get(i-global.properties.size()).annualAppreciation;
			}else{
				localRate = global.properties.get(i).annualAppreciation;
			}
			
			if(localRate !=-999){
				rate += localRate;
				iteratorCounter++;
			}
		}
		if(iteratorCounter>1){
			rate =rate/iteratorCounter; 
		}else{
			rate = CONST.rentReturn;
		}
		actual_bid = p.getValuePrevious()*Math.pow(1+rate, p.time_since_transaction/CONST.year_ticks);
		return actual_bid;
	}

	private double calculateBidByTime(Property p) {
		boolean optimist = global.rnd.nextBoolean();
		if(Math.random()< CONST.optimism){
			optimist = true;
		}else{
			optimist = false;
		}

		int time = p.getTimeSinceTransaction();

		double actual_bid = p.getTransationValue();
		
		for(int i = 1; i<time; i++){
			int current = global.property_market.annualReturnsList.size()-time+i;
			if(optimist){
				actual_bid = actual_bid*(Math.pow(1+global.property_market.annualReturnsList.get(current)+CONST.bidVariation, 1/CONST.year_ticks));
			}else{
				actual_bid = actual_bid*(Math.pow(1+global.property_market.annualReturnsList.get(current)-CONST.bidVariation, 1/CONST.year_ticks));
			}
			
		}
		return actual_bid;
	}

	/**
	 * A specific Property will be added, usually in the context of buying it.
	 * @param p The Property to be added to the list of owned Properties.
	 */
	
	public void addProperty(Property p){
		p.setProjectedValue(buy_expected_sale_price);
		properties.add(p);
		Collections.sort(properties);
		out_cost += p.getCost();

		if(state == State.RENTER){
			out_rent = 0.0;
			state = State.OWNER;
			investment_horizon = (double)global.rnd.nextInt((int)CONST.investmentHorizon)+1;
			FHO = true;
		}
		else{
			in_rent = 0;
			for(int i = 1; i < properties.size(); i++){
				in_rent += properties.get(i).getRent();
			}
			state = State.INVESTOR;
		}
	}

	/**
	 * A specific Property will be removed, usually in the context of selling it.
	 * @param p The Property to be removed from the list of owned Properties.
	 */
	public void removeProperty(Property p){
		properties.remove(p);
		Collections.sort(properties);
		out_cost -= p.getCost();

		if(state == State.OWNER){
			out_rent = in_income * CONST.rentExpense;
			state = State.RENTER;
			investment_horizon = (double)global.rnd.nextInt((int)CONST.investmentHorizonRenter)+1;
		}
		else{
			in_rent = 0;
			for(int i = 1; i < properties.size(); i++){
				in_rent += properties.get(i).getRent();
			}
			if(properties.size() == 1){
				state = State.OWNER;
			}
		}
	}

	public void credit(double a){
		assets += a;
	}
	public void debit(double a){
		assets -= a;
	}

	public int getID(){
		return ID;
	}

	public State getState(){
		return state;
	}

	public double getIncome(){
		return in_income;
	}

	public double getRentIn(){
		return in_rent;
	}

	public double getLivingOut(){
		return out_living;
	}

	public double getRentOut(){
		return out_rent;
	}

	public double getCost(){
		return out_cost;
	}

	public double getAssets(){
		return assets;
	}

	public double getAssetsInitial(){
		return assets_initial;
	}

	/**
	 * Getter
	 * @return The list of Properties owned by this Household.
	 */
	public ArrayList<Property> getProperties(){
		return properties;
	}

	public double getTotalIn(){
		return in_income + in_rent;
	}

	public double getTotalOut(){
		return out_living + out_rent + out_cost;
	}

	public double getDisposableIncome(){
		double base = getTotalIn() - getTotalOut();
		for(Property p : properties){
			if(p.getMortgage() != null){
				base -= p.getMortgage().yearlyInstalments();
			}
		}
		return base;
	}

	public double getInvestmentHorizon(){
		return investment_horizon;
	}

	public boolean isFHO(){
		return FHO;
	}

	public int renter(){
		if(state == State.RENTER){return 1;}
		else{return 0;}
	}

	public double renterFraction(){
		if(state == State.RENTER){return (1.0/(double)CONST.household_ID);}
		else{return 0;}
	}

	public int owner(){
		if(state == State.OWNER){return 1;}
		else{return 0;}
	}

	public double ownerFraction(){
		if(state == State.OWNER){return (1.0/(double)CONST.household_ID);}
		else{return 0;}
	}

	public int investor(){
		if(state == State.INVESTOR){return 1;}
		else{return 0;}
	}

	public double investorFraction(){
		if(state == State.INVESTOR){return (1.0/(double)CONST.household_ID);}
		else{return 0;}
	}

	public String householdToFileFormat(){
		String tff = "";
		tff += ID+",";
		tff += Math.round(in_income);
		tff += ","+Math.round(assets);
		tff += ","+state;
		tff += ","+properties.size();
		tff += ","+FHO;
		tff += "\n";
		return tff;
	}

	public String propertyToFileFormat(){
		String tff = "";
		for(Property p : properties){
			tff += ID+",";
			tff += p.getID()+",";
			tff += Math.round(p.getValue());
			tff += "\n";
		}
		return tff;
	}

	@Override
	public int compareTo(Household h) {
		return Double.compare(this.getIncome(), h.getIncome()); // lowest to highest
	}
}
