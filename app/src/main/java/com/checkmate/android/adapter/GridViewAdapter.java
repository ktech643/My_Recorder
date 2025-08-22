package com.checkmate.android.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.checkmate.android.AppPreference;
import com.checkmate.android.MyApp;
import com.checkmate.android.R;
import com.checkmate.android.model.Pieces.AbstractPiece;
import com.checkmate.android.model.Tools.Constants;
import com.checkmate.android.ui.view.SquareImageView;

public class GridViewAdapter extends BaseAdapter {

    private AbstractPiece[] board;
    private Context context;
    private LayoutInflater layoutInflater;
    public TextView txt_number;

    public GridViewAdapter(Context context, AbstractPiece[] board) {
        this.board = board;
        this.context = context;
        this.layoutInflater = (LayoutInflater.from(context));
    }

    public void setBoard(AbstractPiece[] board) {
        this.board = new AbstractPiece[board.length];
        for (int i = 0; i < board.length; i++) {
            this.board[i] = board[i];
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return board.length;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "ViewHolder"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        view = layoutInflater.inflate(R.layout.tile_layout, null);
        SquareImageView imageView = view.findViewById(R.id.piece_image);
        txt_number = view.findViewById(R.id.txt_number);
        TextView txt_character = view.findViewById(R.id.txt_character);

        view.setBackground(context.getResources().getDrawable(Constants.newBoardBackground[i]));

        if (Constants.newBoardBackground[i] == R.drawable.dark_no_border) {
            txt_number.setTextColor(Constants.light);
            txt_character.setTextColor(Constants.light);
        } else {
            txt_number.setTextColor(Constants.dark);
            txt_character.setTextColor(Constants.dark);
        }

        if (i % 8 == 0) {
            txt_number.setText(String.valueOf(8 - i / 8));
        }
        if (i > 55) {
            txt_character.setText(Constants.characters[i - 56]);
        }

        if (board[i] != null) {
            imageView.setImageResource(board[i].imageID);
        }

        if (i == AppPreference.getInt(AppPreference.KEY.TAPPED_NUMBER, -1)) {
            view.setBackground(MyApp.getContext().getResources().getDrawable(Constants.getBoarderedBackground(i)));
        } else {
            view.setBackground(MyApp.getContext().getResources().getDrawable(Constants.newBoardBackground[i]));
        }

        return view;
    }
}
