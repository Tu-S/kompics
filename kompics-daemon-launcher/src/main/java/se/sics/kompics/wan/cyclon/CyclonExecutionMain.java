package se.sics.kompics.wan.cyclon;

import java.math.BigInteger;
import java.net.UnknownHostException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.PropertyConfigurator;

import se.sics.kompics.ChannelFilter;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.server.BootstrapServer;
import se.sics.kompics.p2p.bootstrap.server.BootstrapServerInit;
import se.sics.kompics.p2p.monitor.cyclon.server.CyclonMonitorServer;
import se.sics.kompics.p2p.monitor.cyclon.server.P2pMonitorServerInit;
import se.sics.kompics.p2p.orchestrator.P2pOrchestrator;
import se.sics.kompics.p2p.orchestrator.P2pOrchestratorInit;
import se.sics.kompics.p2p.simulator.KingLatencyMap;
import se.sics.kompics.p2p.simulator.cyclon.CyclonSimulatorPort;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.SimulationScenarioLoadException;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.CyclonConfiguration;
import se.sics.kompics.wan.util.LocalIPAddressNotFound;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.jetty.JettyWebServer;

/**
 * The <code>CyclonExecutionMain</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 */
public final class CyclonExecutionMain extends ComponentDefinition {
	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}
	private static SimulationScenario scenario;

	private static boolean isSlave = false;
	
	private static int slaveId, numSlaves;

	private static void usage() {
		System.out.println("Usage: <prog> slave|master slaveId numSlaves");
		System.exit(-1);
	}

	public static void main(String[] args) {

		long heapSize = Runtime.getRuntime().totalMemory();
		System.out.println("Heap Size= " + heapSize);

		if (args.length < 3) {
			usage();
		}
		if (args[0].compareTo("slave") == 0) {
			isSlave = true;
		} else if (args[0].compareTo("master") == 0) {
			isSlave = false;
		} else {
			usage();
		}
//		slaveId = Integer.parseInt(args[1]);
//		numSlaves = Integer.parseInt(args[2]);
		
		
		try {
			CyclonConfiguration.init(new String[] {}, CyclonConfiguration.class);
			Kompics.createAndStart(CyclonExecutionMain.class, 2);
		} catch (ConfigurationException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.exit(-1);
		}
	}

	@SuppressWarnings("unchecked")
	public CyclonExecutionMain() throws UnknownHostException, InterruptedException {
		P2pOrchestrator.setSimulationPortType(CyclonSimulatorPort.class);
		// create
		Component p2pOrchestrator = create(P2pOrchestrator.class);
		Component cyclonSimulator = create(CyclonSimulatorWan.class);
		Component jettyWebServer = create(JettyWebServer.class);

		try {
			scenario = SimulationScenario.load(System.getProperty("scenario"));
			System.out
					.println("For web access please go to " + CyclonConfiguration.getWebAddress());
			Thread.sleep(2000);

			trigger(new P2pOrchestratorInit(scenario, new KingLatencyMap()), p2pOrchestrator.getControl());

			se.sics.kompics.p2p.overlay.random.cyclon.CyclonConfiguration cyclonConfiguration = new se.sics.kompics.p2p.overlay.random.cyclon.CyclonConfiguration(
					CyclonConfiguration.getShuffleLength(), CyclonConfiguration.getCacheSize(),
					CyclonConfiguration.getShufflePeriod(),
					CyclonConfiguration.getShuffleTimeout(), CyclonConfiguration.getIdSpaceSize(),
					CyclonConfiguration.getBootstrapRequestPeerCount());

			trigger(new CyclonSimulatorWanInit(slaveId, numSlaves,
					CyclonConfiguration.getBootConfiguration(),
					CyclonConfiguration.getMonitorConfiguration(), cyclonConfiguration,
					CyclonConfiguration.getPeer0Address()), cyclonSimulator.getControl());
			
			trigger(CyclonConfiguration.getJettyWebServerInit(), jettyWebServer.getControl());


		} catch (SimulationScenarioLoadException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (LocalIPAddressNotFound e) {
			e.printStackTrace();
			System.exit(-1);
		}

		final class MessageDestinationFilter extends ChannelFilter<Message, Address> {
			public MessageDestinationFilter(Address address) {
				super(Message.class, address, true);
			}

			public Address getValue(Message event) {
				return event.getDestination();
			}
		}
		final class WebRequestDestinationFilter extends ChannelFilter<WebRequest, Integer> {
			public WebRequestDestinationFilter(Integer destination) {
				super(WebRequest.class, destination, false);
			}

			public Integer getValue(WebRequest event) {
				return event.getDestination();
			}
		}

		// connect

		if (isSlave == false) {
			Component bootstrapServer = create(BootstrapServer.class);
			Component cyclonMonitorServer = create(CyclonMonitorServer.class);

			trigger(new BootstrapServerInit(CyclonConfiguration.getBootConfiguration()),
					bootstrapServer.getControl());
			trigger(new P2pMonitorServerInit(CyclonConfiguration.getMonitorConfiguration()),
					cyclonMonitorServer.getControl());

			connect(bootstrapServer.getNegative(Network.class), p2pOrchestrator.getPositive(Network.class),
					new MessageDestinationFilter(CyclonConfiguration.getBootConfiguration()
							.getBootstrapServerAddress()));
			connect(bootstrapServer.getNegative(Timer.class), p2pOrchestrator.getPositive(Timer.class));
			connect(bootstrapServer.getPositive(Web.class), jettyWebServer.getNegative(Web.class),
					new WebRequestDestinationFilter(CyclonConfiguration.getBootConfiguration()
							.getBootstrapServerAddress().getId()));

			connect(cyclonMonitorServer.getNegative(Network.class), p2pOrchestrator
					.getPositive(Network.class), new MessageDestinationFilter(CyclonConfiguration
					.getMonitorConfiguration().getMonitorServerAddress()));
			connect(cyclonMonitorServer.getNegative(Timer.class), p2pOrchestrator.getPositive(Timer.class));
			connect(cyclonMonitorServer.getPositive(Web.class), jettyWebServer
					.getNegative(Web.class), new WebRequestDestinationFilter(CyclonConfiguration
					.getMonitorConfiguration().getMonitorServerAddress().getId()));

		}
		
		
		connect(cyclonSimulator.getPositive(Web.class), jettyWebServer.getNegative(Web.class));
		connect(cyclonSimulator.getNegative(Network.class), p2pOrchestrator.getPositive(Network.class));
		connect(cyclonSimulator.getNegative(Timer.class), p2pOrchestrator.getPositive(Timer.class));
		connect(cyclonSimulator.getNegative(CyclonSimulatorPort.class), p2pOrchestrator
				.getPositive(CyclonSimulatorPort.class));
	}
}
