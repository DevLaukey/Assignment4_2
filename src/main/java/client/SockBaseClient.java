package client;

import java.net.*;
import java.io.*;
import java.util.List;

import proto.RequestProtos.*;
import proto.ResponseProtos.*;

class SockBaseClient {
    private static BufferedReader stdin;

    public static void main(String args[]) {
        int port = 9099; // default port

        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }

        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be an integer");
            System.exit(2);
        }

        // Handle client operations
        handleClientOperations(host, port);
    }

    private static void handleClientOperations(String host, int port) {
        try (Socket serverSock = new Socket(host, port)) {
            OutputStream out = serverSock.getOutputStream();
            InputStream in = serverSock.getInputStream();
            stdin = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Please provide your name for the server. :-)");
            String name = stdin.readLine();

            // Send the NAME request
            Request nameRequest = Request.newBuilder()
                    .setOperationType(Request.OperationType.NAME)
                    .setName(name)
                    .build();
            nameRequest.writeDelimitedTo(out);

            // Receive the response
            Response response = Response.parseDelimitedFrom(in);

            // Check for errors
            if (response == null) {
                System.out.println("Error: Connection to the server was lost.");
                return;
            }

            if (response.getResponseType() == Response.ResponseType.ERROR) {
                System.out.println("Error: " + response.getMessage());
                return;
            }

            // Print the server response
            System.out.println(response.getHello());


            while (true) {
                // Display menu options
                System.out.println("Select an option:");
                System.out.println("1 - View Leaderboard");
                System.out.println("2 - Start a New Game");
                System.out.println("3 - Quit");

                // Read user's choice
                String choice = stdin.readLine();

                if (choice.equals("1")) {
                    // Send the LEADERBOARD request
                    Request leaderboardRequest = Request.newBuilder()
                            .setOperationType(Request.OperationType.LEADERBOARD)
                            .build();
                    leaderboardRequest.writeDelimitedTo(out);

                    // Receive and display leaderboard
                    response = Response.parseDelimitedFrom(in);

                    if (response == null) {
                        System.out.println("Error: Connection to the server was lost.");
                        break;
                    }

                    // Check for errors
                    if (response.getResponseType() == Response.ResponseType.ERROR) {
                        System.out.println("Error: " + response.getMessage());
                    } else {
                        displayLeaderboard(response.getLeaderboardList());
                    }
                } else if (choice.equals("2")) {
                    // Send the NEW request to start a game
                    Request newGameRequest = Request.newBuilder()
                            .setOperationType(Request.OperationType.NEW)
                            .build();
                    newGameRequest.writeDelimitedTo(out);

                    // Receive the initial game question
                    response = Response.parseDelimitedFrom(in);

                    if (response.getResponseType() == Response.ResponseType.TASK) {
                        System.out.println("Task: " + response.getTask());
                        System.out.println("Image: " + response.getImage());
                    } else {
                        System.out.println("Unexpected response from the server.");
                    }

                    // Handle the game
                    handleGame(in, out);
                } else if (choice.equals("3")) {
                    // Send the QUIT request to exit the game
                    Request quitRequest = Request.newBuilder()
                            .setOperationType(Request.OperationType.QUIT)
                            .build();
                    quitRequest.writeDelimitedTo(out);

                    // Receive the goodbye message
                    response = Response.parseDelimitedFrom(in);

                    if (response == null) {
                        System.out.println("Error: Connection to the server was lost.");
                        break;
                    }

                    // Check for errors
                    if (response.getResponseType() == Response.ResponseType.ERROR) {
                        System.out.println("Error: " + response.getMessage());
                    } else {
                        System.out.println("Goodbye! " + response.getMessage());
                    }

                    break; // Exit the loop and close the client
                } else {
                    System.out.println("Invalid choice. Please select 1, 2, or 3.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleGame(InputStream in, OutputStream out) {
        try {
            while (true) {
                // Handle the game logic
                // Send ANSWER request with the user's answer
                System.out.print("Enter your answer: ");
                String answer = stdin.readLine();
                Request answerRequest = Request.newBuilder()
                        .setOperationType(Request.OperationType.ANSWER)
                        .setAnswer(answer)
                        .build();
                answerRequest.writeDelimitedTo(out);

                // Receive the game response
                Response response = Response.parseDelimitedFrom(in);

                if (response == null) {
                    System.out.println("Error: Connection to the server was lost.");
                    break;
                }

                // Check for errors
                if (response.getResponseType() == Response.ResponseType.ERROR) {
                    System.out.println("Error: " + response.getMessage());
                } else if (response.getResponseType() == Response.ResponseType.WON) {
                    System.out.println("Congratulations! You've won.");
                    System.out.println("Winning Message: " + response.getMessage());
                    break; // Exit the game loop
                } else if (response.getResponseType() == Response.ResponseType.TASK) {
                    System.out.println("Task: " + response.getTask());
                    System.out.println("Image: " + response.getImage());
                    if (response.getEval()) {
                        System.out.println("You've completed the task!");
                    }
                } else {
                    System.out.println("Unknown response type.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void displayLeaderboard(List<Leader> leaderboard) {
        System.out.println("Leaderboard:");
        for (Leader leader : leaderboard) {
            System.out.println(leader.getName() + " - Wins: " + leader.getWins() + " - Logins: " + leader.getLogins());
        }
    }
}
