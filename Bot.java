package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;
import za.co.entelect.challenge.enums.Profession;

import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {
        GameState gameState = new GameState();

        if (currentWorm.profession != Profession.COMMANDO) { //bukan commando
            for(int i = 0; i<=2; i++){
                if  (gameState.myPlayer.worms[i].profession == Profession.COMMANDO){ //mencari worm commando
                 Worm commando = gameState.myPlayer.worms[i]; //worm commando disimpan sebagai sebuah variabel
                    Direction direction = resolveDirection(currentWorm.position,commando.position); 
                return new MoveCommand(direction.x,direction.y);
                }
            }
         }

        else{ 
            Worm enemyWorm = getFirstWormInRange();
            if (enemyWorm != null) {
                Direction direction = resolveDirection(currentWorm.position = Profession.Agent, enemyWorm.position);
                return new ShootCommand(direction);
	        }
        }
        Worm enemyWorm = getFirstWormInRange();
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }

        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        int cellIdx = random.nextInt(surroundingBlocks.size());

        Cell block = surroundingBlocks.get(cellIdx);
        if (block.type == CellType.AIR) {
            return new MoveCommand(block.x, block.y);
        } else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        } else if (block.type == CellType.LAVA){
            int firstdirt = -1;
            for(int i = 0; i<surroundingBlocks.size(); i++){
                block = surroundingBlocks.get(i);
                if(block.type != CellType.LAVA){
                    if(block.type == CellType.AIR){
                        return new MoveCommand(block.x, block.y);
                    }
                    else if(block.type == CellType.DIRT){
                        if(firstdirt == -1){
                            firstdirt = i;
                        }
                    }
                }
            }
            block = surroundingBlocks.get(firstdirt);
            return new DigCommand(block.x, block.y);
        }
        return new DoNothingCommand();
    }

    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition)) {
                return enemyWorm;
            }
        }

        return null;
    }

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }

    private boolean FriendlyFireSnowball(Position a, MyPlayer myPlayer) {
        boolean valid = true;

        for (int i = 0; i < 3; i++) { //Ngeloop seluruh worm yang dimiliki
            if (myPlayer.worms[i].position.x <= a.x + 1 || //Ngecek di daerah range 1
                myPlayer.worms[i].position.x >= a.x - 1 ||
                myPlayer.worms[i].position.y <= a.y + 1 ||
                myPlayer.worms[i].position.y >= a.y - 1) {
                    valid = false;
                }                
        }
        return valid;
    }

    private boolean FriendlyFireBanana(Position a, MyPlayer myPlayer) {
        boolean valid = true;

        if (FriendlyFireSnowball(a, myPlayer)) { //Ngecek apakah di range 1 terlebih dahulu, pakai fungsi lama
            for (int i = 0; i < 3; i++) { //Loop seluruh cacing yang kita punya
                if (((myPlayer.worms[i].position.x == a.x + 2 || //Ngecek apakah di horizontal/vertikal range 2
                    myPlayer.worms[i].position.x == a.x - 2) &&
                    myPlayer.worms[i].position.y == a.y) ||
                    ((myPlayer.worms[i].position.y == a.y + 2 ||
                    myPlayer.worms[i].position.y == a.y - 2) &&
                    myPlayer.worms[i].position.x == a.x)) {
                        valid = false;
                }                
            }
        }
        return valid;
    }
}