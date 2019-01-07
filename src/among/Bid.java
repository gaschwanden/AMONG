package among;

/**
 * Class to encapsulate a Bid made on an Auction by an interested buyer.
 * 
 * @author Gideon Aschwanden && Friedrich Burkhard von der Osten
 */
public class Bid implements Comparable<Bid> {

	private Household bidder;
	private double bid;

	public Bid(Household h, double amount) {
		bidder = h;
		bid = amount;
	}

	/**
	 * Getter
	 * 
	 * @return The amount the Household bid.
	 */
	public double getAmount() {
		return bid;
	}

	/**
	 * Getter
	 * 
	 * @return The Household making a Bid.
	 */
	public Household getBidder() {
		return bidder;
	}

	@Override
	public int compareTo(Bid b) {
		return Double.compare(b.getAmount(), this.getAmount()); // highest to lowest
	}
}
