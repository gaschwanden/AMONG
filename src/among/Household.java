package among;

import java.util.ArrayList;
import java.util.Collections;

import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * Class to encapsulate a Household. Can own a number of Properties. Can decide
 * to sell or buy Properties and make Bids on Auctions in the PropertyMarket.
 * 
 * @author Gideon Aschwanden && Friedrich Burkhard von der Osten
 */
public class Household implements Comparable<Household> {

	public enum State {
		RENTER, OWNER, INVESTOR;
	}

	private Universe global;
	private int ID;

	private double assets_initial;
	public double assets;
	private State state;

	public double in_income;
	private double tax_perCent;
	private double in_rent;

	private double out_living;
	private double ongoing_cost_rent;
	private double out_cost;
	public int incomeDecile;

	private ArrayList<Property> housholdProperties;

	public double investment_horizon;
	private double risk;

	private boolean FHO;
	private boolean buy;
	private double buy_affordable_price;
	//	private double buy_expected_sale_price;
	public boolean disapointedHoushold = false;

	public Household(Universe u) {
		global = u;
		ID = CONST.household_ID;
		CONST.household_ID++;

		state = State.RENTER;
		in_income = global.incomes_assets.Sample1(global.rnd);
		assets = global.incomes_assets.Sample2(global.rnd);
		assets_initial = assets;

		out_living = in_income * CONST.livingExpense;
		ongoing_cost_rent = in_income * CONST.rentExpense;

		housholdProperties = new ArrayList<Property>();
		investment_horizon = (double) global.rnd.nextInt((int) CONST.investmentHorizonRenter) + 1;
		risk = global.rnd.nextDouble();
		tax_perCent = u.getTaxPercent(in_income);
		buy = false;
		FHO = false;
		//		incomeDecile = calculateIncomeDecile();
	}

	public void calculateIncomeDecile() {
		incomeDecile = -1;
		for(int i=0;i<CONST.income_deciles.length;i++){
			if(in_income>CONST.income_deciles[i]){
				continue;
			}else{
				incomeDecile = i;
			}

		}

	}

