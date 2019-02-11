package among;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import among.Household.State;

import java.io.File;
import java.io.FileOutputStream;

import au.edu.unimelb.eresearch.repast.parameterswrapper.ChartsWrapper;
import au.edu.unimelb.eresearch.repast.parameterswrapper.ParametersWrapper;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.util.ContextUtils;

/**
 * Class to hold all objects in the simulation Provides auxiliary methods and
 * global events, as well as data collection
 * 
 * @author Gideon Aschwanden && Friedrich Burkhard von der Osten
 */
public class Universe {

	public Random rnd;
	public CustomDistribution incomes_assets;
	public CustomDistribution property_values;

	public ArrayList<Household> households;
	public ArrayList<Property> properties;

	public AlternativeMarket alternative_market;
	public Bank bank;
	public Developer developer;
	public PropertyMarket property_market;

	private double balance;

	private boolean supply_shock;
	private boolean demand_shock;
	private boolean income_shock;
	private boolean policy_shock;
	private double previous_supply;
	private double previous_demand;
	private double previous_income;
	private double previous_NG;
	private double previous_CGT;
	public double tick;
	private boolean writePropertyPrices;
	private boolean  writeHousholdInformation;
	private boolean writeGlobalInformation;

//	private Writer fnpp;
	private File file;
	private File file2;
	private File file3;
	private PrintWriter writerProperty;
	private PrintWriter writerHousholds;
	private PrintWriter writerGlobal;

	public Universe(long seed) {
		rnd = new Random(seed);
		incomes_assets = new CustomDistribution("income");
		property_values = new CustomDistribution("property");

		households = new ArrayList<Household>();
		properties = new ArrayList<Property>();

		supply_shock = false;
		demand_shock = false;
		income_shock = false;
		writePropertyPrices = true;
		writeHousholdInformation = true;
		writeGlobalInformation = true;

	}

