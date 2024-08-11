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
package handling.channel.handler;

import java.awt.Point;
import java.util.List;

import client.MapleClient;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MonsterFamiliar;
import client.SkillFactory;
import client.SkillFactory.FamiliarEntry;
import client.anticheat.CheatingOffense;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import java.util.ArrayList;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.StructFamiliar;
import server.life.MapleLifeFactory;
import server.maps.MapleMap;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleNodes.MapleNodeInfo;
import server.movement.AbsoluteLifeMovement;
import server.movement.ILifeMovement;
import server.movement.ILifeMovementFragment;

import server.movement.MovementKind;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Triple;
import tools.packet.MobPacket;
import connection.InPacket;

public class MobHandler {

    public static final void MoveMonster(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return; //?
        }
        final int oid = inPacket.decodeInt();
        final MapleMonster monster = chr.getMap().getMonsterByOid(oid);

        if (monster == null) { // movin something which is not a monster
            return;
        }
        if (monster.getLinkCID() > 0) {
            return;
        }
        short moveId = inPacket.decodeShort();
        boolean useSkill = inPacket.decodeByte() > 0;
        byte skill = inPacket.decodeByte();
        int unk = inPacket.decodeInt();

        int realskill = 0;
        int level = 0;

        if (useSkill) {// && (skill == -1 || skill == 0)) {
            final byte size = monster.getNoSkills();
            boolean used = false;

            if (size > 0) {
                final Pair<Integer, Integer> skillToUse = monster.getSkills().get((byte) Randomizer.nextInt(size));
                realskill = skillToUse.getLeft();
                level = skillToUse.getRight();
                // Skill ID and Level
                final MobSkill mobSkill = MobSkillFactory.getMobSkill(realskill, level);

                if (mobSkill != null && !mobSkill.checkCurrentBuff(chr, monster)) {
                    final long now = System.currentTimeMillis();
                    final long ls = monster.getLastSkillUsed(realskill);

                    if (ls == 0 || (((now - ls) > mobSkill.getCoolTime()) && !mobSkill.onlyOnce())) {
                        monster.setLastSkillUsed(realskill, now, mobSkill.getCoolTime());

                        final int reqHp = (int) (((float) monster.getHp() / monster.getMobMaxHp()) * 100); // In case this monster have 2.1b and above HP
                        if (reqHp <= mobSkill.getHP()) {
                            used = true;
                            mobSkill.applyEffect(chr, monster, true);
                        }
                    }
                }
            }
            if (!used) {
                realskill = 0;
                level = 0;
            }
        }
        final List<Pair<Integer, Integer>> unk3 = new ArrayList<>();
        final int size1 = inPacket.decodeByte();
        for (int i = 0; i < size1; i++) {
            unk3.add(new Pair<>((int) inPacket.decodeShort(), (int) inPacket.decodeShort()));
        }
        final List<Integer> unk2 = new ArrayList<>();
        final int size2 = inPacket.decodeByte();
        for (int i = 0; i < size2; i++) {
            unk2.add((int) inPacket.decodeShort());
        }
        inPacket.skip(1);
        inPacket.skip(4); // sometimes 0, 1
        inPacket.skip(4); // CC DD FF 00  same for all mobs
        inPacket.skip(4); // CC DD FF 00  same for all mobs
        inPacket.skip(4); // 9D E1 87 48  same for all mobs
        inPacket.skip(1); // 1?
        
        final Point oldPos = inPacket.decodePosition();
        final Point oldVPos = inPacket.decodePosition();
        
