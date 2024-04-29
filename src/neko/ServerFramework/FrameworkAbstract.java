package neko.ServerFramework;

import arc.util.CommandHandler;

public abstract class FrameworkAbstract {

    public void init() {};

    public void registerServerCommands(CommandHandler handler) {};
    public void registerClientCommands(CommandHandler handler) {};
} 



