
import java.io.*;
import java.lang.System;
import java.net.Socket;


public class CSftp
{
    static final int MAX_LEN = 255;
    static final int ARG_CNT = 2;

    public static void main(String [] args)
    {
	byte cmdString[] = new byte[MAX_LEN];

	if (args.length != ARG_CNT) {
	    System.out.print("Usage: cmd ServerAddress ServerPort\n");
	    return;
	}
        String hostName = args[0];
        int portNumber;
	    if (Integer.parseInt(args[1]) > 0) {
            portNumber = Integer.parseInt(args[1]);
        } else {
            portNumber = 21;
        }

	try {
        Socket ftpSocket = new Socket(hostName, portNumber);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ftpSocket.getOutputStream()));
        BufferedReader in = new BufferedReader(new InputStreamReader(ftpSocket.getInputStream()));
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String fromServer = "000";
        String fromUser;
        String user[];
        String IP[];
        int port;
        String host = "000";
        String command = null;
        String argument = null;
        boolean loggedIn = false;
        String initialConnection = in.readLine();
        int c = 0;
        int bytesRead;
        int current = 0;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        String pathFile[];

        if (initialConnection.startsWith("120")){
            ftpSocket.close();
        }
        else if (!(initialConnection.startsWith("220"))){
            System.out.println("<-- " + initialConnection);
            System.out.println("0xFFFC Control connection to " + hostName + " on port " + portNumber + " failed to open.");
        }
        else {
            System.out.println("<-- " + initialConnection);
            for (int len = 1; len > 0; ) {
                System.out.print("csftp> ");
                fromUser = stdIn.readLine();
                user = fromUser.split(" ");

                if (user.length == 2) {
                    command = user[0];
                    argument = user[1];
                } else if (user.length > 2) {
                    System.out.println("0x002 Incorrect number of arguments.");
                    break;
                } else
                    command = user[0];

                cmdString = fromUser.getBytes();
                len = cmdString.length;

                if (len <= 0) {
                    System.out.println("breaking");
                    break;
                }

                // Start processing the command here.
                if (command.startsWith("quit")) {
                    out.write("QUIT " + "\r\n");
                    out.flush();
                    fromServer = in.readLine();
                    System.out.println("--> QUIT");
                    System.out.println("<-- " + fromServer);
                    ftpSocket.close();
                    break;
                }

                else if (command.equals("user")) {
                    if (argument == null) {
                        System.out.println("0x002 Incorrect number of arguments.");
                    } else {
                        out.write("USER " + argument + "\r\n");
                        out.flush();
                        fromServer = in.readLine();
                        System.out.println("--> USER " + argument);
                        System.out.println("<-- " + fromServer);
                        if(fromServer.startsWith("421")){
                            System.out.println("0xFFFD Control connection I/O error, closing control connection");
                            ftpSocket.close();
                            break;
                        }
                        else if (fromServer.startsWith("230"))
                            loggedIn = true;

                    }
                }
                else if (command.equals("pw")) {
                    if (argument == null) {
                        System.out.println("0x002 Incorrect number of arguments.");
                    } else if (!(fromServer.startsWith("331 "))) {
                        System.out.println("0xFFFF Processing error, not logged in");
                    } else{
                        fromUser = argument;
                        out.write("PASS " + fromUser + "\r\n");
                        out.flush();
                        fromServer = in.readLine();
                        System.out.println("--> PASS " + fromUser);
                        System.out.println("<-- " + fromServer);
                        if(fromServer.startsWith("421")){
                            System.out.println("0xFFFD Control connection I/O error, closing control connection");
                            ftpSocket.close();
                            break;
                        }
                        else if (fromServer.startsWith("230"))
                            loggedIn = true;
                    }

                }
                else if (command.equals("get")) {
                    if (loggedIn == false) {
                        System.out.println("0xFFFF Processing error. not logged in to account.");
                        break;                                   
                    } else if (argument == null){
                        System.out.println("0x002 Incorrect number of arguments.");
                    } else{                                     
                        out.write("PASV" + "\r\n");
                        out.flush();
                        fromServer = in.readLine();
                        System.out.println("--> PASV");
                        System.out.println("<-- " + fromServer);
                        if (fromServer.startsWith("421")){
                            System.out.println("0xFFFD Control connection I/O error, closing control connection");
                            ftpSocket.close();
                            break;
                        }
                        else if (!(fromServer.startsWith("227"))) {
                            System.out.println("0xFFFF Processing error, not able to enter passive mode");
                            break;
                        }
                        else{
                            host = fromServer;
                            host = host.substring(5);
                            host = host.replaceAll("\\D+", " ");
                            IP = host.split(" ");
                            host = "";

                            for (int i = 1; i < 4; i++) {
                                host = host.concat(IP[i]);
                                host = host.concat(".");
                            }

                            host = host.concat(IP[4]);
                            port = Integer.parseInt(IP[5]);
                            port = port * 256;
                            port = port + (Integer.parseInt(IP[6]));
                            try {
                                Socket extra = new Socket(host, port);
                                System.out.println("--> TYPE I");
                                out.write("TYPE I" + "\r\n");
                                out.flush();
                                fromServer = in.readLine();
                                System.out.println("<-- " + fromServer);
                                out.write("RETR " + argument + "\r\n");
                                out.flush();

                                fromServer = in.readLine();
                                System.out.println("--> RETR " + argument);
                                System.out.println("<-- " + fromServer);

                                if (fromServer.startsWith("421")) {
                                    System.out.println("0xFFFD Control connection I/O error, closing control connection");
                                    extra.close();
                                    ftpSocket.close();
                                    break;
                                } else if (fromServer.startsWith("426")) {
                                    System.out.println("0x3A7 Data transfer connection I/O error, closing data connection.");
                                    extra.close();
                                } else if (!(fromServer.startsWith("150") || fromServer.startsWith("125"))) {
                                    System.out.println("0xFFFF connection error.");
                                } else {
                                    try {
                                        byte[] bytes = new byte[16834 * 16384];
                                        InputStream is = extra.getInputStream();
                                        pathFile = argument.split("/");
                                        fos = new FileOutputStream(pathFile[pathFile.length - 1]);
                                        bos = new BufferedOutputStream(fos);
                                        bytesRead = is.read(bytes, 0, bytes.length);
                                        current = bytesRead;
                                        do {
                                            bytesRead = is.read(bytes, current, (bytes.length - current));
                                            if (bytesRead >= 0)
                                                current += bytesRead;
                                        } while (bytesRead > -1);
                                        bos.write(bytes, 0, current);
                                        bos.flush();
                                        fos.close();
                                        bos.close();
                                        extra.close();
                                        fromServer = in.readLine();
                                        System.out.println("<-- " + fromServer);
                                    } catch (FileNotFoundException exception) {
                                        System.out.println("0x38E Access to local file " + argument + " denied.");
                                    }
                                }
                            } catch (IOException e){
                                System.out.println("0x3A2 Data transfer connection to "+host+" on port "+port+" failed to open." );
                            }
                        }
                    }
                }
                else if (command.equals("dir")) {
                    if (loggedIn == false) {
                        System.out.println("0xFFFF Processing error. not logged in to account.");
                        break;
                    } else {
                        out.write("PASV" + "\r\n");
                        out.flush();
                        fromServer = in.readLine();
                        System.out.println("--> PASV");
                        System.out.println("<-- " + fromServer);
                        if (fromServer.startsWith("421")) {
                            System.out.println("0xFFFD Control connection I/O error, closing control connection");
                            ftpSocket.close();
                            break;
                        } else if (!(fromServer.startsWith("227"))) {
                            System.out.println("0xFFFF Processing error, not able to enter passive mode");
                            break;
                        } else {
                            host = fromServer;
                            host = host.substring(5);
                            host = host.replaceAll("\\D+", " ");
                            IP = host.split(" ");
                            host = "";
                            for (int i = 1; i < 4; i++) {
                                host = host.concat(IP[i]);
                                host = host.concat(".");
                            }
                            host = host.concat(IP[4]);
                            port = Integer.parseInt(IP[5]);
                            port = port * 256;
                            port = port + (Integer.parseInt(IP[6]));

                            System.out.println("--> TYPE I");
                            out.write("TYPE I" + "\r\n");
                            out.flush();
                            fromServer = in.readLine();
                            System.out.println("<-- " + fromServer);

                            try {
                                Socket extra = new Socket(host, port);
                                out.write("LIST " + "\r\n");
                                out.flush();
                                BufferedReader inx = new BufferedReader(new InputStreamReader(extra.getInputStream()));
                                System.out.println("--> LIST");
                                fromServer = in.readLine();
                                System.out.println("<-- " + fromServer);
                                String fromServerx = inx.readLine();
                                if (fromServer.startsWith("421")) {
                                    System.out.println("0xFFFD Control connection I/O error, closing control connection");
                                    extra.close();
                                    ftpSocket.close();
                                    break;
                                } else if (fromServer.startsWith("426")) {
                                    System.out.println("0x3A7 Data transfer connection I/O error, closing data connection.");
                                    extra.close();
                                } else if (!(fromServer.startsWith("150") || fromServer.startsWith("125"))) {
                                    System.out.println("0xFFFF Processing error, not able to enter passive mode");
                                    break;
                                } else {
                                    while (fromServerx != null) {
                                        System.out.println("<-- " + fromServerx);
                                        fromServerx = inx.readLine();
                                    }
                                    fromServer = in.readLine();
                                    System.out.println("<-- " + fromServer);
                                }
                            } catch (IOException e) {
                                System.out.println("0x3A2 Data transfer connection to " + host + " on port " + port + " failed to open.");
                            }
                        }
                    }
                }
                else if (command.equals("features")) {
                    out.write("FEAT" + "\r\n");
                    out.flush();
                    System.out.println("--> FEAT");
                    fromServer = in.readLine();
                    System.out.println("<-- " + fromServer);
                    if (!(fromServer.startsWith("211"))) {
                        System.out.println("0xFFFF Processing error. Unknown error.");
                    }
                    else {
                        String extra = "";
                        while (!(extra.startsWith("211"))) {
                            extra = in.readLine();
                            System.out.println("<-- " + extra);
                        }
                    }
                }
                else if (command.equals("cd")) {
                    if (argument == null) {
                        System.out.println("0x002 Incorrect number of arguments.");
                    } else {
                        out.write("CWD " + argument + "\r\n");
                        out.flush();
                        System.out.println("--> CWD " + argument);
                        fromServer = in.readLine();
                        System.out.println("<-- " + fromServer);
                        if(fromServer.startsWith("421")){
                            System.out.println("0xFFFD Control connection I/O error, closing control connection");
                            ftpSocket.close();
                            break;
                        } else if(!fromServer.startsWith("250")){
                            System.out.println("0xFFFF Processing error. Unknown error.");
                        }
                    }
                }
                else if (command.startsWith("#")){
                    //nothing
                }
                else {
                    System.out.println("0x001 Invalid Command.");
                }
            }
        }
	 } catch (IOException exception) {
	    System.err.println("0xFFFE Input error while reading commands, terminating.");
	}
    }
}