        List<ILifeMovementFragment> res = null;
        try {
            res = MovementParse.parseMovement(inPacket, oldPos, MovementKind.MOB_MOVEMENT);
        } catch (ArrayIndexOutOfBoundsException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.Movement_Log, e);
            FileoutputUtil.log(FileoutputUtil.Movement_Log, "MOBID " + monster.getId() + ", AIOBE Type2:\n" + inPacket.toString());
            return;
        }

        if (res != null && chr != null && res.size() > 0) {
            final MapleMap map = chr.getMap();
/*            for (final LifeMovementFragment move : res) {
                if (move instanceof AbsoluteLifeMovement) {
                    final Point endPos = ((LifeMovement) move).getPosition();
                    if (endPos.x < (map.getLeft() - 250) || endPos.y < (map.getTop() - 250) || endPos.x > (map.getRight() + 250) || endPos.y > (map.getBottom() + 250)) { //experimental
                        chr.getCheatTracker().checkMoveMonster(endPos);
                        return;
                    }
                }
            }*/
            c.write(MobPacket.moveMonsterResponse(monster.getObjectId(), moveId, monster.getMp(), monster.isControllerHasAggro(), realskill, level));
/*            if (inPacket.getUnreadAmount() < 9 || inPacket.getUnreadAmount() > 33) { //9.. 0 -> endPos? -> endPos again? -> 0 -> 0
                //FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "slea.available != 25 (movement parsing error)\n" + slea.toString(true));
                return;
            }*/
            MovementParse.updatePosition(res, monster, -1);
            final Point endPos = monster.getTruePosition();
            map.moveMonster(monster, endPos);
            map.broadcastMessage(chr, MobPacket.moveMonster(useSkill, skill, unk, monster.getObjectId(), oldPos, oldVPos, res, unk2, unk3), endPos);
            chr.getCheatTracker().checkMoveMonster(endPos);
        }
    }

    public static final void FriendlyDamage(final InPacket inPacket, final MapleCharacter chr) {
        final MapleMap map = chr.getMap();
        if (map == null) {
            return;
        }
        final MapleMonster mobfrom = map.getMonsterByOid(inPacket.decodeInt());
        inPacket.skip(4); // Player ID
        final MapleMonster mobto = map.getMonsterByOid(inPacket.decodeInt());

        if (mobfrom != null && mobto != null && mobto.getStats().isFriendly()) {
            final int damage = (mobto.getStats().getLevel() * Randomizer.nextInt(mobto.getStats().getLevel())) / 2; // Temp for now until I figure out something more effective
            mobto.damage(chr, damage, true);
            checkShammos(chr, mobto, map);
        }
    }

    public static final void MobBomb(final InPacket inPacket, final MapleCharacter chr) {
        final MapleMap map = chr.getMap();
        if (map == null) {
            return;
        }
        final MapleMonster mobfrom = map.getMonsterByOid(inPacket.decodeInt());
        inPacket.skip(4); // something, 9E 07
        inPacket.decodeInt(); //-204?

        if (mobfrom != null && mobfrom.getBuff(MonsterStatus.MONSTER_BOMB) != null) {
            /* not sure
            12D -    0B 3D 42 00 EC 05 00 00 32 FF FF FF 00 00 00 00 00 00 00 00
            <monsterstatus done>
            108 - 07 0B 3D 42 00 EC 05 00 00 32 FF FF FF 01 00 00 00 7B 00 00 00
             */
        }
    }

    public static final void checkShammos(final MapleCharacter chr, final MapleMonster mobto, final MapleMap map) {
        if (!mobto.isAlive() && mobto.getStats().isEscort()) { //shammos
            for (MapleCharacter chrz : map.getCharactersThreadsafe()) { //check for 2022698
                if (chrz.getParty() != null && chrz.getParty().getLeader().getId() == chrz.getId()) {
                    //leader
                    if (chrz.haveItem(2022698)) {
                        MapleInventoryManipulator.removeById(chrz.getClient(), MapleInventoryType.USE, 2022698, 1, false, true);
                        mobto.heal((int) mobto.getMobMaxHp(), mobto.getMobMaxMp(), true);
                        return;
                    }
                    break;
                }
            }
            map.broadcastMessage(MaplePacketCreator.serverNotice(6, "Your party has failed to protect the monster."));
            final MapleMap mapp = chr.getMap().getForcedReturnMap();
            for (MapleCharacter chrz : map.getCharactersThreadsafe()) {
                chrz.changeMap(mapp, mapp.getPortal(0));
            }
        } else if (mobto.getStats().isEscort() && mobto.getEventInstance() != null) {
            mobto.getEventInstance().setProperty("HP", String.valueOf(mobto.getHp()));
        }
    }

    public static final void MonsterBomb(final int oid, final MapleCharacter chr) {
        final MapleMonster monster = chr.getMap().getMonsterByOid(oid);

        if (monster == null || !chr.isAlive() || chr.isHidden() || monster.getLinkCID() > 0) {
            return;
        }
        final byte selfd = monster.getStats().getSelfD();
        if (selfd != -1) {
            chr.getMap().killMonster(monster, chr, false, false, selfd);
        }
    }

    public static final void AutoAggro(final int monsteroid, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.isHidden()) { //no evidence :)
            return;
        }
        final MapleMonster monster = chr.getMap().getMonsterByOid(monsteroid);

        if (monster != null && chr.getTruePosition().distanceSq(monster.getTruePosition()) < 200000 && monster.getLinkCID() <= 0) {
            if (monster.getController() != null) {
                if (chr.getMap().getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(chr, true);
                } else {
                    monster.switchController(monster.getController(), true);
                }
            } else {
                monster.switchController(chr, true);
            }
        }
    }

    public static final void HypnotizeDmg(final InPacket inPacket, final MapleCharacter chr) {
        final MapleMonster mob_from = chr.getMap().getMonsterByOid(inPacket.decodeInt()); // From
        inPacket.skip(4); // Player ID
        final int to = inPacket.decodeInt(); // mobto
        inPacket.skip(1); // Same as player damage, -1 = bump, integer = skill ID
        final int damage = inPacket.decodeInt();
//	inPacket.skip(1); // Facing direction
//	inPacket.skip(4); // Some type of pos, damage display, I think

        final MapleMonster mob_to = chr.getMap().getMonsterByOid(to);

        if (mob_from != null && mob_to != null && mob_to.getStats().isFriendly()) { //temp for now
            if (damage > 30000) {
                return;
            }
            mob_to.damage(chr, damage, true);
            checkShammos(chr, mob_to, chr.getMap());
        }
    }

    public static final void DisplayNode(final InPacket inPacket, final MapleCharacter chr) {
        final MapleMonster mob_from = chr.getMap().getMonsterByOid(inPacket.decodeInt()); // From
        if (mob_from != null) {
            chr.getClient().write(MaplePacketCreator.getNodeProperties(mob_from, chr.getMap()));
        }
    }

    public static final void MobNode(final InPacket inPacket, final MapleCharacter chr) {
        final MapleMonster mob_from = chr.getMap().getMonsterByOid(inPacket.decodeInt()); // From
        final int newNode = inPacket.decodeInt();
        final int nodeSize = chr.getMap().getNodes().size();
        if (mob_from != null && nodeSize > 0) {
            final MapleNodeInfo mni = chr.getMap().getNode(newNode);
            if (mni == null) {
                return;
            }
            if (mni.attr == 2) { //talk
                switch (chr.getMapId() / 100) {
                    case 9211200:
                    case 9211201:
                    case 9211202:
                    case 9211203:
                    case 9211204:
                        chr.getMap().talkMonster("Please escort me carefully.", 5120035, mob_from.getObjectId()); //temporary for now. itemID is located in WZ file
                        break;
                    case 9320001:
                    case 9320002:
                    case 9320003:
                        chr.getMap().talkMonster("Please escort me carefully.", 5120051, mob_from.getObjectId()); //temporary for now. itemID is located in WZ file
                        break;
                }
            }
            mob_from.setLastNode(newNode);
            if (chr.getMap().isLastNode(newNode)) { //the last node on the map.
                switch (chr.getMapId() / 100) {
                    case 9211200:
                    case 9211201:
                    case 9211202:
                    case 9211203:
                    case 9211204:
                    case 9320001:
                    case 9320002:
                    case 9320003:
                        chr.getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, "Proceed to the next stage."));
                        chr.getMap().removeMonster(mob_from);
                        break;

                }
            }
        }
    }

    public static final void RenameFamiliar(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        MonsterFamiliar mf = c.getPlayer().getFamiliars().get(inPacket.decodeInt());
        String newName = inPacket.decodeString();
        if (mf != null && mf.getName().equals(mf.getOriginalName()) && MapleCharacterUtil.isEligibleCharName(newName, false)) {
            mf.setName(newName);
            //no packet... lol
        } else {
            chr.dropMessage(1, "Name was not eligible.");
        }
        c.write(MaplePacketCreator.enableActions());
    }

    public static final void SpawnFamiliar(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        c.getPlayer().updateTick(inPacket.decodeInt());
        final int mId = inPacket.decodeInt();
	c.write(MaplePacketCreator.enableActions());
        c.getPlayer().removeFamiliar();
        if (c.getPlayer().getFamiliars().containsKey(mId) && inPacket.decodeByte() > 0) {
            final MonsterFamiliar mf = c.getPlayer().getFamiliars().get(mId);
            if (mf.getFatigue() > 0) {
                c.getPlayer().dropMessage(1, "Please wait " + (mf.getFatigue()) + " seconds to summon it.");
            } else {
                c.getPlayer().spawnFamiliar(mf);
            }
        }
    }

    public static final void MoveFamiliar(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        inPacket.skip(5); //0, monster ID, pos, pos
        final Point oldPos = inPacket.decodePosition();
        final Point oldVPos = inPacket.decodePosition();
        final List<ILifeMovementFragment> res = MovementParse.parseMovement(inPacket, oldPos, MovementKind.FAMILIAR_MOVMENT);
        if (chr != null && chr.getSummonedFamiliar() != null && res.size() > 0) {
///            final Point pos = chr.getSummonedFamiliar().getPosition();
            MovementParse.updatePosition(res, chr.getSummonedFamiliar(), 0);
            chr.getSummonedFamiliar().updatePosition(res);
            if (!chr.isHidden()) {
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.moveFamiliar(chr.getId(), oldPos, oldVPos, res), chr.getTruePosition());
            }
        }
    }

    public static final void AttackFamiliar(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        if (chr.getSummonedFamiliar() == null) {
            return;
        }
        inPacket.skip(6); //byte 0 and monster id, then something we don't need
        final int skillid = inPacket.decodeInt();
        FamiliarEntry f = SkillFactory.getFamiliar(skillid);
        if (f == null) {
            return;
        }
        final byte unk = inPacket.decodeByte();
        final byte size = inPacket.decodeByte();
        final List<Triple<Integer, Integer, List<Integer>>> attackPair = new ArrayList<Triple<Integer, Integer, List<Integer>>>(size);
        for (int i = 0; i < size; i++) {
            final int oid = inPacket.decodeInt();
            final int type = inPacket.decodeInt();
            inPacket.skip(10);
            final byte si = inPacket.decodeByte();
            List<Integer> attack = new ArrayList<Integer>(si);
            for (int x = 0; x < si; x++) {
                attack.add(inPacket.decodeInt());
            }
            attackPair.add(new Triple<Integer, Integer, List<Integer>>(oid, type, attack));
        }
        if (attackPair.isEmpty() || !chr.getCheatTracker().checkFamiliarAttack(chr) || attackPair.size() > f.targetCount) {
            return;
        }
        final MapleMonsterStats oStats = chr.getSummonedFamiliar().getOriginalStats();
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.familiarAttack(chr.getId(), unk, attackPair), chr.getTruePosition());
        for (Triple<Integer, Integer, List<Integer>> attack : attackPair) {
            final MapleMonster mons = chr.getMap().getMonsterByOid(attack.left);
            if (mons == null || !mons.isAlive() || mons.getStats().isFriendly() || mons.getLinkCID() > 0 || attack.right.size() > f.attackCount) {
                continue;
            }
            if (chr.getTruePosition().distanceSq(mons.getTruePosition()) > 640000.0 || chr.getSummonedFamiliar().getTruePosition().distanceSq(mons.getTruePosition()) > GameConstants.getAttackRange(f.lt, f.rb)) {
                chr.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER_SUMMON);
            }
            for (int damage : attack.right) {
                if (damage <= (oStats.getPhysicalAttack() * 4)) { //approx.
                    mons.damage(chr, damage, true);
                }
            }
            if (f.makeChanceResult() && mons.isAlive()) {
                for (MonsterStatus s : f.status) {
                    mons.applyStatus(chr, new MonsterStatusEffect(s, (int) f.speed, MonsterStatusEffect.genericSkill(s), null, false), false, f.time * 1000, false, null);
                }
                if (f.knockback) {
                    mons.switchController(chr, true);
                }
            }
        }
        chr.getSummonedFamiliar().addFatigue(chr, attackPair.size());
    }

    public static final void TouchFamiliar(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        //probably where familiar goes upto mob to attack; no skill
        if (chr.getSummonedFamiliar() == null) {
            return;
        }
        inPacket.skip(6); //byte 0 and monster id, then something we don't need
        final byte unk = inPacket.decodeByte();

        final MapleMonster target = chr.getMap().getMonsterByOid(inPacket.decodeInt());
        if (target == null) {
            return;
        }
        final int type = inPacket.decodeInt(); //always 7?
        inPacket.skip(4);
        int damage = inPacket.decodeInt();
        final int maxDamage = (chr.getSummonedFamiliar().getOriginalStats().getPhysicalAttack() * 5);
        if (damage < maxDamage) {
            damage = maxDamage;
        }
        if (!target.getStats().isFriendly() && chr.getCheatTracker().checkFamiliarAttack(chr)) { //approx.
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.touchFamiliar(chr.getId(), unk, target.getObjectId(), type, 600, damage), chr.getTruePosition());
            target.damage(chr, damage, true);
            chr.getSummonedFamiliar().addFatigue(chr);
        }
    }

    public static final void UseFamiliar(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null || chr.hasBlockedInventory()) {
            c.write(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().updateTick(inPacket.decodeInt());
        final short slot = inPacket.decodeShort();
        final int itemId = inPacket.decodeInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        c.write(MaplePacketCreator.enableActions());
        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 10000 != 287) {
            return;
        }
        final StructFamiliar f = MapleItemInformationProvider.getInstance().getFamiliarByItem(itemId);
        if (MapleLifeFactory.getMonsterStats(f.mob).getLevel() <= c.getPlayer().getLevel()) {
            MonsterFamiliar mf = c.getPlayer().getFamiliars().get(f.familiar);
            if (mf != null) {
                if (mf.getVitality() >= 3) {
                    mf.setExpiry((long) Math.min(System.currentTimeMillis() + 90 * 24 * 60 * 60000L, mf.getExpiry() + 30 * 24 * 60 * 60000L));
                } else {
                    mf.setVitality(mf.getVitality() + 1);
                    mf.setExpiry((long) (mf.getExpiry() + 30 * 24 * 60 * 60000L));
                }
            } else {
                mf = new MonsterFamiliar(c.getPlayer().getId(), f.familiar, (long) (System.currentTimeMillis() + 30 * 24 * 60 * 60000L));
                c.getPlayer().getFamiliars().put(f.familiar, mf);
            }
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false, false);
            c.write(MaplePacketCreator.registerFamiliar(mf));
            return;
        }
    }
}