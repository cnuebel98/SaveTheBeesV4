/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mybot;

import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.GreedyPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.List;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import java.util.Random;

/**
 *
 * @author carlo
 */
public class SaveTheBeesV4 extends AbstractionLayerAI{
    UnitTypeTable m_utt = null;
    UnitType worker;
    UnitType base;
    UnitType barracks;
    UnitType ranged;
    UnitType heavy;
    UnitType light;
    
    public SaveTheBeesV4(UnitTypeTable m_utt) {
        this(m_utt, new AStarPathFinding());
    }
    
    public SaveTheBeesV4(UnitTypeTable utt, PathFinding a_pf) {
        super(a_pf);
        reset(utt);
    }
    
    public void reset() {
        super.reset();
    }
    
    public void reset(UnitTypeTable utt){
        m_utt = utt;
        if (m_utt != null) {
            m_utt = utt;
            worker = utt.getUnitType("Worker");
            base = utt.getUnitType("Base");
            barracks = utt.getUnitType("Barracks");
            heavy = utt.getUnitType("Heavy");
            ranged = utt.getUnitType("Ranged");
            light = utt.getUnitType("Light");
        }
    }
    
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        PlayerAction pa = new PlayerAction();
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        // initializing Variables to get an Overview which Player has which 
        // Unit based on that a lot of the decisions are made
        int numberOfResources = 0;
        int numberOfWorkers = 0;
        int numberOfBases = 0;
        int numberOfHeavy = 0;
        int numberOfRanged = 0;
        int numberOfBarracks = 0;
        int numberOfEnemyWorkers = 0;
        int numberOfEnemyBases = 0;
        int numberOfLight = 0;
        int numberOfEnemyHeavy = 0;
        int numberOfEnemyRanged = 0;
        int numberOfEnemyBarracks = 0;
        int TargetWorkerNumber = 0;
        int numberOfEnemyLight = 0;
        int distanceBaseAndClosestResource = 0;
        int sumOfOwnBarrackUnits = 0;
        int sumOfEnemyMovingUnits = 0;
        
        // At a 16x16 Map, both Variables will be 16
        int mapHeight = pgs.getHeight();
        int mapWidth = pgs.getWidth();
        int targetBarracksNo = 0;
        boolean rush = false;
        boolean rangedOnly = false;
        boolean bw = false;
        boolean twoBases = false;
        boolean doubleGame = false;
        boolean weirdLMap = false;
        boolean standard = false;
        // At small open maps, you have to do a worker rush
        if (mapHeight+mapWidth == 16){
            rush = true;
        }
        // At the NoWhereToRun Map you win by building Ranged 
        if (mapHeight+mapWidth == 17){
            rangedOnly = true;
        }
        // At BroodWars Huge 128 Map
        if (mapHeight+mapWidth == 128){
            bw = true;
        }
        if (mapHeight+mapWidth == 48){
            doubleGame = true;
        }
        if (mapHeight+mapWidth == 64){
            weirdLMap = true;
        }
        if (mapHeight+mapWidth == 32){
            standard = true;
        }  
        
        // The desired amount of barracks depends on the size of the map
        if (mapHeight+mapWidth >= 48){
            targetBarracksNo = 4;
        } else if (mapHeight+mapWidth > 40){
            targetBarracksNo = 3;
        } else if (mapHeight+mapWidth > 32){
            targetBarracksNo = 2;
        } else {
            targetBarracksNo = 1;
        }
        
        // Counting up all the units in a GameState
        for (Unit u : pgs.getUnits()){
            if(u.getType() == worker && u.getPlayer() == p.getID()) {
                numberOfWorkers += 1;
            } else if (u.getType() == base && u.getPlayer() == p.getID()){
                numberOfBases += 1;
            } else if (u.getType() == heavy && u.getPlayer() == p.getID()){
                numberOfHeavy += 1;
            } else if (u.getType() == ranged && u.getPlayer() == p.getID()){
                numberOfRanged += 1;
            } else if (u.getType() == light && u.getPlayer() == p.getID()){
                numberOfLight += 1;
            } else if (u.getType() == barracks && u.getPlayer() == p.getID()){
                numberOfBarracks += 1;
            } else if (u.getType() == worker && u.getPlayer() != p.getID()){
                numberOfEnemyWorkers += 1;
            } else if (u.getType() == base && u.getPlayer() != p.getID()){
                numberOfEnemyBases += 1;
            } else if (u.getType() == heavy && u.getPlayer() != p.getID()){
                numberOfEnemyHeavy += 1;
            } else if (u.getType() == ranged && u.getPlayer() != p.getID()){
                numberOfEnemyRanged += 1;
            } else if (u.getType() == barracks && u.getPlayer() != p.getID()){
                numberOfEnemyBarracks += 1;
            } else if (u.getType().isResource){
                numberOfResources += 1;
            } else if (u.getType() == light && u.getPlayer() != p.getID()){
                numberOfEnemyLight += 1;
            }
        }
        
