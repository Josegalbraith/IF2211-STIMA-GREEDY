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

        //Ambil informasi tentang entitas worm musuh
        Worm enemyWorm = getFirstWormInRange();
        Worm mangsa = getFirstWormInRangeWeapon(currentWorm);
        Worm commando = gameState.myPlayer.worms[0];
        Worm technologistenemy = gameState.opponents[0].worms[2];
        Worm agentenemy = gameState.opponents[0].worms[1];

        //Tembak kalo ada yg di range. tembak senjata spesial yang dimiliki jika belum habis, Fungsi Kelayakan
        if (mangsa != null && mangsa.health > 0 && currentWorm.profession == Profession.TECHNOLOGIST) {
            if (currentWorm.snowball.count > 0 && mangsa.roundsUntilUnfrozen == 0) {
                return new SnowCommand(mangsa.position.x, mangsa.position.y);
            }
        }
        if (mangsa != null && mangsa.health > 0 && currentWorm.profession == Profession.AGENT) {
            if (currentWorm.banana.count > 0 && technologistenemy.health <= 0) {
                return new BananaCommand(mangsa.position.x, mangsa.position.y);
            }
        }
        if (enemyWorm != null && enemyWorm.health > 0) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }

        //Commando berburu berdasarkan prioritas technologist - agent - commando
        if(currentWorm.profession == Profession.COMMANDO){
            int idxOpponent = 2;
            Worm prey = gameState.opponents[0].worms[idxOpponent]; //technologist
            if(prey.health <= 0){
                idxOpponent--;
                prey = gameState.opponents[0].worms[idxOpponent]; //agent
                if(prey.health <= 0){
                    idxOpponent--;
                    prey = gameState.opponents[0].worms[idxOpponent]; //commando
                }
            }

            //Bergerak mendekati target
            Direction direction = resolveDirection(currentWorm.position, prey.position);
            Cell target = gameState.map[currentWorm.position.y+direction.y][currentWorm.position.x+direction.x];
            if(target.type == CellType.AIR) {
                return new MoveCommand(target.x, target.y);
            } else if(target.type == CellType.DIRT) {
                return new DigCommand(target.x, target.y);
            }
        }

        //Agent ikut memburu technologist agar musuh tidak bisa membekukan lagi
        if (currentWorm.profession == Profession.AGENT && technologistenemy.health > 0) {
            //Menembak musuh terdekat, meskipun bukan technologist untuk jika musuh berhasil mendekat
            Worm cacingMusuh = getFirstWormInRange();
            if (cacingMusuh != null && cacingMusuh.health > 0) {
                Direction direction = resolveDirection(currentWorm.position, cacingMusuh.position);
                return new ShootCommand(direction);
            } else {
                //Mendekat ke technologist musuh
                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, technologistenemy.position.x, technologistenemy.position.y) > 4) {
                    Direction direction = resolveDirection(currentWorm.position, technologistenemy.position);

                    int targetx = currentWorm.position.x + direction.x;
                    int targety = currentWorm.position.y + direction.y;

                    Cell target = gameState.map[targety][targetx];

                    if (target.type == CellType.AIR) {
                        return new MoveCommand(targetx, targety);
                    } else if (target.type == CellType.DIRT) {
                        return new DigCommand(targetx, targety);
                    }
                }
            }
        }

        //Technologist membekukan enemy terdekat untuk melambatkan pergerakan musuh
        if (currentWorm.profession == Profession.TECHNOLOGIST && agentenemy.health > 0) {
            //membekukan musuh dalam range snow terdekat jika musuh belum beku
            Worm cacingMusuh = getFirstWormInRange();
            if (cacingMusuh != null && cacingMusuh.health > 0 && cacingMusuh.roundsUntilUnfrozen == 0 &&
                    euclideanDistance(currentWorm.position.x, currentWorm.position.y, cacingMusuh.position.x, cacingMusuh.position.y) <= 5 ) {

                Direction direction = resolveDirection(currentWorm.position, cacingMusuh.position);
                int targetx = currentWorm.position.x + direction.x;
                int targety = currentWorm.position.y + direction.y;

                return new SnowCommand(targetx, targety);
            }
            //menembak musuh dalam range snow terdekat jika musuh sudah beku
            if (cacingMusuh != null && cacingMusuh.health > 0 && cacingMusuh.roundsUntilUnfrozen > 0) {
                Direction direction = resolveDirection(currentWorm.position, cacingMusuh.position);
                return new ShootCommand(direction);
            } else { //jika tidak dalam range, maka mendekat
                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, agentenemy.position.x, agentenemy.position.y) > 4 && agentenemy.health>0) {
                    Direction direction = resolveDirection(currentWorm.position, agentenemy.position);

                    int targetx = currentWorm.position.x + direction.x;
                    int targety = currentWorm.position.y + direction.y;

                    Cell target = gameState.map[targety][targetx];

                    if (target.type == CellType.AIR) {
                        return new MoveCommand(targetx, targety);
                    } else if (target.type == CellType.DIRT) {
                        return new DigCommand(targetx, targety);
                    }
                }
            }
        }

        //Check ada healtpack atau tidak, jika masih ada coba ambil dulu
        Cell hpackcell = getHPack();
        if(hpackcell != null){
            Position mid = new Position();
            mid.x = hpackcell.x;
            mid.y = hpackcell.y;
            Direction arah = resolveDirection(currentWorm.position, mid);
            Cell tuju = gameState.map[currentWorm.position.y+arah.y][currentWorm.position.x+arah.x];
            if (tuju.type == CellType.AIR) {
                return new MoveCommand(currentWorm.position.x+arah.x, currentWorm.position.y+arah.y);
            } else if (tuju.type == CellType.DIRT) {
                return new DigCommand(currentWorm.position.x+arah.x, currentWorm.position.y+arah.y);
            }
        }

        //Not commando, mendekat ke commando
        if (currentWorm.profession != Profession.COMMANDO) {
            //jika commando belum mati
            if (commando.health > 0) {
                //mendekat jika belum
                if(euclideanDistance(currentWorm.position.x, currentWorm.position.y, commando.position.x, commando.position.y) >= 5){
                    Direction direction = resolveDirection(currentWorm.position, commando.position);

                    int targetx = currentWorm.position.x+direction.x;
                    int targety = currentWorm.position.y+direction.y;

                    Cell target = gameState.map[targety][targetx];

                    if(target.type == CellType.AIR) {
                        return new MoveCommand(targetx, targety);
                    } else if(target.type == CellType.DIRT) {
                        return new DigCommand(targetx, targety);
                    }
                }
                //bantu menyerang worm-worm musuh jika sudah dalam range
                else {
                    Worm cacingMusuh = getFirstWormInRangeCustom();
                    if (cacingMusuh != null && cacingMusuh.health > 0) {
                        Worm cacingMusuhTembak = getFirstWormInRange();
                        if(cacingMusuhTembak != null && cacingMusuhTembak.health > 0){
                            Direction direction = resolveDirection(currentWorm.position, cacingMusuhTembak.position);
                            return new ShootCommand(direction);
                        }
                        else{
                            Direction direction = resolveDirection(currentWorm.position, cacingMusuh.position);
                            return new MoveCommand(currentWorm.position.x+direction.x, currentWorm.position.y+direction.y);
                        }
                    }
                }
            } else { //jika commando sudah mati
                Worm cacingMatiWoi = getFirstWormInRangeCustom();
                if (cacingMatiWoi != null && cacingMatiWoi.health > 0) {
                    Worm cacingMusuhAneh = getFirstWormInRange();
                    //jika dalam range, tembak
                    if(cacingMusuhAneh != null && cacingMusuhAneh.health > 0){
                        Direction direction = resolveDirection(currentWorm.position, cacingMusuhAneh.position);
                        return new ShootCommand(direction);
                    }
                    //jika tidak, mendekat
                    else{
                        Direction direction = resolveDirection(currentWorm.position, cacingMatiWoi.position);
                        return new MoveCommand(currentWorm.position.x+direction.x, currentWorm.position.y+direction.y);
                    }
                }
            }
        }
        

        //Random untuk meng else agar tidak 12 consecutive idle
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

    private Worm getFirstWormInRangeCustom() {

        Set<String> cells = constructFireDirectionLines(13)
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

    private Worm getFirstWormInRangeWeapon(MyWorm cur) {
        for (Worm enemyWorm : opponent.worms) {

            if (euclideanDistance(cur.position.x, cur.position.y, enemyWorm.position.x, enemyWorm.position.y) <= 5) {
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


    private Cell getHPack(){
        for(int i = 14; i <= 20; i++){
            for(int j = 14; j <= 20; j++){
                if(gameState.map[j][i].powerUp != null){
                    return gameState.map[j][i];
                }
            }
        }
        return null;
    }
}