	@ScheduledMethod(start = 1, interval = 1, priority = 994)
	public void growPopulation() {
		/*
		 * Context<Object> context = ContextUtils.getContext(this); int
		 * increaseHousehold = (int)
		 * Math.ceil((VAR.householdGrowth*CONST.household_ID)/(double)CONST.year_ticks);
		 * 
		 * for(int i = 0; i < increaseHousehold; i++){ Household household = new
		 * Household(this); households.add(household); context.add(household); }
		 * 
		 * ArrayList<Property> new_properties = new ArrayList<Property>();
		 * if(!CONST.delayedResponse){ new_properties = developer.buildProperties(); }
		 * else{ new_properties = developer.delayedSupplyResponse(); } for(Property p :
		 * new_properties){ context.add(p);
		 * property_market.registerPropertyForSale(null, p, p.getValue()); }
		 */
		setPercentiles();
	}
	@ScheduledMethod(start = 1, interval = 1, priority = 992)
	public void dataCollectionAlways() throws Exception {

		if(writePropertyPrices){

		    if(file == null){
		    	file = new File ("snapshot_property.txt");
		    }
		    
			if(writerProperty == null){
				writerProperty = new PrintWriter("snapshot_property.txt");
				writerProperty.println("tick,ID,value_initial,value,value_previous,value_projected,time_since_transaction,transactions,timeOnMarket");
			} else{
				writerProperty = new PrintWriter(new FileOutputStream(new File("snapshot_property.txt"),true));
			}
			
			for(int i =0;i<properties.size();i++){
				Property p = properties.get(i);
				writerProperty.println((int)tick+","+p.ID+"," +(int)p.getValueInitial()+","+(int)p.getValue()+","+(int)p.getValuePrevious()+","+(int)p.getValueProjected()+","+(int)p.time_since_transaction+","+(int)p.transactions+","+(int)p.timeOnMarket);
			}
			writerProperty.close();
		}
		if(writeHousholdInformation){

		    if(file2 == null){
		    	file2 = new File ("snapshot_housholds.txt");
		    }
			if(writerHousholds == null){
				writerHousholds = new PrintWriter("snapshot_housholds.txt");
				writerHousholds.println("tick,ID,asset_initial,asset,investmenthorizon,numberOfProperties");
			} else{
				writerHousholds = new PrintWriter(new FileOutputStream(new File("snapshot_housholds.txt"),true));
			}
			
			for(int i =0;i<households.size();i++){
				 Household h = households.get(i);
				 writerHousholds.println((int)tick+","+h.getID()+"," +(int)h.getAssetsInitial()+","+(int)h.getAssets()+","+(int)h.getInvestmentHorizon()+","+h.getProperties().size());
			}
			writerHousholds.close();
		}
		
		if(writeGlobalInformation){

		    if(file3 == null){
		    	file3 = new File ("snapshot_global.txt");
		    }
			if(writerGlobal == null){
				writerGlobal = new PrintWriter("snapshot_global.txt");
				writerGlobal.println("tick,totalLiquidAssets,totalPropertyAssets,renters,owners,investors");
			} else{
				writerGlobal = new PrintWriter(new FileOutputStream(new File("snapshot_global.txt"),true));
			}
			int renters = 0;
			int owners = 0;
			int investors = 0;
			double totalLiquidAssets = 0;
			double totalPropertyAssets = 0;
			for(int i =0;i<households.size();i++){
				 Household h = households.get(i);
				 if(State.RENTER==h.getState())renters++;
				 if(State.OWNER==h.getState())owners++;
				 if(State.INVESTOR==h.getState())investors++;
				 totalLiquidAssets += h.getAssets();
				 
			}
			for (Property property : properties) {
				totalPropertyAssets+= property.value;
			}
			writerGlobal.println((int)tick+","+totalLiquidAssets+"," +totalPropertyAssets+"," +renters+","+owners+","+investors);
			writerGlobal.close();
		}
	}

//	@ScheduledMethod(start = 1, interval = 52, priority = 993)
//	public void dataCollection() throws Exception {
//
//		Parameters params = ParametersWrapper.getInstance().getParameters();
//		
//		if (params.getBoolean("csv")) {
//			data_collection_count++;
//			Context<Object> context = ContextUtils.getContext(this);
//			IndexedIterable<Object> ii = context.getObjects(Household.class);
//
//			String fnhh = "experiments/snapshot_household_" + data_collection_count + "_run_" + CONST.run + ".txt";
//			String fnpp = "experiments/snapshot_property_" + data_collection_count + "_run_" + CONST.run + ".txt";
//			String fngu = "experiments/snapshot_universe_" + data_collection_count + "_run_" + CONST.run + ".txt";
//			PrintWriter writerhh = new PrintWriter(fnhh);
//			PrintWriter writerpp = new PrintWriter(fnpp);
//			PrintWriter writergu = new PrintWriter(fngu);
//			if (!params.getBoolean("aggregate")) {
//				for (Object o : ii) {
//					Household h = (Household) o;
//					writerhh.print(h.householdToFileFormat());
//					writerpp.print(h.propertyToFileFormat());
//				}
//			} else {
//				int R = 0;
//				int O = 0;
//				int I = 0;
//				int FHO = 0;
//				for (Object o : ii) {
//					Household h = (Household) o;
//					R += h.renter();
//					O += h.owner();
//					I += h.investor();
//					if (h.isFHO() && h.renter() == 0) {
//						FHO++;
//					}
//				}
//				writerhh.println("Renter: " + R);
//				writerhh.println("Owner: " + O);
//				writerhh.println("Inestor: " + I);
//				writerhh.println("FHOS: " + FHO);
//
//				IndexedIterable<Object> ip = context.getObjects(Property.class);
//				double cumulative_property_value = 0;
//				for (Object o : ip) {
//					Property p = (Property) o;
//					cumulative_property_value += p.getValue();
//				}
//				writerpp.println(cumulative_property_value);
//			}
//			for (int i = 0; i < CONST.income_deciles.length; i++) {
//				writergu.print(CONST.income_deciles[i] + ": ");
//				writergu.print(CONST.CGTLost[i] + "," + CONST.NGLost[i] + "\n");
//			}
//			writerhh.close();
//			writerpp.close();
//			writergu.close();
//		}
//	}

