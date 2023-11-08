package server;

import java.util.*; 
import java.io.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


/**
 * Class: Game 
 * Description: Game class that can load an ascii image
 * Class can be used to hold the persistent state for a game for different threads
 * synchronization is not taken care of .
 * You can change this Class in any way you like or decide to not use it at all
 * I used this class in my SockBaseServer to create a new game and keep track of the current image evenon differnt threads. 
 * My threads each get a reference to this Game
 */

public class Game {
    private int idx = 0; // current index where x could be replaced with original
    private int idxMax; // max index of image
    private char[][] original; // the original image
    private char[][] hidden; // the hidden image
    private int col; // columns in original, approx
    private int row; // rows in original and hidden
    private boolean won; // if the game is won or not
    private List<String> files = new ArrayList<String>(); // list of files, each file has one image

    private Map<String, Integer> validAnswers = new HashMap<>();

    public Game(){
        // you can of course add more or change this setup completely. You are totally free to also use just Strings in your Server class instead of this class
        won = true; // setting it to true, since then in newGame() a new image will be created
        files.add("pig.txt");
        files.add("horse.txt");
        files.add("crab.txt");
        files.add("cat.txt");
        files.add("dog.txt");
        files.add("joke1.txt");
        files.add("joke2.txt");
        files.add("joke3.txt");
        // Initialize the valid answers with sample mathematical questions and their answers
        validAnswers.put("2+2", 4);
        validAnswers.put("5-3", 2);
        validAnswers.put("3*4", 12);
        validAnswers.put("10/2", 5);
    }


    public void setWon(){
        won = true;
    }

    /**
     * Method loads in a new image from the specified files and creates the hidden image for it. 
     * @return Nothing.
     */
    public void newGame(){
        if (won) {
            idx = 0;
            won = false;
            List<String> rows = new ArrayList<String>();

            try{
                // loads one random image from list
                Random rand = new Random();
                col = 0;
                int randInt = rand.nextInt(files.size());
                File file = new File(
                        Game.class.getResource("/"+files.get(randInt)).getFile()
                );

                System.out.println(file);
                BufferedReader br = new BufferedReader(new FileReader(file));
                System.out.println(br);
                String line;
                while ((line = br.readLine()) != null) {
                    if (col < line.length()) {
                        col = line.length();
                    }
                    rows.add(line);
                }
            }
            catch (Exception e){
                System.out.println("File load error"); // extremely simple error handling, you can do better if you like.
            }

            // this handles creating the orinal array and the hidden array in the correct size
            String[] rowsASCII = rows.toArray(new String[0]);

            row = rowsASCII.length;

            // Generate original array by splitting each row in the original array.
            original = new char[row][col];
            for(int i = 0; i < row; i++) {
                char[] splitRow = rowsASCII[i].toCharArray();
                for (int j = 0; j < splitRow.length; j++) {
                    original[i][j] = splitRow[j];
                }
            }

            // Generate Hidden array with X's (this is the minimal size for columns)
            hidden = new char[row][col];
            for(int i = 0; i < row; i++){
                for(int j = 0; j < col; j++){
                    hidden[i][j] = 'X';
                }
            }
            setIdxMax(col * row);
        }
        else {
        }
    }

    /**
     * Method returns the String of the current hidden image
     * @return String of the current hidden image
     */
    public String getImage(){
        StringBuilder sb = new StringBuilder();
        for (char[] subArray : hidden) {
            sb.append(subArray);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Method changes the next idx of the hidden image to the character in the original image
     * You can change this method if you want to turn more than one x to the original
     * @return String of the current hidden image
     */
    public String replaceOneCharacter() {
        int colNumber = idx%col;
        int rowNumber = idx/col;
        hidden[rowNumber][colNumber] = original[rowNumber][colNumber];
        idx++;
        return(getImage());
    }
    public String replaceMultipleCharacters(int times) {
        StringBuilder updatedImage = new StringBuilder(getImage());

        for (int i = 0; i < times; i++) {
            if (idx < idxMax) {
                int colNumber = idx % col;
                int rowNumber = idx / col;
                hidden[rowNumber][colNumber] = original[rowNumber][colNumber];
                idx++;

                // Update the corresponding characters in the updated image
                updatedImage.setCharAt((rowNumber * (col + 1)) + colNumber, original[rowNumber][colNumber]);
            } else {
                break; // Stop if max replacements are reached
            }
        }

        return updatedImage.toString();
    }

    public String replaceOneFourthCharacters(int maxReplacements) {
        StringBuilder updatedImage = new StringBuilder(getImage());

        int timesToReveal = Math.min(maxReplacements, idxMax / 8); // Calculate the number of times to reveal (one-fourth)

        for (int i = 0; i < timesToReveal; i++) {
            if (idx < idxMax) {
                int colNumber = idx % col;
                int rowNumber = idx / col;
                hidden[rowNumber][colNumber] = original[rowNumber][colNumber];
                idx++;

                // Update the corresponding characters in the updated image
                updatedImage.setCharAt((rowNumber * (col + 1)) + colNumber, original[rowNumber][colNumber]);
            } else {
                break; // Stop if max replacements are reached
            }
        }

        return updatedImage.toString();
    }

    public String replaceCharacters() {
        // Calculate the minimum fraction to reveal
        double minFractionToReveal = 8.0 / idxMax;

        // Ensure the fraction to reveal is at least the minimum
        double fractionToReveal = Math.max(minFractionToReveal, 1.0);

        // Calculate the number of times to reveal characters based on the fraction
        int timesToReveal = (int) (idxMax * fractionToReveal);

        StringBuilder updatedImage = new StringBuilder(getImage());

        for (int i = 0; i < timesToReveal; i++) {
            if (idx < idxMax) {
                int colNumber = idx % col;
                int rowNumber = idx / col;
                hidden[rowNumber][colNumber] = original[rowNumber][colNumber];
                idx++;

                // Update the corresponding characters in the updated image
                updatedImage.setCharAt((rowNumber * (col + 1)) + colNumber, original[rowNumber][colNumber]);
            } else {
                break; // Stop if the desired fraction of replacements is reached
            }
        }

        return updatedImage.toString();
    }




    public int getIdxMax() {
        return idxMax;
    }

    public void setIdxMax(int idxMax) {
        this.idxMax = idxMax;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }


    public String revealPortionOfImage(String currentImage) {
        // Check if the number of tasks completed is at least 8
        if (idx >= 8) {
            // Calculate the number of characters to reveal (you can adjust this based on your factor)
            int charactersToReveal = (int) (idxMax * 0.1); // For example, revealing 10% of the image

            // Ensure that you don't reveal more characters than are available
            charactersToReveal = Math.min(charactersToReveal, idxMax - idx);

            // Update the hidden image with the revealed characters
            StringBuilder updatedImage = new StringBuilder(currentImage);

            for (int i = 0; i < charactersToReveal; i++) {
                if (idx < idxMax) {
                    int colNumber = idx % col;
                    int rowNumber = idx / col;
                    hidden[rowNumber][colNumber] = original[rowNumber][colNumber];
                    idx++;

                    // Update the corresponding characters in the updated image
                    updatedImage.setCharAt((rowNumber * (col + 1)) + colNumber, original[rowNumber][colNumber]);
                } else {
                    break; // Stop if max replacements are reached
                }
            }

            // Return the updated image
            return updatedImage.toString();
        } else {
            // If less than 8 tasks are completed, return the current image without revealing any more
            return currentImage;
        }
    }

    public boolean checkAnswer(String answer) {
        return validAnswers.containsValue(Integer.parseInt(answer));
    }
}
