package gr.aueb.smcs;


import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

import gr.aueb.smcs.Constants;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class Master {

    static private final int PORT = Constants.MASTER_SERVERSOCKET_PORT;

    ServerSocket serverSocket;
    List<Connection> establishedConnections;
    Queue<Directions> cache;

    int mapWorkers;
    Connection maxHashMapper;


    public Master() {
	this.serverSocket = null;
	this.establishedConnections = new ArrayList<Connection>();
	this.mapWorkers = 0;
	this.maxHashMapper = null;
	this.cache = new CircularFifoQueue<Directions>(Constants.CACHE_SIZE);
    }

    public static void main(String args[]) {
	Master master = new Master();
	master.initializeCache();
	master.startServer();

    }

    public void startServer() {
	new Thread(new Runnable() {
	    public void run() {
		openServer();
	    }
	}).start();
    }

    public void openServer() {

	Socket socket = null;
	ObjectOutputStream out = null;
	ObjectInputStream in = null;
	String connectionType = null;

	try {
	    serverSocket = new ServerSocket(PORT);
	    System.out.println("Master Server is UP and waiting for connections on port " + PORT);
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

		    out.writeObject("Connection with Master on port " + PORT + " is successful!");
		    out.flush();

		    System.out.println(connectionType + " with IP: " + socket.getInetAddress().getHostAddress()
			    + " is now connected with Master");

		    // IF THE CONNECTION IS A CLIENT
		    if (connectionType.equalsIgnoreCase("Client")) {
			new Thread(new Runnable() {
			    public void run() {
				Connection currentClient = establishedConnections
					.get(establishedConnections.size() - 1);
				waitForQueries(currentClient);
			    }
			}).start();

		    }

		} catch (ClassNotFoundException classnot) {
		    System.err.println("Data received in unknown format");
		}
	    }

	} catch (IOException ioException) {
	    ioException.printStackTrace();
	} finally {
	    try {
		for (Connection connection : establishedConnections){
		    connection.in.close();
		    connection.out.close();
		    connection.socket.close();
		}
		serverSocket.close();
	    } catch (IOException ioException) {
		ioException.printStackTrace();
	    }
	}
    }

    public void waitForQueries(Connection client) {

	System.out.println("\n--------------------\nMaster is waiting for Client queries...");
	ObjectInputStream in = client.getIn();

	while(true){
	    try {
		double sourceLat = (Double) in.readObject();
		double sourceLong = (Double) in.readObject();
		double destinationLat = (Double) in.readObject();
		double destinationLong = (Double) in.readObject();

		client.querySourceLat = sourceLat;
		client.querySourceLong = sourceLong;
		client.queryDestinationLat = destinationLat;
		client.queryDestinationLong = destinationLong;

		System.out.println("\nClient query coordinates are: " + client.querySourceLat +"," 
			+ client.querySourceLong  + "," + client.queryDestinationLat + "," + client.queryDestinationLong);


		if (sourceLat == -1.0) {
		    System.out.println("\nThe Client disconnected or something went wrong :/");
		    System.out.println("--------------------");

		    client.in.close();
		    client.out.close();
		    client.socket.close();
		    break;
		}

		System.out.println("Searching in CACHE...");
		Directions foundDirectionsInCache = searchCache(sourceLat, sourceLong, destinationLat, destinationLong);

		if (foundDirectionsInCache == null) {

		    String string = null;
		    System.out.println("The cooridinates were NOT found in CACHE, asking MapWorkers now... ");
		    askMapWorkers(sourceLat, sourceLong, destinationLat, destinationLong);

		    boolean isACK = waitMapWorkersACK();
		    if (isACK){
			sendACKtoReducer();
		    } else {
			System.out.println("Something went wrong with MapWorkers ACK ");
		    }

		    ArrayList<Directions> reducedDirectionsList = getResultsFromReducer();
		    System.out.println("\nThe Reduced_List is received from Reducer");

		    if (reducedDirectionsList.isEmpty()){
			try {

			    String googleDirections = Utils.askGoogleAPI(sourceLat, sourceLong, destinationLat, destinationLong);

			    JSONObject jsonObjectDirections= (JSONObject) Utils.jsonParser.parse(googleDirections);

			    Directions newDirections = new Directions(sourceLat, sourceLong, destinationLat, destinationLong, jsonObjectDirections);

			    cache.add(newDirections);

			    sendDirectionsToClient(client,newDirections);

			    BigInteger queryHash = Utils.calculateHash(String.valueOf(sourceLat + sourceLong),
				    String.valueOf(destinationLat + destinationLong));

			    Connection appropriateMapWorker = findAppropriateMapWorker(queryHash);

			    // TODO find appropriate Mapper to save in his database

			} catch (IOException | ParseException e) {
			    e.printStackTrace();
			}

		    } else {
			Directions bestDirections = findBestDirectionsOption(reducedDirectionsList);
			sendDirectionsToClient(client,bestDirections);
		    } 
		} else if (foundDirectionsInCache != null) {
		    sendDirectionsToClient(client,foundDirectionsInCache);
		} 
		

	    } catch (ClassNotFoundException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
	}
	
    }





    public void askMapWorkers(double sourceLat, double sourceLong, double destinationLat, double destinationLong) {
	for (Connection connection : establishedConnections){
	    if (connection.name.equalsIgnoreCase("MapWorker")){
		ObjectOutputStream out = connection.getOut();
		try {
		    out.writeObject(sourceLat);
		    out.flush();
		    out.writeObject(sourceLong);
		    out.flush();
		    out.writeObject(destinationLat);
		    out.flush();
		    out.writeObject(destinationLong);
		    out.flush();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}
    }

    public boolean waitMapWorkersACK(){
	int count = 0;

	for (Connection connection: establishedConnections){
	    if (connection.name.equalsIgnoreCase("MapWorker")){
		ObjectInputStream in = connection.getIn();
		try {
		    String ACK = (String) in.readObject();
		    if(ACK.equalsIgnoreCase("ACK")){
			count++;
		    }
		} catch (ClassNotFoundException | IOException e) {
		    e.printStackTrace();
		}
		if (count == mapWorkers){
		    System.out.println("\nACK from ALL MapWorkers is received, sending ACK reducer now");
		    //		    sendACKtoReducer();
		    count = 0;
		    return true;
		}
	    }
	}
	return false;
    }

    public void sendACKtoReducer(){
	for (Connection connection: establishedConnections){
	    if (connection.name.equalsIgnoreCase("ReducerWorker")){
		try {
		    connection.out.writeObject("reduceACK");
		    connection.out.flush();
		    System.out.println("ACK to Reducer is sent");

		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}
    }



    public ArrayList<Directions> getResultsFromReducer(){
	ArrayList<Directions> reducedDirectionsList = null;
	for (Connection connection: establishedConnections){
	    if (connection.name.equalsIgnoreCase("ReducerWorker")){
		ObjectInputStream in = connection.getIn();
		try {
		    reducedDirectionsList = (ArrayList<Directions>) in.readObject();
		} catch (IOException | ClassNotFoundException e) {
		    e.printStackTrace();
		}
	    }
	}
	return reducedDirectionsList;
    }



    public static double calculateDistance (double lat1, double lng1, double lat2, double lng2) {
	double earthRadius = 6371.0; //  kilometers (or 3958.75 miles)
	double dLat = Math.toRadians(lat2-lat1);
	double dLng = Math.toRadians(lng2-lng1);
	double sindLat = Math.sin(dLat / 2);
	double sindLng = Math.sin(dLng / 2);
	double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
	* Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
	double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	double dist = earthRadius * c;

	return dist;
    }


    public Directions findBestDirectionsOption(ArrayList<Directions> reducedDirectionsList){

	Connection client = null;
	double minDistance = 1000000000;
	Directions bestDirections = null;

	for (Connection connection : establishedConnections){
	    if (connection.getName().equalsIgnoreCase("Client")){
		client = connection;
	    }
	}

	for (Directions directions : reducedDirectionsList){

	    double distanceWithoutDirections = calculateDistance(directions.sourceLat, directions.sourceLong,
		    client.querySourceLat, client.querySourceLong) + 
		    calculateDistance(directions.destinationLat, directions.destinationLong,
			    client.queryDestinationLat , client.queryDestinationLong);

	    double distanceWithDirections = calculateDistance(directions.sourceLat,directions.sourceLong,
		    directions.destinationLat,directions.destinationLong);

	    double totalDistance = distanceWithoutDirections + distanceWithDirections;

	    if(totalDistance < minDistance){
		minDistance = totalDistance;
		bestDirections = directions;
	    }
	}
	return bestDirections;
    }




    public void sendDirectionsToClient(Connection client, Directions foundDirections) throws IOException {
	client.out.writeObject(foundDirections);
	client.out.flush();
    }




    public Connection findAppropriateMapWorker(BigInteger queryHash){

	while (queryHash.compareTo(maxHashMapper.hash) >0){
	    queryHash = queryHash.mod(maxHashMapper.hash);
	}	


	for (Connection connection : establishedConnections){
	    if (connection.name.equalsIgnoreCase("MapWorker")){
		BigInteger currentMapWorkerHash = connection.getHash();
		if(currentMapWorkerHash.compareTo(queryHash) > 0){
		    return connection;
		} 
	    }
	}
	return null;
    }






    public Directions searchCache(double sourceLat, double sourceLong, double destinationLat, double destinationLong) {

	for (Directions directions : cache) {
	    double currentSourceLat = directions.getSourceLat();
	    double currentSourceLong = directions.getSourceLong();
	    double currentDestinationLat = directions.getDestinationLat();
	    double currentDestinationLong = directions.getDestinationLong();

	    if (currentSourceLat == sourceLat && currentSourceLong == sourceLong
		    && currentDestinationLat == destinationLat && currentDestinationLong == destinationLong) {
		System.out.println("Coordinates were found in CACHE! ..sending them  back to Client now =)");

		return directions;
	    }
	}
	return null;
    }

    public void initializeCache() {

	String jsonStringDirections = "";
	double sourceLat = 0;
	double sourceLong = 0;
	double destinationLat = 0;
	double destinationLong = 0;

	try {
	    Scanner scanner = new Scanner(
		    new FileReader("C:/Users/Ellen/Documents/Eclipse/workspace/SMCS/Data/MasterCache.txt"));
	    String line = "";

	    while (scanner.hasNextLine()) {
		line = scanner.nextLine();

		if (line.equals("END")) {
		    JSONObject jsonObjectDirections = (JSONObject) Utils.jsonParser.parse(jsonStringDirections);
		    Directions directions = new Directions(sourceLat, sourceLong, destinationLat, destinationLong,
			    jsonObjectDirections);
		    cache.add(directions);
		    jsonStringDirections = "";
		    continue;
		} else {
		    sourceLat = Double.parseDouble(line.substring(0, 11));
		    sourceLong = Double.parseDouble(line.substring(12, 22));
		    destinationLat = Double.parseDouble(line.substring(23, 33));
		    destinationLong = Double.parseDouble(line.substring(34, 44));

		    jsonStringDirections += line.substring(45);

		}
	    }
	    scanner.close();
	    //	    System.out.println("Cache size is " + cache.size());
	} catch (IOException | ParseException e) {
	    System.out.println("Problem during cache initialization");
	    e.printStackTrace();
	}
    }

    public boolean connectionValidation(String connectionType, Socket socket, ObjectOutputStream out,
	    ObjectInputStream in) {

	if (connectionType.equalsIgnoreCase("MapWorker") || connectionType.equalsIgnoreCase("ReducerWorker")
		|| connectionType.equalsIgnoreCase("Client")) {

	    establishedConnections.add(new Connection(connectionType, socket, out, in));

	    boolean IsMapWorker = connectionType.equalsIgnoreCase("MapWorker");

	    if (IsMapWorker) {
		String IP = socket.getInetAddress().toString();
		String port = Integer.toString(socket.getPort());
		Connection currentMapWorker = establishedConnections.get(establishedConnections.size() - 1);
		BigInteger currentHash = Utils.calculateHash(IP, port);
		currentMapWorker.setHash(currentHash);
		mapWorkers++;

		if (maxHashMapper == null){
		    maxHashMapper = currentMapWorker;
		} else {
		    if (maxHashMapper.hash.compareTo(currentHash) < 0 ){
			maxHashMapper = currentMapWorker;
		    }  
		}
	    }
	} else {
	    System.out.println("Connection Refused to :" + socket.getInetAddress().toString());
	    return false;
	}
	return true;
    }

}
