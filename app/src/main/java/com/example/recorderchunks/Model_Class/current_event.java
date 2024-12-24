package com.example.recorderchunks.Model_Class;

public class current_event {
    private static  int current_event_no=-1;
    public void current_event(int current_event_no)
    {
        this.current_event_no=current_event_no;
    }
     public int getCurrent_event_no()
     {
         return current_event_no;
     }
     public  void setCurrent_event_no(int current_event_no)
     {
         this.current_event_no=current_event_no;
     }

}
