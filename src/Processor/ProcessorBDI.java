package Processor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;

import jadex.bdiv3.annotation.Belief;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalDropCondition;
import jadex.bdiv3.annotation.GoalParameter;
import jadex.bdiv3.annotation.GoalTargetCondition;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.RawEvent;
import jadex.bdiv3.annotation.Trigger;
import SDMCom.IBuyDataService;
import SDMCom.INegotiationAgent;
import SDMCom.INegotiationGoal;
import common.Gui;
import common.NegotiationReport;
import common.Order;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.runtime.ChangeEvent;
import jadex.bdiv3.runtime.impl.PlanFailureException;
import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IArgumentsResultsFeature;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.Tuple2;
import jadex.commons.future.CollectionResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IResultListener;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.AgentKilled;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;



/**
 * 
 */
@Agent
@RequiredServices(
{
	@RequiredService(name="buyservice", type=IBuyDataService.class, multiple=true),
	@RequiredService(name="clockser", type=IClockService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM))
})
@Arguments(@Argument(name="initial_orders", clazz=Order[].class))
public class ProcessorBDI implements INegotiationAgent
{
	@Agent
	protected IInternalAccess agent;
	
	@Belief
	protected List<NegotiationReport> reports = new ArrayList<NegotiationReport>();
	
	protected Gui gui;
	
