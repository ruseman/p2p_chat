 # P2P chat application
 ## General concept
 The Tracker listens for connections on a port, from a configuration file.  When a client attempts to connect, it is put on hold, and added to a FIFO collection of connections.  When the Tracker has 2 or more connected clients, it pairs them, telling one of them to open a port to listen to.  Once the Client 