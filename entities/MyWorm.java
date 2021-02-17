package za.co.entelect.challenge.entities;

import com.google.gson.annotations.SerializedName;

import za.co.entelect.challenge.enums.Profession;

public class MyWorm extends Worm {
    @SerializedName("weapon")
    public Weapon weapon;

    @SerializedName("profession")
    public Profession profession;

    @SerializedName("bananaBombs")
    public Banana banana;

    @SerializedName("snowballs")
    public Snowball snowball;
}
