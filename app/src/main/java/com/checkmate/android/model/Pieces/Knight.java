package com.checkmate.android.model.Pieces;

import android.graphics.Color;

import com.checkmate.android.R;
import com.checkmate.android.model.GameObjects.Position;
import com.checkmate.android.model.Tools.ArrayDimensionConverter;

import java.util.ArrayList;

public class Knight extends AbstractPiece {

    public Knight(int color) {
        super(color);
        if (color == Color.WHITE) {
            this.imageID = R.drawable.w_knight;
        } else {
            this.imageID = R.drawable.b_knight;
        }
    }

    @Override
    public ArrayList<Integer> getAloudMoves(Integer position, AbstractPiece[] board) {

        ArrayList<Integer> aloudMoves = new ArrayList<>();
        Position temp2D = ArrayDimensionConverter.pieceToTwoDimension(position);

        int pos1 = ArrayDimensionConverter.pieceToOneDimension(temp2D.c-2, temp2D.r-1);
        int pos2 = ArrayDimensionConverter.pieceToOneDimension(temp2D.c-2, temp2D.r+1);
        int pos3 = ArrayDimensionConverter.pieceToOneDimension(temp2D.c+2, temp2D.r-1);
        int pos4 = ArrayDimensionConverter.pieceToOneDimension(temp2D.c+2, temp2D.r+1);
        int pos5 = ArrayDimensionConverter.pieceToOneDimension(temp2D.c-1, temp2D.r+2);
        int pos6 = ArrayDimensionConverter.pieceToOneDimension(temp2D.c-1, temp2D.r-2);
        int pos7 = ArrayDimensionConverter.pieceToOneDimension(temp2D.c+1, temp2D.r+2);
        int pos8 = ArrayDimensionConverter.pieceToOneDimension(temp2D.c+1, temp2D.r-2);

        int[] temp = {pos1, pos2, pos3, pos4, pos5, pos6, pos7, pos8};

        for (int i = 0; i < temp.length; i++){
            if (temp[i] !=100 && board[temp[i]] == null){
                aloudMoves.add(temp[i]);
            }
            else {
                if (temp[i] !=100 && board[temp[i]].color != board[position].color){
                    aloudMoves.add(temp[i]);
                }
            }
        }
        System.out.println(aloudMoves);
        return aloudMoves;
    }

    @Override
    public String toString() {
        if (color == Color.BLACK){
            return "Black Knight";
        }else {
            return "White Knight";
        }
    }
}