	@ScheduledMethod(start = 1, interval = 52, priority = 999)
	public void taxes() {
		// TODO does negative gearing include owner occupied properties? Does out_cost
		// need to be balanced with in_rent?
		double income = getTotalIn();
		double ng_discount = VAR.negativeGearing * out_cost;
		double original_tax = global.projectedIT(income);
		income -= ng_discount;
		double tax = global.incomeTax(income);
		for (int i = 0; i < CONST.income_deciles.length; i++) {
			// TODO Change that lost taxes are only added to one decile, the decile the 
			if (income < CONST.income_deciles[i]) {
				CONST.NGLost[i] += (original_tax - tax);
				break;
			}
		}
		assets -= tax;
		in_income += (in_income * (VAR.wageGrowth));
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 998)
	public void cashflow() {

		assets += in_income / CONST.year_ticks;
		assets += in_rent / CONST.year_ticks;

		assets -= out_living / CONST.year_ticks;
		assets -= ongoing_cost_rent / CONST.year_ticks;
		assets -= out_cost / CONST.year_ticks;

		for (Property p : housholdProperties) {
			Mortgage m = p.getMortgage();
			if (m != null) {
				if (m.getRemainingMonths() % CONST.month_ticks == 0) {
					// System.out.println("Mortgage exists, pay instalment, remaining months
					// "+m.getRemainingMonths());
					assets -= m.pay();
					if (m.isPaid()) {
						// System.out.println("Mortgage has been paid off");
						p.setMortgage(null);
					}
				}
			}
		}

		if (assets < 0) {
			assets -= (assets * VAR.irCredit) / CONST.year_ticks;
		} else {
			assets += (assets * global.alternative_market.getMarketProspect()) / CONST.year_ticks;
		}
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 997)
	public void makeDecision() {
		double expectation_property = global.property_market.getAverageMarketProspect((int) investment_horizon * (int) CONST.year_ticks);
		double expectation_alternative = global.alternative_market.getMarketProspect();
		double disposable_income = getDisposableIncome();
		double projected_loan = global.bank.affordableLoan(assets, state, ongoing_cost_rent, disposable_income);
		double max_lineOfCredit = projected_loan / (1 - CONST.downpayment);

		double anticipated_deposit = max_lineOfCredit - projected_loan;
		double projected_price = max_lineOfCredit * Math.pow(1.0 + expectation_property, investment_horizon);


		if (state == State.RENTER) {
			
			/////////////////////
			// 		Renters	  ///
			/////////////////////
			
			if (assets > CONST.minDeposit && disposable_income > 0) {

				/////////////////////
				// NPV of buying
				/////////////////////

				double cost_selling = global.property_market.transactionCostSeller(projected_price)/ Math.pow(1 + CONST.governmentBondYield, investment_horizon);

				double one_off_costs_buy = anticipated_deposit
						+ global.property_market.transactionCostBuyer(max_lineOfCredit) - CONST.FHOG
						+ cost_selling;

				double npv_projected_cost_buy = 0;
				for (int i = 1; i < investment_horizon; i++) {
					npv_projected_cost_buy += ((projected_loan * CONST.stdVariableRate) 
							+ CONST.maintenance * max_lineOfCredit
							+ CONST.councilRates * max_lineOfCredit 
							+ CONST.buildingInsurance * max_lineOfCredit)
							/ Math.pow(1 + CONST.governmentBondYield, i);
				}
				double ongoing_costs_buy = 
						projected_loan * CONST.stdVariableRate 
						+ CONST.maintenance * max_lineOfCredit
						+ CONST.councilRates * max_lineOfCredit 
						+ CONST.buildingInsurance * max_lineOfCredit
						+ npv_projected_cost_buy;

				double benefits_buy = (projected_price - projected_loan)/ 
						Math.pow(1 + CONST.governmentBondYield, investment_horizon);

				double npv_buy = benefits_buy - one_off_costs_buy - ongoing_costs_buy;


				/////////////////////
				// NPV of renting
				/////////////////////

				double one_off_costs_rent = CONST.rentalBond * ongoing_cost_rent;


				double investment_sum_rent = assets;
				double total_rental_cost_rent = 0;
				for (int i = 1; i < investment_horizon; i++) {
					double sum = 0;
					double savings 		= 	in_income*VAR.irSavings;																//annual saving
					double dividends 	=	(investment_sum_rent+savings)*(expectation_alternative);								//annual dividends
					double dividend_tax = (investment_sum_rent+savings)*(expectation_alternative)*(1 - CONST.marginalTaxRate)*VAR.CGTDiscount;	//taxes on dividends
					//					double rent = ongoing_cost_rent;																	//rental costs

					sum = savings - dividend_tax + dividends;
					if (i % (int) CONST.renterMovingPeriod == 0) {												//checking if they move
						sum -= CONST.movingCost;
					}
					total_rental_cost_rent += ongoing_cost_rent/ Math.pow(1 + CONST.governmentBondYield, i);
					investment_sum_rent += sum / Math.pow(1 + CONST.governmentBondYield, i);					//inflation
				}

				double projected_investment = one_off_costs_rent + investment_sum_rent;

				double npv_rent = projected_investment- total_rental_cost_rent;

				if (npv_buy > npv_rent) {
					buy = true;
					buy_affordable_price = max_lineOfCredit;
					//					buy_expected_sale_price = affordable_price;
				}
			}
		} else {
			
			///////////////////////////////
			// 		Owners/Investors	///
			///////////////////////////////
			
			for (Property p : housholdProperties) {

				if(p.OnMarket){
					// Reduce Price if property has not been sold before
					p.setReservePrice(p.getReservePrice()*Math.pow(1 - CONST.reducedReserveOvertime,1 / CONST.year_ticks));

				}else{
					// calculate price for for potential sale
					p.setReservePrice(p.getReservePrice()*Math.pow((1+p.getAAR()),1 / CONST.year_ticks));
				}
				if (p.getTimeSinceTransaction() > investment_horizon * CONST.year_ticks - (int)(Math.random()*51) 				//selling after investment horizon
						|| p.value_projected + 2 * global.property_market.transactionCostBuyer(p.value_projected) <= p.getMarketValue() //selling if the property has reached the anticipated return
						|| assets < 0																							//selling if under financial stress
						) {
		
					if (assets < 0) { // selling at discount since under pressure
						p.setReservePrice(p.getReservePrice() *(1- CONST.reducedReserve));
					}

					global.property_market.registerPropertyForSale(this, p);
					p.OnMarket = true;
					p.timeOnMarket++;
					return;
				}
			}

			if (expectation_property < -CONST.stableMargin && state == State.INVESTOR) { // putting on market if market is hot
				int property_index = global.rnd.nextInt(housholdProperties.size());
				Property propertyToSell = housholdProperties.get(property_index);

				double scale = global.rnd.nextDouble();
				double projected = propertyToSell.getValueProjected();
				double current = Math.pow(
						1 + global.property_market.getAverageMarketProspect(propertyToSell.getTimeSinceTransaction()),
						propertyToSell.getTimeSinceTransaction() / CONST.year_ticks) * 
						propertyToSell.getValuePrevious();

				double margin = (projected - current) * scale;
				double reserve_price = current + margin;
				global.property_market.registerPropertyForSale(this, propertyToSell, reserve_price);
				return;
			}

			if (expectation_property > CONST.stableMargin && assets > CONST.minDeposit && disposable_income > 0) {
				double holding_cost = (projected_loan * CONST.stdVariableRate + CONST.maintenance * max_lineOfCredit
						+ CONST.councilRates * max_lineOfCredit + CONST.buildingInsurance * max_lineOfCredit)
						* investment_horizon;
				double rental_return = (CONST.rentReturn * max_lineOfCredit) * investment_horizon;
				double cgt = global.projectedCGT(projected_price - max_lineOfCredit,
						(int) (investment_horizon * CONST.year_ticks));
				double valuation_buy = projected_price + rental_return - max_lineOfCredit - holding_cost - cgt;

				double projected_investment = anticipated_deposit + holding_cost;
				double investment_return = (CONST.dividendYield * projected_investment) * investment_horizon;
				double projected_stock_sale = projected_investment
						* Math.pow(1 + expectation_alternative, investment_horizon);
				cgt = global.projectedCGT(projected_stock_sale - projected_investment,
						(int) (investment_horizon * CONST.year_ticks));
				double valuation_invest = projected_stock_sale + investment_return - projected_investment - cgt;


				if (valuation_buy > valuation_invest) {
					buy = true;
					buy_affordable_price = max_lineOfCredit;
					//					buy_expected_sale_price = affordable_price;
				}
			}
		}
	}


