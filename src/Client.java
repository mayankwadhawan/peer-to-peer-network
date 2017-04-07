import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Client {
private static int clientServerPort = 0; // The server will be listening on
private static int downloadPort= 0;
Socket requestSocket; // socket connect to the server
DataOutputStream dout; // stream write to the socket
DataInputStream din; // stream read from the socket
String message; // message send to the server
String MESSAGE; // capitalized message read from the server
int counter = 0;
static int clientNumber=0;
private static  int sPort=0; 
static ArrayList<Integer> packetList;
static int totalPackets=0;
static String newFileName="";
static boolean booleanStartLoops = true;
void run() {
	try {
		getServerPort();
		requestSocket = new Socket("localhost", sPort);
		packetList=new ArrayList<Integer>();
		dout = new DataOutputStream(requestSocket.getOutputStream());
		dout.flush();
		din = new DataInputStream(requestSocket.getInputStream());
		BufferedReader bufferedReader = new BufferedReader(
			new InputStreamReader(System.in));
		
		boolean lastCheck=false;
		while (true && !lastCheck) {
			//Thread.sleep(1000);
			//dout.writeObject(arr);

			//sending client number
			if(clientNumber==0){
				clientNumber = din.readInt();
				newFileName = din.readUTF();
			}
	

			//packet number
			int packetNumber = din.readInt(); 
			packetList.add(packetNumber);
			Collections.sort(packetList);
			System.out.println("DOWNLOAD: Received chunk "+packetNumber+" from main Server");
			
			
			//packet length
			int length = din.readInt(); 
			byte[] message = new byte[length];

			//packet data
			if(length>0) {
				din.readFully(message, 0, message.length);
			}
			saveChunk(message,packetNumber);

			//check if last packet
			lastCheck = din.readBoolean();
			totalPackets = din.readInt();
		}
		
		portSetUp();
		
		System.out.println("localhost server running on port "+sPort);
	} catch (ConnectException e) {
		System.err.println("Connection refused. You need to initiate a server first.");
	}catch (UnknownHostException unknownHost) {
		System.err.println("You are trying to connect to an unknown host!");
	} catch (IOException ioException) {
		ioException.printStackTrace();
	} catch (Exception exception) {
		exception.printStackTrace();
	}finally {
		try {
			din.close();
			dout.close();
			requestSocket.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}
}
private static void portSetUp() throws IOException {
	File fileName = new File("../config.txt");
	BufferedReader br = new BufferedReader(new FileReader(fileName));

	String line = null;
	int lineNo=0;
	while ((line = br.readLine()) != null) {
		String[] data=line.split("\\t");
		for(int i=0;i<data.length;i++){
			if(lineNo==0){
				sPort=Integer.parseInt(data[i]);
			}else if(lineNo==clientNumber){
				if(data[0]!=null){
					clientServerPort=Integer.parseInt(data[0]);
				}else{
					System.out.println("client server port received null from config file");
				}
				if(data[1]!=null){
					downloadPort=Integer.parseInt(data[1]);
				}else{
					System.out.println("downloadPort port received null from config file");
				}
			}
		}
		lineNo++;
	}

	br.close();
}

private static void getServerPort() throws IOException {
	File fileName = new File("../config.txt");
	BufferedReader br = new BufferedReader(new FileReader(fileName));

	String line = null;
	int lineNo=0;
	while ((line = br.readLine()) != null) {
		String[] data=line.split("\\t");
		for(int i=0;i<data.length;i++){
			if(lineNo==0){
				sPort=Integer.parseInt(data[i]);
			}
		}
		lineNo++;
	}

	br.close();
}

private void saveChunk(byte[] chunk, int packetNumber){
	try{
		String dirPath = "data/";
		File fileName = new File(dirPath + packetNumber);
		FileOutputStream fos = new FileOutputStream(fileName, true);
		fos.write(chunk);
		fos.flush();
		chunk = null;
		fos = null;
	}catch (IOException ioException) {
		ioException.printStackTrace();
	}catch (Exception exception) {
		exception.printStackTrace();
	}
	
}

// main method
public static void main(String args[]) {
	Client client=null;
	ServerSocket listener=null;
	try{
		try{
			client = new Client();
			client.run();
			System.out.println("The clint "+clientNumber+" server is running on port "+clientServerPort);
			listener = new ServerSocket(clientServerPort);
			new Receiver().start();
			new Handler(listener.accept()).start();
		} catch(IOException ex){
			ex.printStackTrace();
		}finally {
			listener.close();
		}
	}catch(IOException ex){
		ex.printStackTrace();
	}
	
	
}

private static class Receiver extends Thread {
	private Socket requestSocket;
	private DataInputStream din; // stream write to the socket
	private DataOutputStream dout;
	private boolean connectionEstablished=false;
	public Receiver() {
	}
	public void run() {
		try {
			
			while (booleanStartLoops) {
				Thread.sleep(1000);
				if(isServerAlive()){

						boolean lastCheck=false;

					int serverListSize=din.readInt();
					byte[] serverListByte = new byte[serverListSize];
					if(serverListSize>0) {
						din.readFully(serverListByte, 0, serverListSize);
					}

					ArrayList<Integer> serverPacketList = readServerList(serverListByte);
					System.out.println("DOWNLOAD: Received chunk ID list "+serverPacketList);
					serverPacketList.removeAll(packetList);

					if(serverPacketList.size()==0){
						dout.writeBoolean(false);
					}else{
						System.out.println("DOWNLOAD: Requesting for chunks "+serverPacketList);
						dout.writeBoolean(true);
						dout.flush();
						byte[] byteServerList = sendObjectOverTCP(serverPacketList);
						dout.writeInt(byteServerList.length);
						dout.write(byteServerList);
						while(!lastCheck){
							int packetNumber = din.readInt(); 
							packetList.add(packetNumber);
							Collections.sort(packetList);

							//packet length
							int length = din.readInt(); 
							byte[] message = new byte[length];

							//packet data
							if(length>0) {
								din.readFully(message, 0, message.length);
							}
							saveChunk(message,packetNumber);

							//check if last packet
							lastCheck = din.readBoolean();

							System.out.println("DOWNLOAD: Received chunk " + packetNumber);
							if(packetList.size()==totalPackets){
								combineChunksToFile();
							}
						}
					}
					dout.flush();


						
				}else{
					System.out.println("Cannot connect to download neighbor "+downloadPort+". Retrying in 1 sec");
				}
			}
		}catch(SocketException soe){
			soe.printStackTrace();
		}catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			//ioException.printStackTrace();
		} catch (Exception exception) {
			exception.printStackTrace();
		}finally {
			try {
				din.close();
				dout.close();
				requestSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	public  ArrayList<Integer> readServerList(byte[] list) throws IOException, ClassNotFoundException {
   		ByteArrayInputStream bin = new ByteArrayInputStream(list);
    	ObjectInputStream ois = new ObjectInputStream(bin);
    	ArrayList<Integer> serverPacketList = (ArrayList<Integer>) ois.readObject();
    	bin.close();
    	ois.close();
    	return serverPacketList;
	}

	public byte[] sendObjectOverTCP(Object list) throws IOException{
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
    	ObjectOutputStream oos = new ObjectOutputStream(bout);
    	oos.writeObject(list);
    	byte[] object= bout.toByteArray();
    	bout.close();
    	oos.close();
    	return object;
	}


	public static void combineChunksToFile(){
		//String newFileName="file.mp4";
		File file=new File(newFileName);
		FileInputStream fis;
		FileOutputStream fos;
		byte[] chunk;
		int input=0;
		List<File> fileNames=new ArrayList<File>();
		for(int i=1;i<=totalPackets;i++){
				fileNames.add(new File("data/"+i));
		}
	
		try{
			fos=new FileOutputStream(file,true);
			for(File newFile: fileNames){
				fis=new FileInputStream(newFile);
				chunk=new byte[(int)newFile.length()];
				input=fis.read(chunk, 0, (int) newFile.length());
				fos.write(chunk);
				fos.flush();
				chunk=null;
				fis.close();
				fis=null;
			}
			fos.close();
			fos=null;
			System.out.println("Received all chunks. Combining chunks to create file "+newFileName);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}


	private void saveChunk(byte[] chunk, int packetNumber){
		try{
			String dirPath = "data/";
			File fileName = new File(dirPath + packetNumber);
			FileOutputStream fos = new FileOutputStream(fileName, true);
			fos.write(chunk);
			fos.flush();
			chunk = null;
			fos = null;
		}catch (IOException ioException) {
			ioException.printStackTrace();
		}catch (Exception exception) {
			exception.printStackTrace();
		}

	}

	private  boolean isServerAlive(){
		try{
			if(!connectionEstablished){
				requestSocket = new Socket("localhost", downloadPort);
				System.out.println("Connected to localhost in port "+downloadPort);
				dout = new DataOutputStream(requestSocket.getOutputStream());
				dout.flush();
				din = new DataInputStream(requestSocket.getInputStream());
			}

		} catch (UnknownHostException e){ 
			return false;
		}catch (IOException e) {
			return false;
		}catch (NullPointerException e) {
			return false;
		}
		connectionEstablished = true;
		return true;
	}
}





/**
 * A handler thread class. Handlers are spawned from the listening loop and
 * are responsible for dealing with a single client's requests.
 */
private static class Handler extends Thread {
	private String message; // message received from the client
	private String MESSAGE; // uppercase message send to the client
	private Socket connection;
	private DataInputStream in; // stream read from the socket
	private DataOutputStream dout; // stream write to the socket

	public Handler(Socket connection) {
		this.connection = connection;
	}

	public void run() {
		try {
			dout = new DataOutputStream(connection.getOutputStream());
			in = new DataInputStream(connection.getInputStream());
			try {
				boolean flag=false;
				while (booleanStartLoops) {

					ArrayList<Integer> newList=new ArrayList<Integer>();
					newList.addAll(packetList);
					System.out.println("UPLOAD: Sending chunk ID list "+newList);
					byte[] objectList=sendObjectOverTCP(newList);
					dout.writeInt(objectList.length);
					dout.write(objectList);
					dout.flush();

					boolean isChange=in.readBoolean();
					if(newList.size()==totalPackets && !isChange){
						booleanStartLoops=false;
					}

					if(isChange){
						int length=in.readInt();
						byte[] toSendPacketListByte=new byte[length];
						in.readFully(toSendPacketListByte,0,length);
						ArrayList<Integer> toSendList = readServerList(toSendPacketListByte);
						System.out.println("UPLOAD: Received request for chunks "+toSendList);
						sendFilesOverTcp(toSendList);
					}
				}
			} catch (Exception ex) {
				//ex.printStackTrace();
			}
		} catch (IOException ioException) {
			System.out.println("Disconnect with Client ");
		} finally {
			try {
				in.close();
				dout.close();
				connection.close();
			} catch (IOException ioException) {
				System.out.println("Disconnect with Client ");
			}
		}
	}
	public  ArrayList<Integer> readServerList(byte[] list) throws IOException, ClassNotFoundException {
   		ByteArrayInputStream bin = new ByteArrayInputStream(list);
    	ObjectInputStream ois = new ObjectInputStream(bin);
    	ArrayList<Integer> serverPacketList = (ArrayList<Integer>) ois.readObject();
    	bin.close();
    	ois.close();
    	return serverPacketList;
	}

	private boolean compareLinkedList(ArrayList<Integer> a, ArrayList<Integer> b){
		if(a!=null && b!=null){
			if(a.size() == b.size()){
				for(int i=0;i<a.size();i++){
					if(a.get(i)!=b.get(i)){
						return false;
					}
				}
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	
	private void sendFilesOverTcp(ArrayList<Integer> toGiveList){
		FileInputStream fis;
		byte[] chunk;
		int input=0;
		List<File> fileNames=new ArrayList<File>();
		for(int i=0;i<toGiveList.size();i++){
			fileNames.add(new File("data/"+toGiveList.get(i).toString()));
		}

		try{
			for(int i=0;i<fileNames.size();i++){
				File newFile=fileNames.get(i);
				fis=new FileInputStream(newFile);
				chunk=new byte[(int)newFile.length()];
				input=fis.read(chunk, 0, (int) newFile.length());
				dout.writeInt(Integer.parseInt(newFile.getName()));
				dout.writeInt(chunk.length);
				dout.write(chunk);
				System.out.println("UPLOAD: Sending chunk "+Integer.parseInt(newFile.getName()));
				if(i==fileNames.size()-1){
					dout.writeBoolean(true);
				}else{
					dout.writeBoolean(false);
				}
				dout.flush();
				chunk=null;
				fis.close();
				fis=null;
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	public byte[] sendObjectOverTCP(Object list) throws IOException{
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
    	ObjectOutputStream oos = new ObjectOutputStream(bout);
    	oos.writeObject(list);
    	byte[] object= bout.toByteArray();
    	bout.close();
    	oos.close();
    	return object;
	}

}

}
