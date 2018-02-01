package common;

import java.util.Date;

import SDMCom.IBuyDataService;
import jadex.bridge.service.annotation.Reference;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.clock.ITimedObject;
import jadex.commons.SimplePropertyChangeSupport;
import jadex.commons.beans.PropertyChangeListener;

/**
 * The order for purchasing or selling books.
 */
@Reference
public class Order
{
	//-------- constants --------

	/** The state open. */
	public static final String OPEN = "open";

	/** The state done. */
	public static final String DONE = "done";

	/** The state failed. */
	public static final String FAILED = "failed";

	public static final String Collect = "Collect";
	public static final String Process = "Process";

	//-------- attributes --------

	/** The book title. */
	protected String title;

	/** The deadline. */
	protected Date deadline;

	/** The  TransformationId. */
	protected int Transformationcode;

	/** The CheckRequest. */
	protected String CheckRequest;

	/** The starttime. */
	protected long starttime;

	/** The REquest type. */
	protected Integer Executioncode;

	/** The execution date. */
	protected Date exedate;

	/** The flag indicating if it is a buy (or sell) order. */
	protected boolean buyorder;
	
	/** The state. */
	protected String state;

	/** The clock. */
//	public IExternalAccess clock;
	protected IClockService clock;
	
	/** The helper object for bean events. */
	public SimplePropertyChangeSupport pcs;
	
	//-------- constructors --------

	/**
	 * Create a new order.
	 * @param title	The title.
	 * @param deadline The deadline.
	 * @param req	The req.
	 * @param start	The Check Request
	 */
	public Order(String title,Date deadline, String CheckRequest, int Transformationcode,  boolean buyorder, IClockService clock)
	{
		this.title = title;
		this.CheckRequest = CheckRequest;
		this.Transformationcode = Transformationcode;
		this.buyorder = buyorder;
		this.starttime = clock.getTime();
		this.clock	= clock;
		this.state = OPEN;
		this.pcs = new SimplePropertyChangeSupport(this);
		
		setDeadline(deadline);
	}

	//-------- methods --------

	/**
	 * Get the title.
	 * @return The title.
	 */
	public String gettitle()
	{
		return title;
	}

	/**
	 * Set the title.
	 * @param title The title.
	 */
	public void settitle(String title)
	{
		String oldtitle = this.title;
		this.title = title;
		pcs.firePropertyChange("title", oldtitle, title);
	}

	/**
	 * Get the deadline.
	 * @return The deadline.
	 */
	public Date getDeadline()
	{
		return deadline;
	}

	/**
	 * Set the deadline.
	 * @param deadline The deadline.
	 */
	public void setDeadline(Date deadline)
	{
		Date olddeadline = this.deadline;
		this.deadline = deadline;
		
		if(this.deadline!=null)
		{
//			System.out.println("Order: "+deadline.getTime()+" "+starttime);
			final long wait = Math.max(0, deadline.getTime()-starttime);
			
			clock.createTimer(wait , new ITimedObject()
			{
				public void timeEventOccurred(long currenttime)
				{
					synchronized(Order.this)
					{
						if(getState().equals(OPEN))
						{
//							System.out.println("Order state failed: "+wait+" "+Order.this);
							setState(FAILED);
						}
					}
				}
			});
		}
		
		pcs.firePropertyChange("deadline", olddeadline, deadline);
	}

	/**
	 * Get the deadline time.
	 * @return The deadline time.
	 * /
	public void getDeadlineTime()
	{
		return 
	}*/
	
	/**
	 * Get the code.
	 * @return The code.
	 */
	public int getTransformationcode()
	{
		return Transformationcode;
	}

	/**
	 * Set the code.
	 * @param req The code.
	 */
	public void setTransformationcode(int Transformationcode)
	{
		int oldTransformationcode = this.Transformationcode;
		this.Transformationcode = Transformationcode;
		pcs.firePropertyChange("Request", oldTransformationcode, Transformationcode);
	}

