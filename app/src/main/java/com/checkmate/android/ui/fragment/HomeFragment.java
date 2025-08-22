package com.checkmate.android.ui.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.checkmate.android.R;
import com.checkmate.android.databinding.FragmentHomeBinding;
import com.checkmate.android.ui.activity.HomeActivity;
import com.checkmate.android.ui.view.AspectFrameLayout;
import com.checkmate.android.util.MainActivity;

public class HomeFragment extends BaseFragment {

    public static HomeFragment instance;
    HomeActivity mActivity;
    private FragmentHomeBinding binding;

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
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        mView = binding.getRoot();

        // Set up click listeners
        binding.btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // mActivity.startStream();
            }
        });

        binding.btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // mActivity.stopStream();
            }
        });

//        binding.textureView.setSurfaceTextureListener(mActivity.mSurfaceTextureListener);
//        mActivity.initService();

        return mView;
    }

    @Override
    public void onRefresh() {

    }

    // Getter for the binding to access views
    public FragmentHomeBinding getBinding() {
        return binding;
    }
}
