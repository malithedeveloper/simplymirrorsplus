package com.malidev.simplymirrorsplus.client;

import java.util.Properties;

/** The settings one mirror can have of its own. A mirror without them uses the defaults in {@link MirrorConfig}. */
public final class MirrorSettings {
    /** Shown for the mirror; empty means it is named by its position. */
    public String name="";
    /** MirrorEngine.MODE_NORMAL, MODE_PLAYERS or MODE_OFF. */
    public int mode=MirrorConfig.playersOnly?MirrorEngine.MODE_PLAYERS:MirrorEngine.MODE_NORMAL;
    public final MirrorBackdrop backdrop=new MirrorBackdrop();

    public MirrorSettings() { backdrop.set(MirrorConfig.backdrop); }

    void read(Properties values,String prefix) {
        name=values.getProperty(prefix+"name",name);
        String saved=values.getProperty(prefix+"mode","");
        mode=saved.equals("off")?MirrorEngine.MODE_OFF:saved.equals("players")?MirrorEngine.MODE_PLAYERS:saved.equals("normal")?MirrorEngine.MODE_NORMAL:mode;
        backdrop.read(values,prefix+"backdrop");
    }

    void write(Properties values,String prefix) {
        values.setProperty(prefix+"name",name);
        values.setProperty(prefix+"mode",mode==MirrorEngine.MODE_OFF?"off":mode==MirrorEngine.MODE_PLAYERS?"players":"normal");
        backdrop.write(values,prefix+"backdrop");
    }
}
