package among;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * Central class of the AMONG simulation.
 * Regulates the trading of Properties between Households with an auctioning system.
 * Properties can be offered for sale and Households can make Bids on the Auctions.
 * @author Gideon Aschwanden && Friedrich Burkhard von der Osten
 */
public class PropertyMarket {
	
	private Universe global;
	
	private ArrayList<Auction> auctions;
	private ArrayList<ArrayList<Double>> transaction_values;
	private ArrayList<Double> avg_property_values;
	private ArrayList<ArrayList<Property>> sold_properties;
	public ArrayList<Double> annualReturnsList;
	
	private int cnt_auctions;
	private int cnt_completed;
	private int cnt_remaining;
	private int cnt_developer;
	
	public PropertyMarket(Universe u){
		global = u;
		auctions = new ArrayList<Auction>();
		transaction_values = new ArrayList<ArrayList<Double>>();
		avg_property_values = new ArrayList<Double>();
		sold_properties = new ArrayList<ArrayList<Property>>();
		annualReturnsList = new ArrayList<Double>();
	}
	
	/**
	 * Main method that manages Auctions. Auctions are sorted by number of interested buyers (descending) and then processed. 
	 * Properties are sold to the highest bidder at the price of the second highest Bid (if it exists).
	 * If sold, Properties are transferred to their new owners
	 */
	@ScheduledMethod(start = 1, interval = 1, priority = 995)
	public void executeAuctions(){
		cnt_auctions = auctions.size();
		cnt_completed = 0;
		//System.out.println("----EXECUTING AUCTIONS");
		ArrayList<Double> tv = new ArrayList<Double>();
		ArrayList<Property> ps = new ArrayList<Property>();
		Collections.sort(auctions);
		Iterator<Auction> ait = auctions.iterator();
		while(ait.hasNext()){
			Auction a = ait.next();
			ArrayList<Bid> b = a.getBids();
			//System.out.println("Auction "+a+" with # bids: "+b.size());
			if(b.size() > 0){
				Collections.sort(b);
//				if(a.property.getID()==14){
//					for(Bid bids: b){
//						System.out.println("bid ="+bids.getAmount());
//					}
//				}
				Bid h = b.get(0);
				Household buyer = h.getBidder();
				if(b.size() > 1){
					h = b.get(1);
				}
				double buying_price = h.getAmount();
				//System.out.println("Buying price is "+buying_price+" with current property value "+a.getProperty().getValue()+" and reserve price "+a.getReservePrice()+" by buyer "+buyer);
				tv.add(buying_price);
				ps.add(a.getProperty());
				transferProperty(a.getSeller(), buyer, a.getProperty(), buying_price);
				ait.remove();
				for(Auction r : auctions){
					r.removeBid(buyer);
				}
				cnt_completed++;
			}
		}
		transaction_values.add(tv);
		sold_properties.add(ps);
		clearAuctions();
		avg_property_values.add(global.soldValue());
	}
	
	/**
	 * After processing all Auctions with Bids, the remaining Auctions will be deleted.
	 */
	public void clearAuctions(){
		cnt_remaining = auctions.size();
		//System.out.println("Leftover auctions "+auctions.size());
		Iterator<Auction> it = auctions.iterator();
		while(it.hasNext()){
			Auction a = it.next();
			if(a.getSeller() != null){
				it.remove();
			}
		}
		cnt_developer = auctions.size();
		//System.out.println("Auctions cleared, remaining: "+auctions.size());
	}
	
	/**
	 * A Household can offer one of its Properties for sale on the PropertyMarket.
	 * Creates a new Auction for the Property to be sold.
	 * @param s The Household selling the Property.
	 * @param p The Property to be sold.
	 */
	public void registerPropertyForSale(Household s, Property p, double price){
		Auction a = new Auction(s, p, price);
		auctions.add(a);
	}
	
	/**
	 * Method to transfer a Property from one Household to another following a completed Auction.
	 * @param f The seller of the Property.
	 * @param t The buyer of the Property.
	 * @param p The Property to be transferred between seller and buyer.
	 * @param b The Bid that won the Auction selling the Property.
	 */
	public void transferProperty(Household f, Household t, Property p, double b){
		//System.out.println("Transferring property "+p);
		double cgt = 0;
		if(f != null){
			global.capitalGainsTax((b - p.getTransationValue()), p.getTimeSinceTransaction(), f.getIncome());
			f.removeProperty(p);
		}
		p.transactions++;
		p.setValue(b);
		p.resetTimeSinceTransaction();
		t.addProperty(p);
		
		double repayment = 0;
		if(p.getMortgage() != null){
			//System.out.println("Existing mortgage, needs to be repaid first, yearly instalment is "+p.getMortgage().yearlyInstalments());
			repayment = p.getMortgage().getRemainingTotalAmount();
			double remaining_principal = p.getMortgage().getRemainingPrincipalAmount();
			double remaining_interest = repayment - remaining_principal;
			p.getMortgage().pay(remaining_principal, remaining_interest);
			p.setMortgage(null);
			//System.out.println("Repayment is "+repayment+" remaining principal/interest: "+remaining_principal+"/"+remaining_interest);
		}
		
		double loan = b * (1 - CONST.downpayment);
		Mortgage m = global.bank.issueMortgage(loan);
		//System.out.println("Loan "+loan+" issued for bid "+b+" by HH "+t.getID());
		//System.out.println("Mortgage: "+m.getRemainingMonths()+" / "+m.getRemainingPrincipalAmount()+" / "+m.isPaid());
		//System.out.println("Debit/credit difference: "+((b-loan) - (b-repayment)));
		p.setMortgage(m);
		
		t.debit(b-loan+transactionCostBuyer(b));
		if(f != null){
			f.credit(b-repayment-cgt-transactionCostSeller(b));
		}
	}
	
