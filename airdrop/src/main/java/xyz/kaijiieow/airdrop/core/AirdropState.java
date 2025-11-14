package com.yourplugin.airdrop.core;

public enum AirdropState {
    LOCKED,             // เพิ่งเกิด ยังไม่มีคนปลดล็อก
    UNLOCKED_OWNED,     // มีเจ้าของแล้ว เปิดได้เฉพาะเจ้าของ
    COLLECTING,         // ช่วง Timer B 60 วิ (ให้เก็บของ)
    DESPAWNED           // ลบออกไปแล้ว / หมดอายุ
}
