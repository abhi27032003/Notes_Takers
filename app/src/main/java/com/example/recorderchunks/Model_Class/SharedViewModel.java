package com.example.recorderchunks.Model_Class;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<float[]> cardPosition = new MutableLiveData<>(new float[]{0f, 0f});

    public LiveData<float[]> getCardPosition() {
        return cardPosition;
    }

    public void updateCardPosition(float x, float y) {
        cardPosition.setValue(new float[]{x, y});
    }
}
