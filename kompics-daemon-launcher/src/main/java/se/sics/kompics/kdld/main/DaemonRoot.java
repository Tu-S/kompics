package se.sics.kompics.kdld.main;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.Daemon;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;


public class DaemonRoot extends ComponentDefinition {
	
	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}

	private static String commandScript;

	private Component time;
	private Component network;
	private Component daemon;

	private Address self;
	
	private static final Logger logger = LoggerFactory
	.getLogger(DaemonRoot.class);

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		
		Kompics.createAndStart(DaemonRoot.class);
	}

	/**
	 * Instantiates a new assignment0 group0.
	 */
	public DaemonRoot() {
		
		// create components
		time = create(JavaTimer.class);
		network = create(MinaNetwork.class);
		daemon = create(Daemon.class);			

		// handle possible faults in the components
		subscribe(handleFault, time.getControl());
		subscribe(handleFault, network.getControl());
		subscribe(handleFault, daemon.getControl());

		
		trigger(new MinaNetworkInit(self), network.getControl());
		
		connect(daemon.getNegative(Network.class), network
				.getPositive(Network.class));
		connect(daemon.getNegative(Timer.class), time
				.getPositive(Timer.class));
	}

	Handler<Fault> handleFault = new Handler<Fault>() {
		public void handle(Fault fault) {
			fault.getFault().printStackTrace(System.err);
		}
	};


}
