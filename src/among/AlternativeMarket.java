package among;

public class AlternativeMarket {
	
	Universe global;
	
	public AlternativeMarket(Universe g){
		global = g;
	}
	
	public double getMarketProspect(){
		double magnitude = global.rnd.nextDouble();
		boolean sign = global.rnd.nextBoolean();
		
		if(sign){
			magnitude *= -1;
		}
		magnitude *= CONST.marketVariance;
		magnitude += CONST.alternativeInvestmentReturn;
		
		return magnitude;
	}

}
