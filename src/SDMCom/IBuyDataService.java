package SDMCom;

import jadex.commons.future.IFuture;

/**
 *  The buy Data service is provided by the seller and used by the buyer.
 */
public interface IBuyDataService
{
	/**
	 *  Ask the seller for a a quote on a Data.
	 *  @param title	The Data title.
	 *  @return The price.
	 */
	public IFuture<Integer>	callForProposal(String title);

	/**
	 *  Buy a Data
	 *  @param title	The Data title.
	 *  @param req	The req to apply.
	 *  @return A future indicating if the transaction was successful.
	 */
	public IFuture<Void> acceptProposal(String title, int code);

//	public int intValue();

	//public IFuture<Void> acceptProposal(Integer valueOf);
	
//	/**
//	 *  Refuse to buy a Data
//	 *  @param title	The Data title.
//	 *  @param req	The requested req.
//	 */
//	public void	rejectProposal(String title, int price);
}
