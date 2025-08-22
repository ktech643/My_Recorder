package com.checkmate.android.model.Pieces;

import java.util.ArrayList;

public abstract class AbstractPiece {

    public int color;
    public int imageID;

    public AbstractPiece(int color) {
        this.color = color;
    }

    // TODO: Make this return a Set Collection to prevent duplication
    public abstract ArrayList<Integer> getAloudMoves(Integer position, AbstractPiece[] board);

}
