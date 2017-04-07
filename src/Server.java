import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Server {

	private static  int sPort=0; // The server will be listening on
											// this port number
	private static int fileNumbers=0;

	private static int clientNumber = 0;
	private static String newFileName="";
	public static void main(String[] args){
		ServerSocket listener=null;
		try {
			try {
				portSetUp();
				System.out.println("The server is running on port "+ sPort);
				listener = new ServerSocket(sPort);
				divideFileInChunks();
				/*combineChunksToFile();*/
				while (true && clientNumber!=5) {
					clientNumber++;
					new Handler(listener.accept(), fileNumbers, clientNumber).start();
				}
				System.out.println("sending completed");
			}catch(IOException ioe){
				ioe.printStackTrace();
			} 
			catch(Exception ex){
				ex.printStackTrace();
			}finally {
				listener.close();
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
		}

		

	}
	private static void portSetUp() throws IOException {
		File fileName = new File("config.txt");
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
	public static void divideFileInChunks() {
		String dirPath="";
		//String newFileName="test.mp4";
		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter File Name:");
		newFileName = scanner.next();
		int chunkSize=102400, input=0,counter=0;
		int currentChunkSize=chunkSize;
		File inputFile=new File(dirPath+newFileName);
		int fileLength=(int)inputFile.length();
		int extraFile=fileLength%chunkSize;
		fileNumbers = fileLength/chunkSize;
		if(extraFile!=0){
			fileNumbers++;
		}
		float fl=fileLength;
		float kb=fl/1024;
		System.out.println("File length in kb = "+kb);
		System.out.println("Number of chunks = "+fileNumbers);
		FileInputStream fis;
		FileOutputStream fos;
		byte[] chunk;
		try{
			fis=new FileInputStream(dirPath+newFileName);
			while(fileLength>0){
				if(fileLength<=chunkSize){
					currentChunkSize=fileLength;
				}
				chunk=new byte[currentChunkSize];
				input=fis.read(chunk,0,currentChunkSize);
				fileLength-=input;
				counter++;
				String chunkFileName=dirPath+"data/"+counter;
				fos=new FileOutputStream(new File(chunkFileName));
				fos.write(chunk);
				fos.flush();
				fos.close();
				chunk=null;
				fos=null;
			}
			fis.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
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
		private DataInputStream din; // stream read from the socket
		private DataOutputStream dout; // stream write to the socket
		private int fileNumbers;
		private int clientNumber=0;

		public Handler(Socket connection, int fileNumbers, int cNo) {
			this.connection = connection;
			this.fileNumbers=fileNumbers;
			clientNumber = cNo;
		}

		public void run() {
			try {
				dout = new DataOutputStream(connection.getOutputStream());
				dout.flush();
				din = new DataInputStream(connection.getInputStream());
				try {
					boolean flag=false;
					while (!flag) {

						//ArrayList<Integer> arr = (ArrayList<Integer>)in.readObject();
						dout.writeInt(clientNumber);
						dout.writeUTF(newFileName);
						dout.flush();
						System.out.println("Client "+clientNumber+" connected");
						/*for(int i=0;i<arr.size();i++){
							System.out.println("List received from client="+arr.get(i));
						}*/
						//sendFilesOverTcp(arr);
						if(!flag){
							sendFilesOverTcp();
							flag=true;
						}
						
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} catch (IOException ioException) {
				System.out.println("Disconnect with Client " + clientNumber);
			} finally {
				try {
					din.close();
					dout.close();
					connection.close();
				} catch (IOException ioException) {
					System.out.println("Disconnect with Client " + clientNumber);
				}
			}
		}
		
		private void sendFilesOverTcp(){
			FileInputStream fis;
			byte[] chunk;
			int input=0;
			List<File> fileNames=new ArrayList<File>();
			/*for(int i=0;i<arr.size();i++){
				fileNames.add(new File("data/"+arr.get(i).toString()));
			}*/
			for(int i=clientNumber;i<=fileNumbers;i+=5){
				fileNames.add(new File("data/"+i));
			}
			try{
				for(int i=0;i<fileNames.size();i++){
					File newFile=fileNames.get(i);
					fis=new FileInputStream(newFile);
					chunk=new byte[(int)newFile.length()];
					input=fis.read(chunk, 0, (int) newFile.length());
					dout.writeInt(Integer.parseInt(newFile.getName()));
					System.out.println("UPLOAD: Sending chunk "+Integer.parseInt(newFile.getName())+ " to client "+clientNumber);
					dout.writeInt(chunk.length);
					dout.write(chunk);
					if(i==fileNames.size()-1){
						dout.writeBoolean(true);
					}else{
						dout.writeBoolean(false);
					}
					dout.writeInt(fileNumbers);
					dout.flush();
					chunk=null;
					fis.close();
					fis=null;
				}
				
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}

		// send a message to the output stream
		public void sendObject(Object obj) {
			
		}

	}

}
