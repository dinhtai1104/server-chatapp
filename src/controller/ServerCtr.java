/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
 
import dao.UserDAO;
import model.ConnectionType;
import model.IPAddress;
import model.ObjectWrapper;
import model.User;
 
public class ServerCtr {
    private ServerSocket myServer;
    private ServerListening myListening;
    private ArrayList<ServerProcessing> myProcess;
    private IPAddress myAddress = new IPAddress("192.168.1.103",8888);  //default server host and port
     

     
    public ServerCtr(int serverPort){
        myProcess = new ArrayList<ServerProcessing>();
        myAddress.setPort(serverPort);
        openServer();       
    }
     
     
    private void openServer(){
        try {
            myServer = new ServerSocket(myAddress.getPort());
            myListening = new ServerListening();
            myListening.start();
            myAddress.setHost(InetAddress.getLocalHost().getHostAddress());
            //System.out.println("server started!");
            System.out.println("TCP server is running at the port " + myAddress.getPort() +"...");
        }catch(Exception e) {
            e.printStackTrace();;
        }
    }
     
    public void stopServer() {
        try {
            for(ServerProcessing sp:myProcess)
                sp.stop();
            myListening.stop();
            myServer.close();
            System.out.println("TCP server is stopped!");
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
     
//    public void publicClientNumber() {
//        ObjectWrapper data = new ObjectWrapper(ObjectWrapper.SERVER_INFORM_CLIENT_NUMBER, myProcess.size());
//        for(ServerProcessing sp : myProcess) {
//            sp.sendData(data);
//        }
//    }
     
    /**
     * The class to listen the connections from client, avoiding the blocking of accept connection
     *
     */
    class ServerListening extends Thread{
         
        public ServerListening() {
            super();
        }
         
        public void run() {
            try {
                while(true) {
                    Socket clientSocket = myServer.accept();
                    System.out.println("Connect: " + clientSocket.toString());
                    ServerProcessing sp = new ServerProcessing(clientSocket);
                    sp.start();
                    myProcess.add(sp);
//                    view.showMessage("Number of client connecting to the server: " + myProcess.size());
//                    publicClientNumber();
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
     
    /**
     * The class to treat the requirement from client
     *
     */
    class ServerProcessing extends Thread{
        private Socket mySocket;
        //private ObjectInputStream ois;
        //private ObjectOutputStream oos;
         
        public ServerProcessing(Socket s) {
            super();
            mySocket = s;
        }
         
        public void sendData(Object obj) {
            try {
                ObjectOutputStream oos= new ObjectOutputStream(mySocket.getOutputStream());
                oos.writeObject(obj);
            }catch(Exception e) {
                e.printStackTrace();
            }
        }
         
        public void run() { 
            try {
                while(true) {
                    ObjectInputStream ois = new ObjectInputStream(mySocket.getInputStream());
//                    ObjectOutputStream oos= new ObjectOutputStream(mySocket.getOutputStream());
                    Object o = ois.readObject();
                    if(o instanceof ObjectWrapper){
                        ObjectWrapper data = (ObjectWrapper)o;
                        if (data.getChoice() == ConnectionType.LOGIN) {
                            System.out.println("Login");
                            User user = (User)data.getData();
                            UserDAO dao = new UserDAO();
                            user = dao.checkLogin(user);
                            sendData(new ObjectWrapper(user, ConnectionType.REPLY_LOGIN));
                        } else if (data.getChoice() == ConnectionType.REGISTER){
                            User user = (User)data.getData();
                            UserDAO dao = new UserDAO();
                            String rs = dao.createAccount(user) == true ? "ok" : "false";
                            sendData(new ObjectWrapper(rs, ConnectionType.REPLY_REGISTER));
                        }
                       
                    }
                }
            }catch (EOFException | SocketException e) {             
                //e.printStackTrace();
                myProcess.remove(this);
//                view.showMessage("Number of client connecting to the server: " + myProcess.size());
//                publicClientNumber();
                try {
                    mySocket.close();
                }catch(Exception ex) {
                    ex.printStackTrace();
                }
                this.stop();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        new ServerCtr(9086);
    }
}