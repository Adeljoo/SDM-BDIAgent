package Controller;

	import java.util.ArrayList;
	import java.util.Collection;
	import java.util.Collections;
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
	import jadex.bridge.service.annotation.Service;
	import jadex.bridge.service.component.IRequiredServicesFeature;
	import jadex.bridge.service.types.clock.IClockService;
	import jadex.commons.future.Future;
	import jadex.commons.future.IFuture;
	import jadex.commons.future.IResultListener;
	import jadex.micro.annotation.Agent;
	import jadex.micro.annotation.AgentBody;
	import jadex.micro.annotation.AgentKilled;
	import jadex.micro.annotation.Argument;
	import jadex.micro.annotation.Arguments;
	import jadex.micro.annotation.Binding;
	import jadex.micro.annotation.ProvidedService;
	import jadex.micro.annotation.ProvidedServices;
	import jadex.micro.annotation.RequiredService;
	import jadex.micro.annotation.RequiredServices;

	@Agent
	@Service
	@ProvidedServices(@ProvidedService(type=IBuyDataService.class))
	@RequiredServices(@RequiredService(name="clockser", type=IClockService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)))
	@Arguments(@Argument(name="initial_orders", clazz=Order[].class))
	public class ControllerBDI implements IBuyDataService, INegotiationAgent
	{
	//	private static final Integer Transformationcode = null;
	//	private static final String CheckRequest = null;
		
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
		public class SellData implements INegotiationGoal
		{
			@GoalParameter
			protected Order order;

			/**
			 *  Create a new SellData. 
			 */
			public SellData(Order order)
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
		
		@Goal
		public class MakeProposal
		{
			protected String cfp;
			protected int proposal;
			
			/**
			 *  Create a new MakeProposal. 
			 */
			public MakeProposal(String cfp)
			{
				this.cfp = cfp;
			}

			/**
			 *  Get the cfp.
			 *  @return The cfp.
			 */
			public String getCfp()
			{
				return cfp;
			}

			/**
			 *  Get the proposal.
			 *  @return The proposal.
			 */
			public int getProposal()
			{
				return proposal;
			}

			/**
			 *  Set the proposal.
			 *  @param start The proposal to set.
			 */
			public void setProposal(int proposal)
			{
				this.proposal = proposal;
			}
			
		}
		
		@Goal
		public class ExecuteTask
		{
			protected String cfp;
			protected int proposal;
			
			/**
			 *  Create a new ExecuteTask. 
			 */
			public ExecuteTask(String cfp, int proposal)
			{
				super();
				this.cfp = cfp;
				this.proposal = proposal;
			}

			/**
			 *  Get the cfp.
			 *  @return The cfp.
			 */
			public String getCfp()
			{
				return cfp;
			}

			/**
			 *  Get the proposal.
			 *  @return The proposal.
			 */
			public int getProposal()
			{
				return proposal;
			}
		}

		/**
		 * 
		 */
		@Belief(rawevents={@RawEvent(ChangeEvent.GOALADOPTED), @RawEvent(ChangeEvent.GOALDROPPED)})
		public List<Order> getOrders()
		{
			List<Order> ret = new ArrayList<Order>();
			Collection<SellData> goals = agent.getComponentFeature(IBDIAgentFeature.class).getGoals(SellData.class);
			for(SellData goal: goals)
			{
				ret.add(goal.getOrder());
			}
			return ret;
		}
		
		/**
		 * 
		 */
		public List<Order> getOrders(String title)
		{
			List<Order> ret = new ArrayList<Order>();
			Collection<SellData> goals = agent.getComponentFeature(IBDIAgentFeature.class).getGoals(SellData.class);
			for(SellData goal: goals)
			{
				if(title==null || title.equals(goal.getOrder().gettitle()))
				{
					ret.add(goal.getOrder());
				}
			}
			return ret;
		}
		
		@Plan(trigger=@Trigger(goals=MakeProposal.class))
		protected void makeProposal(MakeProposal goal)
		{
			final long time = getTime();
			List<Order> orders = getOrders(goal.getCfp());
			
			if(orders.isEmpty())
				throw new PlanFailureException();
				
			Collections.sort(orders, new Comparator<Order>()
			{
				public int compare(Order o1, Order o2)
				{
					double prio1 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
					double prio2 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
					return prio1>prio2? 1: prio1<prio2? -1: o1.hashCode()-o2.hashCode();
				}
			});
			Order order = orders.get(0);
			
			// Use most urgent order for preparing proposal.
//			if(suitableorders.length > 0)
			if(order!=null)
			{
//					Order order = suitableorders[0];
			//	if (order.getCheckRequest()=="notallowed"){System.out.print("not according to GDPR");}
				double time_span = order.getDeadline().getTime() - order.getStartTime();
				double elapsed_time = getTime() - order.getStartTime();
				//double price_span = order.getLimit() - order.getCheckRequest();
				if ((order.getTransformationcode()<= 2))
						{ 
					
					int acceptable_Request =  order.getTransformationcode();
		        				
				agent.getLogger().info(agent.getComponentIdentifier().getName()+" proposed: " + acceptable_Request);
				
				// Store proposal data in plan parameters.
				goal.setProposal(acceptable_Request);
				
				String report = "Made proposal: "+ acceptable_Request;
				NegotiationReport nr = new NegotiationReport(order, report, getTime());
				reports.add(nr);
			}}
			/*String error = order.getCheckRequest();
			for (error ="notallowed";;){System.out.print("not according to GDPR");}*/
			}
		
		
		@Plan(trigger=@Trigger(goals=ExecuteTask.class))
		protected void executeTask(ExecuteTask goal)
		{
			// Search suitable open orders.
			final long time = getTime();
			List<Order> orders = getOrders(goal.getCfp());
			
			if(orders.isEmpty())
				throw new PlanFailureException();
				
			Collections.sort(orders, new Comparator<Order>()
			{
				public int compare(Order o1, Order o2)
				{
					double prio1 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
					double prio2 = (time-o1.getStartTime()) / (o1.getDeadline().getTime()-o1.getStartTime());
					return prio1>prio2? 1: prio1<prio2? -1: o1.hashCode()-o2.hashCode();
				}
			});
			Order order = orders.get(0);
			
			// Use most urgent order for preparing proposal.
		//	if(suitableorders.length > 0)
			if(order!=null)
			{
		//		Order order = suitableorders[0];
				
				double time_span = order.getDeadline().getTime() - order.getStartTime();
				double elapsed_time = getTime() - order.getStartTime();
				//String datatype = order.getLabel();
				if ((order.getTransformationcode()<= 2)) 
					{
						int acceptable_Request = order.getTransformationcode();
						//goal.setProposal(acceptable_Request);
						// Extract order data.
						//int Id = goal.getProposal();

						//if (Id >= acceptable_Id)
			
				// Extract order data.
			int code = goal.getProposal();
				
				if( code == acceptable_Request)
				{
		//			getLogger().info("Execute order plan: "+price+" "+order);
		
					// Initiate payment and delivery.
					// IGoal pay = createGoal("payment");
					// pay.getParameter("order").setValue(order);
					// dispatchSubgoalAndWait(pay);
					// IGoal delivery = createGoal("delivery");
					// delivery.getParameter("order").setValue(order);
					// dispatchSubgoalAndWait(delivery);
				
					// Save successful transaction data.
					order.setState(Order.Collect);
					order.setExecutioncode(code);
					order.setExecutionDate(new Date(getTime()));
					
					String report = "Applied for Transformation number: "+ code;
					NegotiationReport nr = new NegotiationReport(order, report, getTime());
					reports.add(nr);
				}
				else
				{
				//	if (order.getCheckRequest() =="notPermitted" ){System.out.print("not according to GDPR");}
					throw new PlanFailureException();
				}
			}
			}
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
		 *  Ask the controller for a a quote on a Data.
		 *  @param label	The Data label.
		 *  @return The Req.
		 */
		public IFuture<Integer> callForProposal(String title)
		{
			final Future<Integer>	ret	= new Future<Integer>();
			final MakeProposal goal = new MakeProposal(title);
			agent.getComponentFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal).addResultListener(new IResultListener<Object>()
			{
				public void resultAvailable(Object result)
				{
					ret.setResult(Integer.valueOf(goal.getProposal()));
				}
				
				public void exceptionOccurred(Exception exception)
				{
					ret.setException(exception);
				}
			});
			return ret;
		}

		/**
		 *  Buy a Data
		 *  @param label	The Data label.
		 *  @param Req 	The norm to apply.
		 *  @return A future indicating if the transaction was successful.
		 */
		public IFuture<Void> acceptProposal(String title, int code)
		{  // Integer i = Integer.valueOf(Req);
			final Future<Void>	ret	= new Future<Void>();
			ExecuteTask goal = new ExecuteTask(title, code);
			agent.getComponentFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal).addResultListener(new IResultListener<Object>()
			{
				public void resultAvailable(Object result)
				{
					ret.setResult(null);
				}
				
				public void exceptionOccurred(Exception exception)
				{
					ret.setException(exception);
				}
			});
			return ret;
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
			SellData goal = new SellData(order);
			agent.getComponentFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(goal);
		}
		
		/**
		 *  Get all purchase or sell goals.
		 */
		@SuppressWarnings("unchecked")
		public Collection<INegotiationGoal> getGoals()
		{
			return (Collection)agent.getComponentFeature(IBDIAgentFeature.class).getGoals(SellData.class); 
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

		

	
		
		/*@Override
		public IFuture<Void> acceptProposal(String title, int i) {
			// TODO Auto-generated method stub
			return null;
		}
*/
//		@Override
//		public IFuture<Void> acceptProposal(String label, String Req) {
//			// TODO Auto-generated method stub
//			return null;
		}
	


