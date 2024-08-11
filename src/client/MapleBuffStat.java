/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client;

import constants.GameConstants;
import handling.Buffstat;
import java.io.Serializable;

public enum MapleBuffStat implements Serializable, Buffstat {

    WATK(0x1, 1),
    WDEF(0x2, 1),
    MATK(0x4, 1),
    MDEF(0x8, 1),

    ACC(0x10, 1),
    AVOID(0x20, 1),
    HANDS(0x40, 1),
    SPEED(0x80, 1),

    JUMP(0x100, 1),
    MAGIC_GUARD(0x200, 1),
    DARKSIGHT(0x400, 1),
    BOOSTER(0x800, 1),

    POWERGUARD(0x1000, 1),
    MAXHP(0x2000, 1),
    MAXMP(0x4000, 1),
    INVINCIBLE(0x8000, 1),

    SOULARROW(0x10000, 1),
    //2 - debuff
    //4 - debuff
    //8 - debuff

    //1 - debuff
    COMBO(0x200000, 1),
    SUMMON(0x200000, 1), //hack buffstat for summons ^.- (does/should not increase damage... hopefully <3)
    WK_CHARGE(0x400000, 1),
    DRAGONBLOOD(0x800000, 1),

    HOLY_SYMBOL(0x1000000, 1),
    MESOUP(0x2000000, 1),
    SHADOWPARTNER(0x4000000, 1),
    PICKPOCKET(0x8000000, 1),
    PUPPET(0x8000000, 1), // HACK - shares buffmask with pickpocket - odin special ^.-

    MESOGUARD(0x10000000, 1),
    HP_LOSS_GUARD(0x20000000, 1),
    //4 - debuff
    //8 - debuff

    //1 - debuff
    MORPH(0x2, 2),
    RECOVERY(0x4, 2),
    MAPLE_WARRIOR(0x8, 2),

    STANCE(0x10, 2),
    SHARP_EYES(0x20, 2),
    MANA_REFLECTION(0x40, 2),
    //8 - debuff

    SPIRIT_CLAW(0x100, 2),
    INFINITY(0x200, 2),
    HOLY_SHIELD(0x400, 2), //advanced blessing after ascension
    HAMSTRING(0x800, 2),

    BLIND(0x1000, 2),
    CONCENTRATE(0x2000, 2),
    //4 - debuff
    ECHO_OF_HERO(0x8000, 2),

    MESO_RATE(0x10000, 2), //confirmed
    GHOST_MORPH(0x20000, 2),
    ARIANT_COSS_IMU(0x40000, 2), // The white ball around you
    //8 - debuff

    DROP_RATE(0x100000, 2), //confirmed
    //2 = unknown
    EXPRATE(0x400000, 2),
    ACASH_RATE(0x800000, 2),

    ILLUSION(0x1000000, 2), //hack buffstat
    //1 = unknown, and for gms, 2 and 4 are unknown
    BERSERK_FURY(0x2000000, 2),
    DIVINE_BODY(0x4000000, 2),
    SPARK(0x8000000, 2),

    ARIANT_COSS_IMU2(0x10000000, 2), // no idea, seems the same
    FINALATTACK(0x20000000, 2),
    //4 = unknown
    ELEMENT_RESET(0x80000000, 2),

    WIND_WALK(0x1, 3),
    //2
    ARAN_COMBO(0x4, 3),
    COMBO_DRAIN(0x8, 3),

    COMBO_BARRIER(0x10, 3),
    BODY_PRESSURE(0x20, 3),
    SMART_KNOCKBACK(0x40, 3),
    PYRAMID_PQ(0x80, 3),

    //1 - unknown
    //2 - debuff
    //4 - debuff
    //8 - debuff

    SLOW(0x1000, 3),
    MAGIC_SHIELD(0x2000, 3),
    MAGIC_RESISTANCE(0x4000, 3),
    SOUL_STONE(0x8000, 3),

    SOARING(0x10000, 3),
    //2 - debuff
    LIGHTNING_CHARGE(0x40000, 3),
    ENRAGE(0x80000, 3),

    OWL_SPIRIT(0x100000, 3),
    //2
    FINAL_CUT(0x400000, 3),
    DAMAGE_BUFF(0x800000, 3),

    ATTACK_BUFF(0x1000000, 3), //attack %? feline berserk
    RAINING_MINES(0x2000000, 3),
    ENHANCED_MAXHP(0x4000000, 3),
    ENHANCED_MAXMP(0x8000000, 3),

    ENHANCED_WATK(0x10000000, 3),
    //2 unknown
    ENHANCED_WDEF(0x40000000, 3),
    ENHANCED_MDEF(0x80000000, 3),

    PERFECT_ARMOR(0x1, 4),
    SATELLITESAFE_PROC(0x2, 4),
    SATELLITESAFE_ABSORB(0x4, 4),
    TORNADO(0x8, 4),

