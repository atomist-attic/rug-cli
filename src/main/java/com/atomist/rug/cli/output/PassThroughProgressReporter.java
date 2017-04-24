package com.atomist.rug.cli.output;

import java.io.PrintStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PassThroughProgressReporter extends Thread implements ProgressReporter {

    private Queue<String> additionalMessages = new ConcurrentLinkedQueue<>();
    private boolean showProgress = true;
    private String message;
    private boolean success;
    private float duration;
    private PrintStream stream;

    public PassThroughProgressReporter(String message, PrintStream stream) {
        this.message = message;
        this.stream = stream;
        report(message);
        start();
    }

    @Override
    public void report(String message) {
        String[] messages = message.split("\n");
        for (String msg : messages) {
            String content = msg.trim();
            if (!content.equals("") && !content.equals("$")) {
                additionalMessages.offer(msg);
            }
        }
    }

    @Override
    public void finish(boolean success, float duration) {
        this.success = success;
        this.duration = duration;
        while (!additionalMessages.isEmpty()) {
            sleep(10);
        }
        this.showProgress = false;
        try {
            this.join();
        }
        catch (InterruptedException e) {
        }
    }

    @Override
    public void detail(String detail) {
        // no op
    }

    public void run() {
        while (showProgress) {
            while (!additionalMessages.isEmpty()) {
                String newMsg = additionalMessages.poll();
                newMsg = newMsg.replace("\t", "  ");
                stream.println(newMsg);
            }
            sleep(50);
        }
        if (success) {
            stream.println(message + " " + Style.green("completed")
                    + (duration > -1 ? " in " + String.format("%.2f", duration) + "s" : ""));
        }
        else {
            stream.println(message + " " + Style.red("failed")
                    + (duration > -1 ? " in " + String.format("%.2f", duration) + "s" : ""));
        }
    }

    private void sleep(int timeout) {
        try {
            Thread.sleep(timeout);
        }
        catch (Exception e) {
        }
    }
}