	//	private double calculateReservePriceLocation(Property p) {
	//		double localAAR = 0;
	//		for(int i = p.ID-(int)investment_horizon;i<p.ID+investment_horizon;i++){
	//			
	//			// Control for property array range
	//			if(i<0){i=global.properties.size()-i;}
	//			if(i>=global.properties.size()){i=i-global.properties.size();}
	//
	//			Property property = global.properties.get(i);
	//			if(property.getAAR()>0){
	//				System.out.println("Try");
	//			};
	//			localAAR += property.getAAR();
	//			
	//		}
	//		localAAR = localAAR/(2*(int)investment_horizon);
	//		
	//		double reserve = p.value_previous* Math.pow(1+localAAR, p.time_since_transaction);
	//		
	//		if(global.rnd.nextBoolean()){
	//			reserve *= (1-CONST.bidVariation);
	//		}else{
	//			reserve *= (1+CONST.bidVariation);
	//		}
	//		
	//		return reserve;
	//	}

	@ScheduledMethod(start = 1, interval = 1, priority = 996)
	public void executeDecision() {
		if (buy) {
			ArrayList<Auction> auctions = global.property_market.getAuctions();
			for (Auction a : auctions) {
				if (a.getReservePrice() <= buy_affordable_price && Math.random()<0.1) { // can the household afford it && limiting ability to bid in all auctions
					double actual_bid = calculateBid(a.property);
					if (actual_bid <= buy_affordable_price ) {
						a.registerInterestToBuy(this, actual_bid);
					} else {
						disapointedHoushold = true;
					}
				}
			}
		}
		buy = false;
	}

	/**
	 * Bellow are the method on how households are calculating the price for a
	 * particular property
	 * @param p
	 * @return the actual bidding price
	 */

