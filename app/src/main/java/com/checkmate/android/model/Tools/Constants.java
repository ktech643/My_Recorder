package com.checkmate.android.model.Tools;

import android.graphics.Color;

import com.checkmate.android.R;
import com.checkmate.android.model.Pieces.AbstractPiece;
import com.checkmate.android.model.Pieces.Bishop;
import com.checkmate.android.model.Pieces.King;
import com.checkmate.android.model.Pieces.Knight;
import com.checkmate.android.model.Pieces.Pawn;
import com.checkmate.android.model.Pieces.Queen;
import com.checkmate.android.model.Pieces.Rook;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

public class Constants {

    public static final int dark = Color.parseColor("#696969");
    public static final int light = Color.parseColor("#b0b0b0");
    public static final String[] characters = {"a", "b", "c", "d", "e", "f", "g", "h", "i"};
//
//    public static final int[] newBoardColors = {
//            light, dark, light, dark, light, dark, light, dark,
//            dark, light, dark, light, dark, light, dark, light,
//            light, dark, light, dark, light, dark, light, dark,
//            dark, light, dark, light, dark, light, dark, light,
//            light, dark, light, dark, light, dark, light, dark,
//            dark, light, dark, light, dark, light, dark, light,
//            light, dark, light, dark, light, dark, light, dark,
//            dark, light, dark, light, dark, light, dark, light};

    public static final int[] newBoardBackground = {
            R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border,
            R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border,
            R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border,
            R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border,
            R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border,
            R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border,
            R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border,
            R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border, R.drawable.dark_no_border, R.drawable.light_no_border};

    public static final AbstractPiece[] newBoard = {
            new Rook(BLACK), new Knight(BLACK), new Bishop(BLACK), new Queen(BLACK), new King(BLACK), new Bishop(BLACK), new Knight(BLACK), new Rook(BLACK),
            new Pawn(BLACK), new Pawn(BLACK), new Pawn(BLACK), new Pawn(BLACK), new Pawn(BLACK), new Pawn(BLACK), new Pawn(BLACK), new Pawn(BLACK),
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            new Pawn(WHITE), new Pawn(WHITE), new Pawn(WHITE), new Pawn(WHITE), new Pawn(WHITE), new Pawn(WHITE), new Pawn(WHITE), new Pawn(WHITE),
            new Rook(WHITE), new Knight(WHITE), new Bishop(WHITE), new Queen(WHITE), new King(WHITE), new Bishop(WHITE), new Knight(WHITE), new Rook(WHITE)
    };

    public static final AbstractPiece[] newTMPBoard = {
            new Rook(BLACK), new Knight(BLACK), new Bishop(BLACK), new Queen(BLACK), new King(BLACK), new Bishop(BLACK), new Knight(BLACK), new Rook(BLACK),
            new Pawn(BLACK), new Pawn(BLACK), new Pawn(BLACK), new Pawn(BLACK), new Pawn(BLACK), new Pawn(BLACK), new Pawn(BLACK), new Pawn(BLACK),
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            new Pawn(WHITE), new Pawn(WHITE), new Pawn(WHITE), new Pawn(WHITE), new Pawn(WHITE), new Pawn(WHITE), new Pawn(WHITE), new Pawn(WHITE),
            new Rook(WHITE), new Knight(WHITE), new Bishop(WHITE), new Queen(WHITE), new King(WHITE), new Bishop(WHITE), new Knight(WHITE), new Rook(WHITE)
    };

    public static int getBoarderedBackground(int position) {
        int[] array = newBoardBackground;
        int val = array[position];
        if (val == R.drawable.light_no_border) {
            return R.drawable.light_border;
        } else {
            return R.drawable.dark_border;
        }
    }

    public static final AbstractPiece[] emptyBoard = {

    };
}