	@ScheduledMethod(start = 1, interval = 1, priority = 992)
	public void eventChecker() {
		tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		Parameters params = ParametersWrapper.getInstance().getParameters();

		// System.out.println(tick);

		if (supply_shock) {
			VAR.propertyGrowth = previous_supply;
			supply_shock = false;
		}
		if (demand_shock) {
			VAR.householdGrowth = previous_demand;
			demand_shock = false;
		}
		if (income_shock) {
			VAR.wageGrowth = previous_income;
			income_shock = false;
		}

		 if(policy_shock){
		 VAR.CGTDiscount = previous_CGT;	
		 }

		if (tick == (double) params.getInteger("policyShock")) {

			System.out.println("Policy Shock!");
			previous_CGT = VAR.CGTDiscount;
			previous_NG = VAR.negativeGearing;
			policy_shock = true;
			System.out.println();
			System.out.println("Negative Gearing old = " + VAR.negativeGearing);
			System.out.println("CGT Discount old = " + VAR.CGTDiscount);
			VAR.CGTDiscount = params.getDouble("CGTMagnitude");
			VAR.negativeGearing = params.getDouble("NGMagnitude");
			System.out.println("Negative Gearing new = " + VAR.negativeGearing);
			System.out.println("CGT Discount new = " + VAR.CGTDiscount);
		}

		if (tick == (double) params.getInteger("optimismShock")) {

			System.out.println("Optimism Shock!");

			System.out.println();
			System.out.println("Optimism old = " + CONST.optimism);

			CONST.optimism = params.getDouble("optimsimValue");
			System.out.println("Optimism new = " + CONST.optimism);

			previous_CGT = VAR.CGTDiscount;
			previous_NG = VAR.negativeGearing;

		}

		if (tick == (double) params.getInteger("shockSupply")) {
			previous_supply = VAR.propertyGrowth;
			supply_shock = true;
			VAR.propertyGrowth = params.getDouble("supplyMagnitude");
		}
		if (tick == (double) params.getInteger("shockDemand")) {
			previous_demand = VAR.householdGrowth;
			demand_shock = true;
			VAR.householdGrowth = params.getDouble("demandMagnitude");
		}
		if (tick == (double) params.getInteger("shockIncome")) {
			previous_income = VAR.wageGrowth;
			income_shock = true;
			VAR.wageGrowth = params.getDouble("incomeMagnitude");
		}

		if (tick > 1) {
			updatePropertyMarketValue();
		}
	}

	private void updatePropertyMarketValue() {
		property_market.makeAverageAnticipatedAnnualReturn();
		double aar = property_market.getAverageAnticipatedAnnualReturn();
//		System.out.println("aar = "+aar);
		for (Property p : properties)
			p.updateMarketValueProperty(aar);
	}

	// @ScheduledMethod(start = 1, interval = 1, priority = 2)
	public void printBalance() {
		System.out.println("----BALANCE");
		double asum = getAssetSum();

		double isum = 0;
		double risum = 0;
		double losum = 0;
		double rosum = 0;
		double cosum = 0;

		double ti = 0;
		double to = 0;
		for (Household h : households) {
			isum += h.getIncome();
			risum += h.getRentIn();
			rosum += h.getRentOut();
			losum += h.getLivingOut();
			cosum += h.getCost();

			ti += h.getTotalIn();
			to += h.getTotalOut();
		}
		isum /= CONST.year_ticks;
		risum /= CONST.year_ticks;
		losum /= CONST.year_ticks;
		rosum /= CONST.year_ticks;
		cosum /= CONST.year_ticks;

		ti /= CONST.year_ticks;
		to /= CONST.year_ticks;

		double csum = 0;
		double rsum = 0;
		double msum = 0;
		for (Property p : properties) {
			csum += p.getCost();
			rsum += p.getRent();
			if (p.getMortgage() != null) {
				if (p.getMortgage().getRemainingMonths() % CONST.month_ticks == 0) {
					msum += (p.getMortgage().getLastInstalment());
				}
			}
		}
		csum /= CONST.year_ticks;
		rsum /= CONST.year_ticks;

		System.out.println("Asset sum " + asum);
		System.out.println("Income sum " + isum);
		System.out.println("Rent in sum " + risum + " / " + rsum);
		System.out.println("Living out sum " + losum);
		System.out.println("Rent out sum " + rosum);
		System.out.println("Cost sum " + csum + " / " + cosum);
		System.out.println("Mortgage sum " + msum);

		double increase = isum + risum - cosum - losum - rosum;
		double inout = ti - to;
		// System.out.println("Simulation balance "+(asum - balance + msum));
		System.out.println("Balance increase " + increase + " / " + inout);
		balance = asum;
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 1)
	public void printSeparator() {
		double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		// System.out.println("TICK "+tick+"\n");
	}

