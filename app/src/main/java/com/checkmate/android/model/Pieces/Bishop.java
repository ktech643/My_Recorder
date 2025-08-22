package com.checkmate.android.model.Pieces;

import android.graphics.Color;

import com.checkmate.android.R;
import com.checkmate.android.model.GameObjects.Position;
import com.checkmate.android.model.Tools.ArrayDimensionConverter;

import java.util.ArrayList;

public class Bishop extends AbstractPiece {

    public Bishop(int color) {

        super(color);

        if (color == Color.WHITE){
            this.imageID = R.drawable.w_bishop;
        } else {
            this.imageID = R.drawable.b_bishop;
        }
    }

    @Override
    public ArrayList<Integer> getAloudMoves(Integer position, AbstractPiece[] board) {
        ArrayList<Integer> aloudMoves = new ArrayList<>();
        Position temp = ArrayDimensionConverter.pieceToTwoDimension(position);

        // TODO: Gotta fix the way i do this
        for (int i = 1; i < 8; i++){
            int newPosition = ArrayDimensionConverter.pieceToOneDimension(temp.c-i, temp.r+i);
            if (newPosition != 100 && board[newPosition] == null) {
                aloudMoves.add(newPosition);
            }
            else {
                System.out.println("ELSE");
                if (newPosition < 64 && board[newPosition].color != board[position].color){
                    aloudMoves.add(newPosition);
                }
                i = 8;
            }
        }
        for (int i = 1; i < 8; i++){
            int newPosition = ArrayDimensionConverter.pieceToOneDimension(temp.c-i, temp.r-i);
            if (newPosition != 100 && board[newPosition] == null) {
                aloudMoves.add(newPosition);
            }
            else {
                System.out.println("ELSE");
                if (newPosition < 64 && board[newPosition].color != board[position].color){
                    aloudMoves.add(newPosition);
                }
                i = 8;
            }
        }
        for (int i = 1; i < 8; i++){
            int newPosition = ArrayDimensionConverter.pieceToOneDimension(temp.c+i, temp.r-i);
            if (newPosition != 100 && board[newPosition] == null) {
                aloudMoves.add(newPosition);
            }
            else {
                System.out.println("ELSE");
                if (newPosition < 64 && board[newPosition].color != board[position].color){
                    aloudMoves.add(newPosition);
                }
                i = 8;
            }
        }
        for (int i = 1; i < 8; i++){
            int newPosition = ArrayDimensionConverter.pieceToOneDimension(temp.c+i, temp.r+i);
            if (newPosition != 100 && board[newPosition] == null) {
                aloudMoves.add(newPosition);
            }
            else {
                System.out.println("ELSE");
                if (newPosition < 64 && board[newPosition].color != board[position].color){
                    aloudMoves.add(newPosition);
                }
                i = 8;
            }
        }
        return aloudMoves;
    }

    @Override
    public String toString() {
        if (color == Color.BLACK){
            return "Black Bishop";
        }else {
            return "White Bishop";
        }
    }
}