        sumOfEnemyMovingUnits = numberOfEnemyWorkers 
                + numberOfEnemyHeavy 
                + numberOfEnemyRanged 
                + numberOfEnemyLight;
        sumOfOwnBarrackUnits = numberOfRanged 
                + numberOfHeavy 
                + numberOfLight;
        if (numberOfBases == 2){
            twoBases = true;
        }
        
        // Calculates all the distances between the Players Base and all
        // Resources and safes the closest
        for (Unit u : pgs.getUnits()){
            if (u.getType() == base
                    && u.getPlayer() == p.getID()){
                for (Unit u2 : pgs.getUnits()){
                    if (u2.getType().isResource){
                        int d = 0;
                        d = distanceFromStartToTarget(
                                u.getX(), 
                                u.getY(), 
                                u2.getX(), 
                                u2.getY());
                        if (distanceBaseAndClosestResource == 0 
                                || d < distanceBaseAndClosestResource){
                            distanceBaseAndClosestResource = d;
                        }
                    }
                }
            }
        }
        
// Base Behaviour START --------------------------------------------------------
        // Here we will decide how many workers to build
        TargetWorkerNumber = 2;
        
        if (weirdLMap == true){
            TargetWorkerNumber = 2;
        }
        if (rush == true){
            TargetWorkerNumber = 4;
        }
        if(gs.getTime() == 0 && twoBases == true){
            
            Unit firstBase = null;
            for (Unit u : pgs.getUnits()){
                if (u.getType() == base
                    && u.getPlayer() == p.getID()
                    && gs.getActionAssignment(u)==null
                    && firstBase == null){
                    firstBase = u;
                }
            }
            
            if (gs.getActionAssignment(firstBase)==null){
                int d = freeNeighborCell(
                        firstBase.getX(),
                        firstBase.getY(),
                        gs);
                UnitAction ua = new UnitAction(
                        UnitAction.TYPE_PRODUCE,
                        d, 
                        worker);
                if (gs.isUnitActionAllowed(firstBase, ua)){
                    pa.addUnitAction(firstBase, ua);
                    numberOfWorkers += 1;
                }
            }
        }
        

