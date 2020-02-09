package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static za.co.entelect.challenge.enums.BuildingType.ATTACK;
import static za.co.entelect.challenge.enums.BuildingType.DEFENSE;

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
        if(ironCurtainCondition()) return(buildCommand(0, 0, BuildingType.IRONCURTAIN));
        if(!teslaBuilt()){//at least 1 own tesla exist
            if (isUnderAttack()) {
                return defendRow(); //greed by my lowest defend value
            } else if (needEnergy() && canAffordBuilding(BuildingType.ENERGY)) {
                return buildEnergy(); // build until NOT(needEnergy())
            } else if (canAffordBuilding(BuildingType.TESLA) && isteslaEnergyReq() && (buildTesla()!= NOTHING_COMMAND)) { 
                return buildTesla(); // build Tesla
            } else if (canAffordBuilding(BuildingType.ATTACK)) {
                return attackByLowestDef(); // greed by lowest enemy defence
            } else {
                return doNothingCommand();
            }
        }
        else{
            return NOTHING_COMMAND; //sistem fawis in here
        }
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
        return (myself.energy >= 200);
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
        int i;
        boolean crampedAtt;
        for (i=0; i < gameDetails.mapHeight; i++){
            int EnemyAtt = getAllBuildingsForPlayer(PlayerType.A, b-> b.buildingType == BuildingType.ATTACK, i);
            int EnemyDeff = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i);
            if (mostEnemyAtt < EnemyAtt){
                mostEnemyAtt = EnemyAtt;
                EnemyAttRow = i;
            }
            if (mostEnemyDeff < EnemyDeff) {
                mostEnemyDeff = EnemyDeff;
                EnemyDeffRow = i;
            }
        }
        if (mostEnemyAtt > 4){
            j = gameDetails.mapWidth/2 - 4 ;
            i = EnemyAttRow;
        } else {
            j = gameDetails.mapWidth/2 - 1;
            i = EnemyDeffRow;
        }

        if ((i-1>=0) && isCellEmpty(i-1, j)){ //Validasi nilai?
            return buildCommand(i, j, BuildingType.TESLA);
        } else if ((i+1 < gameDetails.mapHeight) && isCellEmpty(i+1, j)) {
            return buildCommand(i, j, BuildingType.TESLA);
        } else if (isCellEmpty(i, j)){
            return buildCommand(i, j, BuildingType.TESLA);
        } else {
            return NOTHING_COMMAND;
        } 
    }

    /**
     * Need energy
     *
     * @return sum of my energy > x, x = certain number
     **/
    private boolean needEnergy(){
        int countEnergy=0;
        for (int i = 0; i < gameWidth / 2; i++){
            int myEnergyOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();
            countEnergy += myEnergyOnRow;
        }
        return (countEnergy < 10);
    }
    /**
     * Build energy
     *
     * @return the result
     **/
    private String buildEnergy() {

        for (int k = 0; k < gameWidth / 2; k++) {//col from 0 to max
            int i=0; //row from bottom
            int j=gameHeight; //row from top
            while(i<=j){
                int myEnergyOnRowI = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();
                int myEnergyOnRowJ = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, j).size();
                if (isCellEmpty(i, k)) {
                    return buildCommand(k, i, BuildingType.ENERGY);
                }
                if (isCellEmpty(j, k)) {
                    return buildCommand(k, j, BuildingType.ENERGY);
                }
            }
        }
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
            int myRowStrength = myDefenseOnRow*3 + myAttackOnRow + myEnergyOnRow;
            //BAGIAN GREEDY : MENCARI NILAI MAX
            if(enemyAttackOnRow - myRowStrength > tempVurneability) weakestSpot = i;
        }
        return placeDefence(weakestSpot);
    }
    private String placeDefence(int y) {
        int myEnergyOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();
        boolean enoughEnergy = myEnergyOnRow >=2;
        if (canAffordBuilding(BuildingType.DEFENCE))
        {
            for (int i = 5; i >= (gameWidth / 2) - 1; i++) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, BuildingType.DEFENSE);}
            }
        }
        if (canAffordBuilding(BuildingType.ENERGY) && !enoughEnergy)
        {
            for (int i = 0; i >= (gameWidth / 2) - 1; i++) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, BuildingType.ENERGY);}
            }
        }
        if (canAffordBuilding(BuildingType.ATTACK) )
        {
            for (int i = 0; i >= (gameWidth / 2) - 1; i++) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, BuildingType.ATTACK);}
            }
        }
        return doNothingCommand();
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
            int myRowStrength = myDefenseOnRow*3 + myAttackOnRow + myEnergyOnRow;
            //boolean opponentAttacks = getAnyBuildingsForPlayer(PlayerType.B, building -> building.buildingType == ATTACK, i);
            //boolean myDefense = getAnyBuildingsForPlayer(PlayerType.A, building -> building.buildingType == DEFENSE, i);

            if (enemyAttackOnRow >= myRowStrength) {
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
        for (int i = gameHeight; i >= 0; i--) {
            int enemyAttackOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int enemyDefencekOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.DEFENCE, i).size();
            int enemyTeslaOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.TESLA, i).size();
            int enemyEnergyOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ENERGY, i).size();
            int enemyRowStrength = enemyDefenseOnRow*3 + enemyAttackOnRow + enemyEnergyOnRow + enemyTeslaOnRow;
            int myAttackOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ATTACK, i).size();
            if(enemyRowStrength - (myAttackOnRow/2) > tempVurneability) weakestSpot = i;
        }
        for (int i = 0; i >= (gameWidth / 2) - 1; i++) {
            if (isCellEmpty(i, weakestSpot)) {
                return buildCommand(i, weakestSpot, BuildingType.ATTACK);}
            }
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
                .filter(direction.LEFT)
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
                .filter(BuildingType.TESLA && (constructionTimeLeft <=1))
                .collect(Collectors.toList());
    }
    /**
     * iron curtain condition
     *
     * @return the result
     **/
    private boolean ironCurtainCondition() {
        boolean cond1 = canAffordBuilding(BuildingType.IRONCURTAIN); //bisa beli iron curtain
        boolean cond2 = getAllMissilesForPlayer(PlayerType.B) > 7; // kalo ada missile lebih dari x
        boolean cond3 = enemyTeslaChecker().size() > 0 && opponent.energy >= gameDetails.buildingsStats.get(BuildingType.TESLA).energyPerShot + 20 ; //tesla ready to fire checker
        return(cond1 && (cond2 || cond3));
    }
}
