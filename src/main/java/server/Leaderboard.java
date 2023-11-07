package server;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Leaderboard {
    private Map<String, Integer> players; // Use a map to store player names and their wins
    private static final String LEADERBOARD_FILE = "leaderboard.dat";

    public Leaderboard() {
        players = loadLeaderboard();
    }

    public synchronized void addPlayer(String name, int wins) {
        players.put(name, wins); // Add the player to the leaderboard
        saveLeaderboard();
    }

    public synchronized void updatePlayerWins(String name) {
        // Increment wins for the player with the given name
        players.merge(name, 1, Integer::sum);
        saveLeaderboard();
    }

    public Map<String, Integer> getLeaderboard() {
        if (players != null) {
            // Return a copy of the map of player names and their wins to avoid concurrent modification issues
            return new HashMap<>(players);
        } else {
            // Handle the case where the leaderboard is null (not loaded or initialized)
            return new HashMap<>();
        }
    }

    private Map<String, Integer> loadLeaderboard() {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(LEADERBOARD_FILE))) {
            return (Map<String, Integer>) inputStream.readObject();
        } catch (FileNotFoundException e) {
            // Handle the case when the leaderboard file doesn't exist
            return new ConcurrentHashMap<>();
        } catch (IOException | ClassNotFoundException e) {
            // Handle other exceptions, e.g., deserialization errors
            e.printStackTrace();
            return new ConcurrentHashMap<>();
        }
    }

    private void saveLeaderboard() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(LEADERBOARD_FILE))) {
            outputStream.writeObject(players);
        } catch (IOException e) {
            // Handle exceptions related to saving the leaderboard data
            e.printStackTrace();
        }
    }
}
