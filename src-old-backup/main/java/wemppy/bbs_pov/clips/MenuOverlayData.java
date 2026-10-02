package wemppy.bbs_pov.clips;

public class MenuOverlayData
{
    public final String menuType;
    public final String deathMessage;
    public final String score;
    public final int backgroundColor;
    public final boolean buttonsActive;
    public final boolean hardcore;
    public final float factor;

    public MenuOverlayData(String menuType, String deathMessage, String score, int backgroundColor, boolean buttonsActive, boolean hardcore, float factor)
    {
        this.menuType = menuType;
        this.deathMessage = deathMessage;
        this.score = score;
        this.backgroundColor = backgroundColor;
        this.buttonsActive = buttonsActive;
        this.hardcore = hardcore;
        this.factor = factor;
    }
}
