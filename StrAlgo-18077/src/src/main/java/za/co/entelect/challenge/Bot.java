package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.Direction;
import za.co.entelect.challenge.enums.PlayerType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static za.co.entelect.challenge.enums.BuildingType.*;
import static za.co.entelect.challenge.entities.BuildingStats.*;

public class Bot {
    private static final String NOTHING_COMMAND = "";
    private GameState gameState;
    private GameDetails gameDetails;
    private int gameWidth;
    private int gameHeight;
    private Player myself;
    private Player opponent;
    private List<Building> buildings;
    private List<Missile> missiles;

    /**
     * Constructor
     *
     * @param gameState the game state
     **/
    public Bot(GameState gameState) {
        this.gameState = gameState;
        gameDetails = gameState.getGameDetails();
        gameWidth = gameDetails.mapWidth;
        gameHeight = gameDetails.mapHeight;
        myself = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.A).findFirst().get();
        opponent = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.B).findFirst().get();

        buildings = gameState.getGameMap().stream()
                .flatMap(c -> c.getBuildings().stream())
                .collect(Collectors.toList());

        missiles = gameState.getGameMap().stream()
                .flatMap(c -> c.getMissiles().stream())
                .collect(Collectors.toList());
    }

    /**
     * Run
     *
     * @return the result
     **/
    public String run() {
        String command = "";
        if (isteslaEnergyReq() && (command == "")) {
            command = buildTesla(); // build Tesla
        }
        if (isUnderAttack() && command == "") {
            command = defendRow(); //greed by my lowest defend value
        }
        if (numEnergy() < 10 && canAffordBuilding(BuildingType.ENERGY) && command == "") {
            command = buildEnergy(); // build until NOT(needEnergy())
        }
        if (numEnergy() >= 5 && canAffordBuilding(BuildingType.ATTACK) && command == "") {
            command = attackByLowestDef(); // greed by lowest enemy defence
        }

        // return "";
    return command;
    }
    /**
     * is Tesla already built at least 1
     *
     * @return true if there is at least 1 tesla
     **/
    private boolean teslaBuilt(){
        int myTesla = 0;
        for (int i = 0; i < gameHeight; i++) {
            myTesla += getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.TESLA, i).size();
        }
        return (myTesla > 0);
    }
    /**
     * is Energy enough for Tesla Requirement
     * @return true if energy > Tesla construct Cost + one Tesla fire Cost
     */
    private boolean isteslaEnergyReq(){
        if (numEnergy() == 10) return myself.energy >= 160;
        if (numEnergy() > gameHeight) return myself.energy >= 170;
        return (myself.energy >= 180);
    }

    /**
     * Build Tesla
     * @return command to build tesla
     */
    private String buildTesla() {
        int mostEnemyAtt = 0;
        int EnemyAttRow = 0;
        int mostEnemyDeff = 0;
        int EnemyDeffRow = 0;
        List<Integer> enemyAttRowList = new ArrayList<Integer>();
        List<Integer> enemyDefRowList = new ArrayList<Integer>();
        int pickedRow;
        int i,j;
        for (i=0; i < gameDetails.mapHeight; i++){
            int EnemyAtt = getAllBuildingsForPlayer(PlayerType.B, b-> b.buildingType == BuildingType.ATTACK, i).size();
            int EnemyDeff = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.DEFENSE, i).size();

            enemyAttRowList.add(EnemyAtt * 10 + i);
            enemyDefRowList.add(EnemyDeff * 10 + i);
//            if (mostEnemyAtt < EnemyAtt){
//                mostEnemyAtt = EnemyAtt;
//                EnemyAttRow = i;
//            }
//            if (mostEnemyDeff < EnemyDeff) {
//                mostEnemyDeff = EnemyDeff;
//                EnemyDeffRow = i;
//            }
        }
        Collections.sort(enemyAttRowList, Collections.reverseOrder());
        Collections.sort(enemyDefRowList, Collections.reverseOrder());

        // Checks for attack > 4
        for (i = 0; i<enemyAttRowList.size(); i++){
            if (Math.floorDiv(enemyAttRowList.get(i),10) >= 4){
                pickedRow = enemyAttRowList.get(i) % 10;
                // Checks if occupied
                if (isCellEmpty(gameWidth/2 -1, pickedRow)){
                    return buildCommand(gameWidth/2 -1, pickedRow, BuildingType.TESLA);
                }
            }
        }

        // if fail, checks for defense
        for (i = 0; i<enemyDefRowList.size(); i++){
            pickedRow = enemyDefRowList.get(i) % 10;
            // Checks if occupied
            if (isCellEmpty(gameWidth/2 - 1, pickedRow)){
                return buildCommand(gameWidth/2 -1, pickedRow, BuildingType.TESLA);
            }
        }
        return "";
