package com.example.a42567321.tp8;

import org.cocos2d.nodes.Sprite;

/**
 * Created by 42567321 on 3/10/2017.
 */

public class Enemy {
    private Sprite sprite;
    private int x, y;

    public Sprite getSprite() {
        return sprite;
    }
    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }
    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }
}
