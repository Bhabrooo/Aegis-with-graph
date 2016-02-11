package com.jamal.aegistest;

/**
 * Created by Jamal on 28-Jan-16.
 */

import org.apache.http.util.ByteArrayBuffer;

public class HexAsciiHelper {
    public static int PRINTABLE_ASCII_MIN = 0x20; // ' '
    public static int PRINTABLE_ASCII_MAX = 0x7E; // '~'

    public static boolean isPrintableAscii(int c) {
        return c >= PRINTABLE_ASCII_MIN && c <= PRINTABLE_ASCII_MAX;
    }

    public static String bytesToHex(byte[] data) {
        return bytesToHex(data, 0, data.length);
    }

    public static String bytesToHex(byte[] data, int offset, int length) {
        if (length <= 0) {
            return "";
        }

        StringBuilder hex = new StringBuilder();
        for (int i = offset; i < offset + length; i++) {
            hex.append(String.format(" %02X", data[i] % 0xFF));
        }
        hex.deleteCharAt(0);
        return hex.toString();
    }

    public static String bytesToAsciiMaybe(byte[] data) {
        return bytesToAsciiMaybe(data, 0, data.length);
    }

    public static String bytesToAsciiMaybe(byte[] data, int offset, int length) {
        StringBuilder ascii = new StringBuilder();
        boolean zeros = false;
        for (int i = offset; i < offset + length; i++) {
            int c = data[i] & 0xFF;
            if (isPrintableAscii(c)) {
                if (zeros) {
                    return null;
                }
                ascii.append((char) c);
            } else if (c == 0) {
                zeros = true;
            } else {
                return null;
            }
        }
        return ascii.toString();
    }

    public static double[] bytestodouble(byte[] data){
        //comvert incoming byte stream to ASCII string
        String temporary_string=HexAsciiHelper.bytesToAsciiMaybe(data);

        //split values based on comma
        // values sent from RFduino are in order [ECG, ACC_X, ACC_Y, ACC_Z]
        String separated[] = temporary_string.split(",");

        //generate array for number of values found
        double output[]= new double[separated.length];

        //convert the separated values from String to int
        for (int i=0; i<separated.length; i++){
            output[i] = Double.parseDouble(separated[i]);
        }

        // output has values stored with offsets used for consistency of number of sent bytes
        // output[0]= ECG+1000
        // output[1]= ACC_X+26
        // output[2]= ACC_Y+26
        // output[3]= ACC_Z+26
        //output[0]= output[0]-1000;
        //output[1]= output[1]-26;
        //output[2]= output[2]-26;
        //output[3]= output[3]-26;
        return output;
    };

    public static float[] bytestofloat(byte[] data){
        //comvert incoming byte stream to ASCII string
        String temporary_string=HexAsciiHelper.bytesToAsciiMaybe(data);

        //split values based on comma
        // values sent from RFduino are in order [ECG, ACC_X, ACC_Y, ACC_Z]
        String separated[] = temporary_string.split(",");

        //generate array for number of values found
        float output[]= new float[separated.length];

        //convert the separated values from String to int
        for (int i=0; i<separated.length; i++){
            output[i] = Float.parseFloat(separated[i]);
        }

        // output has values stored with offsets used for consistency of number of sent bytes
        // output[0]= ECG+1000
        // output[1]= ACC_X+26
        // output[2]= ACC_Y+26
        // output[3]= ACC_Z+26
        //output[0]= output[0]-1000;
        //output[1]= output[1]-26;
        //output[2]= output[2]-26;
        //output[3]= output[3]-26;
        return output;
    };

    public static byte[] hexToBytes(String hex) {
        ByteArrayBuffer bytes = new ByteArrayBuffer(hex.length() / 2);
        for (int i = 0; i < hex.length(); i++) {
            if (hex.charAt(i) == ' ') {
                continue;
            }

            String hexByte;
            if (i + 1 < hex.length()) {
                hexByte = hex.substring(i, i + 2).trim();
                i++;
            } else {
                hexByte = hex.substring(i, i + 1);
            }

            bytes.append(Integer.parseInt(hexByte, 16));
        }
        return bytes.buffer();
    }
}