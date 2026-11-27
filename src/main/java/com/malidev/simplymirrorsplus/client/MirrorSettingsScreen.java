package com.malidev.simplymirrorsplus.client;

import com.malidev.simplymirrorsplus.block.MirrorContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The mirror settings: a bar across the top with the mirrors to choose from, the settings of the chosen one on
 * the left and a live preview on the right. Buttons and text boxes are the game's own; panels, captions and
 * the block chooser's grid are drawn with the game's GUI calls. The live preview and the colour picker are put
 * over the finished frame by {@code MirrorRenderer.drawScreen}. A press that changes which buttons there are
 * opens the screen again in its new state, which is the one way to rebuild it that every version has.
 */
public final class MirrorSettingsScreen extends Screen {
    //? if >=26.3 {
    /*/^* Minecraft 26.3 numbers its keys differently, so the code comes from the game. ^/
    private static final int KEY_M=com.mojang.blaze3d.platform.InputConstants.KEY_M;
    *///?} else {
    private static final int KEY_M=77;
    //?}
    //? if >=1.21.9 {
    /*public static final KeyMapping KEY=new KeyMapping("key.simplymirrorsplus.settings",KEY_M,KeyMapping.Category.MISC);
    *///?} else {
    public static final KeyMapping KEY=new KeyMapping("key.simplymirrorsplus.settings",KEY_M,"key.categories.misc");
    //?}
    private enum Page { MAIN, COLOR, BLOCKS, PICTURES }

    /** The three things the screen draws by itself, whatever the version's GUI calls are named. */
    private interface Painter {
        void fill(int x0,int y0,int x1,int y1,int argb);
        void text(String value,int x,int y,int argb);
        void item(ItemStack stack,int x,int y);
    }

    private static final int MARGIN=6,ROW=20,PITCH=22,BAR=24,CELL=18;
    private static final int PANEL=0xB0101218,EDGE=0x38FFFFFF,WHITE=0xFFFFFFFF,GREY=0xFFA0A8B8,ACCENT=0xFF58B8F0;
    private static final String TITLE="Mirror Settings";
    private static final String[] KINDS={"End sky","Block","Color","Gradient","Picture","Panorama"};
    private static final String[] MOTIONS={"Still","Scroll","Rotate","Pulse","Rainbow"};
    private static final int[] SPEEDS={6,12,30,60,120};
    /** Every block that has an item, by name, each with the stack that draws it; filled when the block chooser first opens. */
    private static List<Block> blocks;
    private static final java.util.Map<Block,ItemStack> ICONS=new java.util.HashMap<Block,ItemStack>();

    public final MirrorUi ui=new MirrorUi();
    private final Screen parent;
    /** The mirror whose settings are shown; empty for the settings all mirrors share. */
    private String selected;
    private Page page=Page.MAIN;
    /** Which of a gradient's two colours the picker edits. */
    private int stop;
    /** The first mirror shown in the bar, and the first row of the block grid and of the picture list. */
    private int firstChip,firstRow,firstPicture;
    private String search="";
    private List<MirrorUi.Entry> mirrors=new ArrayList<MirrorUi.Entry>();
    private List<Block> shownBlocks=new ArrayList<Block>();
    private EditBox hex;
    private int row;
    /** Where the chosen mirror's button sits in the bar, to underline it: x, y, width. */
    private final int[] chosenChip=new int[3];
    /** The room the preview has, in GUI units: x, y, width, height. */
    private final int[] previewRoom=new int[4];
    private int gridX,gridY,gridColumns,gridRows;
    private boolean closed;

    public MirrorSettingsScreen() { this(null,""); }

    /** {@code parent} is shown again when the screen closes, so it can be opened from a mod list. */
    public MirrorSettingsScreen(Screen parent) { this(parent,""); }

    public MirrorSettingsScreen(Screen parent,String selected) {
        super(text("simplymirrorsplus.settings.title"));
        this.parent=parent;
        this.selected=selected;
    }

    /**
     * Opens the settings of the mirror the player is sneaking at with an empty hand.
     * @return true when it did, so the click must not go on to the block
     */
    public static boolean openFor(Minecraft mc) {
        String id=aimedMirror(mc);
        if(id==null||!mc.player.getMainHandItem().isEmpty())return false;
        show(new MirrorSettingsScreen(null,id));
        return true;
    }