        for (Unit u : pgs.getUnits()){
            // When there are less than TargetWorkerNumber Workers, build a 
            // worker in a free neighbor cell of the base
            if(u.getType() == base
                    && u.getPlayer() == p.getID()
                    && gs.getActionAssignment(u)==null
                    && numberOfWorkers <= TargetWorkerNumber
                    && worker.cost <= p.getResources()
                    && twoBases == false
                    && standard == false){
                int d = freeNeighborCell(
                        u.getX(),
                        u.getY(),
                        gs);
                UnitAction ua = new UnitAction(
                        UnitAction.TYPE_PRODUCE,
                        d, 
                        worker);
                if (gs.isUnitActionAllowed(u, ua)){
                    pa.addUnitAction(u, ua);
                    numberOfWorkers += 1;
                }
            } else if (twoBases == true 
                    && worker.cost <= p.getResources()
                    && u.getPlayer() == p.getID()
                    && gs.getActionAssignment(u)==null
                    && numberOfWorkers < 2
                    && gs.getTime() > 100){
                int d = freeNeighborCell(
                        u.getX(),
                        u.getY(),
                        gs);
                UnitAction ua = new UnitAction(
                        UnitAction.TYPE_PRODUCE,
                        d,
                        worker);
                if (gs.isUnitActionAllowed(u, ua)){
                    pa.addUnitAction(u, ua);
                    numberOfWorkers += 1;
                }
            } else if(standard == true
                    && twoBases == false
                    && worker.cost <= p.getResources()
                    && u.getPlayer() == p.getID()
                    && gs.getActionAssignment(u)==null
                    && numberOfWorkers < 3){
                int d = freeNeighborCell(
                        u.getX(),
                        u.getY(),
                        gs);
                UnitAction ua = new UnitAction(
                        UnitAction.TYPE_PRODUCE,
                        d,
                        worker);
                if (gs.isUnitActionAllowed(u, ua)){
                    pa.addUnitAction(u, ua);
                    numberOfWorkers += 1;
                }
            }
        
// Base Behaviour End ----------------------------------------------------------
// Barracks Behaviour START ----------------------------------------------------
          
            if(u.getType() == barracks
                    && u.getPlayer() == p.getID()
                    && gs.getActionAssignment(u)==null
                    ){
                int i = 0;
                
                if(rangedOnly == true){
                    i = 0;
                } else if (weirdLMap == true || doubleGame == true || standard == true){
                    i = 1;
                } else if (numberOfEnemyHeavy > numberOfEnemyLight 
                        && numberOfEnemyHeavy > numberOfEnemyRanged){
                    // If enemy has a lot of Heavy, build Light
                    i = 1;
                } else if(numberOfEnemyRanged > numberOfEnemyLight 
                        && numberOfEnemyRanged > numberOfEnemyHeavy){
                    // If enemy has a lot of Ranged, build Heavy
                    i = 2;
                } else if (numberOfEnemyLight > numberOfEnemyRanged
                        && numberOfEnemyLight > numberOfEnemyHeavy){
                    // If enemy has a lot Light, build light
                    i = 1;
                } else {
                    // If enemy has equal Unit amounts, build light
                    i = 1;
                }
                
                if (twoBases == false) {
                    if (i == 0 && p.getResources() >= ranged.cost){
                        int d = freeNeighborCell(
                                u.getX(),
                                u.getY(),
                                gs);
                        UnitAction ua = new UnitAction(
                                UnitAction.TYPE_PRODUCE,
                                d, 
                                ranged);
                        if (gs.isUnitActionAllowed(u, ua)){
                            pa.addUnitAction(u, ua);
                        }
                    } else if (i == 1 && p.getResources() >= light.cost){
                        int d = freeNeighborCell(
                                u.getX(),
                                u.getY(),
                                gs);
                        if (weirdLMap == true){
                            d = 1;
                        }
                        UnitAction ua = new UnitAction(
                                UnitAction.TYPE_PRODUCE,
                                d,
                                light);
                        if (gs.isUnitActionAllowed(u, ua)){
                            pa.addUnitAction(u, ua);
                        }
                    } else if (i == 2 && p.getResources() >= heavy.cost) {
                        int d = freeNeighborCell(
                                u.getX(),
                                u.getY(),
                                gs);
                        UnitAction ua = new UnitAction(
                                UnitAction.TYPE_PRODUCE,
                                d,
                                heavy);
                        if (gs.isUnitActionAllowed(u, ua)){
                            pa.addUnitAction(u, ua);
                        }
                    }
                } else if (twoBases == true){
                    
                    if(gs.getTime() == 0){
                        Unit firstBarracks = null;
                        for (Unit u3 : pgs.getUnits()){
                            if (u3.getType() == barracks
                                && u3.getPlayer() == p.getID()
                                && gs.getActionAssignment(u)==null
                                && firstBarracks == null){  
                                firstBarracks = u3;
                            
                            int d = freeNeighborCell(
                                firstBarracks.getX(), 
                                firstBarracks.getY(), 
                                gs);
                            UnitAction ua = new UnitAction(
                                UnitAction.TYPE_PRODUCE,
                                d,
                                light);
                            if (gs.isUnitActionAllowed(firstBarracks, ua)){
                                pa.addUnitAction(firstBarracks, ua);
                            }}
                        }
                    } else if (i == 0 && p.getResources() > ranged.cost){
                        int d = freeNeighborCell(
                                u.getX(),
                                u.getY(),
                                gs);
                        UnitAction ua = new UnitAction(
                                UnitAction.TYPE_PRODUCE,
                                d, 
                                ranged);
                        if (gs.isUnitActionAllowed(u, ua)){
                            pa.addUnitAction(u, ua);
                        }
                    } else if (i == 1 && p.getResources() > light.cost){
                        int d = freeNeighborCell(
                                u.getX(), 
                                u.getY(), 
                                gs);
                        UnitAction ua = new UnitAction(
                                UnitAction.TYPE_PRODUCE,
                                d, 
                                light);
                        if (gs.isUnitActionAllowed(u, ua)){
                            pa.addUnitAction(u, ua);
                        }
                    } else if (i == 2 && p.getResources() > heavy.cost) {
                        int d = freeNeighborCell(
                                u.getX(), 
                                u.getY(), 
                                gs);
                        UnitAction ua = new UnitAction(
                                UnitAction.TYPE_PRODUCE,
                                d,
                                light);
                        if (gs.isUnitActionAllowed(u, ua)){
                            pa.addUnitAction(u, ua);
                        }
                    }
                }
            }
// Barracks Behaviour END ------------------------------------------------------
// Heavy Behaviour START -------------------------------------------------------
        
            Unit closestEnemyH = null;
            int closestDistanceH = 0;
            //find closest Enemy Unit and attack
            if(u.getType() == heavy
                    && u.getPlayer() == p.getID()
                    && gs.getActionAssignment(u)==null
                    ){
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getPlayer() != u.getPlayer()
                            && !u2.getType().isResource){
                        int distance = 0;
                        distance = distanceFromStartToTarget(
                                u.getX(),
                                u.getY(),
                                u2.getX(),
                                u2.getY());
                        if (closestEnemyH == null
                                || distance < closestDistanceH){
                            closestEnemyH = u2;
                            closestDistanceH = distance;
                        }
                    }
                }
                // now we know which enemy Unit is closest and can go to it if 
                // necessary or attack it, if it is in range
                if (closestDistanceH == 1) {
                    UnitAction ua = new UnitAction(
                            UnitAction.TYPE_ATTACK_LOCATION,
                            closestEnemyH.getX(), 
                            closestEnemyH.getY());
                    pa.addUnitAction(u, ua);
                } else {
                    int targetposition = 0; 
                    targetposition = closestEnemyH.getX() + closestEnemyH.getY()*pgs.getWidth();
                    
                    UnitAction ua = pf.findPathToAdjacentPosition(u, targetposition, gs, null);
                    
                    if (ua != null && gs.isUnitActionAllowed(u, ua)){
                        pa.addUnitAction(u, ua);
                    }
                }
            }
        

