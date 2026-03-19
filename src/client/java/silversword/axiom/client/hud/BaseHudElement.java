package silversword.axiom.client.hud;

public abstract class BaseHudElement implements HudElement {
    protected final String id;
    protected boolean enabled = true;
    protected int x, y;
    protected HudComponentSettings settings = new HudComponentSettings(); // uusi

    protected BaseHudElement(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    @Override public String id() { return id; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean v) { this.enabled = v; }
    @Override public int x() { return x; }
    @Override public int y() { return y; }
    @Override public void setPos(int x, int y) { this.x = x; this.y = y; }

    public HudComponentSettings getSettings() { return settings; }
}