package com.example.recorderchunks.utils;

import android.content.Context;
import android.content.Intent;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;
    private final Class<?> errorActivityClass;

    public CustomExceptionHandler(Context context, Class<?> errorActivityClass) {
        this.context = context;
        this.errorActivityClass = errorActivityClass;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // Capture the stack trace as a String
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();

        // Create an intent to start the error reporting activity
        Intent intent = new Intent(context, errorActivityClass);
        intent.putExtra("stack_trace", stackTrace);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Start the error reporting activity
        context.startActivity(intent);

        // Kill the process to avoid undefined behavior
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
