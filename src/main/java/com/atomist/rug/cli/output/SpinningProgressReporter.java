package com.atomist.rug.cli.output;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SpinningProgressReporter extends Thread implements ProgressReporter {

    private static final String animNix = "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏";
    private static final String animWin32 = "|/-\\";
    private static final boolean isWindows = System.getProperty("os.name").startsWith("Windows");

    private Queue<String> additionalMessages = new ConcurrentLinkedQueue<>();
    private String anim = null;
    private float duration = -1;
    private String message = null;
    private boolean showProgress = true;
    private boolean success = true;

    public SpinningProgressReporter(String msg) {
        this.message = msg;
        if (isWindows) {
            anim = animWin32;
        }
        else {
            anim = animNix;
        }
        start();
    }

    @Override
    public void finish(boolean success, float duration) {
        this.success = success;
        this.duration = duration;
        while (!additionalMessages.isEmpty()) {
            sleep(20);
        }
        this.showProgress = false;
        sleep(80);
    }

    @Override
    public void report(String message) {
        String[] messages = message.split("\n");
        for (String message1 : messages) {
            if (message1 != "") {
                additionalMessages.offer(message1);
            }
        }
    }

    public void run() {
        int x = 0;
        while (showProgress) {
            if (additionalMessages.isEmpty()) {
                System.out.print("\r" + message + " "
                        + Style.yellow("" + anim.charAt(x++ % anim.length())) + " ");
            }
            else {
                System.out.print("\r");
                while (!additionalMessages.isEmpty()) {
                    String newMsg = additionalMessages.poll();
                    newMsg = newMsg.replace("\t", "  ");
                    int diff = ConsoleUtils.width() - newMsg.length();
                    while (diff > 0) {
                        newMsg += " ";
                        diff--;
                    }
                    System.out.println(newMsg);
                }
                System.out.print("\r" + message + " "
                        + Style.yellow("" + anim.charAt(x++ % anim.length())) + " ");
            }
            sleep(80);
        }
        System.out.print("\r");
        if (success) {
            System.out.println(message + " " + Style.green("completed")
                    + (duration > -1 ? " in " + duration + "s" : ""));
        }
        else {
            System.out.println(message + " " + Style.red("failed")
                    + (duration > -1 ? " in " + duration + "s" : ""));
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