//        if (mostEnemyAtt > 4){
//            j = gameDetails.mapWidth/2 - 4 ;
//            i = EnemyAttRow;
//        } else {
//            j = gameDetails.mapWidth/2 - 1;
//            i = EnemyDeffRow;
//        }
//
//        if ((i-1>=0) && isCellEmpty(i-1, j)){ //Validasi nilai?
//            return buildCommand(i, j, BuildingType.TESLA);
//        } else if ((i+1 < gameDetails.mapHeight) && isCellEmpty(i+1, j)) {
//            return buildCommand(i, j, BuildingType.TESLA);
//        } else if (isCellEmpty(i, j)){
//            return buildCommand(i, j, BuildingType.TESLA);
//        } else {
//            return NOTHING_COMMAND;
//        }
    }

    /**
     * Need energy
     *
     * @return sum of my energy > x, x = certain number
     **/
    private int numEnergy(){
        int countEnergy=0;
        for (int i = 0; i < gameHeight; i++){
            int myEnergyOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();
            countEnergy += myEnergyOnRow;
        }
        return countEnergy;
    }
    /**
     * Build energy
     *
     * @return the result
     **/
    private String buildEnergy() {

        for (int k = 0; k < gameWidth / 2; k++) {//x from 0 to max
            int i=0; //row from top
            int j=gameHeight-1; //row from bottom
            while(i<=j){
//                int myEnergyOnRowI = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();
//                int myEnergyOnRowJ = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, j).size();
                if (isCellEmpty(k, i)) {
                    return buildCommand(k, i, BuildingType.ENERGY);
                }
                if (isCellEmpty(k, j)) {
                    return buildCommand(k, j, BuildingType.ENERGY);
                }
                i++; j--;
            }
        }
        return "";
    }
    /**
     * Defend row
     *
     * @return the result
     **/
    private String defendRow() {
        int weakestSpot=0;//most vurneable spot by row
        int tempVurneability=0;
        for (int i = 0; i < gameHeight; i++) {
            //boolean opponentAttacking = getAnyBuildingsForPlayer(PlayerType.B, b -> b.buildingType == ATTACK, i);
            int enemyAttackOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int myDefenseOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size();
            int myEnergyOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();
            int myAttackOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int myRowStrength = myDefenseOnRow*3;
            //BAGIAN GREEDY : MENCARI NILAI MAX
            if (enemyAttackOnRow > 1 && myDefenseOnRow == 0 ){
                placeDefence(i);
            }
            if (enemyAttackOnRow > 0 && myAttackOnRow == 0 && myDefenseOnRow > 0 && canAffordBuilding(BuildingType.ATTACK)){
                weakestSpot = i;
                for (int j = (gameWidth /2) - 4; j >= 1; j--) {
                    if (isCellEmpty(j, weakestSpot)) {
                        return buildCommand(j, weakestSpot, BuildingType.ATTACK);}
                }
            }
            if(enemyAttackOnRow - myRowStrength > tempVurneability){
                tempVurneability = enemyAttackOnRow - myRowStrength;
                weakestSpot = i;
            }
        }
        return placeDefence(weakestSpot);
    }
    private String placeDefence(int y) {
        int myEnergyOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, y).size();
        boolean enoughEnergy = myEnergyOnRow >=2;
        if (canAffordBuilding(BuildingType.DEFENSE))
        {
            for (int i = 5; i < (gameWidth / 2) - 1; i++) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, BuildingType.DEFENSE);}
            }
        }
        if (canAffordBuilding(BuildingType.ENERGY) && !enoughEnergy)
        {
            for (int i = 0; i < (gameWidth / 2) - 1; i++) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, BuildingType.ENERGY);}
            }
        }
        if (canAffordBuilding(BuildingType.ATTACK) ) {
            for (int i = 1; i < (gameWidth / 2) - 1; i++) {
                if (isCellEmpty(i, y)) {
                    return buildCommand(i, y, BuildingType.ATTACK);
                }
            }
        }
        return "";
    }

    /**
     * Checks if this is under attack
     *
     * @return true if this is under attack
     **/
    private boolean isUnderAttack() {
        //if enemy has an attack building and i dont have a blocking wall
        for (int i = 0; i < gameHeight; i++) {
            int enemyAttackOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int myDefenseOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size();
            int myEnergyOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();
            int myAttackOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int myRowStrength = myDefenseOnRow*3;
            //boolean opponentAttacks = getAnyBuildingsForPlayer(PlayerType.B, building -> building.buildingType == ATTACK, i);
            //boolean myDefense = getAnyBuildingsForPlayer(PlayerType.A, building -> building.buildingType == DEFENSE, i);

            if (enemyAttackOnRow > myRowStrength || ((myDefenseOnRow == 0 || myAttackOnRow == 0) && enemyAttackOnRow > 0)) {
                return true;
            }
        }
        return false;
    }
    /**
     * Do nothing command
     *
     * @return the result
     **/
    private String attackByLowestDef() {
        int weakestSpot=0;//most vurneable spot by row
        float tempVurneability=0;
        int pickedRow;
        List<Integer> enemyDefList = new ArrayList<>();
        for (int i = gameHeight-1; i >= 0; i--) {
            int enemyAttackOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int enemyDefenseOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.DEFENSE, i).size();
            int enemyTeslaOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.TESLA, i).size();
            int enemyEnergyOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ENERGY, i).size();
            int enemyRowStrength = enemyDefenseOnRow*3 + enemyAttackOnRow + enemyEnergyOnRow + enemyTeslaOnRow;
            int myAttackOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ATTACK, i).size();
            enemyDefList.add(enemyRowStrength * 10 + i);
            if(enemyRowStrength - myAttackOnRow > tempVurneability) {
                tempVurneability = enemyRowStrength - myAttackOnRow;
                weakestSpot = i;
            }
        }
        Collections.sort(enemyDefList, Collections.reverseOrder());
        for (int i = (gameWidth /2) - 4; i >= 1; i--) {
            if (isCellEmpty(i, weakestSpot)) {
                return buildCommand(i, weakestSpot, BuildingType.ATTACK);}
            }
        // if all cells already occupied, search in other row
        for (int i = 1; i<enemyDefList.size(); i++){
            pickedRow = enemyDefList.get(i) % 10;
            for (int j = gameWidth/2 - 4; j >= 1; j--){
                if (isCellEmpty(j, pickedRow)) {
                    return buildCommand(j, pickedRow, BuildingType.ATTACK);
                }
            }
        }
        return "";
    }
    /**
     * Do nothing command
     *
     * @return the result
     **/
    private String doNothingCommand() {
        return NOTHING_COMMAND;
    }

    /**
     * Place building in row
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
     private String placeBuildingInRowFromFront(BuildingType buildingType, int y) {
        for (int i = (gameWidth / 2) - 1; i >= 0; i--) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return doNothingCommand();
    }

    /**
     * Place building in row y nearest to the back
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowFromBack(BuildingType buildingType, int y) {
        for (int i = 0; i < gameWidth / 2; i++) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return doNothingCommand();
    }

    /**
     * Can afford building
     *
     * @param buildingType the building type
     * @return the result
     **/
    private boolean canAffordBuilding(BuildingType buildingType) {
        return myself.energy >= gameDetails.buildingsStats.get(buildingType).price;
    }
    /**
     * Get all buildings for player in row y
     *
     * @param playerType the player type
     * @param filter     the filter
     * @param y          the y
     * @return the result
     **/
    private List<Building> getAllBuildingsForPlayer(PlayerType playerType, Predicate<Building> filter, int y) {
        return gameState.getGameMap().stream()
                .filter(c -> c.cellOwner == playerType && c.y == y)
                .flatMap(c -> c.getBuildings().stream())
                .filter(filter)
                .collect(Collectors.toList());
    }
    /**
     * Get all missiles for player
     *
     * @param playerType the player type
     * @return the result
     **/
    private List<Missile> getAllMissilesForPlayer(PlayerType playerType) {
        return gameState.getGameMap().stream()
                //.filter(p -> p.playerType == playerType)
                .flatMap(c -> c.getMissiles().stream())
                .filter(p -> p.isPlayers(playerType))
                .collect(Collectors.toList());
    }
    /**
     * Get all enemy active tesla (or at least 1 turn before working)
     *
     * @return the result
     **/
    private List<Building> enemyTeslaChecker() {
        return gameState.getGameMap().stream()
                .filter(c -> c.cellOwner == PlayerType.B)
                .flatMap(c -> c.getBuildings().stream())
                .filter(b -> b.buildingType == BuildingType.TESLA && (b.constructionTimeLeft <= 1 ))
                .collect(Collectors.toList());
    }
    /**
     * iron curtain condition
     *
     * @return the result
     **/
    private boolean ironCurtainCondition() {
        int TeslaEnergyPerShot = 100;
        boolean cond1 = canAffordBuilding(BuildingType.IRONCURTAIN); //bisa beli iron curtain
        boolean cond2 = getAllMissilesForPlayer(PlayerType.B).size() > 7; // kalo ada missile lebih dari x
        boolean cond3 = enemyTeslaChecker().size() > 0 && opponent.energy >= TeslaEnergyPerShot + 20 ; //tesla ready to fire checker
        return(cond1 && (cond2 || cond3));
    }

    /**
     * Construct build command
     *
     * @param x            the x
     * @param y            the y
     * @param buildingType the building type
     * @return the result
     **/
    private String buildCommand(int x, int y, BuildingType buildingType) {
        return String.format("%d,%d,%s", x, y, String.valueOf(buildingType.getType()));
    }

    /**
     * Checks if cell at x,y is empty
     *
     * @param x the x
     * @param y the y
     * @return the result
     **/
    private boolean isCellEmpty(int x, int y) {
        Optional<CellStateContainer> cellOptional = gameState.getGameMap().stream()
                .filter(c -> c.x == x && c.y == y)
                .findFirst();

        if (cellOptional.isPresent()) {
            CellStateContainer cell = cellOptional.get();
            return cell.getBuildings().size() <= 0;
        } else {
            System.out.println("Invalid cell selected");
        }
        return true;
    }
}
