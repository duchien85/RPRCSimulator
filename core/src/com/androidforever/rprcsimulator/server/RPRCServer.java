package com.androidforever.rprcsimulator.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pedja on 2.6.15..
 */
public class RPRCServer implements Runnable
{
    public static final String MESSAGE_START_TAG = "<message";
    public static final String MESSAGE_END_TAG = "</message>";
    public static final int SERVER_PORT = 6000;
    Thread serverThread = null;
    private ServerSocket serverSocket;
    private boolean started;
    private List<Socket> connectedClients;
    private int connectionsLimit;

    public RPRCServer(int connectionsLimit)
    {
        this.connectionsLimit = connectionsLimit;
        connectedClients = new ArrayList<>(connectionsLimit);
        serverThread = new Thread(this);
    }

    public void start() throws ServerClosedException
    {
        if(serverSocket != null && serverSocket.isClosed())
        {
            throw new ServerClosedException();
        }
        if(started)return;
        serverThread.start();
        started = true;
    }

    public void stop()
    {
        try
        {
            serverSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        Socket socket;

        try
        {
            serverSocket = new ServerSocket(SERVER_PORT);
            //setServerMessage("");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            //setServerMessage(e.getMessage());
            return;
        }

        while (!Thread.currentThread().isInterrupted())
        {
            try
            {
                if(serverSocket == null || serverSocket.isClosed())
                {
                    //setServerMessage("Server Stopped");
                    return;
                }
                socket = serverSocket.accept();
                if(connectedClients.size() == connectionsLimit)
                {
                    sendMessageToClient()
                    socket.close();//don't allow
                    continue;
                }
                new CommunicationThread(socket).start();

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private class CommunicationThread extends Thread
    {
        Socket socket;
        BufferedReader inputReader;

        public CommunicationThread(Socket socket) throws IOException
        {
            this.socket = socket;
            inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run()
        {
            Message data = new Message();
            while (!Thread.currentThread().isInterrupted())
            {
                try
                {
                    final String read = inputReader.readLine();
                    if(read == null)return;
                    if(read.startsWith(MESSAGE_START_TAG))
                    {
                        data.start();
                        String type = read.split(" ")[1].split("=")[1];
                        data.type = Message.Type.valueOf(type.substring(0, type.length() - 1));
                        continue;
                    }
                    if(MESSAGE_END_TAG.equals(read))
                    {
                        if(data.isStarted())
                        {
                            String message = data.getData();
                            Message.Type type = data.type;
                            onMessageReceived(message, type);
                            data.close();
                        }
                        else
                        {
                            System.out.println("received closing tag, but there was no starting tag");
                        }
                        continue;
                    }
                    if(data.isStarted())
                    {
                        data.append(read).append("\n");
                    }
                    else
                    {
                        System.out.println("received some data, but there was no starting tag, data will be ignored");
                    }
                    //TODO respon

                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onMessageReceived(final String message, Message.Type type)
    {

    }
}
