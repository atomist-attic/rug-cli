package com.atomist.rug.cli.command.shell;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.atomist.rug.cli.settings.SettingsReader;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovisionaries.ws.client.PayloadGenerator;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketCloseCode;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

public class AgentConnector {

    private static final String AGENT_URL_KEY = "agent_url";

    private WebSocket ws;

    public void connect(Consumer<CommandMessage> consumer) {

        Optional<String> url = SettingsReader.read().getConfigValue(AGENT_URL_KEY, String.class);
        if (!url.isPresent()) {
            return;
        }

        UUID uuid = UUID.randomUUID();
        ObjectMapper mapper = new ObjectMapper();

        // Credentials can be set here.
        // ProxySettings settings = factory.getProxySettings();
        // settings.setServer("http://proxy.example.com");
        // settings.setCredentials("id", "password");

        try {
            WebSocketFactory factory = new WebSocketFactory();
            ws = factory.createSocket(url.get());
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket ws, String message) {
                    try {
                        CommandMessage commandMessage = mapper.readValue(message,
                                CommandMessage.class);
                        consumer.accept(commandMessage);
                    }
                    catch (IOException e) {
                        // TODO handle exception
                    }
                }
            });

            ws.connect();

            ws.setPingInterval(20 * 1000);
            ws.setPingPayloadGenerator(new PayloadGenerator() {

                @Override
                public byte[] generate() {
                    String ping = String.format("{ \"id\": \"%s\", \"type\": \"ping\" }",
                            uuid.toString());
                    return ping.getBytes();
                }
            });
        }
        catch (IOException e) {
            // TODO handle exception
        }
        catch (WebSocketException e) {
            // TODO handle exception
        }
    }
    
    public void send(String message) {
        if (connected()) {
            ws.sendText(String.format("{ \"text\": \"%s\", \"channel\": \"#atomist\" }", message));
        }
    }

    public boolean connected() {
        return ws != null && ws.isOpen();
    }

    public void disconnect() {
        if (ws != null) {
            ws.sendClose(WebSocketCloseCode.NORMAL, "Bye.");
            ws.disconnect();
        }
    }
    
    public static class CommandMessage {
        @JsonProperty
        private List<Command> commands;

        public List<Command> commands() {
            return commands;
        }
    }

    public static class Command {
        @JsonProperty
        private String line;
        @JsonProperty
        private String description;
        @JsonProperty
        private String corrid;

        public String line() {
            return line;
        }

        public String description() {
            return description;
        }
    }

}