// Heavy Behaviour END ---------------------------------------------------------
// Ranged Behaviour START ------------------------------------------------------
        
            Unit closestEnemyR = null;
            int closestDistanceR = 0;
            if(u.getType() == ranged
                    && u.getPlayer() == p.getID()
                    && gs.getActionAssignment(u)==null
                    ){
                // find closest Enemy Unit and attack
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getPlayer() != u.getPlayer()
                            && !u2.getType().isResource){
                        int distance = 0;
                        distance = distanceFromStartToTarget(
                                u.getX(),
                                u.getY(),
                                u2.getX(),
                                u2.getY());
                        if (closestEnemyR == null
                                || distance < closestDistanceR){
                            closestEnemyR = u2;
                            closestDistanceR = distance;
                        }
                    }
                }
                // now we know which enemy Unit is closest and can go to it if 
                // necessary or attack it, if it is in range
                if (closestDistanceR == ranged.attackRange) {
                    UnitAction ua = new UnitAction(
                            UnitAction.TYPE_ATTACK_LOCATION,
                            closestEnemyR.getX(),
                            closestEnemyR.getY());
                    pa.addUnitAction(u, ua);
                } 
                // if enemy Unit is out of Attack Range,towards it
                else if (closestDistanceR > 3) {
                    
                    int targetposition = 0; 
                    targetposition = closestEnemyR.getX() + closestEnemyR.getY()*pgs.getWidth();
                    
                    UnitAction ua = pf.findPathToPositionInRange(u, targetposition, ranged.attackRange, gs, null);
                    if (ua != null && gs.isUnitActionAllowed(u, ua)){
                        pa.addUnitAction(u, ua);
                    }
                }
            }
// Ranged Behaviour END --------------------------------------------------------
// Light Behaviour START -------------------------------------------------------

            Unit closestEnemyL = null;
            int closestDistanceL = 0;
            if(u.getType() == light
                    && u.getPlayer() == p.getID()
                    && gs.getActionAssignment(u)==null
                    ){
                // find closest Enemy Unit and attack
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getPlayer() != u.getPlayer()
                            && !u2.getType().isResource){
                        int distance = 0;
                        distance = distanceFromStartToTarget(
                                u.getX(),
                                u.getY(),
                                u2.getX(),
                                u2.getY());
                        if (closestEnemyL == null
                                || distance < closestDistanceL){
                            closestEnemyL = u2;
                            closestDistanceL = distance;
                        }
                    }
                }
                // now we know which enemy Unit is closest and can go to it if 
                // necessary or attack it, if it is in range
                if (closestDistanceL <= light.attackRange) {
                    UnitAction ua = new UnitAction(
                            UnitAction.TYPE_ATTACK_LOCATION,
                            closestEnemyL.getX(),
                            closestEnemyL.getY());
                    pa.addUnitAction(u, ua);
                } else {
                    
                    int targetposition = 0; 
                    targetposition = closestEnemyL.getX() + closestEnemyL.getY()*pgs.getWidth();
                    
                    UnitAction ua = pf.findPathToAdjacentPosition(u, targetposition, gs, null);
                    
                    if (ua != null && gs.isUnitActionAllowed(u, ua)){
                        pa.addUnitAction(u, ua);
                    }
                }
            }