    CRITICAL_RATE_BUFF(0x10, 4),
    MP_BUFF(0x20, 4),
    DAMAGE_TAKEN_BUFF(0x40, 4),
    DODGE_CHANGE_BUFF(0x80, 4),

    CONVERSION(0x100, 4),
    REAPER(0x200, 4),
    INFILTRATE(0x400, 4),
    MECH_CHANGE(0x800, 4),

    AURA(0x1000, 4),
    DARK_AURA(0x2000, 4),
    BLUE_AURA(0x4000, 4),
    YELLOW_AURA(0x8000, 4),

    BODY_BOOST(0x10000, 4),
    FELINE_BERSERK(0x20000, 4),
    DICE_ROLL(0x40000, 4),
    DIVINE_SHIELD(0x80000, 4),

    PIRATES_REVENGE(0x100000, 4),
    TELEPORT_MASTERY(0x200000, 4),
    COMBAT_ORDERS(0x400000, 4),
    BEHOLDER(0x800000, 4),

    //1 = debuff
    GIANT_POTION(0x2000000, 4),
    ONYX_SHROUD(0x4000000, 4),
    ONYX_WILL(0x8000000, 4),

    //1 = debuff
    BLESS(0x20000000, 4),
    //4 //blue star + debuff
    //8 debuff	 but idk

    THREATEN_PVP(0x1, 5),
	ICE_KNIGHT(0x2, 5),
    //4 debuff idk.
    //8 unknown

    STR(0x10, 5),
    INT(0x20, 5),
    DEX(0x40, 5),
    LUK(0x80, 5),

    //1 unknown
    //2 unknown tornado debuff? - hp
    ANGEL_ATK(0x400, 5, true),
    ANGEL_MATK(0x800, 5, true),
	
    HP_BOOST(0x1000, 5, true), //indie hp
    MP_BOOST(0x2000, 5, true),
	ANGEL_ACC(0x4000, 5, true),
	ANGEL_AVOID(0x8000, 5, true),
	
    ANGEL_JUMP(0x10000, 5, true),
    ANGEL_SPEED(0x20000, 5, true),
    ANGEL_STAT(0x40000, 5, true),
	PVP_DAMAGE(0x80000, 5),
	
    PVP_ATTACK(0x100000, 5), //skills
	INVINCIBILITY(0x200000, 5),
	HIDDEN_POTENTIAL(0x400000, 5),
	ELEMENT_WEAKEN(0x800000, 5),

	SNATCH(0x1000000, 5), //however skillid is 90002000, 1500 duration
	FROZEN(0x2000000, 5),
	//4, unknown
	ICE_SKILL(0x8000000, 5),

    //1 - debuff
    BOUNDLESS_RAGE(0x20000000, 5),
    //4 unknown
    //8 = debuff

    HOLY_MAGIC_SHELL(0x1, 6), //max amount of attacks absorbed
	//2 unknown a debuff
    ARCANE_AIM(0x4, 6, true),
    BUFF_MASTERY(0x8, 6), //buff duration increase

    ABNORMAL_STATUS_R(0x10, 6), // %
    ELEMENTAL_STATUS_R(0x20, 6), // %
	WATER_SHIELD(0x40, 6),
    DARK_METAMORPHOSIS(0x80, 6), // mob count

    //1, unknown
	SPIRIT_SURGE(0x200, 6),
    SPIRIT_LINK(0x400, 6),
	//8 unknown

    VIRTUE_EFFECT(0x1000, 6),
	//2, 4, 8 unknown
	
    NO_SLIP(0x10000, 6),
    FAMILIAR_SHADOW(0x20000, 6),
    SIDEKICK_PASSIVE(0x40000, 6), //skillid 79797980

    //speshul
    ENERGY_CHARGE(0x2000000, 8),
    DASH_SPEED(0x4000000, 8),
    DASH_JUMP(0x8000000, 8),
    MONSTER_RIDING(0x10000000, 8),
    SPEED_INFUSION(0x20000000, 8),
    HOMING_BEACON(0x40000000, 8),
    DEFAULT_BUFFSTAT(0x80000000, 8), //end speshulness
    ;
    private static final long serialVersionUID = 0L;
    private final int buffstat;
    private final int first;
    private boolean stacked = false;

    private MapleBuffStat(int buffstat, int first) {
        this.buffstat = buffstat;
        this.first = first;
    }

    private MapleBuffStat(int buffstat, int first, boolean stacked) {
        this.buffstat = buffstat;
        this.first = first;
        this.stacked = stacked;
    }

    public final int getPosition() {
        return first;
    }

    public final int getValue() {
        return buffstat;
    }

    public final boolean canStack() {
        return stacked;
    }
}
