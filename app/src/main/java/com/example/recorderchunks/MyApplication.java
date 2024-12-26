package com.example.recorderchunks;

import android.app.Application;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

public class MyApplication extends Application implements ViewModelStoreOwner {

    private ViewModelStore viewModelStore;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the ViewModelStore for app-level scope
        viewModelStore = new ViewModelStore();
    }

    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }
}
