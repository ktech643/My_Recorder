package com.checkmate.android.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Spinner;

public class MySpinner extends Spinner {

    OnItemSelectedListener listener;
    private boolean isProgrammaticSelection = false;

    public MySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setSelection(int position) {
        if (isProgrammaticSelection) {
            super.setSelection(position);
            return;
        }
        
        isProgrammaticSelection = true;
        super.setSelection(position);
        if (listener != null) {
            try {
                View v = new View(getContext());
                listener.onItemSelected(null, v, position, 1);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        isProgrammaticSelection = false;
    }

    public void setSelectionNew(int position) {
        if (isProgrammaticSelection) {
            super.setSelection(position);
            return;
        }
        
        isProgrammaticSelection = true;
        super.setSelection(position);
        if (listener != null) {
            try {
                listener.onItemSelected(null, null, position, 1);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        isProgrammaticSelection = false;
    }

    public void setSelectionMain(int position) {
        if (isProgrammaticSelection) {
            super.setSelection(position);
            return;
        }
        
        isProgrammaticSelection = true;
        super.setSelection(position);
        if (listener != null) {
            try {
                listener.onItemSelected(null, null, position, 1);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        isProgrammaticSelection = false;
    }

    public void setOnItemSelectedEvenIfUnchangedListener(
            OnItemSelectedListener listener) {
        this.listener = listener;
    }
}
