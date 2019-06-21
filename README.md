#  Simple Memcached System  #

A Java implementation of a simple memcached system, to quickly and efficiently retrieve directions for an desired destination.

Entities used:

Client: Asks for directions to a specific destination. Particularly, the query is to find directions from
source(Lat, Long) to destination (Lat, Long).

Master: Has a limited cache, which maintains directions about the last 100 queries that he received.
In case a client's query is not in his cache, he asks workers to lookup in their database. 
If none of the workers has directions, then a query to Google Directions API is sent. 

Workers: Connected to a database (i.e., txt files) which contains directions 
[from source (Lat, Long) to destination (Lat, Long), where (Lat, Long) have 2 decimals precission] about various routes.
Workers are waiting for the Master's quiries.

Reducer: Receives from Master, the directions found from all workers regarding a specific query, 
and finds the directions [(Lat, Long) have full precission here] that are more suitable (i.e., closer) to the client's query.

 