    /** While the player sneaks and looks at a mirror, its name stands above the hotbar. */
    public static void showAimedName(Minecraft mc) {
        String id=aimedMirror(mc);
        if(id==null)return;
        //? if >=26.1 {
        /*mc.player.sendOverlayMessage(literal(title(id)));
        *///?} else {
        mc.player.displayClientMessage(literal(title(id)),true);
        //?}
    }

    private static String aimedMirror(Minecraft mc) {
        if(mc.player==null||mc.level==null||!(mc.hitResult instanceof BlockHitResult))return null;
        //? if >=1.15 {
        if(!mc.player.isShiftKeyDown())return null;
        //?} else {
        /*if(!mc.player.isSneaking())return null;
        *///?}
        if(mc.hitResult.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)return null;
        net.minecraft.core.BlockPos pos=((BlockHitResult)mc.hitResult).getBlockPos();
        return MirrorRenderer.mirrorAt(pos.getX(),pos.getY(),pos.getZ());
    }

    /** The mirror's own name, or its position while it has none. */
    static String title(String id) {
        MirrorSettings own=MirrorConfig.mirror(id);
        return own!=null&&!own.name.trim().isEmpty()?own.name.trim():"Mirror "+id.replace("_",", ");
    }

    private static void show(Screen screen) {
        //? if >=26.2 {
        /*Minecraft.getInstance().gui.setScreen(screen);
        *///?} else {
        Minecraft.getInstance().setScreen(screen);
        //?}
    }

    private void reopen() {
        MirrorSettingsScreen next=new MirrorSettingsScreen(parent,selected);
        next.page=page;next.stop=stop;next.firstChip=firstChip;next.firstRow=firstRow;next.firstPicture=firstPicture;next.search=search;
        show(next);
    }

