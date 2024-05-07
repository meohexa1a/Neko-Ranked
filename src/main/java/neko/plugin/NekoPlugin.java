package neko.plugin;

import static mindustry.Vars.net;
import static mindustry.Vars.state;

import arc.Core;
import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Timer;
import mindustry.gen.Call;
import mindustry.mod.Plugin;
import neko.plugin.DataType.NekoEvent.BulkCloseEvent;
import neko.plugin.DataType.ServerInfo.ServerInfoController;
import neko.plugin.DataType.ServerInfo.ServerStatus;

public class NekoPlugin extends Plugin {
    private final String interpro = "server.mindustry-tool.app";
    private final ServerStatus settingsStatus = ServerStatus.Main;
    private ServerInfoController serverInfoController = ServerInfoController.getIns();
    
    @Override 
    public void init() {
        Events.on(BulkCloseEvent.class, event -> System.exit(0));

        if (Core.settings.getString("serverIp") == null) {
            Log.err("Please change your ip to valid IP. Auto change to @", interpro);
            Core.settings.put("serverIp", interpro);
        } else Log.info("Current server IP is @", Core.settings.getString("serverIp"));
        Log.info("Init server. Stage 1: Server status controlling. Will exit if one main exited (beta 1.0.0)");

        if (Core.settings.getInt("serverId") == 0) {
            Log.info("Create new server info...");
            serverInfoController.newServerInfomation(settingsStatus);
        }

        if (settingsStatus == ServerStatus.Main) main();
    }

    public void main() {

    }
    
    
    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("cip", "[IP]", "Change IP for this server", args -> {
            try {
                Log.info("Change to @", args[0]);
                Core.settings.put("serverIp", args[0]);
            } catch (Exception e) {
                Log.err("Invalid server IP@", (args.length == 1) ? ": " + args[0] : "");
            }
        });

        handler.removeCommand("exit");

        handler.register("exit", "Custom exit", args -> {
            Log.info("Server will stop after 10s");
            if (state.isGame()) {
                Call.sendMessage("Server will close after 10s!");
            }

            Timer.schedule(() -> {
                net.dispose();
                Core.app.exit();
            }, 10);

            serverInfoController.updateServerInfomation(ServerStatus.Offline);
        });
    }
    
}
