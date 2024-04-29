package neko;

import static mindustry.Vars.logic;
import static mindustry.Vars.maps;
import static mindustry.Vars.netServer;
import static mindustry.Vars.state;
import static mindustry.Vars.world;

import java.util.Random;

import arc.Events;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Time;
import arc.util.Timer;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.EventType.Trigger;
import mindustry.game.Gamemode;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.mod.Plugin;
import mindustry.net.WorldReloader;

public class NekoPlugin extends Plugin {
    public enum RankedGamemode {
        Solo, Duel, Squad, UnrankedSolo, UnrankedDuel, UnrankedSquad, Invalid
    };

    /** Queue list Seq[] 0 -> 5 cointain modes. */
    @SuppressWarnings("unchecked")
    public static Seq<String>[] QueueList = (Seq<String>[]) new Seq[6];
    /** Next match list. If no match, always empty */
    public static Seq<String> MatchList = new Seq<>();
    /** Now server state. If false, it is server, true is match room */
    public static boolean NowState;
    /** Match state only for if match is ready. Cannot queue in this state */
    public static boolean MatchState;
    public static long refresh, matchFoundTime;
    public static RankedGamemode matchGamemode;

    // room vars
    public static Team team1, team2, disconnetTeam;
    public static boolean GameState, fast, isPortOpened;
    public static Seq<String> player1 = new Seq<>();
    public static Seq<String> player2 = new Seq<>();
    public static Seq<String> totalJoiner = new Seq<>();
    public static int disconnet1, disconnet2, disconnetLimit, totalPlayerJoined, totalPlayerInOneTeam, couter;
    public static long matchStartTime, totalDisconnetTime, disconnetTime;

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("main", "running main server", args -> {
            main();
        });

    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("queue", "[mode]", "ready for next game", (args, player) -> {
            try {
                queue(args[0], player);
            } catch (Exception e) {
                queue("", player);
            }
        });