    @Override public void onClose() { closed=true;show(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() { MirrorConfig.save(); }

    @Override protected void init() {
        hex=null;
        row=0;
        chosenChip[2]=previewRoom[2]=0;
        mirrors=MirrorRenderer.mirrors();
        boolean known=selected.isEmpty();
        for(MirrorUi.Entry entry:mirrors)known|=entry.id.equals(selected);
        if(!known)selected="";
        button(width-MARGIN-58,MARGIN+2,56,ROW,"Done",this::onClose);
        if(page==Page.BLOCKS) { blockPage();return; }
        mirrorBar();
        int left=MARGIN+column()+2*MARGIN+4;
        previewRoom[0]=left+MARGIN;previewRoom[1]=contentTop()+MARGIN;previewRoom[2]=width-MARGIN-left-2*MARGIN;previewRoom[3]=height-MARGIN-contentTop()-2*MARGIN-24;
        if(page==Page.COLOR)colorPage();
        else if(page==Page.PICTURES)picturePage();
        else mainPage();
    }

    private int column() { return Math.max(150,Math.min(200,(width-2*MARGIN)*42/100)); }
    private int contentTop() { return MARGIN+BAR+4; }
    private int rowY() { return contentTop()+MARGIN+row++*PITCH; }

    /** The mirrors to choose from, nearest first, after the settings they all share. With too many for the bar, arrows page through them. */
    private void mirrorBar() {
        List<String> ids=new ArrayList<String>(),names=new ArrayList<String>();
        ids.add("");names.add("All mirrors");
        for(MirrorUi.Entry entry:mirrors) { ids.add(entry.id);names.add(title(entry.id)+" ("+Math.round(entry.distance)+" m)"); }
        int x=MARGIN+8+font.width(TITLE)+10,end=width-MARGIN-58-6,arrow=14,total=0,chosen=Math.max(0,ids.indexOf(selected));
        int[] widths=new int[ids.size()];
        for(int i=0;i<widths.length;i++) { widths[i]=Math.min(130,font.width(names.get(i))+12);total+=widths[i]+2; }
        boolean paged=total>end-x;
        int space=end-x-(paged?2*(arrow+2):0),last;
        firstChip=paged?Math.max(0,Math.min(chosen,firstChip)):0;
        // The chosen mirror is always in the bar: the first one shown moves on until it fits.
        for(;;firstChip++) {
            int used=0;
            last=firstChip;
            for(int i=firstChip;i<widths.length&&(used+widths[i]<=space||i==firstChip);i++) { used+=widths[i]+2;last=i; }
            if(chosen<=last)break;
        }
        int at=x;
        if(paged) { final int back=Math.max(0,firstChip-1);button(at,MARGIN+2,arrow,ROW,"<",firstChip>0?()->firstChip=back:null);at+=arrow+2; }
        for(int i=firstChip;i<=last;i++) {
            final String id=ids.get(i);
            button(at,MARGIN+2,widths[i],ROW,names.get(i),()-> { selected=id;page=Page.MAIN; });
            if(i==chosen) { chosenChip[0]=at;chosenChip[1]=MARGIN+2+ROW;chosenChip[2]=widths[i]; }
            at+=widths[i]+2;
        }
        final int next=last+1;
        if(paged)button(at,MARGIN+2,arrow,ROW,">",next<ids.size()?()->firstChip=next:null);
    }

    private void mainPage() {
        MirrorSettings own=selected.isEmpty()?null:MirrorConfig.mirror(selected);
        boolean players;
        if(selected.isEmpty()) {
            wide("Mirrors: "+(MirrorConfig.enabled?"On":"Off"),()->MirrorConfig.enabled=!MirrorConfig.enabled);
            wide("Performance: "+name(MirrorConfig.performance.name()),()->MirrorConfig.performance=next(MirrorConfig.Performance.values(),MirrorConfig.performance.ordinal()));
            wide("Players only: "+(MirrorConfig.playersOnly?"On":"Off"),()->MirrorConfig.playersOnly=!MirrorConfig.playersOnly);
            players=MirrorConfig.playersOnly;
        } else {
            final boolean customized=own!=null;
            wide("Overwrite default settings: "+(customized?"On":"Off"),()-> { if(customized)MirrorConfig.reset(selected);else MirrorConfig.customize(selected); });
            if(own==null)return;
            final MirrorSettings settings=own;
            EditBox name=box(MARGIN+MARGIN,rowY(),column(),24,own.name);
            name.setResponder(value->settings.name=value);
            wide("Mode: "+(own.mode==MirrorEngine.MODE_OFF?"Off":own.mode==MirrorEngine.MODE_PLAYERS?"Players only":"Normal"),
                ()->settings.mode=settings.mode==MirrorEngine.MODE_NORMAL?MirrorEngine.MODE_PLAYERS:settings.mode==MirrorEngine.MODE_PLAYERS?MirrorEngine.MODE_OFF:MirrorEngine.MODE_NORMAL);
            players=own.mode==MirrorEngine.MODE_PLAYERS;
        }
        // Only a Players only reflection has a backdrop.
        if(players)backdropRows(own!=null?own.backdrop:MirrorConfig.backdrop);
    }

    private void backdropRows(final MirrorBackdrop backdrop) {
        wide("Backdrop: "+KINDS[backdrop.kind.ordinal()],()->backdrop.kind=next(MirrorBackdrop.Kind.values(),backdrop.kind.ordinal()));
        switch(backdrop.kind) {
            case BLOCK: {
                Block block=MirrorContent.block(backdrop.block);
                wide("Block: "+(block==null?backdrop.block:block.getName().getString()),()-> { page=Page.BLOCKS;firstRow=0;search=""; });
                break;
            }
            case COLOR:
                wide("Color: "+MirrorBackdrop.hex(backdrop.color),()-> { page=Page.COLOR;stop=0; });
                break;
            case GRADIENT:
                pair("Color 1: "+MirrorBackdrop.hex(backdrop.color),()-> { page=Page.COLOR;stop=0; },"Color 2: "+MirrorBackdrop.hex(backdrop.color2),()-> { page=Page.COLOR;stop=1; });
                pair("Shape: "+(backdrop.radial?"Radial":"Linear"),()->backdrop.radial=!backdrop.radial,
                    backdrop.radial?null:"Angle: "+backdrop.angle,()->backdrop.angle=(backdrop.angle/45*45+45)%360);
                pair("Motion: "+MOTIONS[backdrop.motion.ordinal()],()->backdrop.motion=next(MirrorBackdrop.Motion.values(),backdrop.motion.ordinal()),
                    backdrop.motion==MirrorBackdrop.Motion.NONE?null:"Speed: "+backdrop.speed,()->backdrop.speed=nextSpeed(backdrop.speed));
                break;
            case IMAGE:
            case PANORAMA:
                wide("Picture: "+(pictures().contains(backdrop.image)?backdrop.image:"None"),()-> { page=Page.PICTURES;firstPicture=0; });
                break;
            default:
        }
    }

    private void colorPage() {
        final MirrorBackdrop backdrop=backdrop();
        int x=2*MARGIN,w=column();
        button(x,rowY(),w,ROW,"Back",()->page=Page.MAIN);
        hex=box(x,rowY(),w,7,MirrorBackdrop.hex(stop==0?backdrop.color:backdrop.color2));
        hex.setResponder(value-> {
            // A typed colour counts once it is complete, with or without the leading #.
            if(value.length()!=(value.startsWith("#")?7:6))return;
            int typed=MirrorBackdrop.color(value,-1);
            if(typed<0)return;
            if(stop==0)backdrop.color=typed;else backdrop.color2=typed;
        });
    }

    /** The pictures in the backgrounds folder, a way to bring one in from anywhere, and the folder itself. */
    private void picturePage() {
        final MirrorBackdrop backdrop=backdrop();
        final List<String> files=pictures();
        int x=2*MARGIN,w=column();
        button(x,rowY(),w,ROW,"Back",()->page=Page.MAIN);
        int bottom=height-MARGIN-MARGIN-2*PITCH,room=Math.max(1,(bottom-(contentTop()+MARGIN+PITCH))/PITCH);
        firstPicture=Math.max(0,Math.min(Math.max(0,files.size()-room),firstPicture));
        boolean paged=files.size()>room;
        for(int i=firstPicture;i<Math.min(files.size(),firstPicture+room);i++) {
            final String file=files.get(i);
            button(x,rowY(),paged?w-32:w,ROW,file,()-> { backdrop.image=file;page=Page.MAIN; });
            if(file.equals(backdrop.image)) { chosenChip[0]=x;chosenChip[1]=contentTop()+MARGIN+(row-1)*PITCH+ROW;chosenChip[2]=paged?w-32:w; }
        }
        if(files.isEmpty())button(x,rowY(),w,ROW,"No pictures yet",null);
        if(paged) {
            button(x+w-30,contentTop()+MARGIN+PITCH,14,ROW,"<",firstPicture>0?()->firstPicture--:null);
            button(x+w-14,contentTop()+MARGIN+PITCH,14,ROW,">",firstPicture+room<files.size()?()->firstPicture++:null);
        }
        int half=(w-2)/2;
        button(x,bottom+MARGIN,half,ROW,"Choose file...",()->chooseFile(backdrop));
        button(x+half+2,bottom+MARGIN,w-half-2,ROW,"Open folder",MirrorSettingsScreen::openFolder);
    }

    /**
     * Asks for a picture with the system's own file window and copies it into the backgrounds folder.
     * The window comes from a library the game ships; it is called by name so that nothing here depends on it.
     */
    private static void chooseFile(MirrorBackdrop backdrop) {
        try {
            Class<?> dialogs=Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
            Object chosen=dialogs.getMethod("tinyfd_openFileDialog",CharSequence.class,CharSequence.class,Class.forName("org.lwjgl.PointerBuffer"),CharSequence.class,boolean.class)
                .invoke(null,"Choose a picture","",null,null,false);
            if(chosen instanceof String)take(java.nio.file.Paths.get((String)chosen),backdrop);
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable) {
            java.util.logging.Logger.getLogger("simplymirrorsplus").log(java.util.logging.Level.WARNING,"No file window on this system; put pictures into the backgrounds folder instead",unavailable);
        }
    }

    /** Copies a picture into the backgrounds folder and makes it the backdrop. Returns false for anything that is not a picture. */
    private static boolean take(java.nio.file.Path file,MirrorBackdrop backdrop) {
        String name=file.getFileName().toString(),lower=name.toLowerCase(Locale.ROOT);
        java.nio.file.Path folder=MirrorConfig.pictures();
        if(folder==null||!(lower.endsWith(".png")||lower.endsWith(".jpg")||lower.endsWith(".jpeg")))return false;
        try {
            java.nio.file.Files.createDirectories(folder);
            if(!folder.resolve(name).equals(file))java.nio.file.Files.copy(file,folder.resolve(name),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch(java.io.IOException unreadable) {
            return false;
        }
        MirrorRenderer.reloadPictures();
        backdrop.image=name;
        return true;
    }

    private static void openFolder() {
        java.nio.file.Path folder=MirrorConfig.pictures();
        if(folder==null)return;
        try { java.nio.file.Files.createDirectories(folder); } catch(java.io.IOException unwritable) { return; }
        //? if >=26.3 {
        /*// The game's own helper for this is gone in 26.3, so the system's opener is started directly.
        String system=System.getProperty("os.name","").toLowerCase(Locale.ROOT),path=folder.toString();
        try {
            new ProcessBuilder(system.contains("win")?new String[]{"explorer",path}:system.contains("mac")?new String[]{"open",path}:new String[]{"xdg-open",path}).start();
        } catch(java.io.IOException noOpener) {
            // Nothing to open folders with; the folder still exists.
        }
        *///?} else if >=1.21.11 {
        /*net.minecraft.util.Util.getPlatform().openFile(folder.toFile());
        *///?} else {
        net.minecraft.Util.getPlatform().openFile(folder.toFile());
        //?}
    }

    //? if >=1.16 {
    /** Pictures dropped on the window go into the backgrounds folder; the first becomes the backdrop. */
    @Override public void onFilesDrop(List<java.nio.file.Path> files) {
        boolean taken=false;
        for(java.nio.file.Path file:files)taken|=take(file,taken?new MirrorBackdrop():backdrop());
        if(taken) { if(page==Page.PICTURES)page=Page.MAIN;reopen(); }
    }
    //?}

    /** Every block as its item, in a grid like an inventory, with a search box above it. */
    private void blockPage() {
        if(blocks==null)blocks=listBlocks();
        button(MARGIN+8+font.width(TITLE)+10,MARGIN+2,50,ROW,"Back",()->page=Page.MAIN);
        EditBox box=box(2*MARGIN,contentTop()+MARGIN,Math.min(200,width-4*MARGIN),40,search);
        gridX=2*MARGIN;gridY=contentTop()+MARGIN+PITCH+2;
        gridColumns=Math.max(1,(width-4*MARGIN)/CELL);gridRows=Math.max(1,(height-MARGIN-MARGIN-14-gridY)/CELL);
        box.setResponder(value-> { search=value;firstRow=0;filterBlocks(); });
        // The search box has the keyboard from the start, so typing narrows the grid at once.
        setInitialFocus(box);
        //? if <1.16 {
        /*// On these versions that alone leaves the box without its cursor.
        box.setFocus(true);
        setFocused(box);
        *///?}
        filterBlocks();
    }

    private void filterBlocks() {
        String wanted=search.trim().toLowerCase(Locale.ROOT);
        shownBlocks=new ArrayList<Block>();
        for(Block block:blocks)if(wanted.isEmpty()||block.getName().getString().toLowerCase(Locale.ROOT).contains(wanted))shownBlocks.add(block);
        scrollBlocks(0);
    }

    private void scrollBlocks(int rows) {
        int last=Math.max(0,(shownBlocks.size()+gridColumns-1)/gridColumns-gridRows);
        firstRow=Math.max(0,Math.min(last,firstRow+rows));
    }

    /** The block under the mouse in the grid, or null. */
    private Block blockAt(double mouseX,double mouseY) {
        if(page!=Page.BLOCKS||mouseX<gridX||mouseY<gridY)return null;
        int column=(int)(mouseX-gridX)/CELL,line=(int)(mouseY-gridY)/CELL,at=(firstRow+line)*gridColumns+column;
        return column<gridColumns&&line<gridRows&&at<shownBlocks.size()?shownBlocks.get(at):null;
    }

    private boolean clickGrid(double mouseX,double mouseY) {
        Block block=blockAt(mouseX,mouseY);
        if(block==null)return false;
        backdrop().block=MirrorContent.id(block);
        page=Page.MAIN;
        reopen();
        return true;
    }

    private static List<Block> listBlocks() {
        List<Block> found=new ArrayList<Block>();
        //? if >=1.19.3 {
        Iterable<Block> all=net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        //?} else {
        /*Iterable<Block> all=net.minecraft.core.Registry.BLOCK;
        *///?}
        for(Block block:all)if(block.asItem()!=net.minecraft.world.item.Items.AIR) { found.add(block);ICONS.put(block,new ItemStack(block)); }
        java.util.Collections.sort(found,new java.util.Comparator<Block>() {
            @Override public int compare(Block a,Block b) { return a.getName().getString().compareToIgnoreCase(b.getName().getString()); }
        });
        return found;
    }

    private MirrorBackdrop backdrop() {
        MirrorSettings own=selected.isEmpty()?null:MirrorConfig.mirror(selected);
        return own!=null?own.backdrop:MirrorConfig.backdrop;
    }

    private static List<String> pictures() {
        List<String> names=new ArrayList<String>();
        java.nio.file.Path folder=MirrorConfig.pictures();
        if(folder==null)return names;
        try {
            java.nio.file.Files.createDirectories(folder);
            try(java.nio.file.DirectoryStream<java.nio.file.Path> stream=java.nio.file.Files.newDirectoryStream(folder)) {
                for(java.nio.file.Path file:stream) {
                    String name=file.getFileName().toString(),lower=name.toLowerCase(Locale.ROOT);
                    if(lower.endsWith(".png")||lower.endsWith(".jpg")||lower.endsWith(".jpeg"))names.add(name);
                }
            }
        } catch(java.io.IOException unreadable) {
            // An unreadable folder shows as an empty list.
        }
        java.util.Collections.sort(names);
        return names;
    }

    private static int nextSpeed(int speed) {
        for(int candidate:SPEEDS)if(candidate>speed)return candidate;
        return SPEEDS[0];
    }

    private static <E> E next(E[] values,int ordinal) { return values[(ordinal+1)%values.length]; }
    private static String name(String constant) { return constant.charAt(0)+constant.substring(1).toLowerCase(Locale.ROOT); }

    /** A button across the settings column, on the next row. */
    private void wide(String label,Runnable action) { button(2*MARGIN,rowY(),column(),ROW,label,action); }

    /** Two buttons sharing the next row; a null label leaves the second one out. */
    private void pair(String first,Runnable firstAction,String second,Runnable secondAction) {
        int half=(column()-2)/2,y=rowY();
        button(2*MARGIN,y,second==null?column():half,ROW,first,firstAction);
        if(second!=null)button(2*MARGIN+half+2,y,column()-half-2,ROW,second,secondAction);
    }

    /** A button that runs the action and then rebuilds the screen. Without an action it is a caption. */
    private Button button(int x,int y,int w,int h,String label,final Runnable action) {
        //? if >=1.19.3 {
        Button button=Button.builder(literal(label),pressed->press(action)).bounds(x,y,w,h).build();
        //?} else if >=1.16 {
        /*Button button=new Button(x,y,w,h,literal(label),pressed->press(action));
        *///?} else {
        /*Button button=new Button(x,y,w,h,label,pressed->press(action));
        *///?}
        button.active=action!=null;
        return add(button);
    }

    private void press(Runnable action) {
        action.run();
        if(!closed)reopen();
    }

    private EditBox box(int x,int y,int w,int limit,String value) {
        //? if >=1.16 {
        EditBox box=new EditBox(font,x+1,y+1,w-2,ROW-2,literal(""));
        //?} else {
        /*EditBox box=new EditBox(font,x+1,y+1,w-2,ROW-2,"");
        *///?}
        box.setMaxLength(limit);
        box.setValue(value);
        return add(box);
    }

    private <W extends AbstractWidget> W add(W widget) {
        //? if >=1.17 {
        return addRenderableWidget(widget);
        //?} else {
        /*return addButton(widget);
        *///?}
    }

    /** What lies under the buttons: the panels, the title and the captions. */
    private void behind(Painter painter) {
        panel(painter,MARGIN,MARGIN,width-2*MARGIN,BAR);
        painter.text(TITLE,MARGIN+8,MARGIN+8,WHITE);
        int top=contentTop(),bottom=height-MARGIN;
        if(page==Page.BLOCKS) { panel(painter,MARGIN,top,width-2*MARGIN,bottom-top);return; }
        int left=MARGIN+column()+2*MARGIN;
        panel(painter,MARGIN,top,left-MARGIN,bottom-top);
        panel(painter,left+4,top,width-MARGIN-left-4,bottom-top);
        String shown=previewed();
        MirrorUi.Entry entry=null;
        for(MirrorUi.Entry candidate:mirrors)if(candidate.id.equals(shown))entry=candidate;
        int x=previewRoom[0],y=previewRoom[1]+previewRoom[3]+4;
        if(entry==null) { painter.text("No mirror nearby",x,previewRoom[1],GREY);return; }
        painter.text(title(entry.id),x,y,WHITE);
        painter.text(entry.blocks+(entry.blocks==1?" block, ":" blocks, ")+Math.round(entry.distance)+" m away"+(selected.isEmpty()?", nearest":""),x,y+11,GREY);
    }

    /** What lies over the buttons: the line under the chosen entry and the block grid. */
    private void over(Painter painter,int mouseX,int mouseY) {
        if(chosenChip[2]>0)painter.fill(chosenChip[0]+1,chosenChip[1]-2,chosenChip[0]+chosenChip[2]-1,chosenChip[1],ACCENT);
        if(page==Page.PICTURES)painter.text("Or drop picture files here",2*MARGIN,height-MARGIN-2*PITCH+ROW+5,GREY);
        if(page!=Page.BLOCKS)return;
        String chosen=backdrop().block;
        Block hovered=blockAt(mouseX,mouseY);
        for(int line=0;line<gridRows;line++)for(int column=0;column<gridColumns;column++) {
            int at=(firstRow+line)*gridColumns+column;
            if(at>=shownBlocks.size())break;
            Block block=shownBlocks.get(at);
            int x=gridX+column*CELL,y=gridY+line*CELL;
            painter.fill(x,y,x+CELL-1,y+CELL-1,block==hovered?0x70FFFFFF:0x30000000);
            if(MirrorContent.id(block).equals(chosen)) { painter.fill(x,y+CELL-3,x+CELL-1,y+CELL-1,ACCENT); }
            painter.item(ICONS.get(block),x+1,y+1);
        }
        int rows=(shownBlocks.size()+gridColumns-1)/gridColumns;
        String status=hovered!=null?hovered.getName().getString():shownBlocks.size()+" blocks"+(rows>gridRows?", scroll for more":"");
        painter.text(status,gridX,height-MARGIN-12,hovered!=null?WHITE:GREY);
    }

    private static void panel(Painter painter,int x,int y,int w,int h) {
        painter.fill(x,y,x+w,y+h,PANEL);
        painter.fill(x,y,x+w,y+1,EDGE);painter.fill(x,y+h-1,x+w,y+h,EDGE);
        painter.fill(x,y+1,x+1,y+h-1,EDGE);painter.fill(x+w-1,y+1,x+w,y+h-1,EDGE);
    }

    /**
     * Called once a frame while the screen is up, with the size of the window in pixels. Lays out the preview and
     * the colour picker, which are drawn over the frame, and keeps the picker and the colour box in step.
     */
    void frame(int pixelWidth,int pixelHeight,float previewAspect) {
        float scale=(float)pixelWidth/Math.max(1,width);
        if(page==Page.BLOCKS||previewRoom[2]<=0)ui.preview(0,0,0,0,0);
        else {
            // The picture gets three quarters of its room: enough to judge the mirror by, without filling the screen.
            int w=previewRoom[2]*3/4,h=previewRoom[3]*3/4;
            ui.preview(Math.round((previewRoom[0]+(previewRoom[2]-w)/2)*scale),Math.round((previewRoom[1]+(previewRoom[3]-h)/2)*scale),Math.round(w*scale),Math.round(h*scale),previewAspect);
        }
        if(page!=Page.COLOR) { ui.picker(0,0,0,0,0);return; }
        MirrorBackdrop backdrop=backdrop();
        int current=stop==0?backdrop.color:backdrop.color2,top=contentTop()+MARGIN+2*PITCH+2;
        ui.picker(Math.round(2*MARGIN*scale),Math.round(top*scale),Math.round(column()*scale),Math.round(Math.min(column()*.8f,height-top-2*MARGIN)*scale),current);
    }

    /** Takes the colour the picker arrived at this frame. */
    void picked() {
        if(page!=Page.COLOR)return;
        MirrorBackdrop backdrop=backdrop();
        int current=stop==0?backdrop.color:backdrop.color2,chosen=ui.color();
        if(chosen==current)return;
        if(stop==0)backdrop.color=chosen;else backdrop.color2=chosen;
        if(hex!=null&&!hex.isFocused())hex.setValue(MirrorBackdrop.hex(chosen));
    }

    /** The mirror the preview shows: the selected one, or the nearest while the shared settings are open. */
    String previewed() { return page==Page.BLOCKS?"":!selected.isEmpty()?selected:mirrors.isEmpty()?"":mirrors.get(0).id; }

    //? if >=26.1 {
    /*@Override public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partial) {
        super.extractBackground(graphics,mouseX,mouseY,partial);
        behind(painter(graphics));
    }
    @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partial) {
        super.extractRenderState(graphics,mouseX,mouseY,partial);
        over(painter(graphics),mouseX,mouseY);
    }
    private Painter painter(final net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        return new Painter() {
            @Override public void fill(int x0,int y0,int x1,int y1,int argb) { graphics.fill(x0,y0,x1,y1,argb); }
            @Override public void text(String value,int x,int y,int argb) { graphics.text(font,value,x,y,argb); }
            @Override public void item(ItemStack stack,int x,int y) { graphics.item(stack,x,y); }
        };
    }
    *///?} else if >=1.20 {
    //? if >=1.20.2 {
    @Override public void renderBackground(net.minecraft.client.gui.GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        super.renderBackground(graphics,mouseX,mouseY,partial);
        behind(painter(graphics));
    }
    @Override public void render(net.minecraft.client.gui.GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        super.render(graphics,mouseX,mouseY,partial);
        over(painter(graphics),mouseX,mouseY);
    }
    //?} else {
    /*@Override public void render(net.minecraft.client.gui.GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        renderBackground(graphics);
        behind(painter(graphics));
        super.render(graphics,mouseX,mouseY,partial);
        over(painter(graphics),mouseX,mouseY);
    }
    *///?}
    private Painter painter(final net.minecraft.client.gui.GuiGraphics graphics) {
        return new Painter() {
            @Override public void fill(int x0,int y0,int x1,int y1,int argb) { graphics.fill(x0,y0,x1,y1,argb); }
            @Override public void text(String value,int x,int y,int argb) { graphics.drawString(font,value,x,y,argb); }
            @Override public void item(ItemStack stack,int x,int y) { graphics.renderItem(stack,x,y); }
        };
    }
    //?} else if >=1.16 {
    /*@Override public void render(final com.mojang.blaze3d.vertex.PoseStack pose,int mouseX,int mouseY,float partial) {
        Painter painter=new Painter() {
            @Override public void fill(int x0,int y0,int x1,int y1,int argb) { net.minecraft.client.gui.GuiComponent.fill(pose,x0,y0,x1,y1,argb); }
            @Override public void text(String value,int x,int y,int argb) { net.minecraft.client.gui.GuiComponent.drawString(pose,font,value,x,y,argb); }
            //? if >=1.19.4 {
            @Override public void item(ItemStack stack,int x,int y) { itemRenderer.renderGuiItem(pose,stack,x,y); }
            //?} else {
            /^@Override public void item(ItemStack stack,int x,int y) { itemRenderer.renderGuiItem(stack,x,y); }
            ^///?}
        };
        renderBackground(pose);
        behind(painter);
        super.render(pose,mouseX,mouseY,partial);
        over(painter,mouseX,mouseY);
    }
    *///?} else {
    /*@Override public void render(int mouseX,int mouseY,float partial) {
        Painter painter=new Painter() {
            @Override public void fill(int x0,int y0,int x1,int y1,int argb) { net.minecraft.client.gui.GuiComponent.fill(x0,y0,x1,y1,argb); }
            @Override public void text(String value,int x,int y,int argb) { drawString(font,value,x,y,argb); }
            @Override public void item(ItemStack stack,int x,int y) {
                //? if <1.15 {
                /^// On 1.14 an item drawn here comes out dark without the GUI's own lighting.
                com.mojang.blaze3d.platform.Lighting.turnOnGui();
                itemRenderer.renderGuiItem(stack,x,y);
                com.mojang.blaze3d.platform.Lighting.turnOff();
                ^///?} else {
                itemRenderer.renderGuiItem(stack,x,y);
                //?}
            }
        };
        renderBackground();
        behind(painter);
        super.render(mouseX,mouseY,partial);
        over(painter,mouseX,mouseY);
    }
    *///?}

    //? if >=1.20.2 {
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) { return scrolled(vertical)||super.mouseScrolled(x,y,horizontal,vertical); }
    //?} else {
    /*@Override public boolean mouseScrolled(double x,double y,double amount) { return scrolled(amount)||super.mouseScrolled(x,y,amount); }
    *///?}

    private boolean scrolled(double amount) {
        if(page!=Page.BLOCKS||amount==0)return false;
        scrollBlocks(amount>0?-2:2);
        return true;
    }

    //? if >=1.21.9 {
    /*@Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick) {
        boolean left=event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
        if(left)ui.button(true);
        return left&&clickGrid(event.x(),event.y())||super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)ui.button(false);
        return super.mouseReleased(event);
    }
    *///?} else {
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0)ui.button(true);
        return button==0&&clickGrid(x,y)||super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseReleased(double x,double y,int button) {
        if(button==0)ui.button(false);
        return super.mouseReleased(x,y,button);
    }
    //?}

    static Component literal(String value) {
        //? if >=1.19 {
        return Component.literal(value);
        //?} else {
        /*return new net.minecraft.network.chat.TextComponent(value);
        *///?}
    }

    static Component text(String key,Object... arguments) {
        //? if >=1.19 {
        return Component.translatable(key,arguments);
        //?} else {
        /*return new net.minecraft.network.chat.TranslatableComponent(key,arguments);
        *///?}
    }
}
