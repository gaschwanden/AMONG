package among;

import among.Household.State;

public class Bank {
	
	private Universe global;
	
	private double interest_paid;
	private double principal_paid;
	private double credit_paid;
		
	public Bank(Universe u){
		global = u;
		interest_paid = 0;
		principal_paid = 0;
		credit_paid = 0;
	}
	
	public double estimateMonthlyPayments(double remainingAmount, double remainingMonths){
		double r = VAR.irMortgage/CONST.months; // monthly interest rate
		double N = remainingMonths; // number of monthly payments remaining (initialized as CONST.mortgagePeriod)
		double P = remainingAmount; // loan principal (borrowed amount)
		
		double c = (r * P) / (1 - Math.pow((1 + r), -N));
		
		/*double i = r * P; // interest portion of c
		double p = c - i; // principal portion of c
		double totalInterest = c * N - P; // Interest over the duration of the loan*/
		
		return c;
	}
	
	public double estimateLoanAmount(double disposableIncome){
		double r = VAR.irMortgage/CONST.months; // monthly interest rate
		double N = CONST.mortgagePeriod; // number of monthly payments
		double c = disposableIncome; // monthly mortgage payments
		
		double P = (c * (1 - Math.pow((1 + r), -N))) / r;
		
		return P;
	}

	public double affordableLoan(double assets, State state, double out_rent, double disposable_income){
		if(state == State.RENTER){
			disposable_income += out_rent;
		}
		double projected_loan = (1 - CONST.downpayment) * assets * CONST.leverage;
		double affordable_loan = estimateLoanAmount((disposable_income / CONST.year_ticks) * CONST.month_ticks);
		//System.out.println("Original projected loan "+projected_loan);
		//System.out.println("Affordable loan "+affordable_loan);
		if(affordable_loan < projected_loan){
			projected_loan = affordable_loan;
			//System.out.println("Corrected loan "+projected_loan);
		}
		return projected_loan;
	}
	
	public Mortgage issueMortgage(double amount){
		Mortgage m = new Mortgage(this, amount);
		return m;
	}
	
	public void payCreditInterest(double a){
		credit_paid += a;
	}
	
	public void payInterest(double a){
		interest_paid += a;
	}
	
	public void payPrincipal(double a){
		principal_paid += a;
	}
	
	public double getInterestPaid(){
		return interest_paid;
	}
	
	public double getPrincipalPaid(){
		return principal_paid;
	}
}
