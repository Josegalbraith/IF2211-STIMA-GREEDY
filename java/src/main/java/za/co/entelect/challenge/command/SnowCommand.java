import za.co.entelect.challenge.entities;

public class SnowCommand extends Weapon{

    private final int x;
    private final int y;

    public SnowCommand(int x, int y,int count) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String render() {
        return String.format("snow %d %d", x, y);
    }
}