	public double calculateBid(Property p) {

		double actual_bid = 0;

		double probablity = Math.random();
		if(probablity<0.1){
			actual_bid = calculateBidByTime(p)+calculateNPVtaxDeduction(p);
		}else if(probablity<0.3) {
			actual_bid = p.value_previous_transaction*Math.pow(1+VAR.propertyGrowth, (p.time_since_transaction/CONST.year_ticks));
		}else if(probablity<0.6) {
			actual_bid = calculateBidByMarketValue(p)+calculateNPVtaxDeduction(p); 		 
		}else{
			actual_bid = calculateBidByRentalReturn(p)+calculateNPVtaxDeduction(p);
		}
		return actual_bid;
	}

	private double calculateBidByRentalReturn(Property p) {
		return 100*(p.getRent()-p.getCost())/(VAR.irMortgage+CONST.maintenance); //calculating by ROI
	}

	private double calculateBidByMarketValue(Property p) {
		double bidVariance = 1;

		if (Math.random() < CONST.optimism) {
			bidVariance += CONST.bidVariation;
		} else {
			bidVariance -= CONST.bidVariation;
		}

		double bid = p.getMarketValue()*bidVariance;
		return bid;
	}

	private double calculateNPVtaxDeduction(Property p) {
		double totalCost = p.getCost()+p.value_previous*0.8*VAR.irMortgage;
		double totalDeductions = totalCost*tax_perCent;
		double NPVtotalDeductions = totalDeductions;//totalDeductions/VAR.inflation; xxx this should be calculated like this but makes huge numbers
		return NPVtotalDeductions;
	}

	//	private double calculateBidByNeighbors(Property p) {
	//
	//		double actual_bid = 0;
	//		double rate = 0;
	//		int iteratorCounter = 0;
	//		for (int i = p.ID - 10; i < p.ID + 10; i++) {
	//			double localRate = -999;
	//			if (i == p.ID)
	//				continue;
	//
	//			// Properties are arranged in a circular manner where the last is adjecant to
	//			// the first in the list
	//			if (i < 0)
	//				i = global.properties.size() - i;
	//			if (i >= global.properties.size()) {
	//				localRate = global.properties.get(i - global.properties.size()).getAAR();
	//			} else {
	//				localRate = global.properties.get(i).getAAR();
	//			}
	//
	//			if (localRate != -999) {
	//				rate += localRate;
	//				iteratorCounter++;
	//			}
	//		}
	//		if (iteratorCounter > 1) {
	//			rate = rate / iteratorCounter;
	//		} else {
	//			rate = CONST.rentReturn;
	//		}
	//		actual_bid = p.getValuePrevious() * Math.pow(1 + rate, p.time_since_transaction / CONST.year_ticks);
	//		return actual_bid;
	//	}

	private double calculateBidByTime(Property p) {
		boolean optimist = global.rnd.nextBoolean();
		if (Math.random() < CONST.optimism) {
			optimist = true;
		} else {
			optimist = false;
		}

		int time = p.getTimeSinceTransaction();

		double actual_bid = p.getTransationValue();

		for (int i = 1; i < time; i++) {
			int current = (int)global.tick - time + i+1;
			double AARaverage = global.property_market.getAverageMarketProspectAt(current);
			if (optimist) {
				actual_bid = actual_bid
						* (Math.pow(1 + AARaverage + CONST.bidVariation,
								1 / CONST.year_ticks));
			} else {
				actual_bid = actual_bid
						* (Math.pow(1 + AARaverage - CONST.bidVariation,
								1 / CONST.year_ticks));
			}
		}
		return actual_bid;
	}

	/**
	 * A specific Property will be added, usually in the context of buying it.
	 * @param p
	 *            The Property to be added to the list of owned Properties.
	 */

	public void addProperty(Property p) {
		p.setProjectedValue(calculateProjectedValue(p));
		housholdProperties.add(p);
		Collections.sort(housholdProperties);
		out_cost += p.getCost();

		if (state == State.RENTER) {
			ongoing_cost_rent = 0.0;
			state = State.OWNER;
			investment_horizon = (double) global.rnd.nextInt((int) CONST.investmentHorizon) + 1;
			FHO = true;
		} else {
			in_rent = 0;
			for (int i = 1; i < housholdProperties.size(); i++) {
				in_rent += housholdProperties.get(i).getRent();
			}
			state = State.INVESTOR;
		}
	}

