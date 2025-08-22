package com.checkmate.android.model.GameObjects;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.checkmate.android.AppPreference;
import com.checkmate.android.adapter.GridViewAdapter;
import com.checkmate.android.model.Pieces.AbstractPiece;
import com.checkmate.android.model.Pieces.King;
import com.checkmate.android.model.Tools.Constants;
import com.checkmate.android.ui.activity.ChessActivity;

import java.util.ArrayList;

public class Match {

    private Context context = null;
    private GridView gridView = null;
    private GridViewAdapter adapter = null;
    private AbstractPiece[] board = {};
    private TextView textView;
    boolean is_refresh = false;

    String pin_code = "";

    public Match(Context context, GridView gridView, TextView textView) {
        this.context = context;
        this.gridView = gridView;
        this.textView = textView;

        board = Constants.newBoard;
        adapter = new GridViewAdapter(context, board);
        gridView.setAdapter(adapter);
    }

    public void refresh() {
        is_refresh = !is_refresh;
        AbstractPiece[] board_refresh;
        if (is_refresh) {
            board_refresh = Constants.newTMPBoard;
        } else {
            board_refresh = Constants.newBoard;
        }
        this.board = new AbstractPiece[board_refresh.length];
        for (int i = 0; i < board_refresh.length; i++) {
            this.board[i] = board_refresh[i];
        }
        if (is_refresh) {
            adapter.setBoard(Constants.newTMPBoard);
        } else {
            adapter.setBoard(Constants.newBoard);
        }
    }

    public void start() {
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            private boolean isFirstClick = true;
            private int clickedTile = 99;
            private AbstractPiece pieceOnTile = null;
            private int colorsTurn = Color.WHITE;
            ArrayList<Integer> aloudMoves = new ArrayList<>();

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                for (int highlight = 0; highlight < 64; highlight++) {
                    adapterView.getChildAt(highlight).setBackground(context.getResources().getDrawable(Constants.newBoardBackground[highlight]));
                }
                AppPreference.setInt(AppPreference.KEY.TAPPED_NUMBER, i);

                if (i % 8 == 0) {
                    String value = String.valueOf(8 - i / 8);
                    pin_code = pin_code + value;
                }

                String pin = AppPreference.getStr(AppPreference.KEY.PIN_NUMBER, "");
                if (pin_code.endsWith(pin)) { // correct pin code
                    pin_code = "";
                    if (ChessActivity.instanceRef != null && ChessActivity.instanceRef.get() != null) {
                        ChessActivity.instanceRef.get().OpenMain();
                        AppPreference.setBool(AppPreference.KEY.CHESS_MODE_PIN, false);
                    }
                }

                //Highlighting the tile on first click
                if (isFirstClick) {
                    // remove all highlighted cells
                    view.setBackgroundColor(Color.CYAN);
                    clickedTile = i;
                    pieceOnTile = board[i];
                    isFirstClick = !isFirstClick;

                    //TODO: Show available moves
                    try {
                        if (board[i] != null) {
                            aloudMoves = board[i].getAloudMoves(i, board);
                            if (aloudMoves != null) {
                                for (int b = 0; b < aloudMoves.size(); b++) {
                                    if (aloudMoves.get(b) != 100) {
                                        adapterView.getChildAt(aloudMoves.get(b)).setBackgroundColor(Color.CYAN);
                                    }
                                }
                            }
                        } else {
                            aloudMoves = new ArrayList<>();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        aloudMoves = new ArrayList<>();
                    }
                    view.setBackground(context.getResources().getDrawable(Constants.getBoarderedBackground(i)));
                }

                // Move is registered
                else if (pieceOnTile != null && i != clickedTile && colorsTurn == pieceOnTile.color && 
                         aloudMoves != null && aloudMoves.contains(i)) {

//                    adapterView.getChildAt(clickedTile).setBackgroundColor(Constants.newBoardColors[clickedTile]);
                    adapterView.getChildAt(clickedTile).setBackground(context.getResources().getDrawable(Constants.newBoardBackground[clickedTile]));
                    if (board[i] != null && board[i].getClass() == King.class) {

                        if (board[i].color == Color.WHITE && textView.getText() == "") {
                            textView.setText("BLACK WINS");
                        } else if (board[i].color == Color.BLACK && textView.getText() == "") {
                            textView.setText("WHITE WINS");
                        }
                    }

                    board[i] = pieceOnTile;
                    board[clickedTile] = null;
                    isFirstClick = true;
                    clickedTile = 99;
                    pieceOnTile = null;

                    if (colorsTurn == Color.WHITE) {
                        colorsTurn = Color.BLACK;
                    } else {
                        colorsTurn = Color.WHITE;
                    }

                    // Reverse board for turn switch
                    for (int a = 0; a < board.length / 2; a++) {
                        AbstractPiece temp = board[a];
                        board[a] = board[board.length - a - 1];
                        board[board.length - a - 1] = temp;
                    }

                    if (aloudMoves != null) {
                        aloudMoves.clear();
                    }
                    aloudMoves = null;
                    adapter.notifyDataSetChanged();
                    gridView.setAdapter(adapter);

                } else {
                    // Reset state when invalid move or clicking on empty square
                    adapter.notifyDataSetChanged();
                    gridView.setAdapter(adapter);
                    isFirstClick = true;
                    clickedTile = 99;
                    pieceOnTile = null;
                    aloudMoves = new ArrayList<>();
                }
            }
        });
    }
}
