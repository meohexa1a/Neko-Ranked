package neko.plugin.DataType.ServerInfo;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import arc.Events;
import arc.util.Log;
import neko.plugin.DataType.NekoEvent.BulkCloseEvent;

@RestController
@RequestMapping("/api/serverinfos")
public class ServerInfoController {

    @Autowired
    protected ServerInfoRepo serverInfoRepo;

    @PostMapping
    @GetMapping
    public int getNewSid() {
        Optional<ServerInfo> defaultSettings = serverInfoRepo.findById("0");

        if (defaultSettings.isPresent()) {
            ServerInfo newSetting = defaultSettings.get();
            newSetting.port++;
            serverInfoRepo.save(newSetting);

            return defaultSettings.get().port;
        } else {
            ServerInfo newSetting = new ServerInfo();
            newSetting.port = 0;

            serverInfoRepo.save(newSetting);
            return 0;
        }
    }

    @GetMapping
    @PostMapping
    public void newServerInfomation(@RequestBody ServerStatus status) {
        List<ServerInfo> main = serverInfoRepo.findByStatus(ServerStatus.Main);
        ServerInfo server = new ServerInfo(status);
        server.port = getNewSid();

        if (main.isEmpty()) {
            serverInfoRepo.insert(server);
            Log.info("Successfully create new server info.");
        } else {
            Log.info("Not support for now. Cook here.");
            Events.fire(new BulkCloseEvent());
        }
    }

    @PostMapping
    public void updateServerInfomation(@RequestBody ServerStatus status) {
        ServerInfo sif = new ServerInfo(status);
        serverInfoRepo.save(sif);
    }

    static final ServerInfoController serverInfoController = new ServerInfoController();

    private ServerInfoController() {
    }

    public static ServerInfoController getIns() {
        return serverInfoController;
    }
}