	/**
	 * Getter for CheckRequest
	 * @return Returns CheckRequest.
	 */
	public String getCheckRequest()
	{
		return CheckRequest;
	}

	/**
	 * Setter for CheckRequest.
	 * @param start The Order.java value to set
	 */
	public void setCheckRequest(String CheckRequest)
	{
		String oldCheckRequest = this.CheckRequest;
		this.CheckRequest = CheckRequest;
		pcs.firePropertyChange("CheckRequest", oldCheckRequest, CheckRequest);
	}

	/**
	 * Get the start time.
	 * @return The start time.
	 */
	public long getStartTime()
	{
		return starttime;
	}

	/**
	 * Set the start time.
	 * @param starttime The start time.
	 */
	public void setStartTime(long starttime)
	{
		long oldstarttime = this.starttime;
		this.starttime = starttime;
		pcs.firePropertyChange("startTime", new Long(oldstarttime), new Long(starttime));
	}

	/**
	 * Get the execution request.
	 * @return The execution request.
	 */
	public Integer getExecutioncode()
	{
		return  Executioncode;
	}

	/**
	 * Set the execution code.
	 * @param exeprice The execution code.
	 */
	public void setExecutioncode(Integer Executioncode)
	{
		Integer oldExecutioncode = this.Executioncode;
		this.Executioncode = Executioncode;
		pcs.firePropertyChange("executioncode", oldExecutioncode, Executioncode);
	}

	/**
	 * Get the execution date.
	 * @return The execution date.
	 */
	public Date getExecutionDate()
	{
		return exedate;
	}

	/**
	 * Set the execution date.
	 * @param exedate The execution date.
	 */
	public void setExecutionDate(Date exedate)
	{
		Date oldexedate = this.exedate;
		this.exedate = exedate;
		pcs.firePropertyChange("executionDate", oldexedate, exedate);
	}

	/**
	 * Test if it is a buyorder.
	 * @return True, if buy order.
	 */
	public boolean isBuyOrder()
	{
		return buyorder;
	}

	/**
	 * Set the order type.
	 * @param buyorder True for buyorder.
	 */
	public void setBuyOrder(boolean buyorder)
	{
		boolean oldbuyorder = this.buyorder;
		this.buyorder = buyorder;
		pcs.firePropertyChange("buyOrder", oldbuyorder ? Boolean.TRUE : Boolean.FALSE, buyorder ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 *  Get the order state.
	 *  @return The order state.
	 */
	public synchronized String getState()
	{
//		String state = FAILED;
//		if(exedate != null)
//		{
//			state = DONE;
//		}
//		else if(clock.getTime() < deadline.getTime())
//		{
//			state = OPEN;
//		}
		return state;
	}

	/**
	 *  Set the state.
	 *  @param state The state.
	 */
	public synchronized void setState(String state)
	{
		String oldstate = this.state;
		this.state = state;
//		System.out.println("Order changed state: "+oldstate+" "+state);
		pcs.firePropertyChange("state", oldstate, state);
	}
	
	/**
	 * Get a string representation of the order.
	 */
	public String toString()
	{
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(isBuyOrder() ? "Processor '" : "Controller '");
		sbuf.append(gettitle());
		sbuf.append("'");
		return sbuf.toString();
	}

	//-------- property methods --------

	/**
	 * Add a PropertyChangeListener to the listener list.
	 * The listener is registered for all properties.
	 *
	 * @param listener The PropertyChangeListener to be added.
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		pcs.addPropertyChangeListener(listener);
	}

	/**
	 * Remove a PropertyChangeListener from the listener list.
	 * This removes a PropertyChangeListener that was registered
	 * for all properties.
	 *
	 * @param listener The PropertyChangeListener to be removed.
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		pcs.removePropertyChangeListener(listener);
	}

	/*public void setExecutionLa(IBuyDataService firstEntity) {
		// TODO Auto-generated method stub
		
	}*/

	
	

	
}
