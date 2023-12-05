package org.example;

public class Utils {
    public static int indexOf(byte[] bigArray, byte[] smallArray, int start, int end) {
        int result = -1;
        for (int i = start; i < end - smallArray.length + 1; i++) {
            result = i;
            for (int j = 0; j < smallArray.length; j++) {
                if (bigArray[i + j] != smallArray[j]) {
                    result = -1;
                    break;
                }
            }
            if (result > -1) {
                return result;
            }
        }
        return result;
    }
}
