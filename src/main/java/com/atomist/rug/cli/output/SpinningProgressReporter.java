package com.atomist.rug.cli.output;

import java.io.PrintStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang3.StringUtils;

import com.github.tomaslanger.chalk.Ansi;

public class SpinningProgressReporter extends Thread implements ProgressReporter {

    private static final String animNix = org.apache.commons.lang3.StringUtils.reverse("⣾⣽⣻⢿⡿⣟⣯⣷");
    private static final String animWin32 = "|/-\\";
    private static final boolean isWindows = System.getProperty("os.name").startsWith("Windows");

    private Queue<String> additionalMessages = new ConcurrentLinkedQueue<>();
    private String anim = null;
    private float duration = -1;
    private String detail = null;
    private String message = null;
    private boolean showProgress = true;
    private boolean success = true;
    private PrintStream stream;

    public SpinningProgressReporter(String msg, PrintStream stream) {
        this.message = msg;
        this.stream = stream;
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
        this.detail = detail;
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

    public void run() {
        int x = 0;
        while (showProgress) {
            if (additionalMessages.isEmpty()) {
                stream.print("\r");
                stream.print(Ansi.eraseLine());
                stream.print(formatDetail(
                        message + " " + Style.yellow("" + anim.charAt(x++ % anim.length())) + " "));
            }
            else {
                stream.print("\r");
                stream.print(Ansi.eraseLine());
                while (!additionalMessages.isEmpty()) {
                    String newMsg = additionalMessages.poll();
                    newMsg = newMsg.replace("\t", "  ");
                    stream.println(newMsg);
                }
                stream.print("\r");
                stream.print(Ansi.eraseLine());
                stream.print(formatDetail(
                        message + " " + Style.yellow("" + anim.charAt(x++ % anim.length())) + " "));
            }
            sleep(50);
        }
        stream.print("\r");
        stream.print(Ansi.eraseLine());
        if (success) {
            stream.println(message + " " + Style.green("completed")
                    + (duration > -1 ? " in " + String.format("%.2f", duration) + "s" : ""));
        }
        else {
            stream.println(message + " " + Style.red("failed")
                    + (duration > -1 ? " in " + String.format("%.2f", duration) + "s" : ""));
        }
    }

    private String formatDetail(String message) {
        int diff = ConsoleUtils.width() - message.length() - 3; 
        if (detail != null && diff > 0) {
            return message + Style.gray(StringUtils.abbreviate(this.detail, diff)) + " ";
        }
        else {
            return message;
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