package com.checkmate.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.checkmate.android.R;
import com.checkmate.android.util.MainActivity;

public class HideFragment extends BaseFragment {

    public static HideFragment instance;
    MainActivity mActivity;

    public static HideFragment newInstance() {
        return new HideFragment();
    }

    @Override
    public void onAttach(Context context) {
        // TODO Auto-generated method stub
        super.onAttach(context);
        instance = this;
        mActivity = MainActivity.instance;
    }

    @Override
    public void onActivityCreated(@androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onViewStateRestored(@androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_hide, container, false);

        return mView;
    }

    boolean is_scrolled = false;

    @Override
    public void onRefresh() {

    }
}
