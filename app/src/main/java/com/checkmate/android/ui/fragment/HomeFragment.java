package com.checkmate.android.ui.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.checkmate.android.R;
import com.checkmate.android.ui.activity.HomeActivity;
import com.checkmate.android.ui.view.AspectFrameLayout;
import com.checkmate.android.util.MainActivity;


public class HomeFragment extends BaseFragment {

    public static HomeFragment instance;
    HomeActivity mActivity;

    public AspectFrameLayout mPreviewFrame;

    TextureView textureView;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onAttach(Context context) {
        // TODO Auto-generated method stub
        super.onAttach(context);
        instance = this;
        mActivity = HomeActivity.instance;
    }

    public void OnClick(View view) {
//        switch (view.getId()) {
//            case R.id.btn_start:
//                mActivity.startStream();
//                break;
//            case R.id.btn_stop:
//                mActivity.stopStream();
//                break;
//        }
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
        mView = inflater.inflate(R.layout.fragment_home, container, false);

        textureView = mView.findViewById(R.id.textureView);

        // Set up click listeners for buttons
        Button btn_start = mView.findViewById(R.id.btn_start);
        Button btn_stop = mView.findViewById(R.id.btn_stop);
        
        btn_start.setOnClickListener(this::OnClick);
        btn_stop.setOnClickListener(this::OnClick);

//        textureView.setSurfaceTextureListener(mActivity.mSurfaceTextureListener);
//        mActivity.initService();

        return mView;
    }

    @Override
    public void onRefresh() {

    }
}