// Light Behaviour END --------------------------------------------------------
// Worker Behaviour START ------------------------------------------------------
            Unit closestResource = null;
            Unit closestBase = null;
            Unit closestEnemy = null;
            int closestDistance = 0;
            
            
            // build the barracks
            // Building barracks for weird L map
            if(u.getType() == worker
                    && u.getPlayer() == p.getID()
                    && gs.getActionAssignment(u) == null
                    && weirdLMap == true
                    && numberOfBarracks == 0
                    && p.getResources() > barracks.cost){
                
                UnitAction ua = new UnitAction(
                        UnitAction.TYPE_PRODUCE,
                        1,
                        barracks);
                
                pa.addUnitAction(u, ua);
                numberOfBarracks += 1;
            }
            // 3.1 Building Barracks for NoWhereToRun Map
            if(u.getType() == worker
                && rangedOnly == true
                && u.getY() == 2
                && u.getX() < 2
                && numberOfBarracks == 0
                && p.getResources() > barracks.cost){
                UnitAction ua = new UnitAction(
                            UnitAction.TYPE_PRODUCE,
                            2, 
                            barracks);
                if (gs.isUnitActionAllowed(u, ua)){
                    pa.addUnitAction(u, ua);
                    numberOfBarracks += 1;
                }
            }
            // 3.2 building barracks for BroodWarMap
            if(u.getType() == worker
                && bw == true
                && numberOfBarracks <= 1
                && u.getPlayer() == p.getID()
                && u.getY() <= 53
                && p.getResources() > barracks.cost){
                UnitAction ua = new UnitAction(
                            UnitAction.TYPE_PRODUCE,
                            0, 
                            barracks);
                if (gs.isUnitActionAllowed(u, ua)){
                    numberOfBarracks += 1;
                    pa.addUnitAction(u, ua);
                }
            }
            // 3.3 building barracks for doubleGame
            if(u.getType() == worker
                && doubleGame == true
                && numberOfBarracks <= 1
                && u.getPlayer() == p.getID()
                && u.getY() <= 3
                && p.getResources() > barracks.cost){
                UnitAction ua = new UnitAction(
                            UnitAction.TYPE_PRODUCE,
                            2, 
                            barracks);
                if (gs.isUnitActionAllowed(u, ua)){
                    numberOfBarracks += 1;
                    pa.addUnitAction(u, ua);
                }
            }
            
            if(u.getType() == worker
                    && standard == true
                    && p.getResources() > barracks.cost
                    && numberOfBarracks == 0
                    && u.getPlayer() == p.getID()
                    && twoBases == false
                    && gs.getActionAssignment(u) == null
                    && u.getY() >= 2){
                UnitAction ua = new UnitAction(
                            UnitAction.TYPE_PRODUCE,
                            2, 
                            barracks);
                
                if (gs.isUnitActionAllowed(u, ua)){
                    pa.addUnitAction(u, ua);
                    numberOfBarracks += 1;
                }
            }
            // 1. When enemy in neighbouring cell, attack that Enemy
            if(u.getType() == worker
                    && u.getPlayer() == p.getID()
                    && gs.getActionAssignment(u) == null){
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getPlayer() != p.getID()
                            && !u2.getType().isResource){
                        int distance = 0;
                        distance = distanceFromStartToTarget(
                                u.getX(),
                                u.getY(),
                                u2.getX(),
                                u2.getY());
                        if (closestEnemy == null 
                                || distance < closestDistance){
                            closestEnemy = u2;
                            closestDistance = distance;
                        }
                    }
                }
                if (closestDistance == 1) {
                    UnitAction ua = new UnitAction(
                            UnitAction.TYPE_ATTACK_LOCATION,
                            closestEnemy.getX(), 
                            closestEnemy.getY());
                    pa.addUnitAction(u, ua);
                }
                // 2. When the enemy is at distance of mapHeight/3, go to that
                // enemy and Attack it
                else if (closestDistance < mapHeight/3){
                    int direction = -1;
                    direction = walkFromStartToTarget(
                            u.getX(), 
                            u.getY(), 
                            closestEnemy.getX(), 
                            closestEnemy.getY(), 
                            gs);

                    if (checkNeighborCellEmpty(u.getX(), 
                            u.getY(), 
                            direction,
                            gs)) {
                        UnitAction ua = new UnitAction(
                                UnitAction.TYPE_MOVE, 
                                direction);
                        pa.addUnitAction(u, ua);
                    }
                }
            }
            
            // 4. Worker goes to closest Resource to harvest, when he does not 
            // have a resource already
            if(u.getType() == worker
                    && u.getPlayer() == p.getID()
                    && u.getResources() == 0
                    && gs.getActionAssignment(u) == null){
                
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getType().isResource) {
                        int distance = 0;
                        distance = distanceFromStartToTarget(
                                u.getX(),
                                u.getY(),
                                u2.getX(),
                                u2.getY());
                        if (closestResource == null 
                                || distance < closestDistance) {
                            closestResource = u2;
                            closestDistance = distance;
                        }
                    }
                }

                // if the worker is next to a resource, he will harvest
                if(closestDistance == 1) {   
                    int harvestDirection = -1;
                    harvestDirection = neighborDirection(
                            u.getX(),
                            u.getY(),
                            closestResource.getX(),
                            closestResource.getY());
                    UnitAction ua = new UnitAction(
                            UnitAction.TYPE_HARVEST, 
                            harvestDirection);
                    if (gs.isUnitActionAllowed(u, ua)){
                        pa.addUnitAction(u, ua);
                    }
                } else {
                    int targetposition = 0; 
                    targetposition = closestResource.getX() + closestResource.getY()*pgs.getWidth();
                    
                    UnitAction ua = pf.findPathToAdjacentPosition(u, targetposition, gs, null);
                    
                    if (ua != null && gs.isUnitActionAllowed(u, ua)){
                        pa.addUnitAction(u, ua);
                    }
                }
            // Worker goes to base, if he has a resource loaded
            }
            if(u.getType() == worker
                    && u.getPlayer() == p.getID()
                    && u.getResources() > 0
                    && gs.getActionAssignment(u)==null){
                
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getType().isStockpile 
                            && u2.getPlayer() == p.getID()) {
                        int distance = 0;
                        distance = distanceFromStartToTarget(
                                u.getX(), 
                                u.getY(), 
                                u2.getX(), 
                                u2.getY());
                        if (closestBase == null || distance < closestDistance){
                            closestBase = u2;
                            closestDistance = distance;
                        }
                    }
                }
                // if the loaded Worker is next to a base, he will return the 
                // resource to it
                if(closestDistance == 1 && numberOfBases != 0) {
                    int returnDirection = -1;
                    returnDirection = neighborDirection(
                            u.getX(),
                            u.getY(),
                            closestBase.getX(),
                            closestBase.getY());
                    UnitAction ua = new UnitAction(
                            UnitAction.TYPE_RETURN, 
                            returnDirection);
                    if (gs.isUnitActionAllowed(u, ua)){
                        pa.addUnitAction(u, ua);
                    }
                } else if (numberOfBases != 0){
                    
                    int targetposition = 0; 
                    targetposition = closestBase.getX() + closestBase.getY()*pgs.getWidth();
                    
                    UnitAction ua = pf.findPathToAdjacentPosition(u, targetposition, gs, null);
                    
                    if (ua != null && gs.isUnitActionAllowed(u, ua)){
                        pa.addUnitAction(u, ua);
                    }
                }
            }
            
            // Build a base if there are none and if there are enough resources
            else if(u.getType() == worker
                    && u.getPlayer() == p.getID()
                    && u.getResources() == 0
                    && gs.getActionAssignment(u) == null
                    && p.getResources() > base.cost
                    && numberOfBases == 0){
                int buildDirection = -1;
                buildDirection = freeNeighborCell(
                    u.getX(),
                    u.getY(),
                    gs);
                UnitAction ua = new UnitAction(
                UnitAction.TYPE_PRODUCE,
            buildDirection, 
            base);
                if (gs.isUnitActionAllowed(u, ua)){
                    pa.addUnitAction(u, ua);
                    numberOfBases += 1;
                }
            }
            
            if (u.getType() == worker
                    && u.getPlayer() == p.getID()
                    && gs.getActionAssignment(u) == null
                    && rush == true){
                Unit closestEnemyW = null;
                int closestDistanceW = 0;
                // find closest Enemy Unit and attack
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getPlayer() != u.getPlayer()
                            && !u2.getType().isResource){
                        int distance = 0;
                        distance = distanceFromStartToTarget(
                                u.getX(),
                                u.getY(),
                                u2.getX(),
                                u2.getY());
                        if (closestEnemyW == null
                                || distance < closestDistanceW){
                            closestEnemyW = u2;
                            closestDistanceW = distance;
                        }
                    }
                }
                // now we know which enemy Unit is closest and can go to it if 
                // necessary or attack it, if it is in range
                if (closestDistanceW <= worker.attackRange) {
                    UnitAction ua = new UnitAction(
                            UnitAction.TYPE_ATTACK_LOCATION,
                            closestEnemyW.getX(),
                            closestEnemyW.getY());
                    pa.addUnitAction(u, ua);
                } else {
                    
                    int targetposition = 0; 
                    targetposition = closestEnemyW.getX() + closestEnemyW.getY()*pgs.getWidth();
                    
                    UnitAction ua = pf.findPathToAdjacentPosition(u, targetposition, gs, null);
                    
                    if (ua != null && gs.isUnitActionAllowed(u, ua)){
                        pa.addUnitAction(u, ua);
                    }
                }
            }
            
            // Go and then attack closest enemy Unit, 
            // if he does not have any more moving Units
            // This is for the case that only one warker can win the game in 
            // the end if it was a close round
            if(u.getType() == worker
                    && u.getPlayer() == p.getID()
                    && u.getResources() == 0
                    && gs.getActionAssignment(u) == null
                    && sumOfEnemyMovingUnits == 0){
                Unit closestEnemyW = null;
                int closestDistanceW = 0;
                // find closest Enemy Unit and attack
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getPlayer() != u.getPlayer()
                            && !u2.getType().isResource){
                        int distance = 0;
                        distance = distanceFromStartToTarget(
                                u.getX(),
                                u.getY(),
                                u2.getX(),
                                u2.getY());
                        if (closestEnemyW == null
                                || distance < closestDistanceW){
                            closestEnemyW = u2;
                            closestDistanceW = distance;
                        }
                    }
                }
                // now we know which enemy Unit is closest and can go to it if 
                // necessary or attack it, if it is in range
                if (closestDistanceW <= worker.attackRange) {
                    UnitAction ua = new UnitAction(
                            UnitAction.TYPE_ATTACK_LOCATION,
                            closestEnemyW.getX(),
                            closestEnemyW.getY());
                    pa.addUnitAction(u, ua);
                } else {
                    
                    int targetposition = 0; 
                    targetposition = closestEnemyW.getX() + closestEnemyW.getY()*pgs.getWidth();
                    
                    UnitAction ua = pf.findPathToAdjacentPosition(u, targetposition, gs, null);
                    
                    if (ua != null && gs.isUnitActionAllowed(u, ua)){
                        pa.addUnitAction(u, ua);
                    }
                }
            }
        }