	/**
	 *  The agent body.
	 */
	@AgentBody
	public void body()
	{
		Order[] ios = (Order[])agent.getComponentFeature(IArgumentsResultsFeature.class).getArguments().get("initial_orders");
		if(ios!=null)
		{
			for(Order o: ios)
			{
				createGoal(o);
			}
		}
		
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					gui = new Gui(agent.getExternalAccess());
				}
				catch(ComponentTerminatedException cte)
				{
 				}
			}
		});
	}
	
	/**
	 *  Called when agent terminates.
	 */
	@AgentKilled
	public void shutdown()
	{
		if(gui!=null)
		{
			gui.dispose();
		}
	}
	
	@Goal(recur=true, recurdelay=10000, unique=true)
	public class PurchaseData implements INegotiationGoal
	{
		@GoalParameter
		protected Order order;

		/**
		 *  Create a new PurchaseData. 
		 */
		public PurchaseData(Order order)
		{
			this.order = order;
		}

		/**
		 *  Get the order.
		 *  @return The order.
		 */
		public Order getOrder()
		{
			return order;
		}
		
		@GoalDropCondition(parameters="order")
		public boolean checkDrop()
		{
			return order.getState().equals(Order.FAILED);
		}
		
		@GoalTargetCondition(parameters="order")
		public boolean checkTarget()
		{
			return Order.DONE.equals(order.getState());
		}
	}
	
	/**
	 * 
	 */
	@Belief(rawevents={@RawEvent(ChangeEvent.GOALADOPTED), @RawEvent(ChangeEvent.GOALDROPPED), 
		@RawEvent(ChangeEvent.PARAMETERCHANGED)})
	public List<Order> getOrders()
	{
//		System.out.println("getOrders belief called");
		List<Order> ret = new ArrayList<Order>();
		Collection<PurchaseData> goals = agent.getComponentFeature(IBDIAgentFeature.class).getGoals(PurchaseData.class);
		for(PurchaseData goal: goals)
		{
			ret.add(goal.getOrder());
		}
		return ret;
	}
	
	/**
	 *  Get the current time.
	 */
	protected long getTime()
	{
		IClockService cs = (IClockService)agent.getComponentFeature(IRequiredServicesFeature.class).getRequiredService("clockser").get();
		return cs.getTime();
	}
	
	/**
	 * 
	 */
	@Plan(trigger=@Trigger(goals=PurchaseData.class))
	protected void purchaseData(PurchaseData goal)
	{
		
	//	
		Order order = goal.getOrder();
		
		double time_span = order.getDeadline().getTime() - order.getStartTime();
		double elapsed_time = getTime() - order.getStartTime();
		//String datatype = order.gettitle();
		//double price_span = order.getLimit() - order.getCheckRequest();
		//if( order.getId() == (order.getrealId())){
	//	int acceptable_Id = (int)(elapsed_time / time_span)+ order.getId();
		
		if ((order.getTransformationcode() <= 2))
			{
			int acceptable_Request = order.getTransformationcode();
			//if (order.getCheckRequest() =="notPermitted" ){System.out.print("not according to GDPR");}
		// Find available processor agents.
		IBuyDataService[]	services = agent.getComponentFeature(IRequiredServicesFeature.class).getRequiredServices("buyservice").get().toArray(new IBuyDataService[0]);
		if(services.length == 0)
		{
//			System.out.println("No processor found, purchase failed.");
			generateNegotiationReport(order, null, acceptable_Request);
			throw new PlanFailureException();
			
		}
		

		// Initiate a call-for-proposal.
		Future<Collection<Tuple2<IBuyDataService, Integer>>>	cfp	= new Future<Collection<Tuple2<IBuyDataService, Integer>>>();
		final CollectionResultListener<Tuple2<IBuyDataService, Integer>>	crl	= new CollectionResultListener<Tuple2<IBuyDataService, Integer>>(services.length, true,
			new DelegationResultListener<Collection<Tuple2<IBuyDataService, Integer>>>(cfp));
		for(int i=0; i<services.length; i++)
		{
			final IBuyDataService	Processor	= services[i];
			Processor.callForProposal(order.gettitle()).addResultListener(new IResultListener<Integer>()
			{
				public void resultAvailable(Integer result)
				{
					crl.resultAvailable(new Tuple2<IBuyDataService, Integer>(Processor, result));
				}
				
				public void exceptionOccurred(Exception exception)
				{
					crl.exceptionOccurred(exception);
				}
			});
		}
		// Sort results by id.
		Tuple2<IBuyDataService, Integer>[]	proposals	= cfp.get().toArray(new Tuple2[0]);
		Arrays.sort(proposals, new Comparator<Tuple2<IBuyDataService, Integer>>()
		{
			public int compare(Tuple2<IBuyDataService, Integer> o1, Tuple2<IBuyDataService, Integer> o2)
			{
				return o1.getSecondEntity().compareTo(o2.getSecondEntity());
			}
			});

		// Do we have a winner?
		//int CheckRequest1 = Integer.parseInt(CheckRequest);
		
		
		if(proposals.length>0 && proposals[0].getSecondEntity().intValue() == acceptable_Request)
		{
			proposals[0].getFirstEntity().acceptProposal(order.gettitle(), proposals[0].getSecondEntity().intValue()).get();
			System.out.println("result: "+ proposals[0].getFirstEntity());
			System.out.println("result: "+ acceptable_Request);
			generateNegotiationReport(order, proposals, acceptable_Request);
			
			// If contract-net succeeds, store result in order object.
			order.setState(Order.Process);
			order.setExecutioncode(proposals[0].getSecondEntity());
			//System.out.println(datatype);
			order.setExecutionDate(new Date(getTime()));
		}
		else
		{
				
			
			generateNegotiationReport(order, proposals,acceptable_Request);
			
			throw new PlanFailureException();
		}
		//System.out.println("result: "+cnp.getParameter("result").getValue());
	}
	
		
	}
	
	/**
	*  Generate and add a negotiation report.
	*/
	protected void generateNegotiationReport(Order order, Tuple2<IBuyDataService, Integer>[] proposals, Integer acceptable_Request)
	{
		String report = "Accepable code: "+acceptable_Request+", proposals: ";
	//	String report
		if(proposals!=null)
		{
			for(int i=0; i<proposals.length; i++)
			{
				report += proposals[i].getSecondEntity()+"-"+proposals[i].getFirstEntity().toString();
				if(i+1<proposals.length)
					report += ", ";
			}
		}
		else
		{
			report	+= "No controller found, purchase failed.";
		}
		NegotiationReport nr = new NegotiationReport(order, report, getTime());
		//System.out.println("REPORT of agent: "+getAgentName()+" "+report);
		reports.add(nr);
	}

	/**
	 *  Get the agent.
	 *  @return The agent.
	 */
	public IInternalAccess getAgent()
	{
		return agent;
	}
	
	/**
	 *  Create a purchase or sell oder.
	 */
	public void createGoal(Order order)
	{
		PurchaseData goal = new PurchaseData(order);
		agent.getComponentFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal);
	}
	
	/**
	 *  Get all purchase or sell goals.
	 */
	public Collection<INegotiationGoal> getGoals()
	{
		return (Collection)agent.getComponentFeature(IBDIAgentFeature.class).getGoals(PurchaseData.class);
	}
	
	/**
	 *  Get all reports.
	 */
	public List<NegotiationReport> getReports(Order order)
	{
		List<NegotiationReport> ret = new ArrayList<NegotiationReport>();
		for(NegotiationReport rep: reports)
		{
			if(rep.getOrder().equals(order))
			{
				ret.add(rep);
			}
		}
		return ret;
	}
}