        handler.<Player>register("ct", "change team", (args, player) -> {
            if (player.team().name.equals(team1.name)) {
                player.team(team2);
            } else
                player.team(team1);
        });
    }

    @Override
    public void init() {
        isPortOpened = false;
        Events.run(Trigger.update, () -> {
            if ((Time.millis() - refresh) > 1000) {
                Events.fire(new ServerActionRefresh());
                refresh = Time.millis();
            }
        }); // only for refreshing lol. im lazy

        Events.on(ServerMatchFoundEvent.class, event -> {
            MatchState = true;
            matchFoundTime = Time.millis();
            MatchList = event.list;
            matchGamemode = event.mode;
        });

        Events.on(ServerActionRefresh.class, event -> {
            if (!NowState) {
                // display
                Groups.player.each(player -> {
                    String display = "Testing. In queue: ";
                    String sec = ((10000 - (Time.millis() - matchFoundTime)) / 1000) + "s";

                    if (MatchList.contains(player.uuid()) && MatchState) {
                        display = display + "\n You are in the match!";
                        display = display + "\n Match starting after " + sec;
                    }

                    Call.infoPopupReliable(player.con, display, 1f, Align.topLeft, 90, 5, 0, 0);
                });

                // match start
                if (MatchState && (Time.millis() - matchFoundTime) > 10000) {
                    MatchState = false;
                    Runnable gameRunnable = () -> {
                        room(MatchList, matchGamemode);
                    };
                    gameRunnable.run();
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void main() {
        QueueList = (Seq<String>[]) new Seq[6];
        for (int i = 0; i < 6; i++) {
            QueueList[i] = new Seq<>();
        }

        MatchList = new Seq<>();
        NowState = MatchState = fast = false;
        matchGamemode = null;

        loadMap();
    }

    public void room(Seq<String> players, RankedGamemode GameMode) {
        Log.info("Starting game round room.");
        loadMap();
        player1 = new Seq<>();
        player2 = new Seq<>();
        totalJoiner = new Seq<>();

        NowState = true;
        GameState = false;

        disconnet1 = disconnet2 = totalPlayerJoined = 0;
        couter = 6;
        totalDisconnetTime = 0;

        players.shuffle();
        for (int i = 0; i < players.size; i++) {
            if (i % 2 == 0) {
                player1.add(players.get(i));
            } else {
                player2.add(players.get(i));
            }
        }

        System.out.println("Team 1: " + player1);
        System.out.println("Team 2: " + player2);

        if (GameMode == RankedGamemode.Solo || GameMode == RankedGamemode.UnrankedSolo) {
            disconnetLimit = 2;
            totalPlayerInOneTeam = 2;
        } else if (GameMode == RankedGamemode.Duel || GameMode == RankedGamemode.UnrankedDuel) {
            disconnetLimit = 5;
            totalPlayerInOneTeam = 4;
        } else {
            disconnetLimit = 8;
            totalPlayerInOneTeam = 6;
        }

        state.set(mindustry.core.GameState.State.paused);

        Events.on(ServerActionRefresh.class, event -> {
            Groups.player.each(p -> {
                if (player1.contains(p.uuid()) && !p.team().name.equals(team1.name)) {
                    p.team(team1);
                } else if (player2.contains(p.uuid()) && !p.team().name.equals(team2.name)) {
                    p.team(team2);
                } else if (!player1.contains(p.uuid()) && !player2.contains(p.uuid())
                        && !p.team().equals(Team.derelict)) {
                    p.team(Team.derelict);
                }
            });

            if (NowState && !GameState) {
                Groups.player.each(p -> {
                    if (player1.contains(p.uuid()) && !totalJoiner.contains(p.uuid())) {

                        totalPlayerJoined++;
                        Log.info("Player 1 joined");
                        totalJoiner.add(p.uuid());

                    } else if (player2.contains(p.uuid()) && !totalJoiner.contains(p.uuid())) {

                        totalPlayerJoined++;
                        Log.info("Player 2 joined");
                        totalJoiner.add(p.uuid());

                    } else if (!players.contains(p.uuid()) && !p.team().name.equals(Team.derelict.name)) {
                        p.team(Team.derelict);
                    }
                });

                if (totalPlayerJoined == totalPlayerInOneTeam) {
                    fast = true;
                }

                if (fast) {
                    Call.setHudText("Time left: " + couter + "s. Get ready!");
                    couter--;
                } else {
                    Call.setHudText("Waiting for joiner...");
                }

                if (fast && (couter < 0 && !GameState)) {
                    Call.setHudText("");

                    state.set(mindustry.core.GameState.State.playing);
                    Call.sendMessage("Game start!");
                    matchStartTime = Time.millis();
                    GameState = true;
                }
            }

            if (NowState && GameState) {
                if (totalPlayerJoined < totalPlayerInOneTeam) {
                    state.set(mindustry.core.GameState.State.paused);
                    Call.setHudText("Waiting reconneting...");
                }

                long elapsedTime = ((Time.millis() - matchStartTime) - totalDisconnetTime) / 1000;
                long hour = elapsedTime / 3600;
                long sec = elapsedTime - (hour * 3600);
                long min = sec / 60;
                sec = sec - (min * 60);

                String clock = String.format("%02d:%02d:%02d", hour, min, sec);
                Call.infoPopupReliable(clock, 1f, Align.topLeft, 90, 5, 0, 0);
            }
        });

        Events.on(PlayerLeave.class, event -> {
            if (NowState && !GameState) {
                if (player1.contains(event.player.uuid())) {
                    totalJoiner.remove(event.player.uuid());
                    totalPlayerJoined--;
                } else if (player2.contains(event.player.uuid())) {
                    totalJoiner.remove(event.player.uuid());
                    totalPlayerJoined--;
                }

            } else if (NowState && GameState) {
                if (player1.contains(event.player.uuid())) {
                    totalPlayerJoined--;
                    disconnet1++;
                    disconnetTeam = team1;
                    if (disconnet1 > disconnetLimit) {
                        // game over
                    } else {
                        disconnetTime = Time.millis();
                    }
                } else if (player2.contains(event.player.uuid())) {
                    totalPlayerJoined--;
                    disconnet2++;
                    disconnetTeam = team2;
                    if (disconnet2 > disconnetLimit) {
                        // go
                    } else {
                        disconnetTime = Time.millis();
                    }
                }
            }

            if (totalPlayerJoined == 0) {
                // force gameover
            }

        });

        Events.on(PlayerJoin.class, event -> {
            if (NowState && GameState) {
                if (player1.contains(event.player.uuid())) {
                    event.player.team(team1);
                    totalPlayerJoined++;
                    totalJoiner.add(event.player.uuid());
                    state.set(mindustry.core.GameState.State.playing);
                    totalDisconnetTime += Time.millis() - disconnetTime;
                    Call.setHudText("");
                } else if (player2.contains(event.player.uuid())) {
                    event.player.team(team2);
                    totalPlayerJoined++;
                    totalJoiner.add(event.player.uuid());
                    state.set(mindustry.core.GameState.State.playing);
                    totalDisconnetTime += Time.millis() - disconnetTime;
                    Call.setHudText("");
                } else {
                    event.player.team(Team.derelict);
                }
            }
        });

        Events.on(GameOverEvent.class, event -> {
            Seq<String> winnerPlayerName = new Seq<>();
            Groups.player.each(p -> p.team().name.equals(event.winner.name), p -> winnerPlayerName.add(p.name));

            String winnerString = new String();
            for (String ex : winnerPlayerName) {
                winnerString = winnerString + ex + " ";
            }

            if (NowState && GameState) {
                Events.fire(new ServerMatchOverEvent(event.winner, winnerPlayerName));
            }
        });

        Events.on(ServerMatchOverEvent.class, event -> {
            Seq<String> winnerPlayerName = new Seq<>();
            Groups.player.each(p -> p.team().name.equals(event.winner.name), p -> winnerPlayerName.add(p.name));

            String winnerString = new String();
            for (String ex : winnerPlayerName) {
                winnerString = winnerString + ex + " ";
            }

            if (NowState && GameState) {
                Runnable mainRunnable = () -> {
                    main();
                };
                Call.infoMessage(
                        "Game over! Victory for " + event.winner.name + " team. \n Winner(s): " + winnerString);
                Timer.schedule(() -> {
                    mainRunnable.run();
                }, 20f);
            }
        });
    }

    public class ServerMatchOverEvent {
        public Team winner;
        public Seq<String> winnerPlayer;

        public ServerMatchOverEvent(Team winner, Seq<String> winnerPlayer) {
            this.winner = winner;
            this.winnerPlayer = winnerPlayer;
        }
    }

    /**
     * Processor for queue.
     * 
     * @param mode   Gamemode as string.
     * @param player Player data for func.
     */
    public synchronized void queue(String mode, Player player) {
        RankedGamemode GameMode = mode(mode);

        if (GameMode == RankedGamemode.Invalid) {
            player.sendMessage("invalid mode: " + mode);
            return;
        }
        check(player, GameMode);
    }

    /**
     * Processor for queue.
     * 
     * @param gameMode Ranked game mode.
     * @param player   Player data for func
     */
    public synchronized void queue(RankedGamemode gameMode, Player player) {
        if (gameMode == RankedGamemode.Invalid) {
            player.sendMessage("You are typing invalid ");
            return;
        }
        check(player, gameMode);
    }

    /**
     * Easy processor for player leave
     * 
     * @param player Player leave
     */
    public synchronized void queue(Player player) {
        for (int i = 0; i < 6; i++) {
            if (QueueList[i].contains(player.uuid())) {
                // 0 -> 5 = 1v1, 2v2, 3v3 u1v1, u2v2, u3v3
                switch (i) {
                    case 0:
                        queue(RankedGamemode.Solo, player);
                        break;
                    case 1:
                        queue(RankedGamemode.Duel, player);
                        break;
                    case 2:
                        queue(RankedGamemode.Squad, player);
                        break;
                    case 3:
                        queue(RankedGamemode.UnrankedSolo, player);
                        break;
                    case 4:
                        queue(RankedGamemode.UnrankedDuel, player);
                        break;
                    case 5:
                        queue(RankedGamemode.UnrankedSquad, player);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Convert gamemode to store addr in queue list.
     * 
     * @param gamemode Just gamemode input
     * @return Store addr
     */
    public int getNum(RankedGamemode gamemode) {
        if (gamemode == RankedGamemode.Solo) {
            return 0;
        } else if (gamemode == RankedGamemode.Duel) {
            return 1;
        } else if (gamemode == RankedGamemode.Squad) {
            return 2;
        } else if (gamemode == RankedGamemode.UnrankedSolo) {
            return 3;
        } else if (gamemode == RankedGamemode.UnrankedDuel) {
            return 4;
        } else if (gamemode == RankedGamemode.UnrankedSquad) {
            return 5;
        } else {
            return 3;
        }
    }

    /**
     * It look like more a queue function. But i want easy in my life, this func
     * process a queue command center
     * 
     * @param player   Input player (any case, it can modifier all)
     * @param gamemode Just gamemode
     */
    public synchronized void check(Player player, RankedGamemode gamemode) {
        if (!NowState) {
            if (!MatchState) {
                if (QueueList[getNum(gamemode)].contains(player.uuid())) {
                    QueueList[getNum(gamemode)].remove(player.uuid());
                    player.sendMessage("Sussesfully removed from the queue");
                } else {
                    checkMatch(player, gamemode);
                }
            } else {
                if (MatchList.contains(player.uuid())) {
                    cancelMatch();
                    return;
                }

                if (QueueList[getNum(gamemode)].contains(player.uuid())) {
                    QueueList[getNum(gamemode)].remove(player.uuid());
                    player.sendMessage("Sussesfully removed from the queue");
                } else {
                    player.sendMessage("You cannot queue if one match is ready to go.");
                }
            }
        } else {
            player.sendMessage("Can't queue/ queue process in game room.");
        }
    }

    /**
     * Cancel a match
     */
    public void cancelMatch() {
        Groups.player.each(p -> MatchList.contains(p.uuid()), p -> p.sendMessage("Match cancelled"));
        MatchList = new Seq<>();
        MatchState = false;
        matchGamemode = null;
    }

    /**
     * Easy proces it gamemode string to ranked gamemode
     * 
     * @param mode Gamemode as string
     * @return Valid gamemode
     */
    public RankedGamemode mode(String mode) {
        if (mode.equals("1v1") || (mode.equals("1"))) {
            return RankedGamemode.Solo;
        } else if (mode.equals("2v2") || mode.equals("2")) {
            return RankedGamemode.Duel;
        } else if (mode.equals("3v3") || mode.equals("3")) {
            return RankedGamemode.Squad;
        } else if (mode.equals("u1v1") || (mode.equals("u1") || mode.equals(""))) {
            return RankedGamemode.UnrankedSolo;
        } else if (mode.equals("u2v2") || mode.equals("u2")) {
            return RankedGamemode.UnrankedDuel;
        } else if (mode.equals("u3v3") || mode.equals("u3")) {
            return RankedGamemode.UnrankedSquad;
        } else {
            return RankedGamemode.Invalid;
        }
    }

    /** Cheching is suitable to make one match */
    public synchronized void checkMatch(Player player, RankedGamemode gamemode) {
        if ((gamemode == RankedGamemode.Duel || gamemode == RankedGamemode.Solo) || gamemode == RankedGamemode.Squad) {
            player.sendMessage("We will open ranking match soon!");
        } else if (gamemode == RankedGamemode.UnrankedSolo) {
            if (QueueList[3].size > 0) {
                Seq<String> playerList = new Seq<>();
                playerList.add(QueueList[3].first());
                QueueList[3].remove(QueueList[3].first());
                playerList.add(player.uuid());
                Events.fire(new ServerMatchFoundEvent(gamemode, playerList));
            } else {
                QueueList[3].add(player.uuid());
                player.sendMessage("You are joining 1v1 unranked queue!");
            }
        } else if (gamemode == RankedGamemode.UnrankedDuel) {
            if (QueueList[4].size > 2) {
                Seq<String> playerList = new Seq<>();
                for (int i = 0; i > 2; i++) {
                    playerList.add(QueueList[4].first());
                    QueueList[4].remove(QueueList[4].first());
                }
                playerList.add(player.uuid());
                Events.fire(new ServerMatchFoundEvent(gamemode, playerList));
            } else {
                QueueList[4].add(player.uuid());
                player.sendMessage("You are joining 2v2 unranked queue!");
            }
        } else if (gamemode == RankedGamemode.UnrankedSquad) {
            if (QueueList[5].size > 4) {
                Seq<String> playerList = new Seq<>();
                for (int i = 0; i > 2; i++) {
                    playerList.add(QueueList[5].first());
                    QueueList[5].remove(QueueList[5].first());
                }
                playerList.add(player.uuid());
                Events.fire(new ServerMatchFoundEvent(gamemode, playerList));
            } else {
                QueueList[5].add(player.uuid());
                player.sendMessage("You are joining 3v3 unranked queue!");
            }
        } else {
            Log.err("Cannot find valid gamemode for player have name: @. ", player.name);
        }
    }

    public class ServerMatchFoundEvent {
        public RankedGamemode mode;
        public Seq<String> list;

        public ServerMatchFoundEvent(RankedGamemode mode, Seq<String> list) {
            this.mode = mode;
            this.list = list;
        }
    }

    public class ServerActionRefresh {
    }

    /**
     * Load map and default team in this server.
     */
    public void loadMap() {
        WorldReloader worldReloader = new WorldReloader();

        maps.reload();
        Seq<Map> mapL = new Seq<>();
        mapL.add(maps.customMaps());
        Random random = new Random();
        Map result = mapL.get(random.nextInt(mapL.size));

        Log.info("Randomized next map to be @. Map loading.", result.plainName());

        try {
            if (isPortOpened) {
                worldReloader.begin();
            }
            world.loadMap(result, result.applyRules(Gamemode.pvp));
            state.rules = result.applyRules(Gamemode.pvp);

            Team[] totalTeams = Team.baseTeams;

            for (Team check : totalTeams) {
                if (!(check.cores().isEmpty())) {
                    if (team1 == null) {
                        team1 = check;
                    } else {
                        team2 = check;
                        break;
                    }
                }
            }

            if (!isPortOpened) {
                isPortOpened = true;
                netServer.openServer();
                logic.play();
            } else {
                worldReloader.end();
                logic.play();
            }
        } catch (Exception e) {
            Log.err(e);
            loadMap();
        }
    }
}