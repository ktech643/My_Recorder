package com.checkmate.android.model.Pieces;

import android.graphics.Color;

import com.checkmate.android.R;
import com.checkmate.android.model.GameObjects.Position;
import com.checkmate.android.model.Tools.ArrayDimensionConverter;

import java.util.ArrayList;

public class Rook extends AbstractPiece {

    public Rook(int color) {

        super(color);

        if (color == Color.WHITE){
            this.imageID = R.drawable.w_rook;
        } else {
            this.imageID = R.drawable.b_rook;
        }
    }

    @Override
    public ArrayList<Integer> getAloudMoves(Integer position, AbstractPiece[] board) {

        ArrayList<Integer> aloudMoves = new ArrayList<>();
        Position temp = ArrayDimensionConverter.pieceToTwoDimension(position);

        for (int i = 1; i < 8; i++){
            int newPosition = ArrayDimensionConverter.pieceToOneDimension(temp.c, temp.r+i);
            if (newPosition != 100 && board[newPosition] == null) {
                aloudMoves.add(newPosition);
            }
            else {
                if (newPosition < 64 && board[newPosition].color != board[position].color){
                    aloudMoves.add(newPosition);
                }
                i = 8;
            }
        }
        for (int i = 1; i < 8; i++){
            int newPosition = ArrayDimensionConverter.pieceToOneDimension(temp.c, temp.r-i);
            if (newPosition != 100 && board[newPosition] == null) {
                aloudMoves.add(newPosition);
            }
            else {
                if (newPosition < 64 && board[newPosition].color != board[position].color){
                    aloudMoves.add(newPosition);
                }
                i = 8;
            }
        }
        for (int i = 1; i < 8; i++){
            int newPosition = ArrayDimensionConverter.pieceToOneDimension(temp.c+i, temp.r);
            if (newPosition != 100 && board[newPosition] == null) {
                aloudMoves.add(newPosition);
            }
            else {
                if (newPosition < 64 && board[newPosition].color != board[position].color){
                    aloudMoves.add(newPosition);
                }
                i = 8;
            }
        }
        for (int i = 1; i < 8; i++){
            int newPosition = ArrayDimensionConverter.pieceToOneDimension(temp.c-i, temp.r);
            if (newPosition != 100 && board[newPosition] == null) {
                aloudMoves.add(newPosition);
            }
            else {
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
            return "Black Rook";
        }else {
            return "White Rook";
        }
    }
}