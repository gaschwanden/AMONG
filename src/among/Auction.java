package among;

import java.util.ArrayList;

/**
 * Class to encapsulate an Auction. In an Auction a Property is offered for sale
 * by a Household. It can attract a number of interested buyers that register
 * their bids. The ProprtyMarket will manage the Auction and execute it.
 * 
 * @author Gideon Aschwanden && Friedrich Burkhard von der Osten
 */
public class Auction implements Comparable<Auction> {

	private Household seller;
	public Property property;
	private double reserve_price;

	private ArrayList<Bid> bids;

	public Auction(Household s, Property p, double price) {
		seller = s;
		property = p;
		reserve_price = price;
		bids = new ArrayList<Bid>();
	}

	/**
	 * Getter
	 * 
	 * @return The Household selling the Property advertised in the Auction.
	 */
	public Household getSeller() {
		return seller;
	}

	/**
	 * Getter
	 * 
	 * @return The property that is advertised in the Auction.
	 */
	public Property getProperty() {
		return property;
	}

	public double getReservePrice() {
		return reserve_price;
	}

	public double getHighestBid() {
		double hb = 0;
		for (Bid b : bids) {
			if (b.getAmount() > hb) {
				hb = b.getAmount();
			}
		}
		return hb;
	}

	/**
	 * Getter
	 * 
	 * @return A list of the bids registered for this Auction.
	 */
	public ArrayList<Bid> getBids() {
		return bids;
	}

	/**
	 * Method to register a Bid for this Auction. Creates a new Bid corresponding to
	 * the Bidder and the amount bid.
	 * 
	 * @param h
	 *            The Household that registers a Bid for this Auction.
	 * @param amount
	 *            The amount the Household bids on the Property advertised in the
	 *            Auction.
	 */
	public void registerInterestToBuy(Household h, double amount) {
		Bid bid = new Bid(h, amount);
		bids.add(bid);
	}

	/**
	 * The Bid corresponding to a particular Household will be removed from the list
	 * of Bids for this Auction.
	 * 
	 * @param h
	 *            The household whose Bid will be removed from the list of Bids.
	 */
	public void removeBid(Household h) {
		Bid r = null;
		for (Bid b : bids) {
			if (b.getBidder().equals(h)) {
				r = b;
				break;
			}
		}
		bids.remove(r);
	}

	@Override
	public int compareTo(Auction a) {
		return Double.compare(a.getHighestBid(), this.getHighestBid()); // highest to lowest
	}
}
