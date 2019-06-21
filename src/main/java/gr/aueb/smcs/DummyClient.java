package gr.aueb.smcs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class DummyClient {

    private static final long serialVersionUID = 2540262674905767116L;
    static Scanner scanner = new Scanner(System.in);

    Socket socketWithMaster;
    ObjectOutputStream outMaster;
    ObjectInputStream inMaster = null;
    String message;


    public DummyClient(){

	socketWithMaster = null;
	outMaster = null;
	inMaster = null;

    }



    public static void main(String args[]){
	DummyClient dummyClient = new DummyClient();
	dummyClient.connectWithMaster();
    }

    public void connectWithMaster() {

	System.out.println("Enter Master IP or press \"Enter\" to use default: ");
	String masterIP = scanner.nextLine();

	if (masterIP == "\n"){
	    masterIP = Constants.IP_MASTER;
	}	

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
	    outMaster.writeObject("Client");
	    outMaster.flush();

	    message = (String) inMaster.readObject();
	    System.out.println("Master: " +"\"" + message +"\"");
	    System.out.println();

	    sendQueryToMaster(outMaster);


	} catch (ClassNotFoundException classNot) {
	    System.err.println("Data received in unknown format");
	} catch (IOException e) {
	    e.printStackTrace();
	}

    }

    public void sendQueryToMaster(ObjectOutputStream out) {

	double sourceLat = -1;
	double sourceLong = -1;
	double destinationLat = -1;
	double destinationLong = -1;

	boolean isProperAnswer = false;

	while (!isProperAnswer){

	    System.out.println("Do you want to use default example? (yes/no)");
	    String answer = scanner.nextLine();
	    if (answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("\n")){
		sourceLat = 40.64531811;
		sourceLong = 22.9548371;
		destinationLat = 40.6314451;
		destinationLong = 22.9500305;
		//		isProperAnswer = true;
	    } 
	    else if (answer.equalsIgnoreCase("no") || answer.equalsIgnoreCase("n") ){

		System.out.println("Please enter SOURCE coordinates (Latitude,Longtitude)");
		String [] sourceLatLong = scanner.nextLine().split(",");

		if (sourceLatLong.length != 2){
		    System.out.println("Please enter Latitude and Longtitude values separated by a comma (,)");
		    sourceLatLong = scanner.nextLine().split(",");
		}

		sourceLat = Double.parseDouble(sourceLatLong[0]);
		sourceLong = Double.parseDouble(sourceLatLong[1]);

		System.out.println("Please enter DESTINATION coordinates (Latitude,Longtitude)");
		String [] destinationLatLong = scanner.nextLine().split(",");

		if (destinationLatLong.length != 2){
		    System.out.println("Please enter Latitude and Longtitude values separated by a comma (,)");
		    destinationLatLong = scanner.nextLine().split(",");
		}

		destinationLat = Double.parseDouble(destinationLatLong[0]);
		destinationLong = Double.parseDouble(destinationLatLong[1]);
		//		isProperAnswer = true;

	    } else {
		System.out.println("Please type \"yes\" or \"no\" ");
		continue;
	    }

	    try {
		out.writeObject(sourceLat);
		out.flush();
		out.writeObject(sourceLong);
		out.flush();
		out.writeObject(destinationLat);
		out.flush();
		out.writeObject(destinationLong);
		out.flush();

		getFoundDirections();

	    } catch (IOException e) {
		e.printStackTrace();
	    }

	    System.out.println("\nDo you want to run another example? (yes/no)");
	    String response = scanner.nextLine();
	    if (response.equalsIgnoreCase("yes") || response.equalsIgnoreCase("y") || response.equalsIgnoreCase("\n")){
		continue;
	    }  else if (response.equalsIgnoreCase("no") || response.equalsIgnoreCase("n") ){
		try {
		    out.writeObject(-1.0);
		    out.flush();
		    out.writeObject(-1.0);
		    out.flush();
		    out.writeObject(-1.0);
		    out.flush();
		    out.writeObject(-1.0);
		    out.flush();
		    inMaster.close();
		    outMaster.close();
		    socketWithMaster.close();
		    break;
		} catch (IOException e) {
		    e.printStackTrace();
		} 


	    }
	}
    }

    public void getFoundDirections (){

	try {
	    Directions directionsFound = (Directions) inMaster.readObject();

	    System.out.println("The directions found are for the query: " + directionsFound.sourceLat +"," +
		    directionsFound.sourceLong + "," + directionsFound.destinationLat + "," + directionsFound.destinationLong);

	    System.out.println("\n... and the json is: \n " + directionsFound.jsonObjectDirections );

	} catch (ClassNotFoundException | IOException e) {
	    e.printStackTrace();
	}

    }
}





