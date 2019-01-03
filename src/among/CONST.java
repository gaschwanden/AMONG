package among;

public class CONST {
	public static int household_ID = 0;
	public static int property_ID = 0;
	public static int run = 1;
	public static double dErr = 0.001;
	public static double optimism = 0.50; //outlook of households:  0.0 pessimism / 1.0 optimism
	
	public static double year_ticks = 52; // Weeks per year
	public static double tick_limit = 5000; // 2000 or 5200
	public static boolean delayedResponse = false;
	
	// property
	public static double maintenance = 0.015; // % of property value spent on maintenance
	public static double rentReturn = 0.05; // % of property value rental return
	public static double propertyDevaluation = 0.025;
	
	// household
	public static double rentExpense = 0.3; // % of income spent on rent
	public static double livingExpense = 0.3; // % of income spent on living costs (excluding rent)
	public static double investmentHorizonRenter = 40.0;
	public static double investmentHorizon = 30.0;
	
	// cashflow and projections
	public static double leverage = 4.0; // multiple of assets that is the affordable property value
	public static double downpayment = 0.2; // % of property value used as downpayment
	public static double bidIncrease = 0.2; // % to go over reserve price when bidding
	public static double bidVariation = 0.05; // % variation in bidding price
	public static double reducedReserve = 0.1; // % slash on reserve price when selling under pressure (assets < 0)
	public static double reducedReserveOvertime = 20; // % slash on reserve price over time annually (assets < 0)

	public static double minDeposit = 50000.0; // minimum assets to buy property
	
	// taxes
	public static double marginalTaxRate = 0.4;
	public static double[] tax_brackets = {0, 18200, 37000, 87000, 180000};
	public static double[] base_tax = {0, 0, 3572, 19822, 54232};
	public static double[] income_tax = {0, 0.19, 0.325, 0.37, 0.45};
	public static double[] CGTPaid = {0,0,0,0,0,0,0,0,0,0};
	public static double[] CGTLost = {0,0,0,0,0,0,0,0,0,0};
	public static double[] NGLost = {0,0,0,0,0,0,0,0,0,0};
	public static double[] income_deciles = {0,0,0,0,0,0,0,0,0,0};
	public static double[] ITPaid = {0,0,0,0,0};
	
	// costs associated with a property transfer
	public static double sellingCostBase = 1000.0; // paid by seller
	public static double sellingCostVariable = 0.025; // % of property value, paid by seller
	public static double solicitorFee = 1300.0; // conveyancing fee paid by seller and buyer
	public static double loanApplicationFee = 500.0; // paid by buyer
	public static double mortgageRegistrationFee = 112.60; // paid by buyer
	public static double[] stamp_brackets = {0,25000,130000,960000}; // paid by buyer
	public static double[] stamp_base = {0, 350, 2870};
	public static double[] stamp_fees = {0.014,0.024,0.06,0.055};
	
	// costs and benefits associated with NPV decision
	public static double stableMargin = 0.01; // market fluctuations that are considered neutral
	public static double marketVariance = 0.1; // random perceptive difference from market rates
	public static double alternativeInvestmentReturn = 0.06;
	public static double stdVariableRate = 0.042;
	public static double governmentBondYield = 0.0275;
	public static double dividendYield = 0.02;

	public static double FHOG = 10000.0;
	public static double valueDepreciation = 0.0125;
	public static double councilRates = 0.003;
	public static double buildingInsurance = 0.0015;
	
	public static double movingCost = 5000.0;
	public static double renterMovingPeriod = 3.0;
	public static double rentalBond = 0.1;
	
	// mortgage calculation
	public static double mortgagePeriod = 360; // Months, 30 years
	public static double months = 12; // Months per year
	public static double month_ticks = 4; // Weeks per month
}
