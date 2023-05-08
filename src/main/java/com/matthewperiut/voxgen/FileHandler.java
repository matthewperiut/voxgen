package com.matthewperiut.voxgen;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class FileHandler {

    public static void appendTextToFile(String filename, String text) throws IOException {
        FileOutputStream output = new FileOutputStream(filename, true);
        output.write(('\n' + text).getBytes());
        output.close();
    }

    public static String readTextFromEndOfFile(String filename) throws IOException {
        RandomAccessFile file = new RandomAccessFile(filename, "r");
        long file_size = file.length();
        file.seek(file_size - 1);

        StringBuilder textBuilder = new StringBuilder();
        char ch = 0;
        while (file.getFilePointer() > 0) {
            ch = (char)file.readByte();
            if (ch == '\n' || ch == '\r') {
                break;
            }
            file.seek(file.getFilePointer() - 2);
        }

        if (ch == '\r' && file.getFilePointer() < file_size - 1) {
            ch = (char)file.readByte();
            if (ch != '\n') {
                file.seek(file.getFilePointer() - 1);
            }
        }

        textBuilder.append(file.readLine());
        file.close();

        return textBuilder.toString();
    }

    public static void removeCustomMetadata(String filename) throws IOException {
        byte[] iend_chunk = {0x49, 0x45, 0x4E, 0x44}; // IEND in ASCII
        RandomAccessFile file = new RandomAccessFile(filename, "rw");
        long file_size = file.length();
        file.seek(0);

        byte[] buffer = new byte[(int)file_size];
        file.read(buffer);

        int iend_pos = indexOf(buffer, iend_chunk, 0);

        if (iend_pos == -1) {
            throw new RuntimeException("IEND chunk not found");
        }

        iend_pos += iend_chunk.length + 4; // Move to the position after IEND chunk and its CRC

        byte[] newBuffer = Arrays.copyOfRange(buffer, 0, iend_pos);

        file.seek(0);
        file.write(newBuffer);
        file.setLength(newBuffer.length);
        file.close();
    }

    private static int indexOf(byte[] haystack, byte[] needle, int fromIndex) {
        int haystackLength = haystack.length;
        int needleLength = needle.length;
        if (needleLength == 0) {
            return 0;
        }
        outer:
        for (int i = fromIndex; i < haystackLength - needleLength + 1; i++) {
            for (int j = 0; j < needleLength; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public static void writeSizeMetadata(String filepath, int a, int b, int c) {
        String text = a + "x" + b + "x" + c;
        try {
            removeCustomMetadata(filepath);
            appendTextToFile(filepath, text);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
