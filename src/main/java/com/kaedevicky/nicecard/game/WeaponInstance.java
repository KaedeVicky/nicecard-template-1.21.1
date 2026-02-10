package com.kaedevicky.nicecard.game;

import net.minecraft.nbt.CompoundTag;

public class WeaponInstance {
    private int attack;
    private int durability;
    private String name; // 武器名字，或者你可以存 CardDefinition 的 ID

    public WeaponInstance(String name, int attack, int durability) {
        this.name = name;
        this.attack = attack;
        this.durability = durability;
    }

    // --- 核心逻辑 ---

    // 砍一刀 (减少耐久)
    // 返回 true 表示武器毁坏
    public boolean looseDurability() {
        this.durability--;
        return this.durability <= 0;
    }

    // --- NBT 序列化 ---

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putInt("Atk", attack);
        tag.putInt("Dur", durability);
        return tag;
    }

    public static WeaponInstance load(CompoundTag tag) {
        String name = tag.getString("Name");
        int atk = tag.getInt("Atk");
        int dur = tag.getInt("Dur");
        return new WeaponInstance(name, atk, dur);
    }

    // Getters
    public int getAttack() { return attack; }
    public int getDurability() { return durability; }
    public String getName() { return name; }
}