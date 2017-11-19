package com.example.a42567321.tp8;

import android.content.Context;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;

import com.github.czyzby.noise4j.map.Grid;
import com.github.czyzby.noise4j.map.generator.room.dungeon.DungeonGenerator;

import org.cocos2d.actions.interval.MoveBy;
import org.cocos2d.actions.interval.MoveTo;
import org.cocos2d.layers.Layer;
import org.cocos2d.nodes.Director;
import org.cocos2d.nodes.Label;
import org.cocos2d.nodes.Scene;
import org.cocos2d.nodes.Sprite;
import org.cocos2d.opengl.CCGLSurfaceView;
import org.cocos2d.types.CCColor3B;
import org.cocos2d.types.CCSize;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by ianfr on 14/09/2017.
 */

public class Game {
    CCGLSurfaceView view;
    CCSize screen;
    Sprite playButton, character, key, trapdoor, beat;
    Sprite upArrow, downArrow, leftArrow, rightArrow;
    Grid map;
    int currCharX, currCharY, mapCharX, mapCharY, keyX, keyY, trapX, trapY, points, level;
    Context context;
    MediaPlayer mediaPlayer;
    ArrayList<Sprite> floor, wall;
    ArrayList<Enemy> imps;
    float bpm = 92, bps;
    boolean canMove = false, alreadyMoved = false, keyWasObtained = false;
    Label offBeat = Label.label("Off beat!","Verdana",80f), k, lpoints, a;
    Layer gameScene, gui;

    public Game(CCGLSurfaceView view, Context context, int points, int level){
        this.view = view;
        this.context = context;
        this.points = points;
        this.level = level;
        mediaPlayer = MediaPlayer.create(context, R.raw.whyd_call_me);
    }

    public void start(){
        Director.sharedDirector().attachInView(view);
        screen = Director.sharedDirector().displaySize();
        Log.d("start", "Screen size: " + screen.getWidth() + " width and " + screen.getHeight() + " height");
        Director.sharedDirector().runWithScene(MainMenu());
    }

    public void lose(){
        gameScene.removeChild(lpoints, true);
        gameScene.removeChild(offBeat, true);
        gameScene.removeChild(beat, true);
        gameScene.unschedule("checkBeat");
        gui.removeChild(a, true);
        gui.removeChild(upArrow, true);
        gui.removeChild(downArrow, true);
        gui.removeChild(leftArrow, true);
        gui.removeChild(rightArrow, true);
        mediaPlayer.stop();

        Sprite tomb = Sprite.sprite("tombstone.png");
        tomb.scale(4f);
        tomb.setPosition(character.getPositionX(), character.getPositionY());
        gameScene.removeChild(character, true);
        gameScene.addChild(tomb, 10);
    }

    public void newLevel() {
        Director.sharedDirector().attachInView(view);
        screen = Director.sharedDirector().displaySize();
        Log.d("start", "Screen size: " + screen.getWidth() + " width and " + screen.getHeight() + " height");
        Director.sharedDirector().runWithScene(Level());
    }

    /*
            START MAIN MENU
     */
    private Scene MainMenu(){
        Scene scene = Scene.node();

        scene.addChild(new Background(), 0);
        scene.addChild(new Items(), 1);

        return scene;
    }

    class Background extends Layer{

    }

    class Items extends Layer{
        public Items(){
            this.setIsTouchEnabled(true);
            playButton = Sprite.sprite("button_play.png");
            playButton.setPosition(screen.width/ 2, screen.height / 2);
            playButton.setScale(4f);
            super.addChild(playButton);
        }

        @Override
        public boolean ccTouchesEnded(MotionEvent event) {
            if (event.getX() > playButton.getPositionX() - playButton.getWidth()*2f &&
                    event.getX() < playButton.getPositionX() + playButton.getWidth()*2f){
                if (event.getY() > playButton.getPositionY() - playButton.getHeight()*2f &&
                        event.getY() < playButton.getPositionY() + playButton.getHeight()*2f) {
                    Log.d("playButton", "Oof! Touched!");
                    Director.sharedDirector().runWithScene(Level());
                }
            }

            return true;
        }
    }
    /*
            END MAIN MENU
     */


