package neko.plugin.DataType.ServerInfo;

import org.springframework.data.mongodb.core.mapping.Document;

import arc.Core;
import mindustry.net.Administration.Config;

@Document(collection = "ServerInfo")
public class ServerInfo {
    ServerStatus status;
    String internetProtocol;
    int port, serverId;

    public ServerInfo(ServerStatus status) {
        this.status = status;
        this.serverId = Core.settings.getInt("serverId");
        this.port = Config.port.num();
        this.internetProtocol = Config.socketInputAddress.string();
    }

    public ServerInfo() {

    }
    
}
