package com.checkmate.android.model.Tools;

import com.checkmate.android.model.GameObjects.Position;

import java.util.ArrayList;

public class ArrayDimensionConverter {

    public static ArrayList<Object> toOneDimension(Object[][] array){
        ArrayList<Object> temp = new ArrayList<>();
        for (int c = 0; c < array.length; c++){
            for (int r = 0; r < array.length; r++){
                temp.add(array[c][r]);

            }
        }
        return temp;
    }
    public static Object[][] toTwoDimension(Object[] array){
        int b = 0;
        Object[][] temp = new Object[8][8];
        for (int c = 0; c < 8; c++) {
            for (int r = 0; r < 8; r++) {
                temp[c][r] = array[b];
                b++;
            }
        }
        return temp;
    }

    public static int pieceToOneDimension(int c, int r){
        int temp = 0;
        for (int i = 0; i < 8; i++){
            for (int b = 0; b < 8; b++){
                if (i == c && r == b){
                    return temp;
                }
                temp++;
            }
        }
        return 100;
    }
    public static Position pieceToTwoDimension(int i){
        int temp = 0;
        for (int r = 0; r < 8; r++){
            for (int c = 0; c < 8; c++){
                if (temp == i){
                    return new Position(c, r);
                }
                temp++;
            }
        }
        return new Position();
    }
}
