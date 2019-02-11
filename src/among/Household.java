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
	private double assets;
	private State state;

	private double in_income;
	private double tax_perCent;
	private double in_rent;

	private double out_living;
	private double out_rent;
	private double out_cost;

	private ArrayList<Property> housholdProperties;

	public double investment_horizon;
	private double risk;

	private boolean FHO;
	private boolean buy;
	private double buy_affordable_price;
	private double buy_expected_sale_price;
	public boolean disapointedHoushold = false;

	public Household(Universe u) {
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

		housholdProperties = new ArrayList<Property>();
		investment_horizon = (double) global.rnd.nextInt((int) CONST.investmentHorizonRenter) + 1;
		risk = global.rnd.nextDouble();
		tax_perCent = u.getTaxPercent(in_income);
		buy = false;
		FHO = false;
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
		assets -= out_rent / CONST.year_ticks;
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
			// TODO this is currently based on the individually perceived market prospect,
			// might wanna centralize the alternative market prospects
			assets += (assets * global.alternative_market.getMarketProspect()) / CONST.year_ticks;
		}
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 997)
	public void makeDecision() {
		
//		System.out.println("\nDecision making HH "+ID);
//		" with investment horizon
		// "+investment_horizon);
		// double expectation_property = global.property_market.getMarketProspect((int) investment_horizon * (int) CONST.year_ticks); //old function
		double expectation_property = global.property_market.getAverageMarketProspect((int) investment_horizon * (int) CONST.year_ticks);
		double expectation_alternative = global.alternative_market.getMarketProspect();
		double disposable_income = getDisposableIncome();
		// System.out.println("Prospect property market "+expectation_property);
		// System.out.println("Prospect alternative market "+expectation_alternative);
		// System.out.println("Disposable income "+disposable_income);

		double projected_loan = global.bank.affordableLoan(assets, state, out_rent, disposable_income);
		double projected_value = projected_loan / (1 - CONST.downpayment);

		double projected_deposit = projected_value - projected_loan;
		// TODO projected sale prices over long investment horizon given high
		// expectations are not realistic
		double affordable_price = projected_value * Math.pow(1.0 + expectation_property, investment_horizon);
		// System.out.println("Resulting deposit "+projected_deposit);
		// System.out.println("Projected sale price "+projected_sale_price);

		if (state == State.RENTER) {
			if (assets > CONST.minDeposit && disposable_income > 0) {
				// NPV of buying
				double one_off_costs_buy = projected_deposit
						+ global.property_market.transactionCostBuyer(projected_value) - CONST.FHOG;

				double projected_sum = 0;
				for (int i = 1; i < investment_horizon; i++) {
					projected_sum += ((projected_loan * CONST.stdVariableRate) + CONST.maintenance * projected_value
							+ CONST.councilRates * projected_value + CONST.buildingInsurance * projected_value)
							/ Math.pow(1 + CONST.governmentBondYield, i);
				}
				double ongoing_costs_buy = projected_loan * CONST.stdVariableRate + CONST.maintenance * projected_value
						+ CONST.councilRates * projected_value + CONST.buildingInsurance * projected_value
						+ projected_sum;

				double benefits_buy = (affordable_price
						* Math.pow((1 - CONST.valueDepreciation), investment_horizon)
						- global.property_market.transactionCostSeller(affordable_price) - projected_loan)
						/ Math.pow(1 + CONST.governmentBondYield, investment_horizon);
				// System.out.println("Depreciation in percent "+(1 - Math.pow((1 -
				// CONST.valueDepreciation), investment_horizon) ));
				// System.out.println("Benefits of buying total "+benefits_buy);
				// System.out.println("One of costs to buy "+one_off_costs_buy);
				// System.out.println("Ongoing costs "+ongoing_costs_buy);
				double npv_buy = benefits_buy - one_off_costs_buy - ongoing_costs_buy;

				// NPV of renting
				double one_off_costs_rent = CONST.rentalBond * out_rent;

				double rent_sum = 0;
				for (int i = 1; i < investment_horizon; i++) {
					rent_sum += out_rent / Math.pow(1 + CONST.governmentBondYield, i);
				}
				double moving_sum = 0;
				for (int i = (int) CONST.renterMovingPeriod; i < investment_horizon; i += (int) CONST.renterMovingPeriod) {
					moving_sum += CONST.movingCost / Math.pow(1 + CONST.governmentBondYield, i);
				}
				double ongoing_costs_rent = out_rent + rent_sum + moving_sum;

				double investment_sum = 0;
				for (int i = 1; i < investment_horizon; i++) {
					double sum = 0;
					sum += ((projected_loan * CONST.stdVariableRate) + CONST.maintenance * projected_value
							+ CONST.councilRates * projected_value + CONST.buildingInsurance * projected_value)
							- out_rent;
					if (i % (int) CONST.renterMovingPeriod == 0) {
						sum -= CONST.movingCost;
					}
					investment_sum += sum / Math.pow(1 + CONST.governmentBondYield, i);
				}
				double projected_investment = one_off_costs_buy - one_off_costs_rent
						+ projected_loan * CONST.stdVariableRate + CONST.maintenance * projected_value
						+ CONST.councilRates * projected_value + CONST.buildingInsurance * projected_value - out_rent
						+ investment_sum;

				double dividend_sum = 0;
				for (int i = 1; i < investment_horizon; i++) {
					dividend_sum += ((1 - CONST.marginalTaxRate) * ((1 - CONST.marginalTaxRate) * CONST.dividendYield
							* (VAR.CGTDiscount * risk
									* (projected_investment + (projected_investment
											* Math.pow(1 + expectation_alternative, investment_horizon))))))
							/ Math.pow(1 + CONST.governmentBondYield, investment_horizon);
				}
				double cgt = VAR.CGTDiscount * risk
						* (projected_investment * Math.pow(1 + expectation_alternative, investment_horizon)
								- projected_investment)
						* CONST.marginalTaxRate;

				double benefits_rent = ((risk * Math.pow(1 + expectation_alternative, investment_horizon)
						+ (1 - risk) * Math.pow(1 + (VAR.irSavings * (1 - CONST.marginalTaxRate)), investment_horizon))
						/ Math.pow(1 + CONST.governmentBondYield, investment_horizon)) * projected_investment
						+ dividend_sum - (cgt / Math.pow(1 + CONST.governmentBondYield, investment_horizon))
						- (one_off_costs_buy - one_off_costs_rent);

				double npv_rent = benefits_rent - one_off_costs_rent - ongoing_costs_rent;
				// System.out.println("NPV buy "+npv_buy);
				// System.out.println("NPV rent "+npv_rent);
				if (npv_buy > npv_rent) {
					buy = true;
					buy_affordable_price = projected_value;
					buy_expected_sale_price = affordable_price;
				}
			}
		} else {
			for (Property p : housholdProperties) {

				if(p.OnMarket){
					p.reservePrice = p.reservePrice*Math.pow(1 - (CONST.reducedReserveOvertime / 100),p.timeOnMarket / CONST.year_ticks);
				}else{
//					System.out.println(housholdProperties.size()+" # "+ID+" ; "+investment_horizon);
					p.updateReservePriceByLocation(this);
				}
				
				
				if (p.getTimeSinceTransaction() > investment_horizon * CONST.year_ticks - (int)(Math.random()*51)
						|| p.reservePrice - 2 * global.property_market.transactionCostBuyer(p.getValue()) == p.getValue() 
						|| assets < 0
						) {
					if (assets < 0) {
						p.reservePrice -= p.reservePrice * CONST.reducedReserve;
					}
					global.property_market.registerPropertyForSale(this, p);
					p.OnMarket = true;
					p.timeOnMarket++;
					return;
				}
			}

			if (expectation_property < -CONST.stableMargin && state == State.INVESTOR) {
				int property_index = global.rnd.nextInt(housholdProperties.size());
				Property propertyToSell = housholdProperties.get(property_index);

				double scale = global.rnd.nextDouble();
				double projected = propertyToSell.getValueProjected();
				double current = Math.pow(
						1 + global.property_market.getAverageMarketProspect(propertyToSell.getTimeSinceTransaction()),
						propertyToSell.getTimeSinceTransaction() / CONST.year_ticks) * propertyToSell.getValue();

				double margin = (projected - current) * scale;
				double reserve_price = current + margin;
				global.property_market.registerPropertyForSale(this, propertyToSell, reserve_price);
				return;
			}

			if (expectation_property > CONST.stableMargin && assets > CONST.minDeposit && disposable_income > 0) {
				double holding_cost = (projected_loan * CONST.stdVariableRate + CONST.maintenance * projected_value
						+ CONST.councilRates * projected_value + CONST.buildingInsurance * projected_value)
						* investment_horizon;
				double rental_return = (CONST.rentReturn * projected_value) * investment_horizon;
				double cgt = global.projectedCGT(affordable_price - projected_value,
						(int) (investment_horizon * CONST.year_ticks));
				double valuation_buy = affordable_price + rental_return - projected_value - holding_cost - cgt;

				double projected_investment = projected_deposit + holding_cost;
				double investment_return = (CONST.dividendYield * projected_investment) * investment_horizon;
				double projected_stock_sale = projected_investment
						* Math.pow(1 + expectation_alternative, investment_horizon);
				cgt = global.projectedCGT(projected_stock_sale - projected_investment,
						(int) (investment_horizon * CONST.year_ticks));
				double valuation_invest = projected_stock_sale + investment_return - projected_investment - cgt;

				// System.out.println("Valuation buy "+valuation_buy);
				// System.out.println("Valuation invest "+valuation_invest);
				if (valuation_buy > valuation_invest) {
					buy = true;
					buy_affordable_price = projected_value;
					buy_expected_sale_price = affordable_price;
				}
			}
		}
	}


	private double calculateReservePriceLocation(Property p) {
		double localAAR = 0;
		for(int i = p.ID-(int)investment_horizon;i<p.ID+investment_horizon;i++){
			
			// Control for property array range
			if(i<0){i=global.properties.size()-i;}
			if(i>=global.properties.size()){i=i-global.properties.size();}

			Property property = global.properties.get(i);
			if(property.getAAR()>0){
				System.out.println("Try");
			};
			localAAR += property.getAAR();
			
		}
		localAAR = localAAR/(2*(int)investment_horizon);
		
		double reserve = p.value_previous* Math.pow(1+localAAR, p.time_since_transaction);
		
		if(global.rnd.nextBoolean()){
			reserve *= (1-CONST.bidVariation);
		}else{
			reserve *= (1+CONST.bidVariation);
		}
		
		return reserve;
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 996)
	public void executeDecision() {
		if (buy) {
			ArrayList<Auction> auctions = global.property_market.getAuctions();
			for (Auction a : auctions) {
				// if(a.property == null)continue;
				if (a.getReservePrice() <= buy_affordable_price && Math.random()<0.2) {
					double actual_bid = calculateBid(a.property);
					if (actual_bid <= buy_affordable_price) {
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
	 * 
	 * @param p
	 * @return the actual bidding price
	 */

	public double calculateBid(Property p) {

		double actual_bid = 0;

		// if(Math.random()<0.8){
		actual_bid = calculateBidByTime(p)+calculateNPVtaxDeduction(p);
		
		// }else {
		// actual_bid = calculateBidByNeighbors(p);
		// }

		return actual_bid;
	}

	private double calculateNPVtaxDeduction(Property p) {
		// TODO Auto-generated method stub
		double totalCost = p.getCost()+p.value_previous*0.8*VAR.irMortgage;
		double totalDeductions = totalCost*tax_perCent;
		double NPVtotalDeductions = totalDeductions;//totalDeductions/VAR.inflation; xxx this should be calculated like this but makes huge numbers
		return NPVtotalDeductions;
	}

	private double calculateBidByNeighbors(Property p) {

		double actual_bid = 0;
		double rate = 0;
		int iteratorCounter = 0;
		for (int i = p.ID - 10; i < p.ID + 10; i++) {
			double localRate = -999;
			if (i == p.ID)
				continue;

			// Properties are arranged in a circular manner where the last is adjecant to
			// the first in the list
			if (i < 0)
				i = global.properties.size() - i;
			if (i >= global.properties.size()) {
				localRate = global.properties.get(i - global.properties.size()).getAAR();
			} else {
				localRate = global.properties.get(i).getAAR();
			}

			if (localRate != -999) {
				rate += localRate;
				iteratorCounter++;
			}
		}
		if (iteratorCounter > 1) {
			rate = rate / iteratorCounter;
		} else {
			rate = CONST.rentReturn;
		}
		actual_bid = p.getValuePrevious() * Math.pow(1 + rate, p.time_since_transaction / CONST.year_ticks);
		return actual_bid;
	}

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
			if (optimist) {
				actual_bid = actual_bid
						* (Math.pow(1 + global.property_market.getAverageMarketProspectAt(current) + CONST.bidVariation,
								1 / CONST.year_ticks));
			} else {
				actual_bid = actual_bid
						* (Math.pow(1 + global.property_market.getAverageMarketProspectAt(current) - CONST.bidVariation,
								1 / CONST.year_ticks));
			}

		}
		return actual_bid;
	}

	/**
	 * A specific Property will be added, usually in the context of buying it.
	 * 
	 * @param p
	 *            The Property to be added to the list of owned Properties.
	 */

	public void addProperty(Property p) {
		p.setProjectedValue(buy_expected_sale_price);
		housholdProperties.add(p);
		Collections.sort(housholdProperties);
		out_cost += p.getCost();

		if (state == State.RENTER) {
			out_rent = 0.0;
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

	/**
	 * A specific Property will be removed, usually in the context of selling it.
	 * 
	 * @param p
	 *            The Property to be removed from the list of owned Properties.
	 */
	public void removeProperty(Property p) {
		housholdProperties.remove(p);
		Collections.sort(housholdProperties);
		out_cost -= p.getCost();

		if (state == State.OWNER) {
			out_rent = in_income * CONST.rentExpense;
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
		return out_rent;
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
	 * 
	 * @return The list of Properties owned by this Household.
	 */
	public ArrayList<Property> getProperties() {
		return housholdProperties;
	}

	public double getTotalIn() {
		return in_income + in_rent;
	}

	public double getTotalOut() {
		return out_living + out_rent + out_cost;
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
