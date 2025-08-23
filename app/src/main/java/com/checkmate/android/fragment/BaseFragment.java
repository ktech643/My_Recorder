package com.checkmate.android.fragment;

import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {
    protected abstract String getFragmentTag();
    protected abstract void setupUI();
    protected abstract void initializeComponents();
}
