package correcter;

import java.io.*;
import java.math.BigInteger;
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

                    dataBitsEncoded = encodeDataHamming(dataAsText, true);
//                    dataBitsEncoded = encodeData3bitPerByte(dataAsText, true);

                    System.out.println("encoded.txt:");
                    System.out.println("expand: " + encodeDataHamming(dataAsText, false));
//                    System.out.println("expand: " + encodeData3bitPerByte(dataAsText, false));
                    System.out.println("parity: " + dataBitsEncoded);
                    System.out.println("hex view: " + hexView(dataBitsEncoded));

                    FileOutputStream encodedFileOut = new FileOutputStream("encoded.txt");
                    byteData = dataBitsEncoded.split(" ");
                    for (String b : byteData) {
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
                    System.out.println("hex view: " + hexView(dataBitsEncoded));
                    System.out.println("bin view: " + dataBitsEncoded);
                    System.out.println("");

                    dataBitsWithErrors = errorData(dataBitsEncoded);

                    System.out.println("received.txt:");
                    System.out.println("bin view: " + dataBitsWithErrors);
                    System.out.println("hex view: " + hexView(dataBitsWithErrors));

                    FileOutputStream receivedText = new FileOutputStream("received.txt");
                    byteData = dataBitsWithErrors.split(" ");
                    for (String b : byteData) {
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

                    dataAsBits = correctDataHamming(dataBitsWithErrors);
                    dataBitsDecoded = removeExtraBits(decodeDataHamming(dataAsBits));
//                    dataAsBits = correctData3bitPerByte(dataBitsWithErrors);
//                    dataBitsDecoded = removeExtraBits(decodeData3bitPerByte(dataAsBits));
                    textDataOutput = bytesToChar(dataBitsDecoded);

                    System.out.println("decoded.txt:");
                    System.out.println("correct: " + dataAsBits);
                    System.out.println("decode: " + decodeDataHamming(dataAsBits));
//                    System.out.println("decode: " + decodeData3bitPerByte(dataAsBits));
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

    public static String bytesToChar(String inputData) {
        // for string of bytes in binary form, return string of characters
        StringBuilder result = new StringBuilder("");
        String[] byteData = inputData.split(" ");
        for (String b : byteData) {
            int parseInt = Integer.parseInt(b, 2);
            result.append((char) parseInt);
        }
        return result.toString();
    }

    public static String hexView(String inputData) {
        // takes string of bytes in binary form, return string of words in hex
        StringBuilder result = new StringBuilder("");
        for (String b : inputData.split(" ")) {
            result.append(String.format("%02x", Integer.parseInt(b, 2)) + " ");
        }
        return result.toString().toUpperCase().trim();
    }

    public static String binView(String inputData) {
        // takes string of characters and return string of bytes in binary form
        StringBuilder result = new StringBuilder("");
        for (char c : inputData.toCharArray()) {
            result.append(Integer.toBinaryString(0x100 + c).substring(1) + " ");
        }
        return result.toString();
    }

    public static String errorData(String binData) throws Exception {
        // for string of bytes in binary form, injects one bit per byte
        StringBuilder result = new StringBuilder("");
        for (String b : binData.split(" ")) {
            result.append(byteWithError(b) + " ");
        }
        return result.toString();
    }

    public static String byteWithError(String inputData) throws Exception {
        // takes byte of binary digits and randomly flips one bit, returns string of bits
        Random random = new Random(System.currentTimeMillis() + (long) Math.pow(Integer.parseInt(inputData, 2), 2));
        return flipBit(inputData, random.nextInt(inputData.length()));
    }

    public static String encodeDataHamming(String inputData, boolean withParity) throws Exception {
        // takes string of bits and encodes them with Hamming code [7,4]
        String binData = binView(inputData).replaceAll(" ", "");
        StringBuilder result = new StringBuilder("");

        for (int bit = 0; bit < binData.length(); bit += 4) {
            StringBuilder oneByte = new StringBuilder("");
            oneByte.append(".." + (getBit(binData, bit) == true ? "1" : "0"))
                    .append("." + (getBit(binData, bit + 1) == true ? "1" : "0"))
                    .append(getBit(binData, bit + 2) == true ? "1" : "0")
                    .append(getBit(binData, bit + 3) == true ? "1" : "0")
                    .append(". ");
            if (withParity)
                result.append(addHammingParity(oneByte.toString()));
            else
                result.append(oneByte);
        }
        return result.toString().trim();
    }

    public static String correctDataHamming(String inputData) throws Exception {
        // takes string of bytes in binary form and corrects for any errors using Hamming code [7,4]
        // returns string with errors corrected
        StringBuilder result = new StringBuilder("");

        for (String oneByte: inputData.split(" ")){
            int p1 = (getBit(oneByte, 0) ? 1 : 0);
            int p2 = (getBit(oneByte, 1) ? 1 : 0);
            int d3 = (getBit(oneByte, 2) ? 1 : 0);
            int p4 = (getBit(oneByte, 3) ? 1 : 0);
            int d5 = (getBit(oneByte, 4) ? 1 : 0);
            int d6 = (getBit(oneByte, 5) ? 1 : 0);
            int d7 = (getBit(oneByte, 6) ? 1 : 0);

            // first check for bad parity bit
            int p1Bad = (p1 != ((d3 + d5 + d7) % 2)) ? 1 : 0;
            int p2Bad = (p2 != ((d3 + d6 + d7) % 2)) ? 1 : 0;
            int p4Bad = (p4 != ((d5 + d6 + d7) % 2)) ? 1 : 0;
            if (p1Bad + p2Bad + p4Bad == 1) {
                // error is with parity bits
                if (p1Bad == 1)
                    result.append(flipBit(oneByte, 0));
                else if (p2Bad == 1)
                    result.append(flipBit(oneByte, 1));
                else  // (p1Bad == 1)
                    result.append(flipBit(oneByte, 3));
            } else {
                // error is with data bits
                if (p1Bad + p2Bad + p4Bad == 3)
                    result.append(flipBit(oneByte, 6));
                else if (p1Bad + p2Bad == 2)
                    result.append(flipBit(oneByte, 2));
                else if (p1Bad + p4Bad == 2)
                    result.append(flipBit(oneByte, 4));
                else if (p2Bad + p4Bad == 2)
                    result.append(flipBit(oneByte, 5));
                else if (p1Bad + p2Bad + p4Bad == 0) {
                    // error in unused last bit
                    result.append(flipBit(oneByte, 7));
                } else {
                    throw new Exception("Unexpected result in correctDataHamming method: " + oneByte
                    + "\ninputData: " + inputData);
                }
            }
            result.append(' ');
        }
        return result.toString();
    }

    public static String decodeDataHamming(String inputData) {
        // takes string of bits and removes Hamming code, returns bytes in binary form
        StringBuilder result = new StringBuilder("");
        String[] inputBytes = inputData.split(" ");
        for (int i = 0; i < inputBytes.length; i++) {
            result.append(inputBytes[i].charAt(2)).append(inputBytes[i].substring(4, 7));
            if (i % 2 == 1)
                result.append(' ');
        }

        return result.toString().trim();
    }

    public static String addHammingParity(String inputData) throws Exception {
        // takes a byte in bit form and returns with byte  with parity bits inserted
        StringBuilder result = new StringBuilder(inputData);
        int d3 = (getBit(inputData, 2) ? 1 : 0);
        int d5 = (getBit(inputData, 4) ? 1 : 0);
        int d6 = (getBit(inputData, 5) ? 1 : 0);
        int d7 = (getBit(inputData, 6) ? 1 : 0);

        char p1 = ((d3 + d5 + d7) % 2 == 1) ? '1' : '0';
        char p2 = ((d3 + d6 + d7) % 2 == 1) ? '1' : '0';
        char p4 = ((d5 + d6 + d7) % 2 == 1) ? '1' : '0';

        result.setCharAt(0, p1);
        result.setCharAt(1, p2);
        result.setCharAt(3, p4);
        result.setCharAt(7, '0');

        return result.toString();
    }

    public static String encodeData3bitPerByte(String inputData, boolean withParity) throws Exception {
        // takes string of bits and encodes them in groups of three with a parity bit, each bit duplicated
        String binData = binView(inputData).replaceAll(" ", "");
        StringBuilder result = new StringBuilder("");
        for (int i = 0; i < binData.length(); i++) {
            result.append(binData.charAt(i)).append(binData.charAt(i));
            while ((i == (binData.length() - 1)) && (result.substring(result.lastIndexOf(" ") + 1).length() < 6))
                result.append(withParity ? "00" : "..");
            if ((((i + 1) % 3) == 0) | (i == (binData.length() - 1)))
                result.append((withParity ? paritySuffix3BitPerByte(result.toString()) : "..") + " ");
        }
        return result.toString().trim();
    }

    public static String correctData3bitPerByte(String inputData) throws Exception {
        // takes string of bytes in binary form and corrects for any errors using parity bit
        // returns string with errors corrected
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
                        String correctedData = paritySuffix3BitPerByte(parityData.toString());
                        result.append(correctedData);
                    }
                }
                result.append(b.substring(6) + " ");
            } else {
                // error is with parity, send good data and new parity suffix
                result.append(b.substring(0, 6));
                result.append(paritySuffix3BitPerByte(result.toString()) + " ");
            }
        }
        return result.toString();
    }

    public static String decodeData3bitPerByte(String inputData) {
        // takes string of bits and removes duplication and parity, returns bytes in binary form
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

    public static String paritySuffix3BitPerByte(String inputData) {
        // takes string of six bits and returns parity bit twice
        int sumThreeBits = (inputData.charAt(inputData.length() - 1) == '1' ? 1 : 0)
                + (inputData.charAt(inputData.length() - 3) == '1' ? 1 : 0)
                + (inputData.charAt(inputData.length() - 5) == '1' ? 1 : 0);
        if (sumThreeBits % 2 == 0)
            return "00";
        else
            return "11";
    }

    public static String removeExtraBits(String inputData) {
        // takes string of bits in binary form and returns string with partial bytes removed
        StringBuilder result = new StringBuilder("");
        for (String b : inputData.split(" ")) {
            if (b.length() == 8)
                result.append(b + " ");
        }
        return result.toString();
    }

    public static boolean getBit(String input, int position) throws Exception {
        // return boolean bit value of bit in input at given position
        if (position < 0 || position > input.length() - 1)
            throw new Exception("position out of bounds");
        return input.charAt(position) == '1' ? true : false;
    }

    public static String setBit(String input, int position, boolean value) throws Exception {
        // return string of bits where bit of input at given position has given value
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
        // return string of bits where bit of input at given position has been flipped
        return setBit(input, position, !getBit(input, position));
    }

    public static String hexToBits(String inputData) {
        // takes string of hex words and return string of bytes in binary form
        StringBuilder result = new StringBuilder("");
        for (String s : inputData.split(" ")) {
            String binaryString = (new BigInteger(s, 16).add(new BigInteger("256"))).toString(2).substring(1);
            result.append(binaryString + " ");
        }
        return result.toString();
    }

}