    /*
            START LEVEL
     */
    private Scene Level(){
        Scene scene = Scene.node();

        bps = 60/bpm;

        gameScene = (GameScene)new GameScene();
        gui = (GUI)new GUI();

        scene.addChild(gameScene, 0);
        scene.addChild(gui, 1);

        offBeat.setPosition(screen.getWidth() /2,100);

        mediaPlayer.start();

        return scene;
    }

    class GameScene extends Layer{
        public GameScene(){
            createDungeon();
            //createEnemies();
            placeKey();
            placeTrapdoor();
            placeCharacter();

            lpoints = Label.label("Points: " + points, "Verdana", 30);
            lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
            super.addChild(lpoints, 4);

            Log.d("GameScene", "BPM: " + bpm);
            Log.d("GameScene", "BPS: " + bps);
            super.schedule("checkBeat", bps / 2 +0.009f);

        }

        private void createDungeon(){
            map = new Grid(30);

            DungeonGenerator dungeonGenerator = new DungeonGenerator();
            dungeonGenerator.setRoomGenerationAttempts(100);
            dungeonGenerator.setTolerance(5);
            dungeonGenerator.setMaxRoomSize(9);
            dungeonGenerator.setMinRoomSize(3);
            dungeonGenerator.generate(map);

            int floorCount = 0, wallCount = 0;

            for (int x = 0; x < map.getWidth(); x++){
                for (int y = 0; y < map.getHeight(); y++){
                    if (map.get(x, y) == 1.0){
                        floorCount++;
                    } else{
                        wallCount++;
                    }
                }
            }


            Sprite sprite;

            floor = new ArrayList<>();
            wall = new ArrayList<>();

            for (int i = 0; i <= floorCount; i++){
                sprite = Sprite.sprite("wall.png");
                sprite.setPosition(0,0);
                sprite.scale(4f);
                floor.add(sprite);
            }

            for (int i = 0; i <= wallCount; i++){
                sprite = Sprite.sprite("floor.png");
                sprite.setPosition(0,0);
                sprite.scale(4f);
                wall.add(sprite);
            }

            Log.d("createDungeon", "Counted " + floor.size() + " floor tiles and " +
                    wall.size() + " wall tiles on a " + map.getHeight() + "x" + map.getWidth() + " tile dungeon!");

            int floorIterator = 0, wallIterator = 0;

            for (int x = 0; x < map.getWidth(); x++){
                for (int y = 0; y < map.getHeight(); y++) {
                    if (map.get(x, y) == 1.0){
                        if(x > 0 && y > 0 && x < map.getWidth()-1 && y < map.getHeight()-1)
                        {
                            floor.get(floorIterator).setPosition
                                    (x * floor.get(floorIterator).getWidth() * 4f,
                                            y * floor.get(floorIterator).getHeight() * 4f);
                            super.addChild(floor.get(floorIterator), 0);
                        } else {
                            floor.get(floorIterator).setPosition
                                    (x * floor.get(floorIterator).getWidth() * 4f,
                                            y * floor.get(floorIterator).getHeight() * 4f);
                            super.addChild(floor.get(floorIterator), 0);
                        }
                        floorIterator++;
                    } else{
                        wall.get(wallIterator).setPosition
                                (x*wall.get(wallIterator).getWidth()*4f, y*wall.get(wallIterator).getHeight()*4f);
                        super.addChild(wall.get(wallIterator), 0);
                        wallIterator++;
                    }
                }
            }

            Log.d("createDungeon", "Finished printing!");
        }
        private void placeCharacter(){
            Random rng = new Random();
            int x, y;

            do{
                x = rng.nextInt(map.getWidth());
                y = rng.nextInt(map.getHeight());
            } while(map.get(x, y) == 1.0 || map.get(x+1, y) == 1.0 ||
                    map.get(x, y+1) == 1.0 || map.get(x-1, y) == 1.0 ||
                    map.get(x, y-1) == 1.0 || map.get(x+1, y+1) == 1.0 ||
                    map.get(x-1, y-1) == 1.0 || map.get(x+1, y-1) == 1.0 || map.get(x-1, y+1) == 1.0);

            mapCharX = x;
            mapCharY = y;

            character = Sprite.sprite("char_front.png");
            character.setPosition(x*character.getWidth()*4f, y*character.getHeight()*4f);
            character.scale(4f);

            currCharX = (int)(screen.getWidth() / (character.getWidth()*4f))/2;
            currCharY = (int)(screen.getHeight() / (character.getHeight()*4f))/2;

            character.runAction(MoveBy.action(0.0001f, (currCharX-x)*character.getWidth()*4f,
                    (currCharY-y)*character.getHeight()*4f));

            for (int i = 0; i < floor.size(); i++){
                floor.get(i).runAction(MoveBy.action(0.01f, (currCharX-x)*floor.get(i).getWidth()*4f,
                        (currCharY-y)*floor.get(i).getHeight()*4f));
            }
            for (int i = 0; i < wall.size(); i++){
                wall.get(i).runAction(MoveBy.action(0.01f, (currCharX-x)*wall.get(i).getWidth()*4f,
                        (currCharY-y)*wall.get(i).getHeight()*4f));
            }/*
            for (int i = 0; i < imps.size(); i++){
                imps.get(i).getSprite().runAction(MoveBy.action(0.01f, (currCharX-x)*imps.get(i).getSprite().getWidth()*4f,
                        (currCharY-y)*imps.get(i).getSprite().getHeight()*4f));
            }*/

            key.runAction(MoveBy.action(0.01f, (currCharX-x)*key.getWidth()*4f,
                    (currCharY-y)*key.getHeight()*4f));

            trapdoor.runAction(MoveBy.action(0.01f, (currCharX-x)*trapdoor.getWidth()*4f,
                    (currCharY-y)*trapdoor.getHeight()*4f));

            currCharY = y;
            currCharX = x;

            Log.d("placeCharacter", "Character placed at " + currCharX + ", " + currCharY + "(" + currCharX*character.getWidth()*4f + ", " + currCharY*character.getHeight()*4f+ ")");

            super.addChild(character, 2);
        }
        public void checkBeat(float n){
            if (canMove) {
                canMove = false;
                super.removeChild(beat, true);
                beat = Sprite.sprite("heart_small.png");
                beat.scale(3f);
                beat.setPosition(screen.getWidth()-beat.getWidth()*1.5f*3f,0+beat.getHeight()*3f);
                super.addChild(beat);

                if (alreadyMoved){
                    alreadyMoved = false;
                }
                moveEnemy();
            }
            else {
                canMove = true;
                super.removeChild(beat,true);
                beat = Sprite.sprite("heart_big.png");
                beat.scale(3f);
                beat.setPosition(screen.getWidth()-beat.getWidth()*1.5f*3f,0+beat.getHeight()*3f);
                super.addChild(beat);

                points--;
                super.removeChild(lpoints, true);
                lpoints = Label.label("Points: " + points, "Verdana", 30);
                lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                super.addChild(lpoints, 4);

                if (points < -500)
                    lose();
            }

        }
        private void createEnemies(){
            imps = new ArrayList<>();
            Random rng = new Random();
            int x, y, enemyAmmount = rng.nextInt(150);

            Log.d("createEnemies", "Game is about to create " + enemyAmmount + " enemies!");

            for (int n = 0; n < enemyAmmount; n++){
                do{
                    x = rng.nextInt(map.getWidth()-1);
                    y = rng.nextInt(map.getHeight()-1);
                } while(map.get(x, y) == 1.0 || map.get(x+1, y) == 1.0 ||
                        map.get(x, y+1) == 1.0 || map.get(x-1, y) == 1.0 ||
                        map.get(x, y-1) == 1.0 || map.get(x+1, y+1) == 1.0 ||
                        map.get(x-1, y-1) == 1.0 || map.get(x+1, y-1) == 1.0 || map.get(x-1, y+1) == 1.0);

                Enemy enemy = new Enemy();
                enemy.setSprite(Sprite.sprite("imp.png"));
                enemy.getSprite().setPosition(x*enemy.getSprite().getWidth()*4f,
                        y*enemy.getSprite().getHeight()*4f);
                enemy.getSprite().scale(4f);

                enemy.setX(x);
                enemy.setY(y);

                Log.d("createEnemies", "Setting enemy number " + n + " position at " + x +
                        ", " + y + " (" + x*enemy.getSprite().getWidth() + ", " + y*enemy.getSprite().getHeight() + ").");

                imps.add(enemy);
                super.addChild(enemy.getSprite(), 2);
            }
        }
        private void moveEnemy(){
            for (int i = 0; i < imps.size(); i++){
                float xDistance = imps.get(i).getSprite().getPositionX() - character.getPositionX();
                float yDistance = imps.get(i).getSprite().getPositionY() - character.getPositionY();
                boolean xNegative = false, yNegative = false;

                if (xDistance < 0) {
                    xDistance *= -1;
                    xNegative = true;
                }
                if (yDistance < 0) {
                    yDistance *= -1;
                    yNegative = true;
                }
                Log.d("moveEnemy","Enemy number " + i + " x position is " + imps.get(i).getX() + " and y position is " + imps.get(i).getY() + ".");
                Log.d("moveEnemy", "Difference between sprites in x is " + xDistance + " (negative " + xNegative + "), and difference in y is "+ yDistance + " (negative " + yNegative + ").");

                if (xDistance < yDistance){
                    //Must move on x axis
                    if (xNegative) {
                        //Must move right
                        Log.d("moveEnemy", "Map position " + (imps.get(i).getX() + 1) + ", " + imps.get(i).getY() + " contains " + map.get(imps.get(i).getX() + 1, imps.get(i).getY()));
                        if (map.get(imps.get(i).getX() + 1, imps.get(i).getY()) != 1.0) {
                            imps.get(i).getSprite().runAction(MoveBy.action
                                    (0.1f, imps.get(i).getSprite().getWidth() * 4f, 0));
                            imps.get(i).setX(imps.get(i).getX() + 1);
                            Log.d("moveEnemy", "Moved right! Enemy's current x is " + imps.get(i).getX());
                        } else {
                            Log.d("moveEnemy", "Enemy " + i + " did not move due to blocked path!");
                        }
                    }
                    else {
                        //Must move left
                        Log.d("moveEnemy","Map position " + (imps.get(i).getX()-1) + ", " + imps.get(i).getY() + " contains " + map.get(imps.get(i).getX()-1, imps.get(i).getY()));
                        if (map.get(imps.get(i).getX() - 1, imps.get(i).getY()) != 1.0) {
                            imps.get(i).getSprite().runAction(MoveBy.action
                                    (0.1f, imps.get(i).getSprite().getWidth() * 4f * -1, 0));
                            imps.get(i).setX(imps.get(i).getX() - 1);
                            Log.d("moveEnemy", "Moved left! Enemy's current x is " + imps.get(i).getX());
                        } else {
                            Log.d("moveEnemy", "Enemy " + i + " did not move due to blocked path!");
                        }
                    }
                } else{
                    //Must move on y axis
                    if (yNegative) {
                        //Must move up
                        Log.d("moveEnemy","Map position " + imps.get(i).getX() + ", " + (imps.get(i).getY()+1) + " contains " + map.get(imps.get(i).getX(), imps.get(i).getY()+1));
                        if (map.get(imps.get(i).getX(), imps.get(i).getY() + 1) != 1.0) {
                            imps.get(i).getSprite().runAction(MoveBy.action
                                    (0.1f, 0, imps.get(i).getSprite().getHeight() * 4f));
                            imps.get(i).setY(imps.get(i).getY() + 1);
                            Log.d("moveEnemy", "Moved up! Enemy's current y is " + imps.get(i).getY());
                        } else {
                            Log.d("moveEnemy", "Enemy " + i + " did not move due to blocked path!");
                        }
                    }
                    else
                        //Must move down
                        Log.d("moveEnemy","Map position " + imps.get(i).getX() + ", " + (imps.get(i).getY()-1) + " contains " + map.get(imps.get(i).getX(), imps.get(i).getY()-1));
                        if (map.get(imps.get(i).getX(), imps.get(i).getY()-1) != 1.0) {
                            imps.get(i).getSprite().runAction(MoveBy.action
                                    (0.1f, 0, imps.get(i).getSprite().getHeight() * 4f * -1));
                            imps.get(i).setY(imps.get(i).getY()-1);
                            Log.d("moveEnemy", "Moved down! Enemy's current y is " + imps.get(i).getY());
                        } else {
                            Log.d("moveEnemy", "Enemy " + i + " did not move due to blocked path!");
                        }
                }
            }
        }
        private void placeKey(){
            int x, y;
            Random rng = new Random();
            do{
                x = rng.nextInt(map.getWidth()-1);
                y = rng.nextInt(map.getHeight()-1);
            } while(map.get(x, y) == 1.0 || map.get(x+1, y) == 1.0 ||
                    map.get(x, y+1) == 1.0 || map.get(x-1, y) == 1.0 ||
                    map.get(x, y-1) == 1.0 || map.get(x+1, y+1) == 1.0 ||
                    map.get(x-1, y-1) == 1.0 || map.get(x+1, y-1) == 1.0 || map.get(x-1, y+1) == 1.0);

            keyX = x;
            keyY = y;
            key = Sprite.sprite("key.png");
            key.scale(4f);
            key.setPosition(x*key.getWidth()*4f,y*key.getHeight()*4f);
            Log.d("placeKey", "Key was placed at " + x + ", " + y + "(" + key.getPositionX() + ", " + key.getPositionY() + ")");
            super.addChild(key, 1);
        }
        private void placeTrapdoor(){
            int x, y;
            Random rng = new Random();
            do{
                x = rng.nextInt(map.getWidth()-1);
                y = rng.nextInt(map.getHeight()-1);
            } while(map.get(x, y) == 1.0 || map.get(x+1, y) == 1.0 ||
                    map.get(x, y+1) == 1.0 || map.get(x-1, y) == 1.0 ||
                    map.get(x, y-1) == 1.0 || map.get(x+1, y+1) == 1.0 ||
                    map.get(x-1, y-1) == 1.0 || map.get(x+1, y-1) == 1.0 || map.get(x-1, y+1) == 1.0);
            trapX = x;
            trapY = y;
            trapdoor = Sprite.sprite("trapdoor.png");
            trapdoor.scale(4f);
            trapdoor.setPosition(x*trapdoor.getWidth()*4f, y*trapdoor.getHeight()*4f);
            Log.d("placeTrapdoor", "Trapdoor was placed at " + x + ", " + y + "(" + trapdoor.getPositionX() + ", " + trapdoor.getPositionY() + ")");
            super.addChild(trapdoor, 1);
        }
    }

