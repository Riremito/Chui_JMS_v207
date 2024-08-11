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
package tools.packet;

import java.util.Map;
import java.util.List;
import java.awt.Point;

import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;

import constants.GameConstants;
import handling.SendPacketOpcode;
import java.util.Collection;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.movement.ILifeMovementFragment;
import tools.MaplePacketCreator;
import connection.OutPacket;
import tools.Pair;

public class MobPacket {

    public static OutPacket damageMonster(final int oid, final long damage) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        outPacket.encodeInt(oid);
        outPacket.encodeByte(0);
        if (damage > Integer.MAX_VALUE) {
            outPacket.encodeInt(Integer.MAX_VALUE);
        } else {
            outPacket.encodeInt((int) damage);
        }

        return outPacket;
    }

    public static OutPacket damageFriendlyMob(final MapleMonster mob, final long damage, final boolean display) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeByte(display ? 1 : 2); //false for when shammos changes map!
        if (damage > Integer.MAX_VALUE) {
            outPacket.encodeInt(Integer.MAX_VALUE);
        } else { 
            outPacket.encodeInt((int) damage);
        }
        if (mob.getHp() > Integer.MAX_VALUE) {
            outPacket.encodeInt((int) (((double) mob.getHp() / mob.getMobMaxHp()) * Integer.MAX_VALUE));
        } else {
            outPacket.encodeInt((int) mob.getHp());
        }
        if (mob.getMobMaxHp() > Integer.MAX_VALUE) {
            outPacket.encodeInt(Integer.MAX_VALUE);
        } else {
            outPacket.encodeInt((int) mob.getMobMaxHp());
        }

        return outPacket;
    }

    public static OutPacket killMonster(final int oid, final int animation) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.KILL_MONSTER.getValue());
        outPacket.encodeInt(oid);
        outPacket.encodeByte(animation); // 0 = dissapear, 1 = fade out, 2+ = special
		if (animation == 4) {
			outPacket.encodeInt(-1);
		}

        return outPacket;
    }

    public static OutPacket suckMonster(final int oid, final int chr) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.KILL_MONSTER.getValue());
        outPacket.encodeInt(oid);
        outPacket.encodeByte(4);
        outPacket.encodeInt(chr);

        return outPacket;
    }

    public static OutPacket healMonster(final int oid, final int heal) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        outPacket.encodeInt(oid);
        outPacket.encodeByte(0);
        outPacket.encodeInt(-heal);

        return outPacket;
    }

    public static OutPacket showMonsterHP(int oid, int remhppercentage) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_MONSTER_HP.getValue());
        outPacket.encodeInt(oid);
        outPacket.encodeByte(remhppercentage);

        return outPacket;
    }

    public static OutPacket showBossHP(final MapleMonster mob) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BOSS_ENV.getValue());
        outPacket.encodeByte(5);
        outPacket.encodeInt(mob.getId() == 9400589 ? 9300184 : mob.getId()); //hack: MV cant have boss hp bar
        if (mob.getHp() > Integer.MAX_VALUE) {
            outPacket.encodeInt((int) (((double) mob.getHp() / mob.getMobMaxHp()) * Integer.MAX_VALUE));
        } else {
            outPacket.encodeInt((int) mob.getHp());
        }
        if (mob.getMobMaxHp() > Integer.MAX_VALUE) {
            outPacket.encodeInt(Integer.MAX_VALUE);
        } else {
            outPacket.encodeInt((int) mob.getMobMaxHp());
        }
        outPacket.encodeByte(mob.getStats().getTagColor());
        outPacket.encodeByte(mob.getStats().getTagBgColor());

        return outPacket;
    }

    public static OutPacket showBossHP(final int monsterId, final long currentHp, final long maxHp) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BOSS_ENV.getValue());
        outPacket.encodeByte(5);
	outPacket.encodeInt(monsterId); //has no image
        if (currentHp > Integer.MAX_VALUE) {
            outPacket.encodeInt((int) (((double) currentHp / maxHp) * Integer.MAX_VALUE));
        } else {
            outPacket.encodeInt((int) (currentHp <= 0 ? -1 : currentHp));
        }
        if (maxHp > Integer.MAX_VALUE) {
            outPacket.encodeInt(Integer.MAX_VALUE);
        } else {
            outPacket.encodeInt((int) maxHp);
        }
        outPacket.encodeByte(6);
        outPacket.encodeByte(5);

	//colour legend: (applies to both colours)
	//1 = red, 2 = dark blue, 3 = light green, 4 = dark green, 5 = black, 6 = light blue, 7 = purple

        return outPacket;
    }

    public static OutPacket moveMonster(boolean useskill, int skill, int unk, int oid, Point oldPos, Point oldVPos, List<ILifeMovementFragment> moves, final List<Integer> unk2, final List<Pair<Integer, Integer>> unk3) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MOVE_MONSTER.getValue());
        outPacket.encodeInt(oid);

        outPacket.encodeByte(useskill ? 1 : 0);
        outPacket.encodeByte(skill);

        outPacket.encodeInt(unk);
        outPacket.encodeByte(unk3 == null ? 0 : unk3.size()); // For each, 2 short
        if (unk3 != null) {
            for (Pair<Integer, Integer> i : unk3) {
                outPacket.encodeShort(i.left);
                outPacket.encodeShort(i.right);
            }
        }
        outPacket.encodeByte(unk2 == null ? 0 : unk2.size()); // For each, 1 short
        if (unk2 != null) {
            unk2.forEach(outPacket::encodeShort);
        }
        
        outPacket.encodePosition(oldPos);
        outPacket.encodePosition(oldVPos);
        PacketHelper.serializeMovementList(outPacket, moves);

        return outPacket;
    }
    
    public static OutPacket spawnMonster(MapleMonster life, int spawnType, int link) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_MONSTER.getValue());
        outPacket.encodeInt(life.getObjectId());
        outPacket.encodeByte(1); // 1 = Control normal, 5 = Control none
        outPacket.encodeInt(life.getId());
        addMonsterStatus(outPacket, life);
        outPacket.encodePosition(life.getTruePosition());
        outPacket.encodeByte(life.getStance());
        outPacket.encodeShort(0); // FH
        outPacket.encodeShort(life.getFh()); // Origin FH
        outPacket.encodeByte(spawnType);
        if (spawnType == -3 || spawnType >= 0) {
            outPacket.encodeInt(link);
        }
        outPacket.encodeByte(life.getCarnivalTeam());
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
//        outPacket.encodeByte(-1);

        return outPacket;
    }

    public static void addMonsterStatus(OutPacket outPacket, MapleMonster life) {
        if (life.getStati().size() <= 1) {
            life.addEmpty(); //not done yet lulz ok so we add it now for the lulz
        }
        outPacket.encodeByte(life.getChangedStats() != null ? 1 : 0);
        if (life.getChangedStats() != null) {
            outPacket.encodeInt(life.getChangedStats().hp > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)life.getChangedStats().hp);
            outPacket.encodeInt(life.getChangedStats().mp);
            outPacket.encodeInt(life.getChangedStats().exp);
            outPacket.encodeInt(life.getChangedStats().watk);
            outPacket.encodeInt(life.getChangedStats().matk);
            outPacket.encodeInt(life.getChangedStats().PDRate);
            outPacket.encodeInt(life.getChangedStats().MDRate);
            outPacket.encodeInt(life.getChangedStats().acc);
            outPacket.encodeInt(life.getChangedStats().eva);
            outPacket.encodeInt(life.getChangedStats().pushed);
            outPacket.encodeInt(life.getChangedStats().level);
        }
	    final boolean ignore_imm = life.getStati().containsKey(MonsterStatus.WEAPON_DAMAGE_REFLECT) || life.getStati().containsKey(MonsterStatus.MAGIC_DAMAGE_REFLECT);
	    Collection<MonsterStatusEffect> buffs = life.getStati().values();
        getLongMask_NoRef(outPacket, buffs, ignore_imm); //AFTERSHOCK: extra int
        for (MonsterStatusEffect buff : buffs) {
            if (buff != null && buff.getStati() != MonsterStatus.WEAPON_DAMAGE_REFLECT && buff.getStati() != MonsterStatus.MAGIC_DAMAGE_REFLECT && (!ignore_imm || (buff.getStati() != MonsterStatus.WEAPON_IMMUNITY && buff.getStati() != MonsterStatus.MAGIC_IMMUNITY && buff.getStati() != MonsterStatus.DAMAGE_IMMUNITY))) {
		        if (buff.getStati() != MonsterStatus.SUMMON && (buff.getStati() != MonsterStatus.EMPTY_2) && (buff.getStati() != MonsterStatus.EMPTY_3 || GameConstants.GMS)) {
                    if (buff.getStati() == MonsterStatus.EMPTY_1 || buff.getStati() == MonsterStatus.EMPTY_2 || buff.getStati() == MonsterStatus.EMPTY_3 || buff.getStati() == MonsterStatus.EMPTY_4 || buff.getStati() == MonsterStatus.EMPTY_5 || buff.getStati() == MonsterStatus.EMPTY_6) {
                        outPacket.encodeInt((int)System.currentTimeMillis()); //wtf
                    } else {
                        outPacket.encodeInt(buff.getX());
                    }
                    if (buff.getMobSkill() != null) {
                        outPacket.encodeShort(buff.getMobSkill().getSkillId());
                        outPacket.encodeShort(buff.getMobSkill().getSkillLevel());
                    } else if (buff.getSkill() > 0) {
                        outPacket.encodeInt(buff.getSkill());
                    }
		        }
                outPacket.encodeShort(buff.getStati() == MonsterStatus.HYPNOTIZE ? 40 : (buff.getStati().isEmpty() ? 0 : 1));
                if (buff.getStati() == MonsterStatus.EMPTY_1 || buff.getStati() == MonsterStatus.EMPTY_3) {
                    outPacket.encodeShort(0);
                } else if (buff.getStati() == MonsterStatus.EMPTY_4 || buff.getStati() == MonsterStatus.EMPTY_5) {
                    outPacket.encodeInt(0);
                }
            }
        }
        //wh spawn - 15 zeroes instead of 16, then 98 F4 56 A6 C7 C9 01 28, then 7 zeroes
        //8 -> wh 6, then timestamp, then 28 00, then 6 zeroes
    }

    public static OutPacket controlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        outPacket.encodeByte(aggro ? 2 : 1);
        outPacket.encodeInt(life.getObjectId());
        outPacket.encodeByte(1); // 1 = Control normal, 5 = Control none
        outPacket.encodeInt(life.getId());
        addMonsterStatus(outPacket, life);
        outPacket.encodePosition(life.getTruePosition());
        outPacket.encodeByte(life.getStance()); // Bitfield
        outPacket.encodeShort(0); // FH
        outPacket.encodeShort(life.getFh()); // Origin FH
        outPacket.encodeByte(life.isFake() ? -4 : newSpawn ? -2 : -1);
        outPacket.encodeByte(life.getCarnivalTeam());
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
//		outPacket.encodeByte(-1);
		
        return outPacket;
    }

    public static OutPacket stopControllingMonster(int oid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeInt(oid);

        return outPacket;
    }

    public static OutPacket makeMonsterReal(MapleMonster life) {
        return spawnMonster(life, -1, 0);
    }

    public static OutPacket makeMonsterFake(MapleMonster life) {
        return spawnMonster(life, -4, 0);
    }

    public static OutPacket makeMonsterEffect(MapleMonster life, int effect) {
        return spawnMonster(life, effect, 0);
    }

    public static OutPacket moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, int skillId, int skillLevel) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MOVE_MONSTER_RESPONSE.getValue());
        outPacket.encodeInt(objectid);
        outPacket.encodeShort(moveid);
        outPacket.encodeByte(useSkills ? 1 : 0);
        outPacket.encodeShort(currentMp);
        outPacket.encodeByte(skillId);
        outPacket.encodeByte(skillLevel);
	outPacket.encodeInt(0);

        return outPacket;
    }

    private static void getLongMask_NoRef(OutPacket outPacket, Collection<MonsterStatusEffect> ss, boolean ignore_imm) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (MonsterStatusEffect statup : ss) {
            if (statup != null && statup.getStati() != MonsterStatus.WEAPON_DAMAGE_REFLECT && statup.getStati() != MonsterStatus.MAGIC_DAMAGE_REFLECT && (!ignore_imm || (statup.getStati() != MonsterStatus.WEAPON_IMMUNITY && statup.getStati() != MonsterStatus.MAGIC_IMMUNITY && statup.getStati() != MonsterStatus.DAMAGE_IMMUNITY))) {
                mask[statup.getStati().getPosition() - 1] |= statup.getStati().getValue();
            }
        }
	for (int i = mask.length; i >= 1; i--) {
            outPacket.encodeInt(mask[i - 1]);
        }
    }

    public static OutPacket applyMonsterStatus(final int oid, final MonsterStatus mse, int x, MobSkill skil) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        outPacket.encodeInt(oid);
        PacketHelper.writeSingleMask(outPacket, mse);

	outPacket.encodeInt(x);
        outPacket.encodeShort(skil.getSkillId());
        outPacket.encodeShort(skil.getSkillLevel());
        outPacket.encodeShort(mse.isEmpty() ? 1 : 0); // might actually be the buffTime but it's not displayed anywhere
        outPacket.encodeShort(0); // delay in ms
        outPacket.encodeByte(1); // size
        outPacket.encodeByte(1); // ? v97

        return outPacket;
    }

    public static OutPacket applyMonsterStatus(final MapleMonster mons, final MonsterStatusEffect ms) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        outPacket.encodeInt(mons.getObjectId());
        //aftershock extra int here
        PacketHelper.writeSingleMask(outPacket, ms.getStati());
	outPacket.encodeInt(ms.getX());
        if (ms.isMonsterSkill()) {
            outPacket.encodeShort(ms.getMobSkill().getSkillId());
            outPacket.encodeShort(ms.getMobSkill().getSkillLevel());
        } else if (ms.getSkill() > 0) {
            outPacket.encodeInt(ms.getSkill());
        }
        outPacket.encodeShort(ms.getStati().isEmpty() ? 1 : 0); // might actually be the buffTime but it's not displayed anywhere
        outPacket.encodeShort(0); // delay in ms
        outPacket.encodeByte(1); // size
        outPacket.encodeByte(1); // ? v97

        return outPacket;
    }

    public static OutPacket applyMonsterStatus(final MapleMonster mons, final List<MonsterStatusEffect> mse) {
	if (mse.size() <= 0 || mse.get(0) == null) {
	    return MaplePacketCreator.enableActions();
	}
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        outPacket.encodeInt(mons.getObjectId());
        final MonsterStatusEffect ms = mse.get(0);
        if (ms.getStati() == MonsterStatus.POISON) { //stack ftw
	    PacketHelper.writeSingleMask(outPacket, MonsterStatus.EMPTY);
            outPacket.encodeByte(mse.size());
            for (MonsterStatusEffect m : mse) {
                outPacket.encodeInt(m.getFromID()); //character ID
                if (m.isMonsterSkill()) {
                    outPacket.encodeShort(m.getMobSkill().getSkillId());
                    outPacket.encodeShort(m.getMobSkill().getSkillLevel());
                } else if (m.getSkill() > 0) {
                    outPacket.encodeInt(m.getSkill());
                }
                outPacket.encodeInt(m.getX()); //dmg
                outPacket.encodeLong(1000); //delay -> tick count
                outPacket.encodeInt(5); //buff time ?
            }
            outPacket.encodeShort(300); // delay in ms
            outPacket.encodeByte(1); // size
	} else {
	    PacketHelper.writeSingleMask(outPacket, ms.getStati());
	    outPacket.encodeInt(ms.getX());
            if (ms.isMonsterSkill()) {
                outPacket.encodeShort(ms.getMobSkill().getSkillId());
                outPacket.encodeShort(ms.getMobSkill().getSkillLevel());
            } else if (ms.getSkill() > 0) {
                outPacket.encodeInt(ms.getSkill());
            }
            outPacket.encodeShort(0); // might actually be the buffTime but it's not displayed anywhere
            outPacket.encodeShort(0); // delay in ms
            outPacket.encodeByte(1); // size
            outPacket.encodeByte(1); // ? v97
        }


        return outPacket;
    }

    public static OutPacket applyMonsterStatus(final int oid, final Map<MonsterStatus, Integer> stati, final List<Integer> reflection, MobSkill skil) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        outPacket.encodeInt(oid);
        PacketHelper.writeMask(outPacket, stati.keySet());

        for (Map.Entry<MonsterStatus, Integer> mse : stati.entrySet()) {
	    outPacket.encodeInt(mse.getValue());
            outPacket.encodeShort(skil.getSkillId());
            outPacket.encodeShort(skil.getSkillLevel());
            outPacket.encodeShort(0); // might actually be the buffTime but it's not displayed anywhere
        }
        for (Integer ref : reflection) {
            outPacket.encodeInt(ref);
        }
        outPacket.encodeLong(0);
        outPacket.encodeShort(0); // delay in ms

        int size = stati.size(); // size
        if (reflection.size() > 0) {
            size /= 2; // This gives 2 buffs per reflection but it's really one buff
        }
        outPacket.encodeByte(size); // size
        outPacket.encodeByte(1); // ? v97

        return outPacket;
    }

    public static OutPacket cancelMonsterStatus(int oid, MonsterStatus stat) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        outPacket.encodeInt(oid);
        PacketHelper.writeSingleMask(outPacket, stat);
        outPacket.encodeByte(1); // reflector is 3~!??
        outPacket.encodeByte(2); // ? v97

        return outPacket;
    }
	
    public static OutPacket cancelPoison(int oid, MonsterStatusEffect m) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        outPacket.encodeInt(oid);
        PacketHelper.writeSingleMask(outPacket, MonsterStatus.EMPTY);
		outPacket.encodeInt(0);
		outPacket.encodeInt(1); //size probably
        outPacket.encodeInt(m.getFromID()); //character ID
        if (m.isMonsterSkill()) {
            outPacket.encodeShort(m.getMobSkill().getSkillId());
            outPacket.encodeShort(m.getMobSkill().getSkillLevel());
        } else if (m.getSkill() > 0) {
            outPacket.encodeInt(m.getSkill());
        }
        outPacket.encodeByte(3); // ? v97

        return outPacket;
    }

    public static OutPacket talkMonster(int oid, int itemId, String msg) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.TALK_MONSTER.getValue());
        outPacket.encodeInt(oid);
        outPacket.encodeInt(500); //?
        outPacket.encodeInt(itemId);
        outPacket.encodeByte(itemId <= 0 ? 0 : 1);
        outPacket.encodeByte(msg == null || msg.length() <= 0 ? 0 : 1);
        if (msg != null && msg.length() > 0) {
            outPacket.encodeString(msg);
        }
        outPacket.encodeInt(1); //?

        return outPacket;
    }

    public static OutPacket removeTalkMonster(int oid) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.REMOVE_TALK_MONSTER.getValue());
        outPacket.encodeInt(oid);
        
        return outPacket;
    }
}
