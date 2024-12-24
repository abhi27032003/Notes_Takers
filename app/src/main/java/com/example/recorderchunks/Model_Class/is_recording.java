package com.example.recorderchunks.Model_Class;



public class is_recording {
    private static  boolean is_recording=false;
    public void current_event(boolean is_recording)
    {
        this.is_recording=is_recording;
    }
     public boolean getIs_Recording()
     {
         return is_recording;
     }
     public  void setIs_recording(boolean is_recording)
     {
         this.is_recording=is_recording;
     }

}
