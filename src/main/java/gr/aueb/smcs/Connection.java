package gr.aueb.smcs;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;

public class Connection {

    String name; //client or map worker or reduce worker
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    BigInteger hash;
    
    double querySourceLat;
    double querySourceLong;
    double queryDestinationLat;
    double queryDestinationLong;
    

    public Connection(String name, Socket socket, ObjectOutputStream out, ObjectInputStream in) {
	this.name = name;
	this.socket = socket;
	this.out = out;
	this.in = in;
	this.hash = null;
    }



    public double getQuerySourceLat() {
        return querySourceLat;
    }



    public void setQuerySourceLat(double querySourceLat) {
        this.querySourceLat = querySourceLat;
    }



    public double getQuerySourceLong() {
        return querySourceLong;
    }



    public void setQuerySourceLong(double querySourceLong) {
        this.querySourceLong = querySourceLong;
    }



    public double getQueryDestinationLat() {
        return queryDestinationLat;
    }



    public void setQueryDestinationLat(double queryDestinationLat) {
        this.queryDestinationLat = queryDestinationLat;
    }



    public double getQueryDestinationLong() {
        return queryDestinationLong;
    }



    public void setQueryDestinationLong(double queryDestinationLong) {
        this.queryDestinationLong = queryDestinationLong;
    }



    public String getName() {
	return name;
    }



    public void setName(String name) {
	this.name = name;
    }



    public Socket getSocket() {
	return socket;
    }



    public void setSocket(Socket socket) {
	this.socket = socket;
    }



    public ObjectOutputStream getOut() {
	return out;
    }



    public void setOut(ObjectOutputStream out) {
	this.out = out;
    }



    public ObjectInputStream getIn() {
	return in;
    }



    public void setIn(ObjectInputStream in) {
	this.in = in;
    }



    public BigInteger getHash() {
	return hash;
    }


    public void setHash(BigInteger hash) {
	this.hash = hash;
    }


}