	@ScheduledMethod(start = 1, interval = 1, priority = 990)
	public void populateChartDataAndSendToWebservice() {
		final Context<Object> context = ContextUtils.getContext(this);
		final ParametersWrapper parametersWrapper = ParametersWrapper.getInstance();
		final ChartsWrapper chartsWrapper = ChartsWrapper.getInstance();

		final double tickCount = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();

		if (tickCount <= 1D) {
			parametersWrapper.initialise();
			chartsWrapper.initialise();
		}

		final Map<String, Double> data = new HashMap<String, Double>();

		// Global Variables
		data.put("tick", tickCount);

		// Universe Variables
		context.getObjects(Universe.class).forEach(o -> {
			final Universe u = (Universe) o;

			data.merge("propertyValue_averagePropertyValue", u.getAveragePropertyValue(), Double::sum);
			data.merge("propertyValue_marketValue", u.marketValue(), Double::sum);
			data.merge("propertyValue_soldValue", u.soldValue(), Double::sum);
		});

		// Household Variables
		context.getObjects(Household.class).forEach(o -> {
			final Household h = (Household) o;

			data.merge("normalisedOs_renter", h.renterFraction(), Double::sum);
			data.merge("normalisedOs_owner", h.ownerFraction(), Double::sum);
			data.merge("normalisedOs_investor", h.investorFraction(), Double::sum);

			data.merge("ownershipState_renter", (double) h.renter(), Double::sum);
			data.merge("ownershipState_owner", (double) h.owner(), Double::sum);
			data.merge("ownershipState_investor", (double) h.investor(), Double::sum);
		});

		// PropertyMarket Variables
		context.getObjects(PropertyMarket.class).forEach(o -> {
			final PropertyMarket p = (PropertyMarket) o;

			data.merge("aar", p.getAverageAnticipatedAnnualReturn(), Double::sum);

			data.merge("auctions_auctions", (double) p.getAuctionsTotal(), Double::sum);
			data.merge("auctions_completed", (double) p.getAuctionsCompleted(), Double::sum);
			data.merge("auctions_remaining", (double) p.getAuctionsRemaining(), Double::sum);
			data.merge("auctions_developer", (double) p.getAuctionsDeveloper(), Double::sum);
		});

		// Convert to required Entry<String, String> format
		final Map<String, String> chartsData = data.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));

		chartsWrapper.publishSingleChartMap(chartsData);
	}

	public void initialize() {
		balance = getAssetSum();
		property_market.initialize();
	}

	public double avgList(ArrayList<Double> l) {
		return sumList(l) / l.size();
	}

	public double sumList(ArrayList<Double> l) {
		double sum = 0;
		for (Double d : l) {
			sum += d;
		}
		return sum;
	}

	public double getAssetSum() {
		double asum = 0;
		for (Household h : households) {
			asum += h.getAssets();
		}
		return asum;
	}

	public double soldValue() {
		double vsum = 0;
		int timer = 0;
		for (Property p : properties) {
			if (p.time_since_transaction < 2) {
				vsum += p.getTransationValue();
				// System.out.println(p.getTransationValue());
				timer++;
			}
		}
		// System.out.println(properties.size());
		// System.out.println("vsum = "+vsum);
		// System.out.println("soldValue = "+vsum/timer+" : "+vsum+"/"+timer);
		return (vsum / timer);
	}

	public double marketValue() {
		double vsum = 0;
		for (Property p : properties) {
			vsum += p.getMarketValue();
		}
		// System.out.println(properties.size());
		// System.out.println("vsum = "+vsum);
		// System.out.println("getAveragePropertyMarketValue =
		// "+vsum/properties.size());

		return (vsum / (properties.size()+1));
	}

	public double getAveragePropertyValue() {
		double vsum = 0;
		int counter = 0;
		int movingAveragePeriod = 1;
		// TODO add second param to make moving average
		for (Property p : properties) {
			if (p.getTimeSinceTransaction() > movingAveragePeriod) {
				vsum += p.getTransationValue();
				counter++;
			}
		}
		// System.out.println("getAveragePropertyBookValue = "+vsum/timer);
		return (vsum / counter);
	}

	public void setPercentiles() {
		// System.out.println("----PERCENTILES");
		Collections.sort(households);
		for (int i = 1; i < 10; i++) {
			// System.out.println("Attempting index "+(i*CONST.household_ID)/10);
			double income = households.get((i * CONST.household_ID) / 10).getIncome();
			// System.out.println("HH ID "+i+" has income "+income);
			CONST.income_deciles[i - 1] = income;
		}
	}

	public double incomeTax(double amount) {
		if (amount < 0) {
			return 0.0;
		}
		double tax = 0;
		int bracketCount = CONST.tax_brackets.length;
		if (amount > CONST.tax_brackets[bracketCount - 1]) {
			double base = CONST.base_tax[bracketCount - 1];
			double variable = CONST.income_tax[bracketCount - 1] * (amount - CONST.tax_brackets[bracketCount - 1]);
			tax = base + variable;
			CONST.ITPaid[bracketCount - 1] += tax;
		} else {
			for (int i = 0; i < bracketCount - 1; i++) {
				if (amount <= CONST.tax_brackets[i + 1]) {
					double base = CONST.base_tax[i];
					double variable = CONST.income_tax[i] * (amount - CONST.tax_brackets[i]);
					tax = base + variable;
					CONST.ITPaid[i] += tax;
					break;
				}
			}
		}
		return tax;
	}
	
	public double getTaxPercent(double incomeAmount){
		double initalIncomeAmount = incomeAmount;
		if (incomeAmount < 0) {
			return 0.0;
		} else {
		double tax_percent = 0;
		int bracketCount = CONST.tax_brackets.length;
		if (incomeAmount > CONST.tax_brackets[bracketCount - 1]) {
			double base = CONST.base_tax[bracketCount - 1];
			double variable = CONST.income_tax[bracketCount - 1] * (incomeAmount - CONST.tax_brackets[bracketCount - 1]);
			tax_percent = (base + variable)/initalIncomeAmount;
		} else {
			for (int i = 0; i < bracketCount - 1; i++) {
				if (incomeAmount <= CONST.tax_brackets[i + 1]) {
					double base = CONST.base_tax[i];
					double variable = CONST.income_tax[i] * (incomeAmount - CONST.tax_brackets[i]);
					tax_percent = (base + variable)/initalIncomeAmount;
					break;
				}
			}
		}
//		System.out.println("from "+incomeAmount+" Real Tax income amount = "+ tax_percent);
		return tax_percent;
		}
	}

	public double projectedIT(double amount) {
		if (amount < 0) {
			return 0.0;
		}
		double tax = 0;
		int bracketCount = CONST.tax_brackets.length;
		if (amount > CONST.tax_brackets[bracketCount - 1]) {
			double base = CONST.base_tax[bracketCount - 1];
			double variable = CONST.income_tax[bracketCount - 1] * (amount - CONST.tax_brackets[bracketCount - 1]);
			tax = base + variable;
		} else {
			for (int i = 0; i < bracketCount - 1; i++) {
				if (amount <= CONST.tax_brackets[i + 1]) {
					double base = CONST.base_tax[i];
					double variable = CONST.income_tax[i] * (amount - CONST.tax_brackets[i]);
					tax = base + variable;
					break;
				}
			}
		}
		return tax;
	}

	public double capitalGainsTax(double amount, int time, double income) {
		double original_tax = projectedIT(amount);
		if (time > CONST.year_ticks) {
			amount = amount * (1 - VAR.CGTDiscount);
		}
		double discounted_tax = projectedIT(amount);
		for (int i = 0; i < CONST.income_deciles.length; i++) {
			if (income < CONST.income_deciles[i]) {
				CONST.CGTPaid[i] += discounted_tax;
				CONST.CGTLost[i] += original_tax - discounted_tax;
				break;
			}
		}
		return discounted_tax;
	}

	public double projectedCGT(double amount, int time) {
		if (time > CONST.year_ticks) {
			amount = amount * (1 - VAR.CGTDiscount);
		}
		double discounted_tax = projectedIT(amount);
		return discounted_tax;
	}
}
