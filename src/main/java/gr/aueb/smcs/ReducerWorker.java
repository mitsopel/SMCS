package gr.aueb.smcs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

import gr.aueb.smcs.Constants;
import org.apache.commons.collections4.queue.CircularFifoQueue;

public class ReducerWorker {

    static private final int PORT = Constants.REDUCER_SERVERSOCKET_PORT;

    Socket socketWithMaster = null;
    ObjectOutputStream outMaster = null;
    ObjectInputStream inMaster = null;
    String messageWithMaster;

    ServerSocket serverSocket;
    List<Connection> connectedMapWorkers;
    int mapWorkers;

    static List<Directions> listToReduce;

    Map <String,List<Directions>> directionsHashMap = new HashMap<String,List<Directions>>();


    public ReducerWorker() {
	this.serverSocket = null;
	this.connectedMapWorkers = new ArrayList<Connection>();
	this.mapWorkers = 0;
	listToReduce = new ArrayList<Directions>();
    }


    public static void main(String args[]){
	ReducerWorker reducerWorker = new ReducerWorker();
	reducerWorker.connectWithMaster();
	reducerWorker.startServer();
    }

    public void startServer() {
	new Thread(new Runnable() {
	    public void run() {
		openServer();
	    }
	}).start();
    }


    public void connectWithMaster() {
	//	System.out.println("Enter Master IP or press \"Enter\" to use default: ");
	//	Scanner scanner = new Scanner(System.in);
	//	String masterIP = scanner.nextLine();
	//
	//	if (masterIP == "\n"){
	//	    masterIP = Constants.IP_MASTER;
	//	}

	String masterIP = Constants.IP_MASTER;



	try {
	    socketWithMaster = new Socket(InetAddress.getByName(masterIP), Constants.MASTER_SERVERSOCKET_PORT);
	    outMaster = new ObjectOutputStream(socketWithMaster.getOutputStream());
	    inMaster = new ObjectInputStream(socketWithMaster.getInputStream());
	}
	catch (UnknownHostException unknownHost) {
	    System.err.println("You are trying to connect to an unknown host!");
	} 
	catch (IOException ioException) {
	    ioException.printStackTrace();
	}

	try {
	    outMaster.writeObject("ReducerWorker");
	    outMaster.flush();

	    messageWithMaster = (String) inMaster.readObject();
	    System.out.println("Master says: " +"\"" + messageWithMaster +"\"");

	} catch (ClassNotFoundException classNot) {
	    System.err.println("Data received in unknown format");
	} catch (IOException e) {
	    e.printStackTrace();
	}
	new Thread(new Runnable() {
	    public void run() {
		waitACKFromMasterToStartReduceProcess();
	    }
	}).start();
	//	scanner.close();
    }

    public void waitACKFromMasterToStartReduceProcess(){
	while (true){
	    try {
		String reduceACK = (String) inMaster.readObject();
		if (reduceACK.equalsIgnoreCase("reduceACK")){

		    System.out.println("\nMaster sent the ACK for starting the reduce process");

		    startReduceProcess();
		}
	    } catch (ClassNotFoundException | IOException e) {
		e.printStackTrace();
	    }
	}
    }

    public void startReduceProcess(){

	try {
	    outMaster.writeObject(listToReduce);
	    outMaster.flush();
	    System.out.println("Reduced List is sent to Master");

	} catch (IOException e) {
	    e.printStackTrace();
	}
	listToReduce.clear();
	
	
//	for (Directions direction : listToReduce){
//	    String currentSourceLat = String.valueOf(direction.getSourceLat());
//	    String currentSourceLong = String.valueOf(direction.getSourceLong());
//	    String currentDestinationLat = String.valueOf(direction.getDestinationLat());
//	    String currentDestinationLong = String.valueOf(direction.getDestinationLong());
//	    String currentKey = currentSourceLat + "," + currentSourceLong + "," + currentDestinationLat + "," + currentDestinationLong ;
//
//	    if (!directionsHashMap.containsKey(currentKey)){
//		directionsHashMap.put(currentKey, new ArrayList<Directions>());
//	    }
//	    directionsHashMap.get(currentKey).add(direction);
//	}
//
//	System.out.println("\nReduce is done, forwarding results to Master now");
//	
//	try {
//	    outMaster.writeObject(directionsHashMap);
//	} catch (IOException e) {
//	    e.printStackTrace();
//	}
	

//	directionsHashMap.clear();
//	remember to clear the reduceList after reducing
    }




    public void openServer() {

	Socket socket = null;
	ObjectOutputStream out = null;
	ObjectInputStream in = null;
	String connectionType = null;

	try {
	    serverSocket = new ServerSocket(PORT);
	    System.out.println("\nReducer Server is UP and waiting for connections on port " + PORT);
	    System.out.println("************************");

	    while (true) {
		socket = serverSocket.accept();
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());

		try {
		    connectionType = (String) in.readObject();
		    boolean isValid = connectionValidation(connectionType, socket, out, in);

		    if (!isValid) {
			out.writeObject("Not valid connection");
			out.flush();
			in.close();
			out.close();
			continue;
		    }

		    out.writeObject("Connection with REDUCER on port " + PORT + " is successful!");
		    out.flush();

		    System.out.println(connectionType + " with IP: " + socket.getInetAddress().getHostAddress()
			    + " is now connected with REDUCER");

		    // IF THE CONNECTION IS A MAPWORKER
		    new Thread(new Runnable() {
			public void run() {
			    Connection currentMapWorker = connectedMapWorkers
				    .get(connectedMapWorkers.size() - 1);
			    waitForMapWorkerDirectionsList(currentMapWorker);
			}
		    }).start();

		} catch (ClassNotFoundException classnot) {
		    System.err.println("Data received in unknown format");
		}
	    }
	} catch (IOException ioException) {
	    ioException.printStackTrace();
	} finally {
	    try {
		for (Connection mapWorker : connectedMapWorkers){
		    mapWorker.in.close();
		    mapWorker.out.close();
		    mapWorker.socket.close();
		}
		serverSocket.close();
	    } catch (IOException ioException) {
		ioException.printStackTrace();
	    }
	}
    }


    public void waitForMapWorkerDirectionsList(Connection mapWorker) {

	System.out.println("\n--------------------\nReducer is waiting to get a directionsList from Mapworker"
		+mapWorker.socket.getInetAddress().toString() +"....");

	ObjectInputStream in = mapWorker.getIn();

	while(true){
	    try {
		List <Directions> directionsList = (List <Directions>) in.readObject();
		System.out.println("Reducer received the found directionsList from Mapworker...");

		synchronized (ReducerWorker.listToReduce){
		    ReducerWorker.listToReduce.addAll(directionsList);
		}
	    } catch (ClassNotFoundException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }



    public boolean connectionValidation(String connectionType, Socket socket, ObjectOutputStream out,
	    ObjectInputStream in) {

	if (connectionType.equalsIgnoreCase("MapWorker")) {
	    connectedMapWorkers.add(new Connection(connectionType, socket, out, in));
	    mapWorkers++;
	} else {
	    System.out.println("Connection Refused to :" + socket.getInetAddress().toString());
	    return false;
	}
	return true;
    }

}