    class GUI extends Layer{
        public GUI(){
            this.setIsTouchEnabled(true);

            upArrow = Sprite.sprite("up.png");
            downArrow = Sprite.sprite("down.png");
            leftArrow = Sprite.sprite("left.png");
            rightArrow = Sprite.sprite("right.png");

            upArrow.setPosition(8*upArrow.getWidth(),12*upArrow.getWidth());
            upArrow.scale(3.5f);
            upArrow.setOpacity(65);
            downArrow.setPosition(8*upArrow.getWidth(),3*upArrow.getWidth());
            downArrow.scale(3.5f);
            downArrow.setOpacity(65);
            leftArrow.setPosition((3.3f*upArrow.getWidth()),(7.5f*upArrow.getWidth()));
            leftArrow.scale(3.5f);
            leftArrow.setOpacity(65);
            rightArrow.setPosition((12.5f*upArrow.getWidth()),(7.5f*upArrow.getWidth()));
            rightArrow.scale(3.5f);
            rightArrow.setOpacity(65);

            super.addChild(upArrow).addChild(downArrow).addChild(leftArrow).addChild(rightArrow);
        }

        @Override
        public boolean ccTouchesEnded(MotionEvent event) {
            super.removeChild(offBeat, true);
            if (event.getX() > upArrow.getPositionX()-upArrow.getWidth()*1.75f &&
                    event.getX() < upArrow.getPositionX()+upArrow.getWidth()*1.75f &&
                    event.getY() > screen.getHeight()-(upArrow.getPositionY()+upArrow.getHeight()*1.75f) &&
                    event.getY() < screen.getHeight()-(upArrow.getPositionY()-upArrow.getHeight()*1.75f)){
                Log.d("ccTouchesEnded", "Up button touched!");
                if (canMove) {
                    if (!alreadyMoved){
                        Log.d("placeCharMovement", "Char position " + mapCharX + ", " + mapCharY);
                        if (super.getChildren().contains(k)){
                            super.removeChild(k, true);
                        }
                        if (map.get(currCharX, currCharY + 1) != 1.0) {
                            if (mapCharX == keyX && mapCharY+1 == keyY){
                                a = Label.label("Key obtained! Head to exit!", "Verdana", 40f);
                                a.setPosition(screen.getWidth()/2, 50);
                                super.addChild(a);
                                keyWasObtained = true;
                                gameScene.removeChild(key, true);

                                points+=250;
                                gameScene.removeChild(lpoints, true);
                                lpoints = Label.label("Points: " + points, "Verdana", 30);
                                lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                                gameScene.addChild(lpoints, 4);
                            }

                            if (mapCharX == trapX && mapCharY+1 == trapY) {
                                if (keyWasObtained){
                                    points+=500;
                                    level++;
                                    gameScene.removeAllChildren(true);
                                    super.removeAllChildren(true);
                                    new Game(view, context, points, level).newLevel();
                                } else {
                                    k = Label.label("Key was not obtained!", "Verdana", 40f);
                                    k.setPosition(screen.getWidth()/2, screen.getHeight()/2);
                                    super.addChild(k);
                                }
                            }
                            currCharY++;
                            mapCharY++;

                            float x = character.getPositionX(), y = character.getPositionY();
                            gameScene.removeChild(character, true);
                            character = Sprite.sprite("char_back.png");
                            character.scale(4f);
                            character.setPosition(x, y);
                            gameScene.addChild(character, 2);
                            for (int i = 0; i < floor.size(); i++) {
                                floor.get(i).runAction(MoveBy.action(0.1f, 0, floor.get(i).getHeight() * 4f * -1));
                            }
                            for (int i = 0; i < wall.size(); i++) {
                                wall.get(i).runAction(MoveBy.action(0.1f, 0, wall.get(i).getHeight() * 4f * -1));
                            }
                            /*for (int i = 0; i < imps.size(); i++) {
                                imps.get(i).getSprite().runAction(MoveBy.action
                                        (0.1f, 0, imps.get(i).getSprite().getHeight() * 4f * -1));
                            }*/
                            key.runAction(MoveBy.action(0.1f, 0, key.getHeight() * 4f * -1));
                            trapdoor.runAction(MoveBy.action(0.1f, 0, trapdoor.getHeight() * 4f * -1));
                        }



                        alreadyMoved = true;
                    } else {
                        points-=2;
                        gameScene.removeChild(lpoints, true);
                        lpoints = Label.label("Points: " + points, "Verdana", 30);
                        lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                        gameScene.addChild(lpoints, 4);

                        if (points < -500)
                            lose();
                    }
                } else {
                    points-=2;
                    gameScene.removeChild(lpoints, true);
                    lpoints = Label.label("Points: " + points, "Verdana", 30);
                    lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                    gameScene.addChild(lpoints, 4);

                    if (points < -500)
                        lose();
                }
            } else if (event.getX() > rightArrow.getPositionX()-rightArrow.getWidth()*1.75f &&
                    event.getX() < rightArrow.getPositionX()+rightArrow.getWidth()*1.75f &&
                    event.getY() > screen.getHeight()-(rightArrow.getPositionY()+rightArrow.getHeight()*1.75f) &&
                    event.getY() < screen.getHeight()-(rightArrow.getPositionY()-rightArrow.getHeight()*1.75f)){
                Log.d("ccTouchesEnded", "Right button touched!");
                if (canMove) {
                    if (!alreadyMoved) {
                        if (super.getChildren().contains(k)){
                            super.removeChild(k, true);
                        }
                        if (map.get(currCharX + 1, currCharY) != 1.0) {
                            if (mapCharX+1 == keyX && mapCharY == keyY){
                                a = Label.label("Key obtained! Head to exit!", "Verdana", 40f);
                                a.setPosition(screen.getWidth()/2, 50);
                                super.addChild(a);
                                keyWasObtained = true;
                                gameScene.removeChild(key, true);

                                points+=250;
                                gameScene.removeChild(lpoints, true);
                                lpoints = Label.label("Points: " + points, "Verdana", 30);
                                lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                                gameScene.addChild(lpoints, 4);
                            }

                            if (mapCharX+1 == trapX && mapCharY == trapY) {
                                if (keyWasObtained){
                                    points+=500;
                                    level++;
                                    gameScene.removeAllChildren(true);
                                    super.removeAllChildren(true);
                                    new Game(view, context, points, level).newLevel();
                                } else {
                                    k = Label.label("Key was not obtained!", "Verdana", 40f);
                                    k.setPosition(screen.getWidth()/2, screen.getHeight()/2);
                                    super.addChild(k);
                                }
                            }

                            currCharX++;
                            mapCharX++;

                            float x = character.getPositionX(), y = character.getPositionY();
                            gameScene.removeChild(character, true);
                            character = Sprite.sprite("char_right.png");
                            character.scale(4f);
                            character.setPosition(x, y);
                            gameScene.addChild(character, 2);
                            for (int i = 0; i < floor.size(); i++) {
                                floor.get(i).runAction(MoveBy.action(0.1f, floor.get(i).getWidth() * 4f * -1, 0));
                            }
                            for (int i = 0; i < wall.size(); i++) {
                                wall.get(i).runAction(MoveBy.action(0.1f, wall.get(i).getWidth() * 4f * -1, 0));
                            }
                            /*for (int i = 0; i < imps.size(); i++) {
                                imps.get(i).getSprite().runAction(MoveBy.action
                                        (0.1f, imps.get(i).getSprite().getWidth() * 4f * -1, 0));
                            }*/
                            key.runAction(MoveBy.action(0.1f, key.getWidth() * 4f * -1, 0));
                            trapdoor.runAction(MoveBy.action(0.1f, trapdoor.getWidth() * 4f * -1, 0));
                        }
                        alreadyMoved = true;
                    } else {
                        points-=2;
                        gameScene.removeChild(lpoints, true);
                        lpoints = Label.label("Points: " + points, "Verdana", 30);
                        lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                        gameScene.addChild(lpoints, 4);

                        if (points < -500)
                            lose();
                    }
                } else{
                    points-=2;
                    gameScene.removeChild(lpoints, true);
                    lpoints = Label.label("Points: " + points, "Verdana", 30);
                    lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                    gameScene.addChild(lpoints, 4);

                    if (points < -500)
                        lose();
                }
            } else if (event.getX() > downArrow.getPositionX()-downArrow.getWidth()*1.75f &&
                    event.getX() < downArrow.getPositionX()+downArrow.getWidth()*1.75f &&
                    event.getY() > screen.getHeight()-(downArrow.getPositionY()+downArrow.getHeight()*1.75f) &&
                    event.getY() < screen.getHeight()-(downArrow.getPositionY()-downArrow.getHeight()*1.75f)){
                Log.d("ccTouchesEnded", "Down button touched!");
                if (canMove) {
                    if (!alreadyMoved) {
                        if (super.getChildren().contains(k)){
                            super.removeChild(k, true);
                        }
                        if (map.get(currCharX, currCharY - 1) != 1.0) {
                            if (mapCharX == keyX && mapCharY-1 == keyY){
                                a = Label.label("Key obtained! Head to exit!", "Verdana", 40f);
                                a.setPosition(screen.getWidth()/2, 50);
                                super.addChild(a);
                                keyWasObtained = true;
                                gameScene.removeChild(key, true);

                                points+=250;
                                gameScene.removeChild(lpoints, true);
                                lpoints = Label.label("Points: " + points, "Verdana", 30);
                                lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                                gameScene.addChild(lpoints, 4);
                            }
                            if (mapCharX == trapX && mapCharY-1 == trapY) {
                                if (keyWasObtained){
                                    points+=500;
                                    level++;
                                    gameScene.removeAllChildren(true);
                                    super.removeAllChildren(true);
                                    new Game(view, context, points, level).newLevel();
                                } else {
                                    k = Label.label("Key was not obtained!", "Verdana", 40f);
                                    k.setPosition(screen.getWidth()/2, screen.getHeight()/2);
                                    super.addChild(k);
                                }
                            }

                            currCharY--;
                            mapCharY--;


                            float x = character.getPositionX(), y = character.getPositionY();
                            gameScene.removeChild(character, true);
                            character = Sprite.sprite("char_front.png");
                            character.scale(4f);
                            character.setPosition(x, y);
                            gameScene.addChild(character, 2);
                            for (int i = 0; i < floor.size(); i++) {
                                floor.get(i).runAction(MoveBy.action(0.1f, 0, floor.get(i).getHeight() * 4f));
                            }
                            for (int i = 0; i < wall.size(); i++) {
                                wall.get(i).runAction(MoveBy.action(0.1f, 0, wall.get(i).getHeight() * 4f));
                            }
                            /*for (int i = 0; i < imps.size(); i++) {
                                imps.get(i).getSprite().runAction(MoveBy.action
                                        (0.1f, 0, imps.get(i).getSprite().getHeight() * 4f));
                            }*/
                            key.runAction(MoveBy.action(0.1f, 0, key.getHeight() * 4f));
                            trapdoor.runAction(MoveBy.action(0.1f, 0, trapdoor.getHeight() * 4f));
                        }
                        alreadyMoved = true;
                    } else{
                        points-=2;
                        gameScene.removeChild(lpoints, true);
                        lpoints = Label.label("Points: " + points, "Verdana", 30);
                        lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                        gameScene.addChild(lpoints, 4);

                        if (points < -500)
                            lose();
                    }
                } else{
                    points-=2;
                    gameScene.removeChild(lpoints, true);
                    lpoints = Label.label("Points: " + points, "Verdana", 30);
                    lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                    gameScene.addChild(lpoints, 4);

                    if (points < -500)
                        lose();
                }
            } else if (event.getX() > leftArrow.getPositionX()-leftArrow.getWidth()*1.75f &&
                    event.getX() < leftArrow.getPositionX()+leftArrow.getWidth()*1.75f &&
                    event.getY() > screen.getHeight()-(leftArrow.getPositionY()+leftArrow.getHeight()*1.75f) &&
                    event.getY() < screen.getHeight()-(leftArrow.getPositionY()-leftArrow.getHeight()*1.75f)){
                Log.d("ccTouchesEnded", "Left button touched!");
                if (canMove) {
                    if (!alreadyMoved) {
                        if (super.getChildren().contains(k)){
                            super.removeChild(k, true);
                        }
                        if (map.get(currCharX - 1, currCharY) != 1.0) {
                            if (mapCharX-1 == keyX && mapCharY == keyY){
                                a = Label.label("Key obtained! Head to exit!", "Verdana", 40f);
                                a.setPosition(screen.getWidth()/2, 50);
                                super.addChild(a);
                                keyWasObtained = true;
                                gameScene.removeChild(key, true);

                                points+=250;
                                gameScene.removeChild(lpoints, true);
                                lpoints = Label.label("Points: " + points, "Verdana", 30);
                                lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                                gameScene.addChild(lpoints, 4);
                            }

                            if (mapCharX-1 == trapX && mapCharY == trapY) {
                                if (keyWasObtained){
                                    points+=500;
                                    level++;
                                    gameScene.removeAllChildren(true);
                                    super.removeAllChildren(true);
                                    new Game(view, context, points, level).newLevel();
                                } else {
                                    k = Label.label("Key was not obtained!", "Verdana", 40f);
                                    k.setPosition(screen.getWidth()/2, screen.getHeight()/2);
                                    super.addChild(k);
                                }
                            }

                            currCharX--;
                            mapCharX--;

                            float x = character.getPositionX(), y = character.getPositionY();
                            gameScene.removeChild(character, true);
                            character = Sprite.sprite("char_left.png");
                            character.scale(4f);
                            character.setPosition(x, y);
                            gameScene.addChild(character, 2);

                            for (int i = 0; i < floor.size(); i++) {
                                floor.get(i).runAction(MoveBy.action(0.1f, floor.get(i).getWidth() * 4f, 0));
                            }
                            for (int i = 0; i < wall.size(); i++) {
                                wall.get(i).runAction(MoveBy.action(0.1f, wall.get(i).getWidth() * 4f, 0));
                            }
                            /*for (int i = 0; i < imps.size(); i++) {
                                imps.get(i).getSprite().runAction(MoveBy.action
                                        (0.1f, imps.get(i).getSprite().getWidth() * 4f, 0));
                            }*/
                            key.runAction(MoveBy.action(0.1f, key.getWidth()*4f, 0));
                            trapdoor.runAction(MoveBy.action(0.1f, trapdoor.getWidth()*4f, 0));
                        }

                        alreadyMoved = true;
                    } else {
                        points-=2;
                        gameScene.removeChild(lpoints, true);
                        lpoints = Label.label("Points: " + points, "Verdana", 30);
                        lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                        gameScene.addChild(lpoints, 4);

                        if (points < -500)
                            lose();
                    }
                } else{
                    points-=2;
                    gameScene.removeChild(lpoints, true);
                    lpoints = Label.label("Points: " + points, "Verdana", 30);
                    lpoints.setPosition((screen.getWidth()-lpoints.getWidth()), (screen.getHeight()-lpoints.getHeight()));
                    gameScene.addChild(lpoints, 4);

                    if (points < -500)
                        lose();
                }
            }
            ArrayList<Sprite>misSprites = new ArrayList<>();
            misSprites.add(key);

            return true;
        }
    }
    /*
            END LEVEL
     */
}
