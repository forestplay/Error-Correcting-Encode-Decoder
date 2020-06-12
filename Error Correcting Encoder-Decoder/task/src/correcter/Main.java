package correcter;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Write a mode: ");
        String mode = scanner.nextLine();

        String dataAsText;
        String dataAsBits;
        String dataBitsEncoded;
        String dataInHex;
        String dataBitsWithErrors;
        String dataBitsDecoded;
        String textDataOutput;
        String[] byteData;

        try {
            switch (mode) {
                case "encode":
                    BufferedReader inputFile = new BufferedReader(new FileReader("send.txt"));
                    dataAsText = inputFile.readLine();
                    inputFile.close();

                    dataAsBits = binView(dataAsText);

                    System.out.println("send.txt:");
                    System.out.println("text view: " + dataAsText);
                    System.out.println("hex view: " + hexView(dataAsBits));
                    System.out.println("bin view: " + dataAsBits);
                    System.out.println("");

                    dataBitsEncoded = encodeData(dataAsText, true);

                    System.out.println("encoded.txt:");
                    System.out.println("expand: " + encodeData(dataAsText, false));
                    System.out.println("parity: " + dataBitsEncoded);
                    System.out.println("hex view: " + hexView(dataBitsEncoded));

                    FileOutputStream encodedFileOut = new FileOutputStream("encoded.txt");
                    byteData = dataBitsEncoded.split(" ");
                    for (String b: byteData) {
                        encodedFileOut.write(Integer.parseInt(b, 2));
                    }
                    encodedFileOut.close();
                    break;
                case "send":
                    FileInputStream encodedFileIn = new FileInputStream("encoded.txt");
                    StringBuilder encodedData = new StringBuilder("");
                    while (true) {
                        int d = encodedFileIn.read();
                        if (d == -1) break;
                        encodedData.append(Integer.toBinaryString(0x100 + d).substring(1) + " ");
                    }
                    encodedFileIn.close();
                    dataBitsEncoded = encodedData.toString();

                    System.out.println("encoded.txt:");
                    System.out.println("hex view: " + hexView(dataBitsEncoded ));
                    System.out.println("bin view: " + dataBitsEncoded );
                    System.out.println("");

                    dataBitsWithErrors = errorData(dataBitsEncoded);

                    System.out.println("received.txt:");
                    System.out.println("bin view: " + dataBitsWithErrors);
                    System.out.println("hex view: " + hexView(dataBitsWithErrors));

                    FileOutputStream receivedText = new FileOutputStream("received.txt");
                    byteData = dataBitsWithErrors.split(" ");
                    for (String b: byteData) {
                        receivedText.write(Integer.parseInt(b, 2));
                    }
                    receivedText.close();
                    break;
                case "decode":
                    FileInputStream receivedFileIn = new FileInputStream("received.txt");
                    StringBuilder errorData = new StringBuilder("");
                    while (true) {
                        int d = receivedFileIn.read();
                        if (d == -1) break;
                        errorData.append(Integer.toBinaryString(0x100 + d).substring(1) + " ");
                    }
                    receivedFileIn.close();
                    dataBitsWithErrors = errorData.toString();

                    System.out.println("received.txt:");
                    System.out.println("hex view: " + hexView(dataBitsWithErrors));
                    System.out.println("bin view: " + dataBitsWithErrors);
                    System.out.println("");

                    dataAsBits = correctData(dataBitsWithErrors);
                    dataBitsDecoded = remove(decodeData(dataAsBits));
                    textDataOutput = bitsToChar(dataBitsDecoded);

                    System.out.println("decoded.txt:");
                    System.out.println("correct: " + dataAsBits);
                    System.out.println("decode: " + decodeData(dataAsBits));
                    System.out.println("remove: " + dataBitsDecoded);
                    System.out.println("hex view: " + hexView(dataBitsDecoded));
                    System.out.println("text view: " + textDataOutput);

                    FileWriter decodedText = new FileWriter("decoded.txt");
                    decodedText.write(textDataOutput);
                    decodedText.close();
                    break;
            }
        } catch (
                Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static String bitsToChar(String inputData) {
        StringBuilder result = new StringBuilder("");
        String[] byteData = inputData.split(" ");
        for (String b : byteData) {
            int parseInt = Integer.parseInt(b, 2);
            result.append((char) parseInt);
        }
        return result.toString();
    }

    public static String hexToBits(String inputData) {
        StringBuilder result = new StringBuilder("");
        for (String s : inputData.split(" ")) {
            String binaryString = (new BigInteger(s, 16).add(new BigInteger("256"))).toString(2).substring(1);
            result.append(binaryString + " ");
        }
        return result.toString();
    }

    public static String hexView(String inputData) {
        StringBuilder result = new StringBuilder("");
        for (String b : inputData.split(" ")) {
            result.append(String.format("%02x", Integer.parseInt(b, 2)) + " ");
        }
        return result.toString().toUpperCase().trim();
    }

    public static String binView(String inputData) {
        StringBuilder result = new StringBuilder("");
        for (char c : inputData.toCharArray()) {
            result.append(Integer.toBinaryString(0x100 + c).substring(1) + " ");
        }
        return result.toString();
    }

    public static String errorData(String binData) throws Exception {
        StringBuilder result = new StringBuilder("");
        for (String b : binData.split(" ")) {
            result.append(byteWithError(b) + " ");
        }
        return result.toString();
    }

    public static String byteWithError(String inputData) throws Exception {
        Random random = new Random(System.currentTimeMillis() + (long) Math.pow(Integer.parseInt(inputData, 2), 2));
        return flipBit(inputData, random.nextInt(inputData.length()));
    }

    public static String encodeData(String inputData, boolean withParity) throws Exception {
        String binData = binView(inputData).replaceAll(" ", "");
        StringBuilder result = new StringBuilder("");
        for (int i = 0; i < binData.length(); i++) {
            result.append(binData.charAt(i)).append(binData.charAt(i));
            while ((i == (binData.length() - 1)) && (result.substring(result.lastIndexOf(" ") + 1).length() < 6))
                result.append(withParity ? "00" : "..");
            if ((((i + 1) % 3) == 0) | (i == (binData.length() - 1)))
                result.append((withParity ? paritySuffix(result.toString()) : "..") + " ");
        }
        return result.toString().trim();
    }

    public static String decodeData(String inputData) {
        StringBuilder partialResult = new StringBuilder("");
        for (String b : inputData.split(" ")) {
            partialResult.append(b.charAt(0)).append(b.charAt(2)).append(b.charAt(4));
        }

        StringBuilder result = new StringBuilder("");
        while (partialResult.length() > 0) {
            result.append(partialResult.substring(0, Integer.min(8, partialResult.length())) + " ");
            partialResult.delete(0, Integer.min(8, partialResult.length()));
        }
        return result.toString().trim();
    }

    public static String remove(String inputData) {
        StringBuilder result = new StringBuilder("");
        for (String b : inputData.split(" ")) {
            if (b.length() == 8)
                result.append(b + " ");
        }
        return result.toString();
    }

    public static String correctData(String inputData) throws Exception {
        StringBuilder result = new StringBuilder("");
        for (String b : inputData.split(" ")) {
            if (getBit(b, 6) == getBit(b, 7)) {
                // error is with one of the data bits
                for (int i = 0; i < 6; i += 2) {
                    if (b.charAt(i) == b.charAt(i + 1)) {
                        // this data is good
                        result.append(Character.toString(b.charAt(i)) + Character.toString(b.charAt(i)));
                    } else {
                        // this data is bad, good value is inverse of parity
                        StringBuilder parityData = new StringBuilder("");
                        switch (i) {
                            case 0:
                                parityData.append(b.substring(2));
                                break;
                            case 2:
                                parityData.append(b.substring(0, 1)).append(b.substring(4));
                                break;
                            case 4:
                                parityData.append(b.substring(0, 3)).append(b.substring(6));
                                break;
                        }
                        String correctedData = paritySuffix(parityData.toString());
                        result.append(correctedData);
                    }
                }
                result.append(b.substring(6) + " ");
            } else {
                // error is with parity, send good data and new parity suffix
                result.append(b.substring(0, 6));
                result.append(paritySuffix(result.toString()) + " ");
            }
        }
        return result.toString();
    }

    public static String paritySuffix(String inputData) {
        int sumThreeBits = (inputData.charAt(inputData.length() - 1) == '1' ? 1 : 0)
                + (inputData.charAt(inputData.length() - 3) == '1' ? 1 : 0)
                + (inputData.charAt(inputData.length() - 5) == '1' ? 1 : 0);
        if (sumThreeBits % 2 == 0)
            return "00";
        else
            return "11";
    }

    public static boolean getBit(String input, int position) throws Exception {
        if (position < 0 || position > input.length() - 1)
            throw new Exception("position out of bounds");
        return input.charAt(position) == '1' ? true : false;
    }

    public static String setBit(String input, int position, boolean value) throws Exception {
        if (position < 0 || position > input.length() - 1)
            throw new Exception("position out of bounds");
        StringBuilder result = new StringBuilder(input);
        if (value == true)
            result.setCharAt(position, '1');
        else
            result.setCharAt(position, '0');
        return result.toString();
    }

    public static String flipBit(String input, int position) throws Exception {
        return setBit(input, position, !getBit(input, position));
    }
}

