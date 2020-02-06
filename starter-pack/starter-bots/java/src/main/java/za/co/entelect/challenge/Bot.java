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
        if(!teslaBuilt){//at least 1 own tesla exist
            if (isUnderAttack()) {
                return defendRow(); //greed by my lowest defend value
            } else if (needEnergy() && canAffordBuilding(BuildingType.ENERGY)) {
                return buildEnergy(); // build until NOT(needEnergy())
            } /*else if (canAffordBuilding(BuildingType.TESLA)) { 
                return buildEnergy(); // ini butuh mekanisme kalo udah 200 energy
            } */else if (canAffordBuilding(BuildingType.ATTACK)) {
                return attackByLowestDef(); // greed by lowest enemy defence
            } else {
                return doNothingCommand();
            }
        }
        else{}//sistem fawis
    }
    /**
     * Need energy
     *
     * @return sum of my energy > x, x = certain number
     **/
    private boolean needEnergy(){
        int countEnergy=0;
        for (int i = 0; i < gameState.gameDetails.mapWidth / 2; i++){
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
}
