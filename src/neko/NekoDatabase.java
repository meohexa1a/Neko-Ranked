package neko;

import arc.Core;
import arc.util.Log;

import java.util.*;

public class NekoDatabase {
    private static NekoDatabase nekoDatabase = new NekoDatabase();
    Random random = new Random();
    
    /** Load first player data and load server state*/
    public void load(boolean state) {
        new PlayerData("examplePlayer" + random.nextInt(1234567890)).NewPlayerData();
        new PlayerData("examplePlayer" + random.nextInt(1234567890)).NewPlayerData();
        new PlayerData("examplePlayer" + random.nextInt(1234567890)).NewPlayerData();
    }

    /** Change state for failsafe*/
    public void failsafe() {
        
    }

    public class PlayerData {
        String uuid;
        public PlayerData(String uuid) {
            this.uuid = uuid;
        }

        public PlayerData NewPlayerData() {
            int playerIndex = Core.settings.getInt("playerTotalCreatedIndex");
            Core.settings.put("playerTotalCreatedIndex", playerIndex++);

            Core.settings.put("Player" + playerIndex, uuid);
            Log.info("Loaded playerData #" + playerIndex + " to database. \n UUID: " + uuid);

            return this;
        }
    }

    private NekoDatabase() {}
    public static NekoDatabase getIns() {return nekoDatabase;}
}
