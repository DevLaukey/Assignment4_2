package server;

import java.net.*;
import java.io.*;
import java.util.*;
import proto.RequestProtos.*;
import proto.ResponseProtos.*;

class SockBaseServer {
    static String logFilename = "logs.txt";

    ServerSocket socket = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int port = 9099; // default port
    Game game;

    public SockBaseServer(Socket sock, Game game){
        this.clientSocket = sock;
        this.game = game;
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e){
            System.out.println("Error in constructor: " + e);
        }
    }

    public void start() throws IOException {
        String name = "";
        System.out.println("Ready...");
        try {
            while (true) {
                Request op = Request.parseDelimitedFrom(in);
                String result = null;

                if (op.getOperationType() == Request.OperationType.NAME) {
                    name = op.getName();
                    writeToLog(name, Message.CONNECT);
                    System.out.println("Got a connection and a name: " + name);
                    Response response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.HELLO)
                            .setHello("Hello " + name + " and welcome.")
                            .build();
                    response.writeDelimitedTo(out);
                } else if (op.getOperationType() == Request.OperationType.NEW) {
                    // Start a new game and send the first question to the client
                    game.newGame();

                    String question = "Name the 3rd prime number."; // Set the question
                    Response response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.TASK)
                            .setImage(game.getImage())
                            .setTask(question)
                            .build();
                    response.writeDelimitedTo(out);

                    System.out.println("Task: " + response.getResponseType());
                    System.out.println("Image: \n" + response.getImage());
                    System.out.println("Task: \n" + response.getTask());

                } else if (op.getOperationType() == Request.OperationType.LEADERBOARD) {
                    System.out.println("IN LEADERBOARD");
                    Response.Builder res = Response.newBuilder()
                            .setResponseType(Response.ResponseType.LEADERBOARD);

                    // building a leader entry
                    Leader leader = Leader.newBuilder()
                            .setName("name")
                            .setWins(0)
                            .setLogins(0)
                            .build();

                    // building a leader entry
                    Leader leader2 = Leader.newBuilder()
                            .setName("name2")
                            .setWins(1)
                            .setLogins(1)
                            .build();

                    res.addLeaderboard(leader);
                    res.addLeaderboard(leader2);

                    Response response3 = res.build();

                    response3.writeDelimitedTo(out);

                    for (Leader lead : response3.getLeaderboardList()) {
                        System.out.println(lead.getName() + ": " + lead.getWins());
                    }
                } else if (op.getOperationType() == Request.OperationType.ANSWER) {
                    String answer = op.getAnswer();
                    int userAnswer;

                    try {
                        userAnswer = Integer.parseInt(answer);
                    } catch (NumberFormatException e) {
                        // Invalid answer format
                        Response invalidAnswerResponse = Response.newBuilder()
                                .setResponseType(Response.ResponseType.TASK)
                                .setImage(game.getImage())
                                .setTask("Name the 3rd prime number.")
                                .setEval(false)
                                .setMessage("Invalid answer format. Please enter a number.")
                                .build();
                        invalidAnswerResponse.writeDelimitedTo(out);
                        return;
                    }

                    int correctAnswer = 5; // The 3rd prime number (prime numbers: 2, 3, 5, ...)

                    if (userAnswer == correctAnswer) {
                        // If the answer is correct, replace one character in the hidden image
                        String updatedImage = game.replaceOneFourthCharacters(15);

                        // Check if the game is won
                        if (game.getIdx() == game.getIdxMax()) {
                            // The game is won
                            Response wonResponse = Response.newBuilder()
                                    .setResponseType(Response.ResponseType.WON)
                                    .setImage(updatedImage)
                                    .setMessage("Congratulations! You've won the game.")
                                    .build();
                            wonResponse.writeDelimitedTo(out);
                        } else {
                            // The game is not yet won, send TASK response with the updated image
                            String newQuestion = "Name the 4th prime number."; // Set a new question
                            Response taskResponse = Response.newBuilder()
                                    .setResponseType(Response.ResponseType.TASK)
                                    .setImage(updatedImage)
                                    .setTask(newQuestion)
                                    .setEval(true)
                                    .build();
                            taskResponse.writeDelimitedTo(out);
                        }
                    } else {
                        // Answer is incorrect, send a TASK response with the same image and the same question
                        Response taskResponse = Response.newBuilder()
                                .setResponseType(Response.ResponseType.TASK)
                                .setImage(game.getImage())
                                .setTask("Name the 3rd prime number.")
                                .setEval(false)
                                .setMessage("Incorrect answer. Try again.")
                                .build();
                        taskResponse.writeDelimitedTo(out);
                    }
                } else if (op.getOperationType() == Request.OperationType.QUIT) {
                    // Handle QUIT request
                    // Exit the while loop to close the connection
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
                if (clientSocket != null)
                    clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    private boolean checkAnswer(String answer) {
//        try {
//            int ans = Integer.parseInt(answer);
//            return ans == 4; // Check if the answer is correct (e.g., 2 + 2 = 4)
//        } catch (NumberFormatException e) {
//            return false; // Invalid answer
//        }
//    }

    public static void writeToLog(String name, Message message) {
        try {
            Logs.Builder logs = readLogFile();
            Date date = java.util.Calendar.getInstance().getTime();
            logs.addLog(date.toString() + ": " + name + " - " + message);

            // open log file
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            // Write to log file
            logsObj.writeTo(output);
        } catch (Exception e) {
            System.out.println("Issue while trying to save");
        }
    }

    public static Logs.Builder readLogFile() throws Exception {
        Logs.Builder logs = Logs.newBuilder();

        try {
            // Read the file and put its content into the logs object
            return logs.mergeFrom(new FileInputStream(logFilename));
        } catch (FileNotFoundException e) {
            System.out.println(logFilename + ": File not found. Creating a new file.");
            return logs;
        }
    }

    public static void main(String[] args) {
        Game game = new Game();

        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <delay(int>");
            System.exit(1);
        }

        int port = 9099; // Default port
        int sleepDelay = 10000; // Default delay

        try {
            port = Integer.parseInt(args[0]);
            sleepDelay = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                SockBaseServer server = new SockBaseServer(clientSocket, game);
                Thread thread = new Thread(() -> {
                    try {
                        server.start();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

}
