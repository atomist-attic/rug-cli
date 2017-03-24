package com.atomist.rug.cli.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.util.StringUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.tree.Node.Type;

public class LogVisitor implements NodeVisitor {

    private List<ChildInfo> childInfos = new ArrayList<>();

    private StringBuilder log;

    private String indent = "  ";
    
    public LogVisitor() {
        this("  ");
    }
    
    public LogVisitor(String indent) {
        this.log = new StringBuilder();
        this.indent = indent;
    }
    
    public String toString() {
        String content = log.toString();
        int ix = content.lastIndexOf('\n');
        if (ix > 0) {
            return content.substring(0, ix);
        }
        else {
            return content;
        }
    }
    
    public void log(Log log) {
        String content = toString();
        if (content != null && StringUtils.hasText(content)) {
            log.info(content);
        }
    }

    public boolean visitEnter(Node node) {
        if (node.id() != null) {
            String indentation = indent + formatIndentation(node);
            log.append(indentation + formatNode(node, indentation)).append("\n");
        }
        childInfos.add(new ChildInfo(node.children().size()));
        return true;
    }

    public void visitLeave(Node node) {
        if (!childInfos.isEmpty()) {
            childInfos.remove(childInfos.size() - 1);
        }
        if (!childInfos.isEmpty()) {
            childInfos.get(childInfos.size() - 1).index++;
        }
    }

    private String formatIndentation(Node node) {
        StringBuilder buffer = new StringBuilder(128);
        for (Iterator<ChildInfo> it = childInfos.iterator(); it.hasNext();) {
            buffer.append(it.next().formatIndentation(node, !it.hasNext()));
        }
        return buffer.toString();
    }

    private String formatNode(Node node, String indentation) {
        String msg = node.id();
        // The following is really ugly but gets the job done.
        if (msg.contains("\n")) {
            String subIndentation = indentation.substring(0,
                    indentation.length() - Constants.TREE_NODE.length());
            if (indentation.endsWith(Constants.LAST_TREE_NODE_WITH_CHILDREN)) {
                subIndentation = subIndentation
                        + (Constants.TREE_CONNECTOR.length() == 2 ? "  " : "   ")
                        + Constants.TREE_CONNECTOR;
            }
            else if (indentation.endsWith(Constants.TREE_NODE_WITH_CHILDREN)) {
                subIndentation = subIndentation + Constants.TREE_CONNECTOR
                        + Constants.TREE_CONNECTOR;
            }
            else {
                subIndentation = subIndentation
                        + (Constants.TREE_NODE.length() == 2 ? "   " : "    ");
            }

            String[] parts = msg.split("\n");
            msg = (node.type() == Type.DETAIL ? Style.gray(parts[0]) : parts[0]);
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                msg = msg + "\n" + subIndentation
                        + (node.type() == Type.DETAIL ? Style.gray(part) : part);
            }
            return msg;
        }
        else {
            if (node.type() == Type.DETAIL) {
                return Style.gray(msg);
            }
            else {
                return msg;
            }
        }
    }

    private static class ChildInfo {

        final int count;

        int index;

        public ChildInfo(int count) {
            this.count = count;
        }

        public String formatIndentation(Node node, boolean end) {
            boolean last = index + 1 >= count;
            if (end) {
                return last
                        ? (!node.children().isEmpty() ? Constants.LAST_TREE_NODE_WITH_CHILDREN
                                : Constants.LAST_TREE_NODE)
                        : (!node.children().isEmpty() ? Constants.TREE_NODE_WITH_CHILDREN
                                : Constants.TREE_NODE);
            }
            return last ? (Constants.TREE_CONNECTOR.length() == 2 ? "  " : "   ")
                    : Constants.TREE_CONNECTOR;
        }
    }
}
