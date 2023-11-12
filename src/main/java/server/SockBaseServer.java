package server;

import java.net.*;
import java.io.*;
import java.util.*;
import proto.RequestProtos.*;
import proto.ResponseProtos.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


class SockBaseServer {
    static String logFilename = "logs.txt";

    ServerSocket socket = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int port = 9099; // default port
    Game game;
    private List<Leader> leaderboard = new ArrayList<>(); // Example leaderboard data structure
    private List<Task> taskList; // List of tasks
    private Random random; // Random number generator

    private String currentImage; // The current state of the image
    private Map<String, Integer> taskSolutions;
    private String currentTask;
    private static final String LEADERBOARD_FILE = "leaderboard.txt";

    public SockBaseServer(Socket sock, Game game){
        this.clientSocket = sock;
        this.game = game;

        taskSolutions = new HashMap<>();
        initializeTasks();
        currentTask = getNextTask();
        taskList = new ArrayList<>();
        random = new Random();
        loadLeaderboardFromFile();
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e){
            System.out.println("Error in constructor: " + e);
        }
    }

    private String getNextTask() {
        // Get the next task from the map
        if (taskSolutions.isEmpty()) {
            // No tasks left
            return "No more tasks available. Game over!";
        }

        // Get the first task in the map
        String nextTask = taskSolutions.keySet().iterator().next();
        taskSolutions.remove(nextTask); // Remove the task from the map
        return nextTask;
    }

    private void initializeTasks() {
        taskSolutions.put("Name the 3rd prime number", 5);
        taskSolutions.put("Name the 4th prime number", 7);
        taskSolutions.put("Name the 5th prime number", 11);
    }


    public void start() throws IOException {
        String name = "";
        System.out.println("Ready...");
        try {
            while (true) {
                Request op = Request.parseDelimitedFrom(in);
                String result = null;

                if (op == null) {
                    // The client disconnected unexpectedly
                    System.out.println("Client disconnected.");
                    break; // Exit the loop to close the connection
                }

                if (op.getOperationType() == Request.OperationType.NAME) {
                    name = op.getName();
                    writeToLog(name, Message.CONNECT);
                    System.out.println("Got a connection and a name: " + name);
                    Response response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.HELLO)
                            .setHello("Hello " + name + " and welcome.")
                            .build();
                    updateLeaderboard(name, false);
                    response.writeDelimitedTo(out);
                }
                else if (op.getOperationType() == Request.OperationType.NEW) {
                    // Start a new game and send the first question to the client
                    game.newGame();
                    currentImage = game.getImage();
                    currentTask = getNextTask(); // Get a new random task
                    String question = currentTask;
                    Response response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.TASK)
                            .setImage(currentImage)
                            .setTask(question)
                            .build();
                    response.writeDelimitedTo(out);
                    System.out.println("Task: " + response.getResponseType());
                    System.out.println("Image: \n" + response.getImage());
                    System.out.println("Task: \n" + response.getTask());
                }
                else if (op.getOperationType() == Request.OperationType.LEADERBOARD) {
                    Response.Builder responseBuilder = Response.newBuilder()
                            .setResponseType(Response.ResponseType.LEADERBOARD);

                    if (leaderboard.isEmpty()) {
                        responseBuilder.setMessage("Leaderboard is empty.");
                    } else {
                        List<Leader> sortedLeaderboard = new ArrayList<>(leaderboard);
                        sortedLeaderboard.sort((leader1, leader2) -> leader2.getWins() - leader1.getWins());

                        for (Leader leader : sortedLeaderboard) {
                            responseBuilder.addLeaderboard(leader);
                        }
                    }

                    Response response = responseBuilder.build();
                    response.writeDelimitedTo(out);
                }

                else if (op.getOperationType() == Request.OperationType.ANSWER) {
                    String answer = op.getAnswer();
                    int userAnswer;

                    if (!isNumeric(answer)) {
                        // Invalid answer format
                        Response invalidAnswerResponse = Response.newBuilder()
                                .setResponseType(Response.ResponseType.TASK)
                                .setImage(game.getImage())
                                .setTask("Invalid answer format. Please enter a number.")
                                .setEval(false)
                                .setMessage("Invalid answer format. Please enter a number.")
                                .build();
                        invalidAnswerResponse.writeDelimitedTo(out);
                        return;
                    }

                    try {
                        userAnswer = Integer.parseInt(answer);
                    } catch (NumberFormatException e) {
                        // Invalid answer format
                        Response invalidAnswerResponse = Response.newBuilder()
                                .setResponseType(Response.ResponseType.TASK)
                                .setImage(game.getImage())
                                .setTask("Name a prime number.")
                                .setEval(false)
                                .setMessage("Invalid answer format. Please enter a number.")
                                .build();
                        invalidAnswerResponse.writeDelimitedTo(out);
                        return;
                    }

                    // Get the correct answer for the current task (prime numbers)
                    List<Integer> primeNumbers = Arrays.asList(2, 3, 5, 7, 11, 13, 17, 19); // Add more prime numbers as needed

                    if (primeNumbers.contains(userAnswer)) {
                        // If the answer is correct, replace one character in the hidden image
                        String updatedImage = game.replaceOneFourthCharacters(25); // Replace one character

                        if (game.getIdx() == game.getIdxMax()) {
                            // The game is won
                            Response wonResponse = Response.newBuilder()
                                    .setResponseType(Response.ResponseType.WON)
                                    .setImage(updatedImage)
                                    .setMessage("Congratulations! You've won the game.")
                                    .build();
                            wonResponse.writeDelimitedTo(out);

                            // Add the player to the leaderboard
                            updateLeaderboard(name, true);
                            // Save the updated leaderboard to the file
                            saveLeaderboardToFile();
                        } else {
                            // The game is not yet won, send TASK response with the updated image
                            String newQuestion = "Name a prime number below 20."; // Set a new question
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
                                .setTask("Name a prime number.")
                                .setEval(false)
                                .setMessage("Incorrect answer. Try again.")
                                .build();
                        taskResponse.writeDelimitedTo(out);
                    }
                }



                else if (op.getOperationType() == Request.OperationType.QUIT) {
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

    // Check if a string is a valid number
    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void updateLeaderboard(String playerName, boolean isWin) {
        // Search for the player in the leaderboard
        Leader player = null;
        int playerIndex = -1;
        for (int i = 0; i < leaderboard.size(); i++) {
            Leader leader = leaderboard.get(i);
            if (leader.getName().equals(playerName)) {
                player = leader;
                playerIndex = i;
                break;
            }
        }

        if (player == null) {
            // If the player is not in the leaderboard, create a new entry
            int wins = 0;
            int logins = 1;
            player = Leader.newBuilder()
                    .setName(playerName)
                    .setWins(wins)
                    .setLogins(logins)
                    .build();
            leaderboard.add(player);
        } else {
            // Increment the login count
            int logins = player.getLogins() + 1;
            Leader updatedPlayer = Leader.newBuilder()
                    .mergeFrom(player)
                    .setLogins(logins)
                    .build();
            leaderboard.set(playerIndex, updatedPlayer);

            // Update the wins count for the player if isWin is true
            if (isWin) {
                int wins = player.getWins() + 1;
                updatedPlayer = Leader.newBuilder()
                        .mergeFrom(updatedPlayer)
                        .setWins(wins)
                        .build();
                leaderboard.set(playerIndex, updatedPlayer);
            }
        }

        // Save the updated leaderboard to the file
        saveLeaderboardToFile();
    }

    private void loadLeaderboardFromFile() {
        File leaderboardFile = new File(LEADERBOARD_FILE);
        if (leaderboardFile.exists()) {
            try (FileInputStream fileInputStream = new FileInputStream(leaderboardFile)) {
                leaderboard.clear();

                while (true) {
                    Leader leader = Leader.parseDelimitedFrom(fileInputStream);
                    if (leader == null) {
                        break;
                    }
                    leaderboard.add(leader);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveLeaderboardToFile() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(LEADERBOARD_FILE)) {
            for (Leader leader : leaderboard) {
                leader.writeDelimitedTo(fileOutputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendNewTask() throws IOException {
        Response response = Response.newBuilder()
                .setResponseType(Response.ResponseType.TASK)
                .setImage(currentImage)
                .setTask(currentTask)
                .build();
        response.writeDelimitedTo(out);
    }

    // Implement the checkAnswer method to validate the user's answer
    private boolean checkAnswer(String answer) {
        try {
            int userAnswer = Integer.parseInt(answer);

            System.out.println("curre"+currentTask);
            // Get the correct answer for the current task
            Integer correctAnswer = taskSolutions.get(currentTask);

            System.out.println(correctAnswer);
            System.out.println(userAnswer);
            // Check if the user's answer is correct for the current task
            if (correctAnswer != null && userAnswer == correctAnswer) {
                return true;
            } else {
                return false; // Incorrect answer
            }
        } catch (NumberFormatException e) {
            return false; // Invalid answer format
        }
    }


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
 class Task {
    private String task;
    private int answer;

    public Task(String task, int answer) {
        this.task = task;
        this.answer = answer;
    }

    public String getTask() {
        return task;
    }

    public int getAnswer() {
        return answer;
    }
}