// Worker Behaviour End --------------------------------------------------------
        return pa;
    }
    
    public int smartBarracksPlacementRightAndBelowBase(
            int w_X,
            int w_Y,
            int b_X,
            int b_Y,
            GameState gs){
        
        if (w_X == b_X && w_Y-b_Y == 1) {
            return 1;
        } else if (w_X - b_X == 1 && w_Y == b_Y){
            return 2;
        } else {
            return -1;
        }
    }
    
    public int smartBarracksPlacementUnderBase(
            int w_X, 
            int w_Y, 
            int b_X, 
            int b_Y, 
            GameState gs){
        
        if (b_X - w_X == 1 && w_Y-b_Y == 1){
            return 1;
        } else if (w_X - b_X == 1 && w_Y-b_Y == 1) {
            return 3;
        } else {
            return -1;
        }
        
    }
    
    public static int randInt(
            int min, 
            int max) {
        Random random = new Random();
        int RandomInt = random.nextInt((max - min)+1)+min;
        
        return RandomInt;
    }
    
    public int walkFromStartToTarget(
            int start_X,
            int start_Y,
            int target_X,
            int target_Y, 
            GameState gs){
        
        /**Returns a Move Direction to go from a start to a Target Coordinate*/
        int walkingDirection = -1;
        
        int x_diff = Math.abs(start_X - target_X);
        int y_diff = Math.abs(start_Y - target_Y);
        
        
        if (x_diff > y_diff || y_diff == 0){
            if (start_X > target_X) {
                walkingDirection = 3;
            } else if (start_X < target_X) {
                walkingDirection = 1;
            }
        }
        if (x_diff < y_diff || x_diff == 0) {
            if (start_Y > target_Y) {
                walkingDirection = 0;
            } else if (start_Y < target_Y) {
                walkingDirection = 2;
            }
        }
        if (x_diff == y_diff){
            if (start_X < target_X){
                walkingDirection = 1;
            } else if (start_X > target_X) {
                walkingDirection = 3;
            } else if (start_Y < target_Y) {
                walkingDirection = 2;
            } else if (start_Y > target_Y) {
                walkingDirection = 0;
            }
        }
        return walkingDirection;
    }
    
    public int neighborDirection(
            int start_X, 
            int start_Y,
            int target_X,
            int target_Y) {
        
        // Returns the direction in which a unit should look for a neighbor 
        // object
        
        int neighborDirection = -1;
        if(start_Y == target_Y) {
            if(start_X < target_X){
                // Base is right of worker
                neighborDirection = 1;
            }
            else if(start_X > target_X){
                // Base is left of worker
                neighborDirection = 3;
            }
        }
        else if(start_X == target_X){
            if(start_Y < target_Y){
                // Base is below worker
                neighborDirection = 2;
            }
            else if(start_Y > target_Y){
                // Base above the Worker
                neighborDirection = 0;
            }
        } else {
            neighborDirection = -1;
        }
        return neighborDirection;
    }
    
    public int distanceFromStartToTarget(
            int start_X,
            int start_Y,
            int target_X,
            int target_Y) {
        return Math.abs(target_X - start_X) + Math.abs(target_Y - start_Y);
    }
    
    public int freeNeighborCell(
            int X, 
            int Y, 
            GameState gs) {
        
        ArrayList<Integer> freeNeighbors = new ArrayList();
        
        int X_left = X - 1;
        int X_right = X + 1;
        int Y_down = Y + 1;
        int Y_up = Y - 1;
        
            if (gs.free(X, Y_up)
                    && gs.getPhysicalGameState().getTerrain(X, Y_up) != PhysicalGameState.TERRAIN_WALL
                    && Y_up > 0){
                freeNeighbors.add(0);
            }
            if (gs.free(X, Y_down) 
                    && gs.getPhysicalGameState().getTerrain(X, Y_down) != PhysicalGameState.TERRAIN_WALL
                    && Y_down < gs.getPhysicalGameState().getHeight()-1){
                freeNeighbors.add(2);
            }
            if (gs.free(X_right, Y) 
                    && gs.getPhysicalGameState().getTerrain(X_right, Y) != PhysicalGameState.TERRAIN_WALL){
                freeNeighbors.add(1);
            }
            if (gs.free(X_left, Y)
                    && gs.getPhysicalGameState().getTerrain(X_left, Y) != PhysicalGameState.TERRAIN_WALL) {
                freeNeighbors.add(3);
            }
        
        if (freeNeighbors.isEmpty()){
            freeNeighbors.clear();    
            return -1;
        } else {
            int r = randInt(0, freeNeighbors.size()-1);
            int dir = freeNeighbors.get(r);
            freeNeighbors.clear();
        
            if(checkNeighborCellEmpty(X, Y, dir, gs)){
                return dir;
            }
        return -1;
        }
    }
    
    public boolean checkNeighborCellEmpty(
            int X, 
            int Y, 
            int dir, 
            GameState gs) {
        int X_left = X - 1;
        int X_right = X + 1;
        int Y_down = Y + 1;
        int Y_up = Y - 1;

        
        if (dir == 0 && !gs.free(X, Y_up) 
                || dir == 0 && gs.getPhysicalGameState().getTerrain(X, Y_up) == PhysicalGameState.TERRAIN_WALL){
            return false;
        }
        else if (dir == 2 && !gs.free(X, Y_down)
                || dir == 2 && gs.getPhysicalGameState().getTerrain(X, Y_down) == PhysicalGameState.TERRAIN_WALL){
            return false;
        }
        else if (dir == 1 && !gs.free(X_right, Y)
                || dir == 1 && gs.getPhysicalGameState().getTerrain(X_right, Y) == PhysicalGameState.TERRAIN_WALL) {
            return false;
        }
        else if (dir == 3 && !gs.free(X_left, Y)
                || dir == 3 && gs.getPhysicalGameState().getTerrain(X_left, Y) == PhysicalGameState.TERRAIN_WALL){
            return false;
        } else {
            return true;
        }
    }
    
    public UnitAction returnRandomMoveToFreeCell(
            Unit u, 
            GameState gs){
        int direction = -1;
        
        direction = freeNeighborCell(u.getX(), u.getY(), gs);
        
        UnitAction ua = new UnitAction(
                UnitAction.TYPE_MOVE, 
                direction);
        return ua;
    }
    
    public int towardsWhichCoordinate(
            int start_X,
            int start_Y,
            int target_X,
            int target_Y){
   
        int diff_X = Math.abs(start_X-target_X);
        int diff_Y = Math.abs(start_Y-target_Y);
        
        if (diff_X == 0){
            return target_X;
        } else if (diff_Y == 0){
            return target_Y;
        } else if (diff_X > diff_Y){
            return target_Y;
        } else if(diff_X < diff_Y){
            return target_X;
        }
        return target_X;
    }
    
    public int walkAwayFromTarget(
            int start_X,
            int start_Y,
            int target_X,
            int target_Y,
            GameState gs){
        
        /**Returns a Move Direction to go away from Target Coordinate*/
        int walkingDirection = -1;
        
        int x_diff = Math.abs(start_X - target_X);
        int y_diff = Math.abs(start_Y - target_Y);
        
        if (x_diff > y_diff || y_diff == 0){
            if (start_X > target_X) {
                walkingDirection = 1;
            } else if (start_X < target_X) {
                walkingDirection = 3;
            }
        }
        if (x_diff < y_diff || x_diff == 0) {
            if (start_Y > target_Y) {
                walkingDirection = 2;
            } else if (start_Y < target_Y) {
                walkingDirection = 0;
            }
        }
        if (x_diff == y_diff){
            if (start_X < target_X){
                walkingDirection = 3;
            } else if (start_X > target_X) {
                walkingDirection = 1;
            } else if (start_Y < target_Y) {
                walkingDirection = 0;
            } else if (start_Y > target_Y) {
                walkingDirection = 2;
            }
        }
        if (start_Y - 1 < 0){
            walkingDirection = -1;
        }
        return walkingDirection;
    }
    
    public AI clone() {
        return new SaveTheBeesV4(m_utt, pf);
    }
    
    @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new GreedyPathFinding()));

        return parameters;
    }
}