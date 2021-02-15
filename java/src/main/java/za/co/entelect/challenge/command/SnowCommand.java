package za.co.entelect.challenge.command;

public class SnowCommand extends Weapon{

    private final int x;
    private final int y;

    public SnowCommand(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String render() {
        return String.format("snow %d %d", x, y);
    }
}
