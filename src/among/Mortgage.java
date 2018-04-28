package among;


public class Mortgage {
	private Bank bank;
	
	private double remaining_months;
	private double remaining_amount;
	
	private double last_instalment;
	
	public Mortgage(Bank b, double amount){
		remaining_amount = amount;
		remaining_months = CONST.mortgagePeriod;
		bank = b;
	}
	
	public double pay(){
		double monthly_interest_rate = VAR.irMortgage/CONST.months;
		double instalment = bank.estimateMonthlyPayments(remaining_amount, remaining_months);
		double interest = monthly_interest_rate * remaining_amount;
		double principal = instalment - interest;
		bank.payInterest(interest);
		bank.payPrincipal(principal);
		remaining_amount -= principal;
		last_instalment = instalment;
		return instalment;
	}
	
	public void pay(double principal, double interest){
		remaining_amount -= principal;
		bank.payPrincipal(principal);
		bank.payInterest(interest);
		//System.out.println("Mortgage paid "+isPaid());
	}
	
	public void decreaseRemainingMonths(){
		remaining_months -= 1.0/CONST.month_ticks;
	}
	
	public double yearlyInstalments(){
		return bank.estimateMonthlyPayments(remaining_amount, remaining_months) * CONST.months;
	}
	
	public boolean isPaid(){
		if((int)remaining_months == 0 || Math.abs(remaining_amount) < CONST.dErr){
			return true;
		}
		return false;
	}
	
	public int getRemainingMonths(){
		//System.out.println("Remaining months "+remaining_months);
		return (int) remaining_months;
	}
	
	public double getRemainingPrincipalAmount(){
		return remaining_amount;
	}
	
	public double getRemainingTotalAmount(){
		double interest = bank.estimateMonthlyPayments(remaining_amount, remaining_months) * remaining_months - remaining_amount;
		return remaining_amount+interest;
	}
	
	public double getLastInstalment(){
		return last_instalment;
	}
}
