package com.checkmate.android.model.Pieces;

import android.graphics.Color;

import com.checkmate.android.R;
import com.checkmate.android.model.GameObjects.Position;
import com.checkmate.android.model.Tools.ArrayDimensionConverter;

import java.util.ArrayList;

public class King extends AbstractPiece {

    public King(int color) {
        super(color);
        if (color == Color.WHITE){
            this.imageID = R.drawable.w_king;
        } else {
            this.imageID = R.drawable.b_king;
        }
    }

    @Override
    public ArrayList<Integer> getAloudMoves(Integer position, AbstractPiece[] board) {

        ArrayList<Integer> aloudMoves = new ArrayList<>();
        Position temp2D = ArrayDimensionConverter.pieceToTwoDimension(position);

        int newPosition = ArrayDimensionConverter.pieceToOneDimension(temp2D.c-1, temp2D.r);
        if (newPosition != 100 && board[newPosition] == null){
            aloudMoves.add(newPosition);

        }
        newPosition = ArrayDimensionConverter.pieceToOneDimension(temp2D.c-1, temp2D.r+1);
        if (newPosition != 100 && board[newPosition] == null){
            aloudMoves.add(newPosition);
        }
        newPosition = ArrayDimensionConverter.pieceToOneDimension(temp2D.c, temp2D.r+1);
        if (newPosition != 100 && board[newPosition] == null){
            aloudMoves.add(newPosition);
        }
        newPosition = ArrayDimensionConverter.pieceToOneDimension(temp2D.c+1, temp2D.r+1);
        if (newPosition != 100 && board[newPosition] == null){
            aloudMoves.add(newPosition);
        }
        newPosition = ArrayDimensionConverter.pieceToOneDimension(temp2D.c+1, temp2D.r);
        if (newPosition != 100 && board[newPosition] == null){
            aloudMoves.add(newPosition);
        }
        newPosition = ArrayDimensionConverter.pieceToOneDimension(temp2D.c+1, temp2D.r-1);
        if (newPosition != 100 && board[newPosition] == null){
            aloudMoves.add(newPosition);
        }
        newPosition = ArrayDimensionConverter.pieceToOneDimension(temp2D.c, temp2D.r-1);
        if (newPosition != 100 && board[newPosition] == null){
            aloudMoves.add(newPosition);
        }
        newPosition = ArrayDimensionConverter.pieceToOneDimension(temp2D.c-1, temp2D.r-1);
        if (newPosition != 100 && board[newPosition] == null){
            aloudMoves.add(newPosition);
        }

        return aloudMoves;
    }

    @Override
    public String toString() {
        if (color == Color.BLACK){
            return "Black King";
        }else {
            return "White King";
        }
    }
}