	private double calculateProjectedValue(Property p) {

		double projectedValue = 0;
		double averageAAR = global.property_market.getAARaverage((int)(global.tick-investment_horizon*CONST.year_ticks), (int)(global.tick))/100;
		projectedValue = p.value_previous_transaction*Math.pow((1+averageAAR),investment_horizon);

		if(averageAAR<0){
			System.out.println("hello World 2");
		}
		if(projectedValue>2*p.getValue() ){
			System.out.println("hello World 1");
		}
		return projectedValue;
	}

	/**
	 * A specific Property will be removed, usually in the context of selling it.
	 * @param p
	 *            The Property to be removed from the list of owned Properties.
	 */
	public void removeProperty(Property p) {
		housholdProperties.remove(p);
		Collections.sort(housholdProperties);
		out_cost -= p.getCost();

		if (state == State.OWNER) {
			ongoing_cost_rent = in_income * CONST.rentExpense;
			state = State.RENTER;
			investment_horizon = (double) global.rnd.nextInt((int) CONST.investmentHorizonRenter) + 1;
		} else {
			in_rent = 0;
			for (int i = 1; i < housholdProperties.size(); i++) {
				in_rent += housholdProperties.get(i).getRent();
			}
			if (housholdProperties.size() == 1) {
				state = State.OWNER;
			}
		}
	}

	public void credit(double a) {
		assets += a;
	}

	public void debit(double a) {
		assets -= a;
	}

	public int getID() {
		return ID;
	}

	public State getState() {
		return state;
	}

	public double getIncome() {
		return in_income;
	}

	public double getRentIn() {
		return in_rent;
	}

	public double getLivingOut() {
		return out_living;
	}

	public double getRentOut() {
		return ongoing_cost_rent;
	}

	public double getCost() {
		return out_cost;
	}

	public double getAssets() {
		return assets;
	}

	public double getAssetsInitial() {
		return assets_initial;
	}

	/**
	 * Getter
	 * @return The list of Properties owned by this Household.
	 */
	public ArrayList<Property> getProperties() {
		return housholdProperties;
	}

	public double getTotalIn() {
		return in_income + in_rent;
	}

	public double getTotalOut() {
		return out_living + ongoing_cost_rent + out_cost;
	}

	public double getDisposableIncome() {
		double base = getTotalIn() - getTotalOut();
		for (Property p : housholdProperties) {
			if (p.getMortgage() != null) {
				base -= p.getMortgage().yearlyInstalments();
			}
		}
		return base;
	}

	public double getInvestmentHorizon() {
		return investment_horizon;
	}

	public boolean isFHO() {
		return FHO;
	}

	public int renter() {
		if (state == State.RENTER) {
			return 1;
		} else {
			return 0;
		}
	}

	public double renterFraction() {
		if (state == State.RENTER) {
			return (1.0 / CONST.household_ID);
		} else {
			return 0;
		}
	}

	public int owner() {
		if (state == State.OWNER) {
			return 1;
		} else {
			return 0;
		}
	}

	public double ownerFraction() {
		if (state == State.OWNER) {
			return (1.0 / CONST.household_ID);
		} else {
			return 0;
		}
	}

	public int investor() {
		if (state == State.INVESTOR) {
			return 1;
		} else {
			return 0;
		}
	}

	public double investorFraction() {
		if (state == State.INVESTOR) {
			return (1.0 / CONST.household_ID);
		} else {
			return 0;
		}
	}

	public String householdToFileFormat() {
		String tff = "";
		tff += ID + ",";
		tff += Math.round(in_income);
		tff += "," + Math.round(assets);
		tff += "," + state;
		tff += "," + housholdProperties.size();
		tff += "," + FHO;
		tff += "\n";
		return tff;
	}

	public String propertyToFileFormat() {
		String tff = "";
		for (Property p : housholdProperties) {
			tff += ID + ",";
			tff += p.getID() + ",";
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