	public double transactionCostSeller(double price){
		return CONST.sellingCostBase + CONST.sellingCostVariable * price + CONST.solicitorFee;
	}
	
	public double transactionCostBuyer(double price){
		return CONST.loanApplicationFee + CONST.mortgageRegistrationFee + stampFee(price) + CONST.solicitorFee;
	}
	
	public double stampFee(double price){
		double stamp = 0;
		int bcnt = CONST.stamp_brackets.length;
		if(price > CONST.stamp_brackets[bcnt-1]){
			stamp = CONST.stamp_fees[bcnt-1] * price;
		}
		else{
			for(int i = 0; i < bcnt-1; i++){
				if(price <= CONST.stamp_brackets[i+1]){
					double base = CONST.stamp_base[i];
					double variable = CONST.stamp_fees[i] * (price - CONST.stamp_brackets[i]);
					stamp = base + variable;
					break;
				}
			}
		}
		return stamp;
	}
	
	public void initialize(){
		avg_property_values.add(global.soldValue());
	}

	/**
	 * Getter
	 * @return The list of all Auctions.
	 */
	public ArrayList<Auction> getAuctions(){
		return auctions;
	}
	
	public int getAuctionsTotal(){
		return cnt_auctions;
	}
	
	/**
	 * Getter (statistics)
	 * @return The number of Auctions where a Property changes owners.
	 */
	public int getAuctionsCompleted(){
		return cnt_completed;
	}
	
	/**
	 * Getter (statistics)
	 * @return The number of Auctions with no Bids that get cleared, because the Property is not sold.
	 */
	public int getAuctionsRemaining(){
		return cnt_remaining;
	}
	
	public int getAuctionsDeveloper(){
		return cnt_developer;
	}
	
	public double getAnticipatedAnnualReturn(){
		// TODO this isnt just right yet
		int sz = sold_properties.size();
		if(sz <= 1){
			annualReturnsList.add(0.0);
			return 0;
		}
		int ts = sold_properties.get(sz-2).size();
		if(ts == 0){
			annualReturnsList.add(0.0);
			return 0;
		}
		double aar = 0;
		for(Property p : sold_properties.get(sz-2)){
			double p0 = p.getpreviousTransationValue();
			double pt = p.getTransationValue();
			double t = p.getPreviousTimeSinceTransaction();
			double aar_p = Math.exp(Math.log(pt/p0)/(t/CONST.year_ticks)) - 1;
////			if(Math.abs(aar_p)>0.10){
//			System.out.println();
//			System.out.println("Current Value (pt) of "+p.getID()+": "+pt);
//			System.out.println("Previous value (p): "+p0);
//			System.out.println("Timeperiod: "+t);
//			System.out.println("Resulting AAR: "+aar_p);
//			System.out.println("ID = "+p.getID());
////			}
			aar+= aar_p;
			
		}
		aar /= ts;
//		System.out.println();
//		System.out.println();
//
//		System.out.println("AAR "+aar+" for "+ts+" transactions");
		annualReturnsList.add(aar);
//		Printing out the Anticipated Annual Returns
//		System.out.println("printing aars at "+RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
//		for (int i = 0; i<annualReturnsList.size();i++){
//			System.out.print(annualReturnsList.get(i)+",");
//			System.out.println();
//			
//		}
		return aar;
	}
	
	public double getAnticipatedAnnualReturn(int time){
		// TODO hwo to calculate the average annual return from the list of saved annualized returns?
		int sz = annualReturnsList.size();
		if(sz == 0){
			return 0;
		}
		if(time >= sz){
			return (annualReturnsList.get(sz-1) + annualReturnsList.get(0))/2.0;
		}
		return (annualReturnsList.get(sz-1) + annualReturnsList.get(sz-time-1))/2.0;
	}
	
	public double getAnticipatedAnnualReturn(int start, int time){
		int ls = avg_property_values.size();
		double avg_now = avg_property_values.get(ls-1);
		if(ls > time){
			avg_now = avg_property_values.get(ls-time-1);
		}
		
		double avg_past = avg_property_values.get(0);
		if(ls - start - time > 0){
			avg_past = avg_property_values.get(ls-start-time-1);
		}
		
		return (avg_now - avg_past)/avg_past;
	}
	
	public double getMarketProspect(int time){
		double magnitude = global.rnd.nextDouble();
		boolean sign = global.rnd.nextBoolean();
		
		if(sign){
			magnitude *= -1;
		}
		magnitude *= CONST.marketVariance;
		magnitude += getAnticipatedAnnualReturn(time);
		
		return magnitude;
	}
}
