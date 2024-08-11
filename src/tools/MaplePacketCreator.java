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
package tools;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import client.inventory.MapleMount;
import client.BuddylistEntry;
import client.inventory.Item;
import constants.GameConstants;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import client.MapleKeyLayout;
import client.inventory.MaplePet;
import client.MapleQuestStatus;
import client.MapleStat;
import client.inventory.Equip.ScrollResult;
import client.MapleDisease;
import client.MapleTrait.MapleTraitType;
import client.MonsterFamiliar;
import client.inventory.MapleRing;
import client.SkillMacro;
import client.inventory.MapleAndroid;
import client.inventory.MapleImp;
import client.inventory.MapleImp.ImpFlag;
import connection.Packet;
import connection.OutPacket;
import handling.SendPacketOpcode;
import constants.ServerConstants;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import handling.channel.MapleGuildRanking.GuildRankingInfo;
import handling.channel.handler.InventoryHandler;
import handling.world.MapleSidekickCharacter;
import handling.world.World;
import handling.world.exped.MapleExpedition;
import handling.world.exped.PartySearch;
import handling.world.exped.PartySearchType;
import handling.world.guild.MapleBBSThread;
import handling.world.guild.MapleBBSThread.MapleBBSReply;
import handling.world.guild.MapleGuildAlliance;
import handling.world.guild.MapleGuildSkill;
import handling.world.sidekick.MapleSidekick;
import java.util.EnumMap;
import server.MapleStatEffect;
import server.MapleTrade;
import server.MapleDueyActions;
import server.MapleItemInformationProvider;
import server.MapleShop;
import server.Randomizer;
import server.StructFamiliar;
import server.maps.MapleSummon;
import server.life.MapleNPC;
import server.life.PlayerNPC;
import server.maps.MapleMap;
import server.maps.MapleReactor;
import server.maps.MapleMist;
import server.maps.MapleMapItem;
import server.events.MapleSnowball.MapleSnowballs;
import server.life.MapleMonster;
import server.maps.MapleDragon;
import server.maps.MapleNodes.MapleNodeInfo;
import server.maps.MapleNodes.MaplePlatform;
import server.maps.MechDoor;
import server.movement.ILifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.HiredMerchant;
import server.shops.MaplePlayerShopItem;
import tools.packet.PacketHelper;

public class MaplePacketCreator {

    public final static Map<MapleStat, Integer> EMPTY_STATUPDATE = new EnumMap<MapleStat, Integer>(MapleStat.class);
    public static int DEFAULT_BUFFMASK = 0; //???CONFIRM

    static {
        DEFAULT_BUFFMASK |= MapleBuffStat.ENERGY_CHARGE.getValue();
        DEFAULT_BUFFMASK |= MapleBuffStat.DASH_SPEED.getValue();
        DEFAULT_BUFFMASK |= MapleBuffStat.DASH_JUMP.getValue();
        DEFAULT_BUFFMASK |= MapleBuffStat.MONSTER_RIDING.getValue();
        DEFAULT_BUFFMASK |= MapleBuffStat.SPEED_INFUSION.getValue();
        DEFAULT_BUFFMASK |= MapleBuffStat.HOMING_BEACON.getValue();
        DEFAULT_BUFFMASK |= MapleBuffStat.DEFAULT_BUFFSTAT.getValue();
    }

    public static final OutPacket getServerIP(final MapleClient c, final int port, final int clientId) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SERVER_IP.getValue());
        outPacket.encodeShort(0);
        if (c.getTempIP().length() > 0) {
            for (String s : c.getTempIP().split(",")) {
                outPacket.encodeByte(Integer.parseInt(s));
            }
        } else {
            outPacket.encodeArr(ServerConstants.getIP());
        }
        outPacket.encodeShort(port);
        outPacket.encodeInt(clientId);
        outPacket.encodeByte(0/*GameConstants.GMS ? 0 : 1*/); //?  not sure
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static final OutPacket getChannelChange(final MapleClient c, final int port) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CHANGE_CHANNEL.getValue());
        outPacket.encodeByte(1);
/*        if (c.getTempIP().length() > 0) {
            for (String s : c.getTempIP().split(",")) {
                outPacket.encodeByte(Integer.parseInt(s));
            }
        } else {*/
            outPacket.encodeArr(ServerConstants.getIP());
//        }
        outPacket.encodeShort(port);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static final OutPacket getCharInfo(final MapleCharacter chr) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.WARP_TO_MAP.getValue());

        outPacket.encodeShort(2); // size
        outPacket.encodeInt(1);
        outPacket.encodeInt(0);
        outPacket.encodeInt(2);
        outPacket.encodeInt(0);

        outPacket.encodeInt(chr.getClient().getChannel() - 1);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);

        outPacket.encodeByte(1);
        outPacket.encodeInt(0);
        outPacket.encodeByte(1);
        outPacket.encodeShort(0);
        
        // 攻擊種子
        outPacket.encodeInt(Randomizer.nextInt());
        outPacket.encodeInt(Randomizer.nextInt());
        outPacket.encodeInt(Randomizer.nextInt());

        PacketHelper.addCharacterInfo(outPacket, chr);
        // 在functon裡面 lucky login out gift
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        for (int i = 0; i < 3; i++) {
            outPacket.encodeInt(0);
        }
        
        outPacket.encodeLong(PacketHelper.getTime(System.currentTimeMillis()));

        outPacket.encodeInt(100/*50*/); // 怪物屬性百分比
        outPacket.encodeByte(0);
        outPacket.encodeByte(1);
        //outPacket.encodeByte(GameConstants.isResist(chr.getJob()) ? 0 : 1);

        return outPacket;
    }

    public static final OutPacket enableActions() {
        return updatePlayerStats(EMPTY_STATUPDATE, true, 0);
    }

    public static final OutPacket updatePlayerStats(final Map<MapleStat, Integer> stats, final int evan) {
        return updatePlayerStats(stats, false, evan);
    }

    public static final OutPacket updatePlayerStats(final Map<MapleStat, Integer> mystats, final boolean itemReaction, final int evan) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_STATS.getValue());
        outPacket.encodeByte(itemReaction ? 1 : 0);
        long updateMask = 0;
        for (MapleStat statupdate : mystats.keySet()) {
            updateMask |= statupdate.getValue();
        }
        outPacket.encodeLong(updateMask);
        Long value;

        for (final Entry<MapleStat, Integer> statupdate : mystats.entrySet()) {
            value = statupdate.getKey().getValue();

            if (value >= 1) {
                if (value == MapleStat.SKIN.getValue()) {
                    outPacket.encodeShort(statupdate.getValue().shortValue());
                } else if (value <= MapleStat.HAIR.getValue()) {
                    outPacket.encodeInt(statupdate.getValue());
                } else if (value < MapleStat.JOB.getValue() || value == MapleStat.FATIGUE.getValue() || value == MapleStat.ICE_GAGE.getValue()) {
                    outPacket.encodeByte(statupdate.getValue().byteValue());
                } else if (value == MapleStat.AVAILABLESP.getValue()) { //availablesp
                    if (GameConstants.isEvan(evan) || GameConstants.isResist(evan) || GameConstants.isMercedes(evan)) {
                        throw new UnsupportedOperationException("Evan/Resistance/Mercedes wrong updating");
                    } else {
                        outPacket.encodeShort(statupdate.getValue().shortValue());
                    }
                } else if (value >= MapleStat.HP.getValue() && value <= MapleStat.MAXMP.getValue()) {
                    outPacket.encodeInt(statupdate.getValue().intValue());
                } else if (value < MapleStat.EXP.getValue()) {
                    outPacket.encodeShort(statupdate.getValue().shortValue()); //bb - hp/mp are ints
                } else if (value == MapleStat.TRAIT_LIMIT.getValue()) {
                    outPacket.encodeInt(statupdate.getValue().intValue()); //actually 6 shorts.
                    outPacket.encodeInt(statupdate.getValue().intValue());
                    outPacket.encodeInt(statupdate.getValue().intValue());
                } else if (value == MapleStat.PET.getValue()) {
                    outPacket.encodeLong(statupdate.getValue().intValue()); //uniqueID of 3 pets
                    outPacket.encodeLong(statupdate.getValue().intValue());
                    outPacket.encodeLong(statupdate.getValue().intValue());
                } else {
                    outPacket.encodeInt(statupdate.getValue().intValue());
                }
            }
        }
        if (updateMask == 0 && !itemReaction) {
            outPacket.encodeByte(1); //O_o
        }
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static final OutPacket updateSp(MapleCharacter chr, final boolean itemReaction) { //this will do..
        return updateSp(chr, itemReaction, false);
    }

    public static final OutPacket updateSp(MapleCharacter chr, final boolean itemReaction, final boolean overrideJob) { //this will do..
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_STATS.getValue());
        outPacket.encodeByte(itemReaction ? 1 : 0);
        outPacket.encodeLong(MapleStat.AVAILABLESP.getValue());// mask
        if (overrideJob || GameConstants.isEvan(chr.getJob()) || GameConstants.isResist(chr.getJob()) || GameConstants.isMercedes(chr.getJob())) {
            outPacket.encodeByte(chr.getRemainingSpSize());
            for (int i = 0; i < chr.getRemainingSps().length; i++) {
                if (chr.getRemainingSp(i) > 0) {
                    outPacket.encodeByte(i + 1);
                    outPacket.encodeByte(chr.getRemainingSp(i));
                }
            }
        } else {
            outPacket.encodeShort(chr.getRemainingSp());
        }
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        return outPacket;

    }

    public static final OutPacket getWarpToMap(final MapleMap to, final int spawnPoint, final MapleCharacter chr) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.WARP_TO_MAP.getValue());

        outPacket.encodeShort(2); // size -> long
        outPacket.encodeLong(1);
        outPacket.encodeLong(2);

        outPacket.encodeInt(chr.getClient().getChannel() - 1);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(1);
        outPacket.encodeInt(0);
        outPacket.encodeByte(0); // >0=getcharinfo
        outPacket.encodeShort(0);

        outPacket.encodeByte(0); // 10001075 女王の祈り
        outPacket.encodeInt(to.getId());
        outPacket.encodeByte(spawnPoint);
        outPacket.encodeInt(chr.getStat().getHp()); //bb - int
        outPacket.encodeLong(PacketHelper.getTime(System.currentTimeMillis()));
        outPacket.encodeInt(100/*50*/); // 怪物屬性百分比
        outPacket.encodeByte(0);
        outPacket.encodeByte(1/*GameConstants.isResist(chr.getJob()) ? 0 : 1*/);

        return outPacket;
    }

    public static final OutPacket instantMapWarp(final byte portal) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CURRENT_MAP_WARP.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeByte(portal); // 6

        return outPacket;
    }

    public static final OutPacket spawnPortal(final int townId, final int targetId, final int skillId, final Point pos) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_PORTAL.getValue());
        outPacket.encodeInt(townId);
        outPacket.encodeInt(targetId);
        if (townId != 999999999 && targetId != 999999999) {
            outPacket.encodeInt(skillId);
            outPacket.encodePosition(pos);
        }

        return outPacket;
    }

    public static final OutPacket spawnDoor(final int oid, final Point pos, final boolean animation) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_DOOR.getValue());
        outPacket.encodeByte(animation ? 0 : 1);
        outPacket.encodeInt(oid);
        outPacket.encodePosition(pos);

        return outPacket;
    }

    public static OutPacket removeDoor(int oid, boolean animation) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.REMOVE_DOOR.getValue());
        outPacket.encodeByte(animation ? 0 : 1);
        outPacket.encodeInt(oid);

        return outPacket;
    }

    public static OutPacket spawnSummon(MapleSummon summon, boolean animated) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_SUMMON.getValue());
        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        outPacket.encodeInt(summon.getSkill());
        outPacket.encodeByte(summon.getOwnerLevel() - 1);
        outPacket.encodeByte(summon.getSkillLevel());
        outPacket.encodePosition(summon.getPosition());
        outPacket.encodeByte(summon.getSkill() == 32111006 || summon.getSkill() == 33101005 ? 5 : 4); //reaper = 5?
        outPacket.encodeShort(0/*summon.getFh()*/);
        outPacket.encodeByte(summon.getMovementType().getValue());
        outPacket.encodeByte(summon.getSummonType()); // 0 = Summon can't attack - but puppets don't attack with 1 either ^.-
        outPacket.encodeByte(animated ? 1 : 0);
        outPacket.encodeByte(1); //no idea
        final MapleCharacter chr = summon.getOwner();
        outPacket.encodeByte(summon.getSkill() == 4341006 && chr != null ? 1 : 0); //mirror target
        if (summon.getSkill() == 4341006 && chr != null) {
            PacketHelper.addCharLook(outPacket, chr, true);
        }
        if (summon.getSkill() == 35111002) {
            outPacket.encodeByte(0);
        }

        return outPacket;
    }

    public static OutPacket removeSummon(int ownerId, int objId) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.REMOVE_SUMMON.getValue());
        outPacket.encodeInt(ownerId);
        outPacket.encodeInt(objId);
        outPacket.encodeByte(10);
        return outPacket;
    }

    public static OutPacket removeSummon(MapleSummon summon, boolean animated) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.REMOVE_SUMMON.getValue());
        outPacket.encodeInt(summon.getOwnerId());
        outPacket.encodeInt(summon.getObjectId());
        if (animated) {
            switch (summon.getSkill()) {
                case 35121003:
                    outPacket.encodeByte(10);
                    break;
                case 35111001:
                case 35111010:
                case 35111009:
                case 35111002:
                case 35111005:
                case 35111011:
                case 35121009:
                case 35121010:
                case 35121011:
                case 33101008:
                    outPacket.encodeByte(5);
                    break;
                default:
                    outPacket.encodeByte(4);
                    break;
            }
        } else {
            outPacket.encodeByte(1);
        }
        return outPacket;
    }

    /**
     * Possible values for <code>type</code>:<br>
     * 1: You cannot move that channel. Please try again later.<br>
     * 2: You cannot go into the cash shop. Please try again later.<br>
     * 3: The Item-Trading shop is currently unavailable, please try again later.<br>
     * 4: You cannot go into the trade shop, due to the limitation of user count.<br>
     * 5: You do not meet the minimum level requirement to access the Trade Shop.<br>
     *
     * @param type The type
     * @return The "block" packet.
     */
    public static OutPacket serverBlocked(int type) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SERVER_BLOCKED.getValue());
        outPacket.encodeByte(type);

        return outPacket;
    }
	
	//9 = cannot join due to party, 1 = cannot join at this time, sry
    public static OutPacket pvpBlocked(int type) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_BLOCKED.getValue());
        outPacket.encodeByte(type);

        return outPacket;
    }

    public static OutPacket serverMessage(String message) {
        return serverMessage(4, 0, message, false);
    }

    public static OutPacket serverNotice(int type, String message) {
        return serverMessage(type, 0, message, false);
    }

    public static OutPacket serverNotice(int type, int channel, String message) {
        return serverMessage(type, channel, message, false);
    }

    public static OutPacket serverNotice(int type, int channel, String message, boolean smegaEar) {
        return serverMessage(type, channel, message, smegaEar);
    }

    private static OutPacket serverMessage(int type, int channel, String message, boolean megaEar) {
        

        /*	* 0: [Notice]<br>
         * 1: Popup<br>
         * 2: Megaphone<br>
         * 3: Super Megaphone<br>
         * 4: Scrolling message at top<br>
         * 5: Pink Text<br>
         * 6: Lightblue Text
         * 8: Item megaphone
         * 9: Heart megaphone
         * 10: Skull Super megaphone
         * 11: Green megaphone message?
         * 12: Three line of megaphone text
         * 13: End of file =.="
         * 14: Ani msg
         * 15: Red Gachapon box
         * 18: Blue Notice (again)*/

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SERVERMESSAGE.getValue());
        outPacket.encodeByte(type);
        if (type == 4) {
            outPacket.encodeByte(1);
        }
        outPacket.encodeString(message);

        switch (type) {
            case 3:
            case 14:
            case 15:
                outPacket.encodeByte(channel - 1); // 頻道
                outPacket.encodeByte(megaEar ? 1 : 0);
                break;
            case 9:
            case 10:
                outPacket.encodeByte(0); // 伺服器ID
                outPacket.encodeByte(channel - 1); // 頻道
                outPacket.encodeByte(megaEar ? 1 : 0);
                break;
            case 6:
            case 18:
                outPacket.encodeInt(channel >= 1000000 && channel < 6000000 ? channel : 0); //cash itemID, displayed in yellow by the {name}
                //E.G. All new EXP coupon {Ruby EXP Coupon} is now available in the Cash Shop!
                //with Ruby Exp Coupon being in yellow and with item info
                break;
        }
        return outPacket;
    }

    public static OutPacket getGachaponMega(final String name, final String message, final Item item, final byte rareness) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SERVERMESSAGE.getValue());
        outPacket.encodeByte(15);
        outPacket.encodeString(name + message);
        outPacket.encodeInt(0); // 0~3 i think
        outPacket.encodeString(name);
        PacketHelper.addItemInfo(outPacket, item, true, true);

        return outPacket;
    }

    public static OutPacket getAniMsg(final int questID, final int time) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SERVERMESSAGE.getValue());
        outPacket.encodeByte(14);
        outPacket.encodeShort(questID);
        outPacket.encodeInt(time);

        return outPacket;
    }

    public static OutPacket tripleSmega(List<String> message, boolean ear, int channel) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SERVERMESSAGE.getValue());
        outPacket.encodeByte(12);

        if (message.get(0) != null) {
            outPacket.encodeString(message.get(0));
        }
        outPacket.encodeByte(message.size());
        for (int i = 1; i < message.size(); i++) {
            if (message.get(i) != null) {
                outPacket.encodeString(message.get(i));
            }
        }
        outPacket.encodeByte(channel - 1);
        outPacket.encodeByte(ear ? 1 : 0);

        return outPacket;
    }

    public static OutPacket getAvatarMega(MapleCharacter chr, int channel, int itemId, String message, boolean ear) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.AVATAR_MEGA.getValue());
        outPacket.encodeInt(itemId);
        outPacket.encodeString(chr.getName());
        outPacket.encodeString(message);
        outPacket.encodeInt(channel - 1); // channel
        outPacket.encodeByte(ear ? 1 : 0);
        PacketHelper.addCharLook(outPacket, chr, true);

        return outPacket;
    }

    public static OutPacket itemMegaphone(String msg, boolean whisper, int channel, Item item) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SERVERMESSAGE.getValue());
        outPacket.encodeByte(8);
        outPacket.encodeString(msg);
        outPacket.encodeByte(channel - 1);
        outPacket.encodeByte(whisper ? 1 : 0);

        if (item == null) {
            outPacket.encodeByte(0);
        } else {
            PacketHelper.addItemInfo(outPacket, item, false, false, true);
        }
        return outPacket;
    }

    public static OutPacket echoMegaphone(String name, String message) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ECHO_MESSAGE.getValue());
        outPacket.encodeByte(0); //1 = Your echo message has been successfully sent
        outPacket.encodeLong(PacketHelper.getTime(System.currentTimeMillis()));
        outPacket.encodeString(name); //name
        outPacket.encodeString(message); //message

        return outPacket;
    }

    public static OutPacket spawnNPC(MapleNPC life, boolean show) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_NPC.getValue());
        outPacket.encodeInt(life.getObjectId());
        outPacket.encodeInt(life.getId());
        outPacket.encodeShort(life.getPosition().x);
        outPacket.encodeShort(life.getCy());
        outPacket.encodeByte(life.getF() == 1 ? 0 : 1);
        outPacket.encodeShort(life.getFh());
        outPacket.encodeShort(life.getRx0());
        outPacket.encodeShort(life.getRx1());
        outPacket.encodeByte(show ? 1 : 0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket removeNPC(final int objectid) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.REMOVE_NPC.getValue());
        outPacket.encodeInt(objectid);

        return outPacket;
    }

    public static OutPacket removeNPCController(final int objectid) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeInt(objectid);

        return outPacket;
    }

    public static OutPacket spawnNPCRequestController(MapleNPC life, boolean MiniMap) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeInt(life.getObjectId());
        outPacket.encodeInt(life.getId());
        outPacket.encodeShort(life.getPosition().x);
        outPacket.encodeShort(life.getCy());
        outPacket.encodeByte(life.getF() == 1 ? 0 : 1);
        outPacket.encodeShort(life.getFh());
        outPacket.encodeShort(life.getRx0());
        outPacket.encodeShort(life.getRx1());
        outPacket.encodeByte(MiniMap ? 1 : 0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket spawnPlayerNPC(PlayerNPC npc) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.OnNpcImitateData.getValue());
        outPacket.encodeByte(npc.getF() == 1 ? 0 : 1);
        outPacket.encodeInt(npc.getId());
        outPacket.encodeString(npc.getName());
        outPacket.encodeByte(npc.getGender());
        outPacket.encodeByte(npc.getSkin());
        outPacket.encodeInt(npc.getFace());
        outPacket.encodeInt(0); //job lol
        outPacket.encodeByte(0);
        outPacket.encodeInt(npc.getHair());
        Map<Byte, Integer> equip = npc.getEquips();
        Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
        Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
        for (Entry<Byte, Integer> position : equip.entrySet()) {
            byte pos = (byte) (position.getKey() * -1);
            if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, position.getValue());
            } else if (pos > 100 && pos != 111) { // don't ask. o.o
                pos = (byte) (pos - 100);
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, position.getValue());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, position.getValue());
            }
        }
        for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
            outPacket.encodeByte(entry.getKey());
            outPacket.encodeInt(entry.getValue());
        }
        outPacket.encodeByte(0xFF);
        for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            outPacket.encodeByte(entry.getKey());
            outPacket.encodeInt(entry.getValue());
        }
        outPacket.encodeByte(0xFF);
        Integer cWeapon = equip.get((byte) -111);
        if (cWeapon != null) {
            outPacket.encodeInt(cWeapon);
        } else {
            outPacket.encodeInt(0);
        }
        for (int i = 0; i < 3; i++) {
            outPacket.encodeInt(npc.getPet(i));
        }

        return outPacket;
    }

    public static OutPacket getChatText(int cidfrom, String text, boolean whiteBG, int show) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CHATTEXT.getValue());
        outPacket.encodeInt(cidfrom);
        outPacket.encodeByte(whiteBG ? 1 : 0);
        outPacket.encodeString(text);
        outPacket.encodeByte(show);

        outPacket.encodeByte(0); // 大於0的話對話框會變成新年的
        outPacket.encodeString(""); // 不知道

        return outPacket;
    }

    public static OutPacket GameMaster_Func(int value) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GM_EFFECT.getValue());
        outPacket.encodeByte(value);
        outPacket.encodeZeroBytes(17);

        return outPacket;
    }

    public static OutPacket testCombo(int value) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ARAN_COMBO.getValue());
        outPacket.encodeInt(value);

        return outPacket;
    }

    public static OutPacket rechargeCombo(int value) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ARAN_COMBO_RECHARGE.getValue());
        outPacket.encodeInt(value);

        return outPacket;
    }

    public static OutPacket getPacketFromHexString(String hex) {
        return new OutPacket(HexTool.getByteArrayFromHexString(hex));
    }

    public static final OutPacket GainEXP_Monster(final int gain, final boolean white, final int partyinc, final int Class_Bonus_EXP, final int Equipment_Bonus_EXP, final int Premium_Bonus_EXP, final int Sidekick_Bonus_EXP) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(3); // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints
        outPacket.encodeByte(white ? 1 : 0);
        outPacket.encodeInt(gain);
        outPacket.encodeByte(0); // Not in chat
        outPacket.encodeZeroBytes(3); //i think
        outPacket.encodeInt(0); // Event Bonus
        outPacket.encodeInt(0); //wedding bonus
        outPacket.encodeInt(0); //party ring bonus
        outPacket.encodeInt(partyinc); // Party size
        outPacket.encodeInt(Equipment_Bonus_EXP); //Equipment Bonus EXP
        outPacket.encodeInt(Premium_Bonus_EXP); // Premium bonus EXP
        outPacket.encodeInt(0); //Rainbow Week Bonus EXP
        outPacket.encodeInt(Class_Bonus_EXP); // Class bonus EXP
	    if (GameConstants.GMS) {
	        outPacket.encodeInt(0); //mercedes skill exp
//	        outPacket.encodeInt(Sidekick_Bonus_EXP);
	    }
        outPacket.encodeInt(0); //summer week
	    outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(2); //?

        return outPacket;
    }

    public static final OutPacket GainEXP_Others(final int gain, final boolean inChat, final boolean white) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(3); // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints
        outPacket.encodeByte(white ? 1 : 0);
        outPacket.encodeInt(gain);
        outPacket.encodeByte(inChat ? 1 : 0);
        outPacket.encodeZeroBytes(48);
        if (inChat) {
            outPacket.encodeZeroBytes(6);
        }

        return outPacket;
    }

    public static final OutPacket getShowFameGain(final int gain) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(5);
        outPacket.encodeInt(gain);

        return outPacket;
    }

    public static final OutPacket showMesoGain(final int gain, final boolean inChat) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        if (!inChat) {
            outPacket.encodeByte(0);
            outPacket.encodeByte(1);
            outPacket.encodeByte(0);
            outPacket.encodeInt(gain);
            outPacket.encodeShort(0); // inet cafe meso gain ?.o
        } else {
            outPacket.encodeByte(6);
            outPacket.encodeInt(gain);
            outPacket.encodeInt(-1);
        }

        return outPacket;
    }

    public static OutPacket getShowItemGain(int itemId, short quantity) {
        return getShowItemGain(itemId, quantity, false);
    }

    public static OutPacket getShowItemGain(int itemId, short quantity, boolean inChat) {
        OutPacket outPacket;

        if (inChat) {
            outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
            outPacket.encodeByte(5);
            outPacket.encodeByte(1); // item count
            outPacket.encodeInt(itemId);
            outPacket.encodeInt(quantity);
            /*	    for (int i = 0; i < count; i++) { // if ItemCount is handled.
            outPacket.encodeInt(itemId);
            outPacket.encodeInt(quantity);
            }*/
        } else {
            outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            outPacket.encodeByte(0);
            outPacket.encodeByte(0);
            outPacket.encodeInt(itemId);
            outPacket.encodeInt(quantity);
        }
        return outPacket;
    }

    public static OutPacket showRewardItemAnimation(int itemId, String effect) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(0x11);
        outPacket.encodeInt(itemId);
        outPacket.encodeByte(effect != null && effect.length() > 0 ? 1 : 0);
        if (effect != null && effect.length() > 0) {
            outPacket.encodeString(effect);
        }

        return outPacket;
    }

    public static OutPacket showRewardItemAnimation(int itemId, String effect, int from_playerid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        outPacket.encodeInt(from_playerid);
        outPacket.encodeByte(0x11);
        outPacket.encodeInt(itemId);
        outPacket.encodeByte(effect != null && effect.length() > 0 ? 1 : 0);
        if (effect != null && effect.length() > 0) {
            outPacket.encodeString(effect);
        }

        return outPacket;
    }
    
    public static OutPacket dropItemFromMapObject(MapleMapItem drop, Point dropfrom, Point dropto, byte mod) {
        return dropItemFromMapObject(drop, dropfrom, dropto, mod, 0);
    }

    public static OutPacket dropItemFromMapObject(MapleMapItem drop, Point dropfrom, Point dropto, byte mod, int delay) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        outPacket.encodeByte(mod); // 1 animation, 2 no animation, 3 spawn disappearing item [Fade], 4 spawn disappearing item
        outPacket.encodeInt(drop.getObjectId()); // item owner id
        outPacket.encodeByte(drop.getMeso() > 0 ? 1 : 0); // 1 mesos, 0 item, 2 and above all item meso bag,
        outPacket.encodeInt(drop.getItemId()); // drop object ID
        outPacket.encodeInt(drop.getOwner()); // owner charid
        outPacket.encodeByte(drop.getDropType()); // 0 = timeout for non-owner, 1 = timeout for non-owner's party, 2 = FFA, 3 = explosive/FFA
        outPacket.encodePosition(dropto);
        outPacket.encodeInt(0);

        if (mod != 2) {
            outPacket.encodePosition(dropfrom);
            outPacket.encodeShort(delay); // 延遲 1000 = 1秒
        }
        if (drop.getMeso() == 0) {
            PacketHelper.addExpirationTime(outPacket, drop.getItem().getExpiration());
        }
        outPacket.encodeByte(drop.isPlayerDrop() ? 0 : 1); // pet EQP pickup
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket spawnPlayerMapobject(MapleCharacter chr) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_PLAYER.getValue());
        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(chr.getLevel());
        outPacket.encodeString(chr.getName());
        final MapleQuestStatus ultExplorer = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER));
        if (ultExplorer != null && ultExplorer.getCustomData() != null) {
            outPacket.encodeString(ultExplorer.getCustomData());
        } else {
            outPacket.encodeString("");
        }
        if (chr.getGuildId() <= 0) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        } else {
            final MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                outPacket.encodeString(gs.getName());
                outPacket.encodeShort(gs.getLogoBG());
                outPacket.encodeByte(gs.getLogoBGColor());
                outPacket.encodeShort(gs.getLogo());
                outPacket.encodeByte(gs.getLogoColor());
            } else {
                outPacket.encodeString("");
                outPacket.encodeShort(0);
                outPacket.encodeByte(0);
                outPacket.encodeShort(0);
                outPacket.encodeByte(0);
            }
        }
        outPacket.encodeInt(5); // // 性向 0=領導力 1=洞察 2=意志 3=手藝 4=感性 5=魅力
        outPacket.encodeInt(6); // 性向等級 100等是6
        outPacket.encodeByte(1); // 性向暱稱開關 角色名稱左邊出現的 0=關 1=開
        outPacket.encodeByte(1); // 性向效果開關 頭上會出現的 0=關 1=開

        final List<Pair<Integer, Integer>> buffvalue = new ArrayList<Pair<Integer, Integer>>();
        final int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        mask[0] |= DEFAULT_BUFFMASK;
        //NOT SURE: FINAL_CUT, OWL_SPIRIT, SPARK
        if (chr.getBuffedValue(MapleBuffStat.DARKSIGHT) != null || chr.isHidden()) {
            mask[mask.length - 1] |= MapleBuffStat.DARKSIGHT.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.SOULARROW) != null) {
            mask[mask.length - 1] |= MapleBuffStat.SOULARROW.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.COMBO) != null) {
            mask[mask.length - 1] |= MapleBuffStat.COMBO.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.COMBO).intValue()), 1));
        }
        if (chr.getBuffedValue(MapleBuffStat.WK_CHARGE) != null) {
            mask[mask.length - 1] |= MapleBuffStat.WK_CHARGE.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.WK_CHARGE).intValue()), 2));
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffSource(MapleBuffStat.WK_CHARGE)), 3));
        }
        if (chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null) {
            mask[mask.length - 1] |= MapleBuffStat.SHADOWPARTNER.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER).intValue()), 2));
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffSource(MapleBuffStat.SHADOWPARTNER)), 3));
        }
        //---------------------------------------------------------------
        if (chr.getBuffedValue(MapleBuffStat.MORPH) != null) {
            mask[mask.length - 2] |= MapleBuffStat.MORPH.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getStatForBuff(MapleBuffStat.MORPH).getMorph(chr)), 2));
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffSource(MapleBuffStat.MORPH)), 3));
        }
        if (chr.getBuffedValue(MapleBuffStat.DIVINE_BODY) != null) {
            mask[mask.length - 2] |= MapleBuffStat.DIVINE_BODY.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.BERSERK_FURY) != null) {
            mask[mask.length - 2] |= MapleBuffStat.BERSERK_FURY.getValue();
        }
        //---------------------------------------------------------------
        if (chr.getBuffedValue(MapleBuffStat.WIND_WALK) != null) {
            mask[mask.length - 3] |= MapleBuffStat.WIND_WALK.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.PYRAMID_PQ) != null) {
            mask[mask.length - 3] |= MapleBuffStat.PYRAMID_PQ.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.PYRAMID_PQ).intValue()), 2));
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getTrueBuffSource(MapleBuffStat.PYRAMID_PQ)), 3));
        }
/*        if (chr.getBuffedValue(MapleBuffStat.SOARING) != null) {
            mask[mask.length - 3] |= MapleBuffStat.SOARING.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.SOARING).intValue()), 2));
        }*/
        //---------------------------------------------------------------
        //if (chr.getBuffedValue(MapleBuffStat.TORNADO) != null) {
        //    mask[mask.length - 4] |= MapleBuffStat.TORNADO.getValue();
        //}
        if (chr.getBuffedValue(MapleBuffStat.INFILTRATE) != null) {
            mask[mask.length - 4] |= MapleBuffStat.INFILTRATE.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.MECH_CHANGE) != null) {
            mask[mask.length - 4] |= MapleBuffStat.MECH_CHANGE.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.MECH_CHANGE).intValue()), 2));
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getTrueBuffSource(MapleBuffStat.MECH_CHANGE)), 3));
        }
        if (chr.getBuffedValue(MapleBuffStat.DARK_AURA) != null) {
            mask[mask.length - 4] |= MapleBuffStat.DARK_AURA.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.DARK_AURA).intValue()), 2));
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getTrueBuffSource(MapleBuffStat.DARK_AURA)), 3));
        }
        if (chr.getBuffedValue(MapleBuffStat.BLUE_AURA) != null) {
            mask[mask.length - 4] |= MapleBuffStat.BLUE_AURA.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.BLUE_AURA).intValue()), 2));
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getTrueBuffSource(MapleBuffStat.BLUE_AURA)), 3));
        }
        if (chr.getBuffedValue(MapleBuffStat.YELLOW_AURA) != null) {
            mask[mask.length - 4] |= MapleBuffStat.YELLOW_AURA.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.YELLOW_AURA).intValue()), 2));
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getTrueBuffSource(MapleBuffStat.YELLOW_AURA)), 3));
        }
        if (chr.getBuffedValue(MapleBuffStat.DIVINE_SHIELD) != null) {
            mask[mask.length - 4] |= MapleBuffStat.DIVINE_SHIELD.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.GIANT_POTION) != null) {
            mask[mask.length - 4] |= MapleBuffStat.GIANT_POTION.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.GIANT_POTION).intValue()), 2));
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getTrueBuffSource(MapleBuffStat.GIANT_POTION)), 3));
        }
        //---------------------------------------------------------------
        //---------------------------------------------------------------
        if (chr.getBuffedValue(MapleBuffStat.WATER_SHIELD) != null) {
            mask[mask.length - 1] |= MapleBuffStat.WATER_SHIELD.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.SPIRIT_SURGE) != null) {
            mask[mask.length - 1] |= MapleBuffStat.SPIRIT_SURGE.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.SPIRIT_SURGE).intValue()), 2));
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffSource(MapleBuffStat.SPIRIT_SURGE)), 3));
        }
        if (chr.getBuffedValue(MapleBuffStat.SPIRIT_LINK) != null) {
            mask[mask.length - 2] |= MapleBuffStat.SPIRIT_LINK.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.SPIRIT_LINK).intValue()), 2));
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getTrueBuffSource(MapleBuffStat.SPIRIT_LINK)), 3));
        }
        if (chr.getBuffedValue(MapleBuffStat.FAMILIAR_SHADOW) != null) {
            mask[mask.length - 2] |= MapleBuffStat.FAMILIAR_SHADOW.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.FAMILIAR_SHADOW).intValue()), 3));
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getStatForBuff(MapleBuffStat.FAMILIAR_SHADOW).getCharColor()), 3));
        }
        for (int i = 0; i < mask.length; i++) {
            outPacket.encodeInt(mask[i]);
        }
        //TODO: convert this into proper format
        //AFTERSHOCK: extra int here
        for (Pair<Integer, Integer> i : buffvalue) {
            if (i.right == 3) {
                outPacket.encodeInt(i.left.intValue());
            } else if (i.right == 2) {
                outPacket.encodeShort(i.left.shortValue());
            } else if (i.right == 1) {
                outPacket.encodeByte(i.left.byteValue());
            }
        }
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        final int CHAR_MAGIC_SPAWN = Randomizer.nextInt();
        //CHAR_MAGIC_SPAWN is really just tickCount
        //this is here as it explains the 7 "dummy" buffstats which are placed into every character
        //these 7 buffstats are placed because they have irregular packet structure.
        //they ALL have writeShort(0); first, then a long as their variables, then server tick count
        outPacket.encodeLong(0); // energy charge
        outPacket.encodeByte(1);
        outPacket.encodeInt(CHAR_MAGIC_SPAWN);
        outPacket.encodeShort(0); //start of dash_speed
        outPacket.encodeLong(0);
        outPacket.encodeByte(1);
        outPacket.encodeInt(CHAR_MAGIC_SPAWN);
        outPacket.encodeShort(0); //start of dash_jump
        outPacket.encodeLong(0);
        outPacket.encodeByte(1);
        outPacket.encodeInt(CHAR_MAGIC_SPAWN);
        outPacket.encodeShort(0); //start of Monster Riding
        int buffSrc = chr.getBuffSource(MapleBuffStat.MONSTER_RIDING);
        if (buffSrc > 0) {
            final Item c_mount = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -118);
            final Item mount = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
            if (GameConstants.getMountItem(buffSrc, chr) == 0 && c_mount != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -119) != null) {
                outPacket.encodeInt(c_mount.getItemId());
            } else if (GameConstants.getMountItem(buffSrc, chr) == 0 && mount != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -19) != null) {
                outPacket.encodeInt(mount.getItemId());
            } else {
                outPacket.encodeInt(GameConstants.getMountItem(buffSrc, chr));
            }
            outPacket.encodeInt(buffSrc);
        } else {
            outPacket.encodeLong(0);
        }
        outPacket.encodeByte(1);
        outPacket.encodeInt(CHAR_MAGIC_SPAWN);
        outPacket.encodeLong(0); //speed infusion behaves differently here
        outPacket.encodeByte(1);
        outPacket.encodeInt(CHAR_MAGIC_SPAWN);
        outPacket.encodeInt(1);
        outPacket.encodeLong(0); //homing beacon
        outPacket.encodeByte(0); //random
        outPacket.encodeShort(0);
        outPacket.encodeByte(1);
        outPacket.encodeInt(CHAR_MAGIC_SPAWN);
        outPacket.encodeZeroBytes(16); // DEFAULT_BUFFSTAT
        outPacket.encodeByte(1);
        outPacket.encodeInt(CHAR_MAGIC_SPAWN);
        outPacket.encodeShort(0);
        
        outPacket.encodeShort(chr.getJob());
        PacketHelper.addCharLook(outPacket, chr, true);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        
        // 在function裡面
        int unksize = 0;
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(unksize); // [int]+[int]

        outPacket.encodeInt(Math.min(250, chr.getInventory(MapleInventoryType.CASH).countById(5110000))); //max is like 100. but w/e
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        final MapleQuestStatus stat = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.ITEM_TITLE));
        outPacket.encodeInt(stat != null && stat.getCustomData() != null ? Integer.valueOf(stat.getCustomData()) : 0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(chr.getItemEffect());
        outPacket.encodeInt(GameConstants.getInventoryType(chr.getChair()) == MapleInventoryType.SETUP ? chr.getChair() : 0);
        outPacket.encodeInt(0);
        outPacket.encodePosition(chr.getTruePosition());
        outPacket.encodeByte(chr.getStance());
        outPacket.encodeShort(0); //FH
        outPacket.encodeByte(0); //pets, follows same structure as charinfo/spawninfo
        outPacket.encodeByte(0); //好像是familiar
        outPacket.encodeInt(chr.getMount().getLevel()); // mount lvl
        outPacket.encodeInt(chr.getMount().getExp()); // exp
        outPacket.encodeInt(chr.getMount().getFatigue()); // tiredness
        PacketHelper.addAnnounceBox(outPacket, chr);
        outPacket.encodeByte(chr.getChalkboard() != null && chr.getChalkboard().length() > 0 ? 1 : 0);
        if (chr.getChalkboard() != null && chr.getChalkboard().length() > 0) {
            outPacket.encodeString(chr.getChalkboard());
        }
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> rings = chr.getRings(false);
        addRingInfo(outPacket, rings.getLeft());
        addRingInfo(outPacket, rings.getMid());
        addMRingInfo(outPacket, rings.getRight(), chr);
        outPacket.encodeByte(chr.getStat().Berserk ? 1 : 0);
        outPacket.encodeInt(0);
        final boolean pvp = chr.inPVP();
        if (pvp) {
            outPacket.encodeByte(Integer.parseInt(chr.getEventInstance().getProperty("type")));
        }
        if (chr.getCarnivalParty() != null) {
            outPacket.encodeByte(chr.getCarnivalParty().getTeam());
        } else if (GameConstants.isTeamMap(chr.getMapId())) {
            outPacket.encodeByte(chr.getTeam() + (pvp ? 1 : 0)); //is it 0/1 or is it 1/2?
        }
        return outPacket;
    }

    public static OutPacket removePlayerFromMap(int cid) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
        outPacket.encodeInt(cid);

        return outPacket;
    }

    public static OutPacket facialExpression(MapleCharacter from, int expression) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.FACIAL_EXPRESSION.getValue());
        outPacket.encodeInt(from.getId());
        outPacket.encodeInt(expression);
        outPacket.encodeInt(-1); //itemid of expression use
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket movePlayer(int cid, List<ILifeMovementFragment> moves, Point oldPos, Point oldVPos) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MOVE_PLAYER.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodePosition(oldPos);
        outPacket.encodePosition(oldVPos);
        PacketHelper.serializeMovementList(outPacket, moves);

        return outPacket;
    }

    public static OutPacket moveSummon(int cid, int summonoid, Point oldPos, Point oldVPos, List<ILifeMovementFragment> moves) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MOVE_SUMMON.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(summonoid);
        outPacket.encodePosition(oldPos);
        outPacket.encodePosition(oldVPos);
        PacketHelper.serializeMovementList(outPacket, moves);

        return outPacket;
    }

    public static OutPacket summonAttack(final int cid, final int summonSkillId, final byte animation, final List<Pair<Integer, Integer>> allDamage, final int level, final boolean darkFlare) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SUMMON_ATTACK.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(summonSkillId);
        outPacket.encodeByte(level - 1); //? guess
        outPacket.encodeByte(animation);
        outPacket.encodeByte(allDamage.size());

        for (final Pair<Integer, Integer> attackEntry : allDamage) {
            outPacket.encodeInt(attackEntry.left); // oid
            outPacket.encodeByte(7); // who knows
            outPacket.encodeInt(attackEntry.right); // damage
        }
        outPacket.encodeByte(darkFlare ? 1 : 0);
        return outPacket;
    }

    public static OutPacket closeRangeAttack(int cid, int tbyte, int skill, int level, int display, byte speed, List<AttackPair> damage, final boolean energy, int lvl, byte mastery, byte unk, int charge) {
        
        OutPacket outPacket = new OutPacket(energy ? SendPacketOpcode.ENERGY_ATTACK.getValue() : SendPacketOpcode.CLOSE_RANGE_ATTACK.getValue());
        
        outPacket.encodeInt(cid);
        outPacket.encodeByte(tbyte);
        outPacket.encodeByte(lvl); //?
        if (skill > 0) {
            outPacket.encodeByte(level);
            outPacket.encodeInt(skill);
        } else {
            outPacket.encodeByte(0);
        }
        outPacket.encodeByte(unk); // Added on v.82
        outPacket.encodeShort(display);
        outPacket.encodeByte(speed);
        outPacket.encodeByte(mastery); // Mastery
        outPacket.encodeInt(0);  // E9 03 BE FC

        if (skill == 4211006) {
            for (AttackPair oned : damage) {
                if (oned.attack != null) {
                    outPacket.encodeInt(oned.objectid);
                    outPacket.encodeByte(0x07);
                    outPacket.encodeByte(oned.attack.size());
                    for (Pair<Integer, Boolean> eachd : oned.attack) {
                        // highest bit set = crit
                        outPacket.encodeByte(eachd.right ? 1 : 0);
                        outPacket.encodeInt(eachd.left); //m.e. is never crit
                    }
                }
            }
        } else {
            for (AttackPair oned : damage) {
                if (oned.attack != null) {
                    outPacket.encodeInt(oned.objectid);
                    outPacket.encodeByte(0x07);
                    for (Pair<Integer, Boolean> eachd : oned.attack) {
                        outPacket.encodeByte(eachd.right ? 1 : 0); //TODO JUMP
                        outPacket.encodeInt(eachd.left.intValue());
                    }
                }
            }
        }
        //if (charge > 0) {
        //	outPacket.encodeInt(charge); //is it supposed to be here
        //}
        return outPacket;
    }

    public static OutPacket strafeAttack(int cid, byte tbyte, int skill, int level, int display, byte speed, int itemid, List<AttackPair> damage, final Point pos, int lvl, byte mastery, byte unk, int ultLevel) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.RANGED_ATTACK.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(tbyte);
        outPacket.encodeByte(lvl); //?
        if (skill > 0) {
            outPacket.encodeByte(level);
            outPacket.encodeInt(skill);
        } else {
            outPacket.encodeByte(0);
        }
        outPacket.encodeByte(ultLevel);
        if (ultLevel > 0) {
            outPacket.encodeInt(3220010);
        }
        outPacket.encodeByte(0); // Added on v.82
        outPacket.encodeShort(display);
        outPacket.encodeByte(speed);
        outPacket.encodeByte(mastery); // Mastery level, who cares
        outPacket.encodeInt(itemid);

        for (AttackPair oned : damage) {
            if (oned.attack != null) {
                outPacket.encodeInt(oned.objectid);
                outPacket.encodeByte(0x07);
                for (Pair<Integer, Boolean> eachd : oned.attack) {
                    // highest bit set = crit
                    outPacket.encodeByte(eachd.right ? 1 : 0); //TODO JUMP
                    outPacket.encodeInt(eachd.left.intValue());
                }
            }
        }
        outPacket.encodePosition(pos); // Position

        return outPacket;
    }

    public static OutPacket rangedAttack(int cid, byte tbyte, int skill, int level, int display, byte speed, int itemid, List<AttackPair> damage, final Point pos, int lvl, byte mastery, byte unk) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.RANGED_ATTACK.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(tbyte);
        outPacket.encodeByte(lvl); //?
        if (skill > 0) {
            outPacket.encodeByte(level);
            outPacket.encodeInt(skill);
        } else {
            outPacket.encodeByte(0);
        }
        outPacket.encodeByte(unk); // Added on v.82
        outPacket.encodeShort(display);
        outPacket.encodeByte(speed);
        outPacket.encodeByte(mastery); // Mastery level, who cares
        outPacket.encodeInt(itemid);

        for (AttackPair oned : damage) {
            if (oned.attack != null) {
                outPacket.encodeInt(oned.objectid);
                outPacket.encodeByte(0x07); //
                for (Pair<Integer, Boolean> eachd : oned.attack) {
                    outPacket.encodeByte(eachd.right ? 1 : 0); //TODO JUMP
                    outPacket.encodeInt(eachd.left.intValue());
                }
            }
        }
        outPacket.encodePosition(pos); // Position

        return outPacket;
    }

    public static OutPacket magicAttack(int cid, int tbyte, int skill, int level, int display, byte speed, List<AttackPair> damage, int charge, int lvl, byte unk) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MAGIC_ATTACK.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(tbyte);
        outPacket.encodeByte(lvl);
        outPacket.encodeByte(level);
        outPacket.encodeInt(skill);

        outPacket.encodeByte(unk); // Added on v.82
        outPacket.encodeShort(display);
        outPacket.encodeByte(speed);
        outPacket.encodeByte(0); // Mastery byte is always 0 because spells don't have a swoosh
        outPacket.encodeInt(0);

        for (AttackPair oned : damage) {
            if (oned.attack != null) {
                outPacket.encodeInt(oned.objectid);
                outPacket.encodeByte(0x07); //
                for (Pair<Integer, Boolean> eachd : oned.attack) {
                    outPacket.encodeByte(eachd.right ? 1 : 0);
                    outPacket.encodeInt(eachd.left.intValue());
                }
            }
        }
        if (charge > 0) {
            outPacket.encodeInt(charge);
        }
        return outPacket;
    }

    public static OutPacket getNPCShop(int sid, MapleShop shop, MapleClient c) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.OPEN_NPC_SHOP.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeInt(sid);
        PacketHelper.addShopInfo(outPacket, shop, c);
        return outPacket;
    }

    public static OutPacket confirmShopTransaction(byte code, MapleShop shop, MapleClient c, int indexBought) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
        outPacket.encodeByte(code); // 8 = sell, 0 = buy, 0x20 = due to an error
        if (code == 5/*4*/) {
            outPacket.encodeByte(0);
            outPacket.encodeInt(shop.getNpcId()); //oops
            PacketHelper.addShopInfo(outPacket, shop, c);
        } else {
            outPacket.encodeByte(indexBought >= 0 ? 1 : 0);
            if (indexBought >= 0) {
                outPacket.encodeInt(indexBought);
            }
        }
        return outPacket;
    }

    public static OutPacket addInventorySlot(MapleInventoryType type, Item item) {
        return addInventorySlot(type, item, false);
    }

    public static OutPacket addInventorySlot(MapleInventoryType type, Item item, boolean fromDrop) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(fromDrop ? 1 : 0);
        outPacket.encodeByte(1); // how many items to add
        outPacket.encodeByte(1); // used for remove case only. related to 2230000 (EXP Item), if its a 0, function executed.

        outPacket.encodeByte(GameConstants.isInBag(item.getPosition(), type.getType()) ? 9 : 0/*item.getPosition() > 100 && type == MapleInventoryType.ETC ? 9 : 0*/);
        outPacket.encodeByte(type.getType()); // iv type
        outPacket.encodeShort(item.getPosition()); // slot id
        PacketHelper.addItemInfo(outPacket, item, true, true);
        outPacket.encodeByte(0); // only needed here when size is <= 1

        return outPacket;
    }

    public static OutPacket updateInventorySlot(MapleInventoryType type, Item item, boolean fromDrop) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(fromDrop ? 1 : 0);
        outPacket.encodeByte(1); //how many items to update
        outPacket.encodeByte(0);

        outPacket.encodeByte(GameConstants.isInBag(item.getPosition(), type.getType()) ? 6 : 1/*item.getPosition() > 100 && type == MapleInventoryType.ETC ? 6 : 1*/); //bag
        outPacket.encodeByte(type.getType()); // iv type
        outPacket.encodeShort(item.getPosition()); // slot id
        outPacket.encodeShort(item.getQuantity());
        outPacket.encodeByte(0);
        return outPacket;
    }

    public static OutPacket moveInventoryItem(MapleInventoryType type, short src, short dst, boolean bag, boolean bothBag) {
        return moveInventoryItem(type, src, dst, (byte) -1, bag, bothBag);
    }

    public static OutPacket moveInventoryItem(MapleInventoryType type, short src, short dst, short equipIndicator, boolean bag, boolean bothBag) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeByte(1); //how many items to update
        outPacket.encodeByte(0);

        outPacket.encodeByte(bag ? (bothBag ? 8 : 5) : 2);
        outPacket.encodeByte(type.getType());
        outPacket.encodeShort(src);
        outPacket.encodeShort(dst);
        if (bag) {
            outPacket.encodeShort(0);
        }
        if (equipIndicator != -1) {
            outPacket.encodeShort(equipIndicator);
        }
        return outPacket;
    }

    public static OutPacket moveAndMergeInventoryItem(MapleInventoryType type, short src, short dst, short total, boolean bag, boolean switchSrcDst, boolean bothBag) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeByte(2); //how many items to update
        outPacket.encodeByte(0);

        outPacket.encodeByte(bag && (switchSrcDst || bothBag) ? 7 : 3);
        outPacket.encodeByte(type.getType());
        outPacket.encodeShort(src);

        outPacket.encodeByte(bag && (!switchSrcDst || bothBag) ? 6 : 1); // merge mode?
        outPacket.encodeByte(type.getType());
        outPacket.encodeShort(dst);
        outPacket.encodeShort(total);

        return outPacket;
    }

    public static OutPacket moveAndMergeWithRestInventoryItem(MapleInventoryType type, short src, short dst, short srcQ, short dstQ, boolean bag, boolean switchSrcDst, boolean bothBag) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeByte(2); //how many items to update
        outPacket.encodeByte(0);

        outPacket.encodeByte(bag && (switchSrcDst || bothBag) ? 6 : 1);
        outPacket.encodeByte(type.getType());
        outPacket.encodeShort(src);
        outPacket.encodeShort(srcQ);

        outPacket.encodeByte(bag && (!switchSrcDst || bothBag) ? 6 : 1);
        outPacket.encodeByte(type.getType());
        outPacket.encodeShort(dst);
        outPacket.encodeShort(dstQ);

        return outPacket;
    }

    public static OutPacket clearInventoryItem(MapleInventoryType type, short slot, boolean fromDrop) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(fromDrop ? 1 : 0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(0);

        outPacket.encodeByte(slot > 100 && type == MapleInventoryType.ETC ? 7 : 3); //bag
        outPacket.encodeByte(type.getType());
        outPacket.encodeShort(slot);

        return outPacket;
    }

    public static OutPacket updateSpecialItemUse(Item item, byte invType, MapleCharacter chr) {
        return updateSpecialItemUse(item, invType, item.getPosition(), false, chr);
    }

    public static OutPacket updateSpecialItemUse(Item item, byte invType, short pos, boolean theShort, MapleCharacter chr) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(0); // could be from drop
        outPacket.encodeByte(2); // always 2
        outPacket.encodeByte(0);
        //clears the slot and puts item in same slot in one packet
        outPacket.encodeByte(invType == MapleInventoryType.ETC.getType() && pos > 100 ? 7 : 3); // quantity > 0 (?)
        outPacket.encodeByte(invType); // Inventory type
        outPacket.encodeShort(pos); // item slot

        outPacket.encodeByte(0);
        outPacket.encodeByte(invType);
        if (item.getType() == 1 || theShort) {
            outPacket.encodeShort(pos);
        } else {
            outPacket.encodeByte(pos);
        }
        PacketHelper.addItemInfo(outPacket, item, true, true, false, false, chr);
        if (pos < 0) {
            outPacket.encodeByte(2); //?
        }

        return outPacket;
    }

    public static OutPacket updateSpecialItemUse_(Item item, byte invType, MapleCharacter chr) {
        return updateSpecialItemUse_(item, invType, item.getPosition(), chr);
    }

    public static OutPacket updateSpecialItemUse_(Item item, byte invType, short pos, MapleCharacter chr) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(0); // could be from drop
        outPacket.encodeByte(1); // always 2
        outPacket.encodeByte(0);

        outPacket.encodeByte(0); // quantity > 0 (?)
        outPacket.encodeByte(invType); // Inventory type
        if (item.getType() == 1) {
            outPacket.encodeShort(pos);
        } else {
            outPacket.encodeByte(pos);
        }
        PacketHelper.addItemInfo(outPacket, item, true, true, false, false, chr);
        if (pos < 0) {
            outPacket.encodeByte(1); //?
        }

        return outPacket;
    }

    public static OutPacket scrolledItem(Item scroll, Item item, boolean destroyed, boolean potential) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(1); // fromdrop always true
        outPacket.encodeByte(destroyed ? 2 : 3);
        outPacket.encodeByte(0);

        outPacket.encodeByte(scroll.getQuantity() > 0 ? 1 : 3);
        outPacket.encodeByte(GameConstants.getInventoryType(scroll.getItemId()).getType()); //can be cash
        outPacket.encodeShort(scroll.getPosition());
        if (scroll.getQuantity() > 0) {
            outPacket.encodeShort(scroll.getQuantity());
        }

        outPacket.encodeByte(3);
        outPacket.encodeByte(MapleInventoryType.EQUIP.getType());
        outPacket.encodeShort(item.getPosition());
        if (!destroyed) {
            outPacket.encodeByte(0);
            outPacket.encodeByte(MapleInventoryType.EQUIP.getType());
            outPacket.encodeShort(item.getPosition());
            PacketHelper.addItemInfo(outPacket, item, true, true);
        }
        if (!potential) {
            outPacket.encodeByte(1);
        }

        return outPacket;
    }

    public static OutPacket moveAndUpgradeItem(MapleInventoryType type, Item item, short oldpos, short newpos, MapleCharacter chr) {//equipping some items  
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(1); //fromdrop
        outPacket.encodeByte(3);
        outPacket.encodeByte(0);

        outPacket.encodeByte(type == MapleInventoryType.ETC && newpos > 100 ? 7 : 3);
        outPacket.encodeByte(type.getType());
        outPacket.encodeShort(oldpos);

        outPacket.encodeByte(0);
        outPacket.encodeByte(1);
        outPacket.encodeShort(oldpos);
        PacketHelper.addItemInfo(outPacket, item, true, true, false, false, chr);

        outPacket.encodeByte(2);
        outPacket.encodeByte(type.getType());
        outPacket.encodeShort(oldpos);//oldslot
        outPacket.encodeShort(newpos);//new slot
        outPacket.encodeByte(0);//?
        return outPacket;
    }

    public static OutPacket getScrollEffect(int chr, ScrollResult scrollSuccess, boolean legendarySpirit, boolean whiteScroll) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_SCROLL_EFFECT.getValue());
        outPacket.encodeInt(chr);
        outPacket.encodeByte(scrollSuccess == ScrollResult.SUCCESS ? 1 : 0);
        outPacket.encodeByte(scrollSuccess == ScrollResult.CURSE ? 1 : 0);
        outPacket.encodeByte(legendarySpirit ? 1 : 0);
        outPacket.encodeByte(1); //idk
        outPacket.encodeByte(whiteScroll ? 1 : 0);
        outPacket.encodeInt(0);
        return outPacket;
    }

    //miracle cube?
    public static OutPacket getPotentialEffect(final int chr, final int itemid) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_POTENTIAL_EFFECT.getValue());
        outPacket.encodeInt(chr);
        outPacket.encodeInt(itemid);
        return outPacket;
    }

    //magnify glass
    public static OutPacket getPotentialReset(final int chr, final short pos) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_POTENTIAL_RESET.getValue());
        outPacket.encodeInt(chr);
        outPacket.encodeShort(pos);
        return outPacket;
    }

    public static final OutPacket ItemMaker_Success() {
        
//D6 00 00 00 00 00 01 00 00 00 00 DC DD 40 00 01 00 00 00 01 00 00 00 8A 1C 3D 00 01 00 00 00 00 00 00 00 00 B0 AD 01 00
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(0x14); //bb +2
        outPacket.encodeZeroBytes(4);

        return outPacket;
    }

    public static final OutPacket ItemMaker_Success_3rdParty(final int from_playerid) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        outPacket.encodeInt(from_playerid);
        outPacket.encodeByte(0x14);
        outPacket.encodeZeroBytes(4);

        return outPacket;
    }

    public static OutPacket explodeDrop(int oid) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        outPacket.encodeByte(4); // 4 = Explode
        outPacket.encodeInt(oid);
        outPacket.encodeShort(655);

        return outPacket;
    }

    public static OutPacket removeItemFromMap(int oid, int animation, int cid) {
        return removeItemFromMap(oid, animation, cid, 0);
    }

    public static OutPacket removeItemFromMap(int oid, int animation, int cid, int slot) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        outPacket.encodeByte(animation); // 0 = Expire, 1 = without animation, 2 = pickup, 4 = explode, 5 = pet pickup
        outPacket.encodeInt(oid);
        if (animation >= 2) {
            outPacket.encodeInt(cid);
            if (animation == 5) { // allow pet pickup?
                outPacket.encodeInt(slot);
            }
        }
        return outPacket;
    }

    public static OutPacket updateCharLook(MapleCharacter chr) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_CHAR_LOOK.getValue());
        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(1);
        PacketHelper.addCharLook(outPacket, chr, false);
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> rings = chr.getRings(false);
        addRingInfo(outPacket, rings.getLeft());
        addRingInfo(outPacket, rings.getMid());
        addMRingInfo(outPacket, rings.getRight(), chr);
        outPacket.encodeInt(0); // -> charid to follow (4)
        return outPacket;
    }

    public static void addRingInfo(OutPacket outPacket, List<MapleRing> rings) {
        outPacket.encodeByte(rings.size());
        for (MapleRing ring : rings) {
            outPacket.encodeInt(1);
            outPacket.encodeLong(ring.getRingId());
            outPacket.encodeLong(ring.getPartnerRingId());
            outPacket.encodeInt(ring.getItemId());
        }
    }

    public static void addMRingInfo(OutPacket outPacket, List<MapleRing> rings, MapleCharacter chr) {
        outPacket.encodeByte(rings.size());
        for (MapleRing ring : rings) {
//            outPacket.encodeInt(1);
            outPacket.encodeInt(chr.getId());
            outPacket.encodeInt(ring.getPartnerChrId());
            outPacket.encodeInt(ring.getItemId());
        }
    }

    public static OutPacket dropInventoryItem(MapleInventoryType type, short src) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeByte(1); //how many items to update
        outPacket.encodeByte(0);

        outPacket.encodeByte(3);
        outPacket.encodeByte(type.getType());
        outPacket.encodeShort(src);
        if (src < 0) {
            outPacket.encodeByte(1);
        }
        return outPacket;
    }

    public static OutPacket dropInventoryItemUpdate(MapleInventoryType type, Item item) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeByte(1); //how many items to update
        outPacket.encodeByte(0);

        outPacket.encodeByte(1);
        outPacket.encodeByte(type.getType());
        outPacket.encodeShort(item.getPosition());
        outPacket.encodeShort(item.getQuantity());

        return outPacket;
    }

    public static OutPacket damagePlayer(int skill, int monsteridfrom, int cid, int damage, int fake, byte direction, int reflect, boolean is_pg, int oid, int pos_x, int pos_y, int offset) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DAMAGE_PLAYER.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(skill);
        outPacket.encodeInt(damage);
        outPacket.encodeByte(0);
        outPacket.encodeInt(monsteridfrom);
        outPacket.encodeByte(direction);

        if (reflect > 0) {
            outPacket.encodeByte(reflect);
            outPacket.encodeByte(is_pg ? 1 : 0);
            outPacket.encodeInt(oid);
            outPacket.encodeByte(6);
            outPacket.encodeShort(pos_x);
            outPacket.encodeShort(pos_y);
            outPacket.encodeByte(0);
        } else {
            outPacket.encodeShort(0);
        }
        outPacket.encodeByte(offset);
        outPacket.encodeInt(damage);
        if (fake > 0) {
            outPacket.encodeInt(fake);
        }
        return outPacket;
    }

    public static final OutPacket updateQuest(final MapleQuestStatus quest) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeShort(quest.getQuest().getId());
        outPacket.encodeByte(quest.getStatus());
        switch (quest.getStatus()) {
            case 0:
                outPacket.encodeZeroBytes(10);
                break;
            case 1:
                outPacket.encodeString(quest.getCustomData() != null ? quest.getCustomData() : "");
                break;
            case 2:
                if (GameConstants.GMS) {
                    outPacket.encodeShort(0);
                }
                outPacket.encodeLong(PacketHelper.getTime(System.currentTimeMillis()));
                break;
        }

        return outPacket;
    }

    public static final OutPacket updateInfoQuest(final int quest, final String data) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(0x0C); //AFTERSHOCK: 0x0C
        outPacket.encodeShort(quest);
        outPacket.encodeString(data);

        return outPacket;
    }

    public static OutPacket updateQuestInfo(MapleCharacter c, int quest, int npc, byte progress) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        outPacket.encodeByte(progress); //bb - 10
        outPacket.encodeShort(quest);
        outPacket.encodeInt(npc);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket updateQuestFinish(int quest, int npc, int nextquest) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        outPacket.encodeByte(10); //bb - 10
        outPacket.encodeShort(quest);
        outPacket.encodeInt(npc);
        outPacket.encodeInt(nextquest);
        return outPacket;
    }

    public static final OutPacket charInfo(final MapleCharacter chr, final boolean isSelf) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CHAR_INFO.getValue());
        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(chr.getLevel());
        outPacket.encodeShort(chr.getJob());
        outPacket.encodeByte(chr.getStat().pvpRank);
        outPacket.encodeInt(chr.getFame());
        outPacket.encodeByte(chr.getMarriageId() > 0 ? 1 : 0); // heart red or gray
        List<Integer> prof = chr.getProfessions();
        outPacket.encodeByte(prof.size());
        for (int i : prof) {
            outPacket.encodeShort(i);
        }
        if (chr.getGuildId() <= 0) {
            outPacket.encodeString("-");
            outPacket.encodeString("");
        } else {
            final MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                outPacket.encodeString(gs.getName());
                if (gs.getAllianceId() > 0) {
                    final MapleGuildAlliance allianceName = World.Alliance.getAlliance(gs.getAllianceId());
                    if (allianceName != null) {
                        outPacket.encodeString(allianceName.getName());
                    } else {
                        outPacket.encodeString("");
                    }
                } else {
                    outPacket.encodeString("");
                }
            } else {
                outPacket.encodeString("-");
                outPacket.encodeString("");
            }
        }
        outPacket.encodeByte(isSelf ? 1 : 0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        byte index = 1;
        for (final MaplePet pet : chr.getPets()) {
            if (pet.getSummoned()) {
                outPacket.encodeByte(index);
                outPacket.encodeInt(0); //derp
                outPacket.encodeInt(pet.getPetItemId()); // petid
                outPacket.encodeString(pet.getName());
                outPacket.encodeByte(pet.getLevel()); // pet level
                outPacket.encodeShort(pet.getCloseness()); // pet closeness
                outPacket.encodeByte(pet.getFullness()); // pet fullness
                outPacket.encodeShort(pet.getFlags()/*0*/); // pet flag
                final Item inv = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) (index == 1 ? -114 : (index == 2 ? -130 : -138)));
                outPacket.encodeInt(inv == null ? 0 : inv.getItemId());
                index++;
            }
        }
        outPacket.encodeByte(0); // End of pet

        if (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18) != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -19) != null) {
            final MapleMount mount = chr.getMount();
            outPacket.encodeByte(1);
            outPacket.encodeInt(mount.getLevel());
            outPacket.encodeInt(mount.getExp());
            outPacket.encodeInt(mount.getFatigue());
        } else {
            outPacket.encodeByte(0);
        }

        final int wishlistSize = chr.getWishlistSize();
        outPacket.encodeByte(wishlistSize);
        if (wishlistSize > 0) {
            final int[] wishlist = chr.getWishlist();
            for (int x = 0; x < wishlistSize; x++) {
                outPacket.encodeInt(wishlist[x]);
            }
        }

        // 怪物圖鑑
        outPacket.encodeInt(1);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
//        chr.getMonsterBook().addCharInfoPacket(chr.getMonsterBookCover(), outPacket);

        Item medal = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21/*-46*/);
        outPacket.encodeInt(medal == null ? 0 : medal.getItemId());
        List<Pair<Integer, Long>> medalQuests = chr.getCompletedMedals();
        outPacket.encodeShort(medalQuests.size());
        for (Pair<Integer, Long> x : medalQuests) {
            outPacket.encodeShort(x.left);
            if (GameConstants.GMS) { // JUMP之後才有 這是取得勳章時候的時間
                outPacket.encodeLong(x.right);
            }
        }

        for (MapleTraitType t : MapleTraitType.values()) {
            outPacket.encodeByte(chr.getTrait(t).getLevel());
        }

/*        if (GameConstants.GMS) { // 椅子 JMS BB前才有這個
            List<Integer> chairs = new ArrayList<Integer>();
            for (Item i : chr.getInventory(MapleInventoryType.SETUP).newList()) {
                if (i.getItemId() / 10000 == 301 && !chairs.contains(i.getItemId())) {
                    chairs.add(i.getItemId());
                }
            }
            outPacket.encodeInt(chairs.size());
            for (int i : chairs) {
                outPacket.encodeInt(i);
            }
        }*/
        
        return outPacket;
    }

    public static OutPacket giveDice(int buffid, int skillid, int duration, Map<MapleBuffStat, Integer> statups) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_BUFF.getValue());

        PacketHelper.writeBuffMask(outPacket, statups);

        outPacket.encodeShort(Math.max(buffid / 100, Math.max(buffid / 10, buffid % 10))); // 1-6

        outPacket.encodeInt(skillid); // skillid
        outPacket.encodeInt(duration);
        outPacket.encodeShort(0);
        outPacket.encodeByte(0);
		
		outPacket.encodeInt(GameConstants.getDiceStat(buffid, 3));
		outPacket.encodeInt(GameConstants.getDiceStat(buffid, 3));
		outPacket.encodeInt(GameConstants.getDiceStat(buffid, 4));
		outPacket.encodeZeroBytes(20); //idk
		outPacket.encodeInt(GameConstants.getDiceStat(buffid, 2));
		outPacket.encodeZeroBytes(12); //idk
		outPacket.encodeInt(GameConstants.getDiceStat(buffid, 5));
		outPacket.encodeZeroBytes(16); //idk
		outPacket.encodeInt(GameConstants.getDiceStat(buffid, 6));
		outPacket.encodeZeroBytes(16);
        outPacket.encodeByte(1);
        outPacket.encodeByte(4); // Total buffed times

        return outPacket;
    }

    public static OutPacket giveMount(int buffid, int skillid, Map<MapleBuffStat, Integer> statups) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_BUFF.getValue());

        PacketHelper.writeBuffMask(outPacket, statups);

        outPacket.encodeShort(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(buffid); // 1902000 saddle
        outPacket.encodeInt(skillid); // skillid
        outPacket.encodeInt(0); // Server tick value
        outPacket.encodeShort(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(4); // Total buffed times

        return outPacket;
    }

    //monster oid, %damage increase
    public static OutPacket giveArcane(Map<Integer, Integer> statups, int duration) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_BUFF.getValue());
        PacketHelper.writeSingleMask(outPacket, MapleBuffStat.ARCANE_AIM);

        outPacket.encodeShort(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(statups.size());
        for (Entry<Integer, Integer> stat : statups.entrySet()) {
            outPacket.encodeInt(stat.getKey());
            outPacket.encodeLong(stat.getValue());
            outPacket.encodeInt(duration);
        }
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        return outPacket;
    }

    public static OutPacket givePirate(Map<MapleBuffStat, Integer> statups, int duration, int skillid) {
        final boolean infusion = skillid == 5121009 || skillid == 15111005 || skillid % 10000 == 8006;

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_BUFF.getValue());
        PacketHelper.writeBuffMask(outPacket, statups);

        outPacket.encodeShort(0);
        outPacket.encodeByte(0);
        for (Integer stat : statups.values()) {
            outPacket.encodeInt(stat.intValue());
            outPacket.encodeLong(skillid);
            outPacket.encodeZeroBytes(infusion ? 6 : 1);
            outPacket.encodeShort(duration);
        }
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1); //does this only come in dash?
        return outPacket;
    }

    public static OutPacket giveForeignPirate(Map<MapleBuffStat, Integer> statups, int duration, int cid, int skillid) {
        final boolean infusion = skillid == 5121009 || skillid == 15111005;

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        outPacket.encodeInt(cid);
        PacketHelper.writeBuffMask(outPacket, statups);
        outPacket.encodeShort(0);
        outPacket.encodeByte(0);
        for (Integer stat : statups.values()) {
            outPacket.encodeInt(stat.intValue());
            outPacket.encodeLong(skillid);
            outPacket.encodeZeroBytes(infusion ? 6 : 1);
            outPacket.encodeShort(duration);//duration... seconds
        }
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        return outPacket;
    }

    public static OutPacket giveHoming(int skillid, int mobid, int x) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_BUFF.getValue());
        PacketHelper.writeSingleMask(outPacket, MapleBuffStat.HOMING_BEACON);
        outPacket.encodeShort(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(1);
        outPacket.encodeLong(skillid);
        outPacket.encodeByte(0);
        outPacket.encodeLong(mobid);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeByte(0);
        return outPacket;
    }

    public static OutPacket giveEnergyChargeTest(int bar, int bufflength) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_BUFF.getValue());
        PacketHelper.writeSingleMask(outPacket, MapleBuffStat.ENERGY_CHARGE);
        outPacket.encodeShort(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(Math.min(bar, 10000)); // 0 = no bar, 10000 = full bar
        outPacket.encodeLong(0); //skillid, but its 0 here
        outPacket.encodeByte(0);
        outPacket.encodeInt(bar >= 10000 ? bufflength : 0);//short - bufflength...50
        return outPacket;
    }

    public static OutPacket giveEnergyChargeTest(int cid, int bar, int bufflength) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        outPacket.encodeInt(cid);
        PacketHelper.writeSingleMask(outPacket, MapleBuffStat.ENERGY_CHARGE);
        outPacket.encodeShort(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(Math.min(bar, 10000)); // 0 = no bar, 10000 = full bar
        outPacket.encodeLong(0); //skillid, but its 0 here
        outPacket.encodeByte(0);
        outPacket.encodeInt(bar >= 10000 ? bufflength : 0);//short - bufflength...50
        return outPacket;
    }

    public static OutPacket giveBuff(int buffid, int bufflength, Map<MapleBuffStat, Integer> statups, MapleStatEffect effect) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_BUFF.getValue());
        // 17 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 07 00 AE E1 3E 00 68 B9 01 00 00 00 00 00

        //lhc patch adds an extra int here
        PacketHelper.writeBuffMask(outPacket, statups);
        boolean stacked = false;
        for (Entry<MapleBuffStat, Integer> stat : statups.entrySet()) {
            if (stat.getKey().canStack()) {
                if (!stacked) {
                    outPacket.encodeZeroBytes(3);
                    stacked = true;
                }
                outPacket.encodeInt(1); //amount of stacked buffs
                outPacket.encodeInt(buffid);
                outPacket.encodeLong(stat.getValue().longValue());
            } else {
                outPacket.encodeShort(stat.getValue().intValue());
                outPacket.encodeInt(buffid);
            }
            outPacket.encodeInt(bufflength);
        }
        outPacket.encodeShort(0); // delay,  wk charges have 600 here o.o
        if (effect != null && effect.isDivineShield()) {
            outPacket.encodeInt(effect.getEnhancedWatk());
        } else if (effect != null && effect.getCharColor() > 0) {
            outPacket.encodeInt(effect.getCharColor());
        } else if (effect != null && effect.isInflation()) {
            outPacket.encodeInt(effect.getInflation());
        }

        outPacket.encodeShort(0); // combo 600, too
        outPacket.encodeByte(1);
        outPacket.encodeByte(effect != null && effect.isShadow() ? 1 : 4); // Test

        return outPacket;
    }

    public static OutPacket giveDebuff(MapleDisease statups, int x, int skillid, int level, int duration) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_BUFF.getValue());

        PacketHelper.writeSingleMask(outPacket, statups);

        outPacket.encodeShort(x);
        outPacket.encodeShort(skillid);
        outPacket.encodeShort(level);
        outPacket.encodeInt(duration);
        outPacket.encodeShort(0); // ??? wk charges have 600 here o.o
        outPacket.encodeShort(0); //Delay
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket giveForeignDebuff(int cid, final MapleDisease statups, int skillid, int level, int x) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        outPacket.encodeInt(cid);

        PacketHelper.writeSingleMask(outPacket, statups);
        if (skillid == 125) {
            outPacket.encodeShort(0);
            outPacket.encodeByte(0); //todo test
        }
        outPacket.encodeShort(x);
        outPacket.encodeShort(skillid);
        outPacket.encodeShort(level);
        outPacket.encodeShort(0); // same as give_buff
        outPacket.encodeShort(0); //Delay
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        return outPacket;
    }

    public static OutPacket cancelForeignDebuff(int cid, MapleDisease mask) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        outPacket.encodeInt(cid);
        PacketHelper.writeSingleMask(outPacket, mask);
        outPacket.encodeByte(3);
        outPacket.encodeByte(1);
        
        return outPacket;
    }

    public static OutPacket showMonsterRiding(int cid, Map<MapleBuffStat, Integer> statups, int itemId, int skillId) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        outPacket.encodeInt(cid);

        PacketHelper.writeBuffMask(outPacket, statups);

        outPacket.encodeShort(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(itemId);
        outPacket.encodeInt(skillId);
        outPacket.encodeInt(0);
        outPacket.encodeShort(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(4);

        return outPacket;
    }

    public static OutPacket giveForeignBuff(int cid, Map<MapleBuffStat, Integer> statups, MapleStatEffect effect) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        outPacket.encodeInt(cid);

        PacketHelper.writeBuffMask(outPacket, statups);
        for (Entry<MapleBuffStat, Integer> statup : statups.entrySet()) {
            if (statup.getKey() == MapleBuffStat.SHADOWPARTNER || statup.getKey() == MapleBuffStat.MECH_CHANGE || statup.getKey() == MapleBuffStat.DARK_AURA || statup.getKey() == MapleBuffStat.YELLOW_AURA || statup.getKey() == MapleBuffStat.BLUE_AURA || statup.getKey() == MapleBuffStat.GIANT_POTION || statup.getKey() == MapleBuffStat.SPIRIT_LINK || statup.getKey() == MapleBuffStat.PYRAMID_PQ || statup.getKey() == MapleBuffStat.WK_CHARGE || statup.getKey() == MapleBuffStat.SPIRIT_SURGE || statup.getKey() == MapleBuffStat.MORPH) {
                outPacket.encodeShort(statup.getValue().shortValue());
                outPacket.encodeInt(effect.isSkill() ? effect.getSourceId() : -effect.getSourceId());
            } else if (statup.getKey() == MapleBuffStat.FAMILIAR_SHADOW) {
                outPacket.encodeInt(statup.getValue().intValue());
                outPacket.encodeInt(effect.getCharColor());
            } else {
                outPacket.encodeShort(statup.getValue().shortValue());
            }
        }
        outPacket.encodeShort(0); // same as give_buff
        outPacket.encodeShort(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket cancelForeignBuff(int cid, List<MapleBuffStat> statups) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        outPacket.encodeInt(cid);
        PacketHelper.writeMask(outPacket, statups);
        outPacket.encodeByte(3);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket cancelBuff(List<MapleBuffStat> statups) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CANCEL_BUFF.getValue());

        PacketHelper.writeMask(outPacket, statups);
        for (MapleBuffStat z : statups) {
            if (z.canStack()) {
                outPacket.encodeInt(0); //amount of buffs still in the stack? dunno mans
            }
        }
        outPacket.encodeByte(3);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket cancelHoming() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CANCEL_BUFF.getValue());

        PacketHelper.writeSingleMask(outPacket, MapleBuffStat.HOMING_BEACON);

        return outPacket;
    }

    public static OutPacket cancelDebuff(MapleDisease mask) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CANCEL_BUFF.getValue());

        PacketHelper.writeSingleMask(outPacket, mask);
        outPacket.encodeByte(3);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket updateMount(MapleCharacter chr, boolean levelup) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_MOUNT.getValue());
        outPacket.encodeInt(chr.getId());
        outPacket.encodeInt(chr.getMount().getLevel());
        outPacket.encodeInt(chr.getMount().getExp());
        outPacket.encodeInt(chr.getMount().getFatigue());
        outPacket.encodeByte(levelup ? 1 : 0);

        return outPacket;
    }

    public static OutPacket mountInfo(MapleCharacter chr) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_MOUNT.getValue());
        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(1);
        outPacket.encodeInt(chr.getMount().getLevel());
        outPacket.encodeInt(chr.getMount().getExp());
        outPacket.encodeInt(chr.getMount().getFatigue());

        return outPacket;
    }

    public static OutPacket getTradeInvite(MapleCharacter c) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 11 : 2);
        outPacket.encodeByte(3);
        outPacket.encodeString(c.getName());
        outPacket.encodeInt(0); // Trade ID

        return outPacket;
    }

    public static OutPacket getTradeItemAdd(byte number, Item item) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0 : 0x0D);
        outPacket.encodeByte(number);
        PacketHelper.addItemInfo(outPacket, item, false, false, true);

        return outPacket;
    }

    public static OutPacket getTradeMesoSet(byte number, int meso) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 1 : 0x0E);
        outPacket.encodeByte(number);
        outPacket.encodeInt(meso);

        return outPacket;
    }

    public static OutPacket getTradeStart(MapleClient c, MapleTrade trade, byte number) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 10 : 5);
        outPacket.encodeByte(3);
        outPacket.encodeByte(2);
        outPacket.encodeByte(number);

        if (number == 1) {
            outPacket.encodeByte(0);
            PacketHelper.addCharLook(outPacket, trade.getPartner().getChr(), false);
            outPacket.encodeString(trade.getPartner().getChr().getName());
            outPacket.encodeShort(trade.getPartner().getChr().getJob());
        }
        outPacket.encodeByte(number);
        PacketHelper.addCharLook(outPacket, c.getPlayer(), false);
        outPacket.encodeString(c.getPlayer().getName());
        outPacket.encodeShort(c.getPlayer().getJob());
        outPacket.encodeByte(0xFF);

        return outPacket;
    }

    public static OutPacket getTradeConfirmation() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 2 : 0x0F); //or 7? what

        return outPacket;
    }

    public static OutPacket TradeMessage(final byte UserSlot, final byte message) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 18 : 0xA);
        outPacket.encodeByte(UserSlot);
        outPacket.encodeByte(message);
        //0x02 = cancelled
        //0x07 = success [tax is automated]
        //0x08 = unsuccessful
        //0x09 = "You cannot make the trade because there are some items which you cannot carry more than one."
        //0x0A = "You cannot make the trade because the other person's on a different map."

        return outPacket;
    }

    public static OutPacket getTradeCancel(final byte UserSlot, final int unsuccessful) { //0 = canceled 1 = invent space 2 = pickuprestricted
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 18 : 0xA);
        outPacket.encodeByte(UserSlot);
        outPacket.encodeByte(unsuccessful == 0 ? (GameConstants.GMS ? 7 : 2) : (unsuccessful == 1 ? 8 : 9));

        return outPacket;
    }

    public static OutPacket getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte type) {
        return getNPCTalk(npc, msgType, talk, endBytes, type, npc);
    }

    public static OutPacket getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte type, int diffNPC) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.NPC_TALK.getValue());
        outPacket.encodeByte(4);
        outPacket.encodeInt(npc);
        outPacket.encodeByte(msgType);
        outPacket.encodeByte(type); // mask; 1 = no ESC, 2 = playerspeaks, 4 = diff NPC 8 = something, ty KDMS
        if ((type & 0x4) != 0) {
            outPacket.encodeInt(diffNPC);
        }
        outPacket.encodeString(talk);
        outPacket.encodeArr(HexTool.getByteArrayFromHexString(endBytes));

        return outPacket;
    }

    public static final OutPacket getMapSelection(final int npcid, final String sel) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.NPC_TALK.getValue());
        outPacket.encodeByte(4);
        outPacket.encodeInt(npcid);
        outPacket.encodeShort(GameConstants.GMS ? 0x11 : 0x10);
        outPacket.encodeInt(npcid == 2083006 ? 1 : 0); //neo city
        outPacket.encodeInt(npcid == 9010022 ? 1 : 0); //dimensional
        outPacket.encodeString(sel);

        return outPacket;
    }

    public static OutPacket getNPCTalkStyle(int npc, String talk, int... args) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.NPC_TALK.getValue());
        outPacket.encodeByte(4);
        outPacket.encodeInt(npc);
        outPacket.encodeShort(9);
        outPacket.encodeString(talk);
        outPacket.encodeByte(args.length);

        for (int i = 0; i < args.length; i++) {
            outPacket.encodeInt(args[i]);
        }
        return outPacket;
    }

    public static OutPacket getNPCTalkNum(int npc, String talk, int def, int min, int max) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.NPC_TALK.getValue());
        outPacket.encodeByte(4);
        outPacket.encodeInt(npc);
        outPacket.encodeShort(4);
        outPacket.encodeString(talk);
        outPacket.encodeInt(def);
        outPacket.encodeInt(min);
        outPacket.encodeInt(max);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket getNPCTalkText(int npc, String talk) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.NPC_TALK.getValue());
        outPacket.encodeByte(4);
        outPacket.encodeInt(npc);
        outPacket.encodeShort(3);
        outPacket.encodeString(talk);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket showForeignEffect(int cid, int effect) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(effect); // 0 = Level up, 8 = job change

        return outPacket;
    }

    public static OutPacket showBuffeffect(int cid, int skillid, int effectid, int playerLevel, int skillLevel) {
        return showBuffeffect(cid, skillid, effectid, playerLevel, skillLevel, (byte) 3);
    }

    public static OutPacket showBuffeffect(int cid, int skillid, int effectid, int playerLevel, int skillLevel, byte direction) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(effectid); //ehh?
        outPacket.encodeInt(skillid);
        outPacket.encodeByte(playerLevel - 1); //player level
        outPacket.encodeByte(skillLevel); //skill level
        if (direction != (byte) 3) {
            outPacket.encodeByte(direction);
        }
        return outPacket;
    }

    public static OutPacket showOwnBuffEffect(int skillid, int effectid, int playerLevel, int skillLevel) {
        return showOwnBuffEffect(skillid, effectid, playerLevel, skillLevel, (byte) 3);
    }

    public static OutPacket showOwnBuffEffect(int skillid, int effectid, int playerLevel, int skillLevel, byte direction) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(effectid);
        outPacket.encodeInt(skillid);
        outPacket.encodeByte(playerLevel - 1); //player level
        outPacket.encodeByte(skillLevel); //skill level
        if (direction != (byte) 3) {
            outPacket.encodeByte(direction);
        }

        return outPacket;
    }

    public static OutPacket showOwnDiceEffect(int skillid, int effectid, int effectid2, int level) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(3);
        outPacket.encodeInt(effectid);
        if (GameConstants.GMS) { // JUMP之後才有
            outPacket.encodeInt(effectid2); //lol
        }
        outPacket.encodeInt(skillid);
        outPacket.encodeByte(level);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket showDiceEffect(int cid, int skillid, int effectid, int effectid2, int level) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(3);
        outPacket.encodeInt(effectid);
        outPacket.encodeInt(effectid2); //lol
        outPacket.encodeInt(skillid);
        outPacket.encodeByte(level);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket showItemLevelupEffect() {
        return showSpecialEffect(19); //bb +2
    }

    public static OutPacket showForeignItemLevelupEffect(int cid) {
        return showSpecialEffect(cid, 19); //bb +2
    }

    public static OutPacket showSpecialEffect(int effect) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(effect);

        return outPacket;
    }

    public static OutPacket showSpecialEffect(int cid, int effect) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(effect);

        return outPacket;
    }

    public static OutPacket updateSkill(int skillid, int level, int masterlevel, long expiration) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_SKILLS.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeByte(0);
        outPacket.encodeShort(1);
        outPacket.encodeInt(skillid);
        outPacket.encodeInt(level);
        outPacket.encodeInt(masterlevel);
        PacketHelper.addExpirationTime(outPacket, expiration);
        outPacket.encodeByte(4);

        return outPacket;
    }

    public static final OutPacket updateQuestMobKills(final MapleQuestStatus status) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeShort(status.getQuest().getId());
        outPacket.encodeByte(1);

        final StringBuilder sb = new StringBuilder();
        for (final int kills : status.getMobKills().values()) {
            sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
        }
        outPacket.encodeString(sb.toString());
        outPacket.encodeZeroBytes(8);

        return outPacket;
    }

    public static OutPacket getShowQuestCompletion(int id) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_QUEST_COMPLETION.getValue());
        outPacket.encodeShort(id);

        return outPacket;
    }

    public static OutPacket getKeymap(MapleKeyLayout layout) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.KEYMAP.getValue());

        layout.writeData(outPacket);

        return outPacket;
    }

    public static OutPacket petAutoHP(int itemId) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PET_AUTO_HP.getValue());
        outPacket.encodeInt(itemId);

        return outPacket;
    }

    public static OutPacket petAutoMP(int itemId) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PET_AUTO_MP.getValue());
        outPacket.encodeInt(itemId);

        return outPacket;
    }

    public static OutPacket getWhisper(String sender, int channel, String text) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.WHISPER.getValue());
        outPacket.encodeByte(0x12);
        outPacket.encodeString(sender);
        outPacket.encodeShort(channel - 1);
        outPacket.encodeString(text);

        return outPacket;
    }

    public static OutPacket getWhisperReply(String target, byte reply) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.WHISPER.getValue());
        outPacket.encodeByte(0x0A); // whisper?
        outPacket.encodeString(target);
        outPacket.encodeByte(reply);//  0x0 = cannot find char, 0x1 = success

        return outPacket;
    }

    public static OutPacket getFindReplyWithMap(String target, int mapid, final boolean buddy) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.WHISPER.getValue());
        outPacket.encodeByte(buddy ? 72 : 9);
        outPacket.encodeString(target);
        outPacket.encodeByte(1);
        outPacket.encodeInt(mapid);
        outPacket.encodeZeroBytes(8); // ?? official doesn't send zeros here but whatever

        return outPacket;
    }

    public static OutPacket getFindReply(String target, int channel, final boolean buddy) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.WHISPER.getValue());
        outPacket.encodeByte(buddy ? 72 : 9);
        outPacket.encodeString(target);
        outPacket.encodeByte(3);
        outPacket.encodeInt(channel - 1);

        return outPacket;
    }

    public static OutPacket getInventoryFull() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket getInventoryStatus() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket getShowInventoryFull() {
        return getShowInventoryStatus(0xff);
    }

    public static OutPacket showItemUnavailable() {
        return getShowInventoryStatus(0xfe);
    }

    public static OutPacket getShowInventoryStatus(int mode) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeByte(mode);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket getStorage(int npcId, byte slots, Collection<Item> items, int meso) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.OPEN_STORAGE.getValue());
        outPacket.encodeByte(0x15/*0x16*/);
        outPacket.encodeInt(npcId);
        outPacket.encodeByte(slots);
        outPacket.encodeShort(0x7E);
        outPacket.encodeShort(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(meso);
        outPacket.encodeShort(0);
        outPacket.encodeByte((byte) items.size());
        for (Item item : items) {
            PacketHelper.addItemInfo(outPacket, item, true, true);
        }
        outPacket.encodeShort(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket getStorageFull() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.OPEN_STORAGE.getValue());
        outPacket.encodeByte(0x11);

        return outPacket;
    }

    public static OutPacket mesoStorage(byte slots, int meso) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.OPEN_STORAGE.getValue());
        outPacket.encodeByte(0x13);
        outPacket.encodeByte(slots);
        outPacket.encodeShort(2);
        outPacket.encodeShort(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(meso);

        return outPacket;
    }

    public static OutPacket arrangeStorage(byte slots, Collection<Item> items, boolean changed) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.OPEN_STORAGE.getValue());
        outPacket.encodeByte(0x0F);
        outPacket.encodeByte(slots);
        outPacket.encodeByte(0x7C); //4 | 8 | 10 | 20 | 40
        outPacket.encodeZeroBytes(10);
        outPacket.encodeByte(items.size());
        for (Item item : items) {
            PacketHelper.addItemInfo(outPacket, item, true, true);
        }
        outPacket.encodeByte(0);
        return outPacket;
    }

    public static OutPacket storeStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.OPEN_STORAGE.getValue());
        outPacket.encodeByte(0x0D);
        outPacket.encodeByte(slots);
        outPacket.encodeShort(type.getBitfieldEncoding());
        outPacket.encodeShort(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(items.size());
        for (Item item : items) {
            PacketHelper.addItemInfo(outPacket, item, true, true);
        }
        return outPacket;
    }

    public static OutPacket takeOutStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.OPEN_STORAGE.getValue());
        outPacket.encodeByte(0x9);
        outPacket.encodeByte(slots);
        outPacket.encodeShort(type.getBitfieldEncoding());
        outPacket.encodeShort(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(items.size());
        for (Item item : items) {
            PacketHelper.addItemInfo(outPacket, item, true, true);
        }
        return outPacket;
    }

    public static OutPacket fairyPendantMessage(int type, int percent) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.FAIRY_PEND_MSG.getValue());
        outPacket.encodeShort(21); // 0x15
        outPacket.encodeInt(0); // idk
        outPacket.encodeShort(0); // idk
        outPacket.encodeShort(percent); // percent
        outPacket.encodeShort(0); // idk

        return outPacket;
    }

    public static OutPacket giveFameResponse(int mode, String charname, int newfame) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.FAME_RESPONSE.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeString(charname);
        outPacket.encodeByte(mode);
        outPacket.encodeInt(newfame);
        outPacket.encodeShort(0);

        return outPacket;
    }

    public static OutPacket giveFameErrorResponse(int status) {
        

        /*	* 0: ok, use giveFameResponse<br>
         * 1: the username is incorrectly entered<br>
         * 2: users under level 15 are unable to toggle with fame.<br>
         * 3: can't raise or drop fame anymore today.<br>
         * 4: can't raise or drop fame for this character for this month anymore.<br>
         * 5: received fame, use receiveFame()<br>
         * 6: level of fame neither has been raised nor dropped due to an unexpected error*/

        OutPacket outPacket = new OutPacket(SendPacketOpcode.FAME_RESPONSE.getValue());
        outPacket.encodeByte(status);

        return outPacket;
    }

    public static OutPacket receiveFame(int mode, String charnameFrom) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.FAME_RESPONSE.getValue());
        outPacket.encodeByte(5);
        outPacket.encodeString(charnameFrom);
        outPacket.encodeByte(mode);

        return outPacket;
    }

    public static OutPacket partyCreated(int partyid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PARTY_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 10 : 8);
        outPacket.encodeInt(partyid);
        outPacket.encodeInt(999999999);
        outPacket.encodeInt(999999999);
        outPacket.encodeLong(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket sidekickInvite(MapleCharacter from) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SIDEKICK_OPERATION.getValue());
        outPacket.encodeByte(0x41);
        outPacket.encodeInt(from.getId());
        outPacket.encodeString(from.getName());
        outPacket.encodeInt(from.getLevel());
        outPacket.encodeInt(from.getJob());
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket disbandSidekick(MapleSidekick s) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SIDEKICK_OPERATION.getValue());
        outPacket.encodeByte(0x4B);
        outPacket.encodeInt(s.getId());
        outPacket.encodeInt(s.getCharacter(0).getId());
        outPacket.encodeByte(0);
        outPacket.encodeInt(s.getCharacter(1).getId());

        return outPacket;
    }

    public static OutPacket updateSidekick(MapleCharacter first, MapleSidekick s, boolean f) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SIDEKICK_OPERATION.getValue());
        outPacket.encodeByte(f ? 0x4E : 0x46);
        final MapleSidekickCharacter second = s.getCharacter(s.getCharacter(0).getId() == first.getId() ? 1 : 0);
        final boolean online = first.getMap().getCharacterById(second.getId()) != null;
        outPacket.encodeInt(s.getId());
        if (f) {
            outPacket.encodeString(second.getName());
        }
        final List<String> msg = s.getSidekickMsg(online);
        outPacket.encodeInt(msg.size());
        for (String m : msg) {
            outPacket.encodeString(m);
        }

        outPacket.encodeInt(first.getId());
        outPacket.encodeInt(second.getId());

        outPacket.encodeString(first.getName(), 13);
        outPacket.encodeString(second.getName(), 13);

        outPacket.encodeInt(first.getJob());
        outPacket.encodeInt(second.getJobId());

        outPacket.encodeInt(first.getLevel());
        outPacket.encodeInt(second.getLevel());

        outPacket.encodeInt(first.getClient().getChannel() - 1);
        outPacket.encodeInt(online ? (first.getClient().getChannel() - 1) : 0);

        outPacket.encodeLong(0);

        outPacket.encodeInt(first.getId());
        if (f) {
            outPacket.encodeInt(first.getId());
        }
        outPacket.encodeInt(second.getId());
        if (!f) {
            outPacket.encodeInt(first.getId());
        }

        outPacket.encodeInt(first.getMapId());
        outPacket.encodeInt(online ? first.getMapId() : 999999999);

        outPacket.encodeInt(1); //??? random bytes after 1

        outPacket.encodeByte(Math.abs(first.getLevel() - second.getLevel()));
        outPacket.encodeInt(0); //can be 1 or 0
        outPacket.encodeInt(0); //time left til next buff
        outPacket.encodeInt(Integer.MAX_VALUE); //dunno, random
        outPacket.encodeInt(1); //can be 1 or 0
        //TODO FAMILIAR

        return outPacket;
    }

    public static OutPacket partyInvite(MapleCharacter from) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PARTY_OPERATION.getValue());
        outPacket.encodeByte(4);
        outPacket.encodeInt(from.getParty() == null ? 0 : from.getParty().getId());
        outPacket.encodeString(from.getName());
        outPacket.encodeInt(from.getLevel());
        outPacket.encodeInt(from.getJob());
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket partyRequestInvite(MapleCharacter from) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PARTY_OPERATION.getValue());
        outPacket.encodeByte(7);
        outPacket.encodeInt(from.getId());
        outPacket.encodeString(from.getName());
        outPacket.encodeInt(from.getLevel());
        outPacket.encodeInt(from.getJob());

        return outPacket;
    }

    public static OutPacket partyStatusMessage(int message) {
        

        /*	* 10: A beginner can't create a party.
         * 1/11/14/19: Your request for a party didn't work due to an unexpected error.
         * 13: You have yet to join a party.
         * 16: Already have joined a party.
         * 17: The party you're trying to join is already in full capacity.
         * 19: Unable to find the requested character in this channel.*/

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PARTY_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS && message >= 7 ? (message + 2) : message);

        return outPacket;
    }

    public static OutPacket partyStatusMessage(int message, String charname) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PARTY_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS && message >= 7 ? (message + 2) : message); // 23: 'Char' have denied request to the party.
        outPacket.encodeString(charname);

        return outPacket;
    }

    private static void addPartyStatus(int forchannel, MapleParty party, OutPacket outPacket, boolean leaving) {
        addPartyStatus(forchannel, party, outPacket, leaving, false);
    }

    private static void addPartyStatus(int forchannel, MapleParty party, OutPacket outPacket, boolean leaving, boolean exped) {
        List<MaplePartyCharacter> partymembers;
        if (party == null) {
            partymembers = new ArrayList<MaplePartyCharacter>();
        } else {
            partymembers = new ArrayList<MaplePartyCharacter>(party.getMembers());
        }
        while (partymembers.size() < 6) {
            partymembers.add(new MaplePartyCharacter());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            outPacket.encodeInt(partychar.getId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            outPacket.encodeString(partychar.getName(), 13);
        }
        for (MaplePartyCharacter partychar : partymembers) {
            outPacket.encodeInt(partychar.getJobId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            outPacket.encodeInt(partychar.getLevel());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.isOnline()) {
                outPacket.encodeInt(partychar.getChannel() - 1);
            } else {
                outPacket.encodeInt(-2);
            }
        }
        for (MaplePartyCharacter partychar : partymembers) {
            outPacket.encodeInt(0); //dunno, TODOO CHECK MSEA
        }
        outPacket.encodeInt(party == null ? 0 : party.getLeader().getId());
        if (exped) {
            return;
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel) {
                outPacket.encodeInt(partychar.getMapid());
            } else {
                outPacket.encodeInt(0);
            }
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel && !leaving) {
                outPacket.encodeInt(partychar.getDoorTown());
                outPacket.encodeInt(partychar.getDoorTarget());
                outPacket.encodeInt(partychar.getDoorSkill());
                outPacket.encodeInt(partychar.getDoorPosition().x);
                outPacket.encodeInt(partychar.getDoorPosition().y);
            } else {
                outPacket.encodeInt(leaving ? 999999999 : 0);
                outPacket.encodeLong(leaving ? 999999999 : 0);
                outPacket.encodeLong(leaving ? -1 : 0);
            }
        }
        //bb
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getId() > 0) { //exists
                outPacket.encodeInt(255);
            } else {
                outPacket.encodeInt(0);
            }
        }
        for (int i = 0; i < 4; i++) {
            outPacket.encodeLong(0);
        }
    }

    public static OutPacket updateParty(int forChannel, MapleParty party, PartyOperation op, MaplePartyCharacter target) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PARTY_OPERATION.getValue());
        switch (op) {
            case DISBAND:
            case EXPEL:
            case LEAVE:
                outPacket.encodeByte(GameConstants.GMS ? 0xE : 0xC);
                outPacket.encodeInt(party.getId());
                outPacket.encodeInt(target.getId());
                outPacket.encodeByte(op == PartyOperation.DISBAND ? 0 : 1);
                if (op == PartyOperation.DISBAND) {
                    outPacket.encodeInt(target.getId());
                } else {
                    outPacket.encodeByte(op == PartyOperation.EXPEL ? 1 : 0);
                    outPacket.encodeString(target.getName());
                    addPartyStatus(forChannel, party, outPacket, op == PartyOperation.LEAVE);
                }
                break;
            case JOIN:
                outPacket.encodeByte(GameConstants.GMS ? 0x11 : 0xF);
                outPacket.encodeInt(party.getId());
                outPacket.encodeString(target.getName());
                addPartyStatus(forChannel, party, outPacket, false);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                outPacket.encodeByte(GameConstants.GMS ? 0x9 : 0x7);
                outPacket.encodeInt(party.getId());
                addPartyStatus(forChannel, party, outPacket, op == PartyOperation.LOG_ONOFF);
                break;
            case CHANGE_LEADER:
            case CHANGE_LEADER_DC:
                outPacket.encodeByte(GameConstants.GMS ? 0x21 : 0x1F); //test
                outPacket.encodeInt(target.getId());
                outPacket.encodeByte(op == PartyOperation.CHANGE_LEADER_DC ? 1 : 0);
                break;
            //1D = expel function not available in this map.
        }
        return outPacket;
    }

    public static OutPacket partyPortal(int townId, int targetId, int skillId, Point position, boolean animation) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PARTY_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x2F : 0x2D);
        outPacket.encodeByte(animation ? 0 : 1);
        outPacket.encodeInt(townId);
        outPacket.encodeInt(targetId);
        outPacket.encodeInt(skillId);
        outPacket.encodePosition(position);

        return outPacket;
    }

    public static OutPacket updatePartyMemberHP(int cid, int curhp, int maxhp) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_PARTYMEMBER_HP.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(curhp);
        outPacket.encodeInt(maxhp);

        return outPacket;
    }

    public static OutPacket multiChat(String name, String chattext, int mode) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MULTICHAT.getValue());
        outPacket.encodeByte(mode); //  0 buddychat; 1 partychat; 2 guildchat
        outPacket.encodeString(name);
        outPacket.encodeString(chattext);

        return outPacket;
    }

    public static OutPacket getClock(int time) { // time in seconds
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CLOCK.getValue());
        outPacket.encodeByte(2); // clock type. if you send 3 here you have to send another byte (which does not matter at all) before the timestamp
        outPacket.encodeInt(time);

        return outPacket;
    }

    public static OutPacket getClockTime(int hour, int min, int sec) { // Current Time
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CLOCK.getValue());
        outPacket.encodeByte(1); //Clock-Type
        outPacket.encodeByte(hour);
        outPacket.encodeByte(min);
        outPacket.encodeByte(sec);

        return outPacket;
    }

    public static OutPacket spawnMist(final MapleMist mist) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_MIST.getValue());
        outPacket.encodeInt(mist.getObjectId());
        outPacket.encodeInt(mist.isMobMist() ? 0 : mist.isPoisonMist()); //2 = invincible, so put 1 for recovery aura
        outPacket.encodeInt(mist.getOwnerId());
        if (mist.getMobSkill() == null) {
            outPacket.encodeInt(mist.getSourceSkill().getId());
        } else {
            outPacket.encodeInt(mist.getMobSkill().getSkillId());
        }
        outPacket.encodeByte(mist.getSkillLevel());
        outPacket.encodeShort(mist.getSkillDelay());
        outPacket.encodeRectInt(mist.getBox());
        outPacket.encodeInt(0);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeInt(0);
        return outPacket;
    }

    public static OutPacket removeMist(final int oid, final boolean eruption) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.REMOVE_MIST.getValue());
        outPacket.encodeInt(oid);
        outPacket.encodeByte(eruption ? 1 : 0);

        return outPacket;
    }

    public static OutPacket damageSummon(int cid, int summonSkillId, int damage, int unkByte, int monsterIdFrom) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DAMAGE_SUMMON.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(summonSkillId);
        outPacket.encodeByte(unkByte);
        outPacket.encodeInt(damage);
        outPacket.encodeInt(monsterIdFrom);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket buddylistMessage(byte message) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BUDDYLIST.getValue());
        outPacket.encodeByte(message);

        return outPacket;
    }

    public static OutPacket updateBuddylist(Collection<BuddylistEntry> buddylist) {
        return updateBuddylist(buddylist, 7);
    }

    public static OutPacket updateBuddylist(Collection<BuddylistEntry> buddylist, int deleted) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BUDDYLIST.getValue());
        outPacket.encodeByte(deleted);
        outPacket.encodeByte(buddylist.size());

        for (BuddylistEntry buddy : buddylist) {
            outPacket.encodeInt(buddy.getCharacterId());
            outPacket.encodeString(buddy.getName(), 13);
            outPacket.encodeByte(buddy.isVisible() ? 0 : 1);
            outPacket.encodeInt(buddy.getChannel() == -1 ? -1 : (buddy.getChannel() - 1));
            outPacket.encodeString(buddy.getGroup(), 17);
        }
        for (int x = 0; x < buddylist.size(); x++) {
            outPacket.encodeInt(0);
        }
        return outPacket;
    }

    public static OutPacket requestBuddylistAdd(int cidFrom, String nameFrom, int levelFrom, int jobFrom) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BUDDYLIST.getValue());
        outPacket.encodeByte(9);
        outPacket.encodeInt(cidFrom);
        outPacket.encodeString(nameFrom);
        outPacket.encodeInt(levelFrom);
        outPacket.encodeInt(jobFrom);
        outPacket.encodeInt(cidFrom);
        outPacket.encodeString(nameFrom, 13);
        outPacket.encodeByte(1);
        outPacket.encodeInt(0);
        outPacket.encodeString("ETC", 16);
        outPacket.encodeShort(1);

        return outPacket;
    }

    public static OutPacket updateBuddyChannel(int characterid, int channel) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BUDDYLIST.getValue());
        outPacket.encodeByte(0x14);
        outPacket.encodeInt(characterid);
        outPacket.encodeByte(0);
        outPacket.encodeInt(channel);

        return outPacket;
    }

    public static OutPacket itemEffect(int characterid, int itemid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_EFFECT.getValue());
        outPacket.encodeInt(characterid);
        outPacket.encodeInt(itemid);

        return outPacket;
    }

    public static OutPacket updateBuddyCapacity(int capacity) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BUDDYLIST.getValue());
        outPacket.encodeByte(0x15);
        outPacket.encodeByte(capacity);

        return outPacket;
    }

    public static OutPacket showChair(int characterid, int itemid) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_CHAIR.getValue());
        outPacket.encodeInt(characterid);
        outPacket.encodeInt(itemid);
        outPacket.encodeInt(0); // ?

        return outPacket;
    }

    public static OutPacket cancelChair(int id) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CANCEL_CHAIR.getValue());
        if (id == -1) {
            outPacket.encodeByte(0);
        } else {
            outPacket.encodeByte(1);
            outPacket.encodeShort(id);
        }
        return outPacket;
    }

    public static OutPacket spawnReactor(MapleReactor reactor) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.REACTOR_SPAWN.getValue());
        outPacket.encodeInt(reactor.getObjectId());
        outPacket.encodeInt(reactor.getReactorId());
        outPacket.encodeByte(reactor.getState());
        outPacket.encodePosition(reactor.getTruePosition());
        outPacket.encodeByte(reactor.getFacingDirection()); // stance
        outPacket.encodeString(reactor.getName());

        return outPacket;
    }

    public static OutPacket triggerReactor(MapleReactor reactor, int stance) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.REACTOR_HIT.getValue());
        outPacket.encodeInt(reactor.getObjectId());
        outPacket.encodeByte(reactor.getState());
        outPacket.encodePosition(reactor.getTruePosition());
        outPacket.encodeInt(stance);
        return outPacket;
    }

    public static OutPacket destroyReactor(MapleReactor reactor) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.REACTOR_DESTROY.getValue());
        outPacket.encodeInt(reactor.getObjectId());
        outPacket.encodeByte(reactor.getState());
        outPacket.encodePosition(reactor.getPosition());

        return outPacket;
    }

    public static OutPacket musicChange(String song) {
        return environmentChange(song, 6);
    }

    public static OutPacket showEffect(String effect) {
        return environmentChange(effect, 3);
    }

    public static OutPacket playSound(String sound) {
        return environmentChange(sound, 4);
    }

    public static OutPacket environmentChange(String env, int mode) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BOSS_ENV.getValue());
        outPacket.encodeByte(mode);
        outPacket.encodeString(env);

        return outPacket;
    }

    public static OutPacket environmentMove(String env, int mode) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MOVE_ENV.getValue());
        outPacket.encodeString(env);
        outPacket.encodeInt(mode);

        return outPacket;
    }

    public static OutPacket startMapEffect(String msg, int itemid, boolean active) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MAP_EFFECT.getValue());
//        outPacket.encodeByte(active ? 0 : 1);
        outPacket.encodeInt(itemid);
        if (active) {
            outPacket.encodeString(msg);
        }
        return outPacket;
    }

    public static OutPacket removeMapEffect() {
        return startMapEffect(null, 0, false);
    }

    public static OutPacket showGuildInfo(MapleCharacter c) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x20); //signature for showing guild info - 0x20 aftershock

        if (c == null || c.getMGC() == null) { //show empty guild (used for leaving, expelled)
            outPacket.encodeByte(0);
            return outPacket;
        }
        MapleGuild g = World.Guild.getGuild(c.getGuildId());
        if (g == null) { //failed to read from DB - don't show a guild
            outPacket.encodeByte(0);
            return outPacket;
        }
        outPacket.encodeByte(1); //bInGuild
        getGuildInfo(outPacket, g);

        return outPacket;
    }

    private static void getGuildInfo(OutPacket outPacket, MapleGuild guild) {
        outPacket.encodeInt(guild.getId());
        outPacket.encodeString(guild.getName());
        for (int i = 1; i <= 5; i++) {
            outPacket.encodeString(guild.getRankTitle(i));
        }
        guild.addMemberData(outPacket);
        outPacket.encodeInt(guild.getCapacity());
        outPacket.encodeShort(guild.getLogoBG());
        outPacket.encodeByte(guild.getLogoBGColor());
        outPacket.encodeShort(guild.getLogo());
        outPacket.encodeByte(guild.getLogoColor());
        outPacket.encodeString(guild.getNotice());
        outPacket.encodeInt(guild.getGP()); //written twice, aftershock?
        outPacket.encodeInt(guild.getGP());
        outPacket.encodeInt(guild.getAllianceId() > 0 ? guild.getAllianceId() : 0);
        outPacket.encodeByte(guild.getLevel());
        outPacket.encodeShort(0); //probably guild rank or somethin related, appears to be 0
        outPacket.encodeShort(guild.getSkills().size()); //AFTERSHOCK: uncomment
        for (MapleGuildSkill i : guild.getSkills()) {
            outPacket.encodeInt(i.skillID);
            outPacket.encodeShort(i.level);
            outPacket.encodeLong(PacketHelper.getTime(i.timestamp));
            outPacket.encodeString(i.purchaser);
            outPacket.encodeString(i.activator);
        }

    }

    public static OutPacket guildSkillPurchased(int gid, int sid, int level, long expiration, String purchase, String activate) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x55); //0x55 aftershock
        outPacket.encodeInt(gid);
        outPacket.encodeInt(sid);
        outPacket.encodeShort(level);
        outPacket.encodeLong(PacketHelper.getTime(expiration));
        outPacket.encodeString(purchase);
        outPacket.encodeString(activate);

        return outPacket;
    }

    public static OutPacket guildLeaderChanged(int gid, int oldLeader, int newLeader, int allianceId) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x59); //0x59 aftershock
        outPacket.encodeInt(gid);
        //01 36 00 00 00
        outPacket.encodeInt(oldLeader);
        outPacket.encodeInt(newLeader);
        outPacket.encodeByte(1); //new rank lol
        outPacket.encodeInt(allianceId);


        return outPacket;
    }

    public static OutPacket guildMemberOnline(int gid, int cid, boolean bOnline) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x43);
        outPacket.encodeInt(gid);
        outPacket.encodeInt(cid);
        outPacket.encodeByte(bOnline ? 1 : 0);

        return outPacket;
    }

    public static OutPacket guildContribution(int gid, int cid, int c) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x48);
        outPacket.encodeInt(gid);
        outPacket.encodeInt(cid);
        outPacket.encodeInt(c);

        return outPacket;
    }

    public static OutPacket guildInvite(int gid, String charName, int levelFrom, int jobFrom) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x05);
        outPacket.encodeInt(gid);
        outPacket.encodeString(charName);
        outPacket.encodeInt(levelFrom);
        outPacket.encodeInt(jobFrom);

        return outPacket;
    }

    public static OutPacket denyGuildInvitation(String charname) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x3D);
        outPacket.encodeString(charname);

        return outPacket;
    }

    public static OutPacket genericGuildMessage(byte code) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(code);

        return outPacket;
    }

    public static OutPacket newGuildMember(MapleGuildCharacter mgc) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x2D);
        outPacket.encodeInt(mgc.getGuildId());
        outPacket.encodeInt(mgc.getId());
        outPacket.encodeString(mgc.getName(), 13);
        outPacket.encodeInt(mgc.getJobId());
        outPacket.encodeInt(mgc.getLevel());
        outPacket.encodeInt(mgc.getGuildRank()); //should be always 5 but whatevs
        outPacket.encodeInt(mgc.isOnline() ? 1 : 0); //should always be 1 too
        outPacket.encodeInt(mgc.getAllianceRank()); //? could be guild signature, but doesn't seem to matter
        outPacket.encodeInt(mgc.getGuildContribution()); //should always 3

        return outPacket;
    }

    //someone leaving, mode == 0x2c for leaving, 0x2f for expelled
    public static OutPacket memberLeft(MapleGuildCharacter mgc, boolean bExpelled) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(bExpelled ? 0x35 : 0x33);

        outPacket.encodeInt(mgc.getGuildId());
        outPacket.encodeInt(mgc.getId());
        outPacket.encodeString(mgc.getName());

        return outPacket;
    }

    public static OutPacket changeRank(MapleGuildCharacter mgc) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x46); //+4 aftershock
        outPacket.encodeInt(mgc.getGuildId());
        outPacket.encodeInt(mgc.getId());
        outPacket.encodeByte(mgc.getGuildRank());

        return outPacket;
    }

    public static OutPacket guildNotice(int gid, String notice) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x4B);
        outPacket.encodeInt(gid);
        outPacket.encodeString(notice);

        return outPacket;
    }

    public static OutPacket guildMemberLevelJobUpdate(MapleGuildCharacter mgc) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x42);
        outPacket.encodeInt(mgc.getGuildId());
        outPacket.encodeInt(mgc.getId());
        outPacket.encodeInt(mgc.getLevel());
        outPacket.encodeInt(mgc.getJobId());

        return outPacket;
    }

    public static OutPacket rankTitleChange(int gid, String[] ranks) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x44);
        outPacket.encodeInt(gid);

        for (String r : ranks) {
            outPacket.encodeString(r);
        }
        return outPacket;
    }

    public static OutPacket guildDisband(int gid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x38);
        outPacket.encodeInt(gid);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket guildEmblemChange(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x49);
        outPacket.encodeInt(gid);
        outPacket.encodeShort(bg);
        outPacket.encodeByte(bgcolor);
        outPacket.encodeShort(logo);
        outPacket.encodeByte(logocolor);

        return outPacket;
    }

    public static OutPacket guildCapacityChange(int gid, int capacity) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x40);
        outPacket.encodeInt(gid);
        outPacket.encodeByte(capacity);

        return outPacket;
    }

    public static OutPacket removeGuildFromAlliance(MapleGuildAlliance alliance, MapleGuild expelledGuild, boolean expelled) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x10);
        addAllianceInfo(outPacket, alliance);
        getGuildInfo(outPacket, expelledGuild);
        outPacket.encodeByte(expelled ? 1 : 0); //1 = expelled, 0 = left
        return outPacket;
    }

    public static OutPacket changeAlliance(MapleGuildAlliance alliance, final boolean in) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x01);
        outPacket.encodeByte(in ? 1 : 0);
        outPacket.encodeInt(in ? alliance.getId() : 0);
        final int noGuilds = alliance.getNoGuilds();
        MapleGuild[] g = new MapleGuild[noGuilds];
        for (int i = 0; i < noGuilds; i++) {
            g[i] = World.Guild.getGuild(alliance.getGuildId(i));
            if (g[i] == null) {
                return enableActions();
            }
        }
        outPacket.encodeByte(noGuilds);
        for (int i = 0; i < noGuilds; i++) {
            outPacket.encodeInt(g[i].getId());
            //must be world
            Collection<MapleGuildCharacter> members = g[i].getMembers();
            outPacket.encodeInt(members.size());
            for (MapleGuildCharacter mgc : members) {
                outPacket.encodeInt(mgc.getId());
                outPacket.encodeByte(in ? mgc.getAllianceRank() : 0);
            }
        }
        return outPacket;
    }

    public static OutPacket changeAllianceLeader(int allianceid, int newLeader, int oldLeader) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x02);
        outPacket.encodeInt(allianceid);
        outPacket.encodeInt(oldLeader);
        outPacket.encodeInt(newLeader);
        return outPacket;
    }

    public static OutPacket updateAllianceLeader(int allianceid, int newLeader, int oldLeader) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x19);
        outPacket.encodeInt(allianceid);
        outPacket.encodeInt(oldLeader);
        outPacket.encodeInt(newLeader);
        return outPacket;
    }

    public static OutPacket sendAllianceInvite(String allianceName, MapleCharacter inviter) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x03);
        outPacket.encodeInt(inviter.getGuildId());
        outPacket.encodeString(inviter.getName());
        //alliance invite did NOT change
        outPacket.encodeString(allianceName);
        return outPacket;
    }

    public static OutPacket changeGuildInAlliance(MapleGuildAlliance alliance, MapleGuild guild, final boolean add) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x04);
        outPacket.encodeInt(add ? alliance.getId() : 0);
        outPacket.encodeInt(guild.getId());
        Collection<MapleGuildCharacter> members = guild.getMembers();
        outPacket.encodeInt(members.size());
        for (MapleGuildCharacter mgc : members) {
            outPacket.encodeInt(mgc.getId());
            outPacket.encodeByte(add ? mgc.getAllianceRank() : 0);
        }
        return outPacket;
    }

    public static OutPacket changeAllianceRank(int allianceid, MapleGuildCharacter player) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x05);
        outPacket.encodeInt(allianceid);
        outPacket.encodeInt(player.getId());
        outPacket.encodeInt(player.getAllianceRank());
        return outPacket;
    }

    public static OutPacket createGuildAlliance(MapleGuildAlliance alliance) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x0F);
        addAllianceInfo(outPacket, alliance);
        final int noGuilds = alliance.getNoGuilds();
        MapleGuild[] g = new MapleGuild[noGuilds];
        for (int i = 0; i < alliance.getNoGuilds(); i++) {
            g[i] = World.Guild.getGuild(alliance.getGuildId(i));
            if (g[i] == null) {
                return enableActions();
            }
        }
        for (MapleGuild gg : g) {
            getGuildInfo(outPacket, gg);
        }
        return outPacket;
    }

    public static OutPacket getAllianceInfo(MapleGuildAlliance alliance) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x0C);
        outPacket.encodeByte(alliance == null ? 0 : 1); //in an alliance
        if (alliance != null) {
            addAllianceInfo(outPacket, alliance);
        }
        return outPacket;
    }

    public static OutPacket getAllianceUpdate(MapleGuildAlliance alliance) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x17);
        addAllianceInfo(outPacket, alliance);
        return outPacket;
    }

    public static OutPacket getGuildAlliance(MapleGuildAlliance alliance) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x0D);
        if (alliance == null) {
            outPacket.encodeInt(0);
            return outPacket;
        }
        final int noGuilds = alliance.getNoGuilds();
        MapleGuild[] g = new MapleGuild[noGuilds];
        for (int i = 0; i < alliance.getNoGuilds(); i++) {
            g[i] = World.Guild.getGuild(alliance.getGuildId(i));
            if (g[i] == null) {
                return enableActions();
            }
        }
        outPacket.encodeInt(noGuilds);
        for (MapleGuild gg : g) {
            getGuildInfo(outPacket, gg);
        }
        return outPacket;
    }

    public static OutPacket addGuildToAlliance(MapleGuildAlliance alliance, MapleGuild newGuild) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x12);
        addAllianceInfo(outPacket, alliance);
        outPacket.encodeInt(newGuild.getId()); //???
        getGuildInfo(outPacket, newGuild);
        outPacket.encodeByte(0); //???
        return outPacket;
    }

    private static void addAllianceInfo(OutPacket outPacket, MapleGuildAlliance alliance) {
        outPacket.encodeInt(alliance.getId());
        outPacket.encodeString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            outPacket.encodeString(alliance.getRank(i));
        }
        outPacket.encodeByte(alliance.getNoGuilds());
        for (int i = 0; i < alliance.getNoGuilds(); i++) {
            outPacket.encodeInt(alliance.getGuildId(i));
        }
        outPacket.encodeInt(alliance.getCapacity()); // ????
        outPacket.encodeString(alliance.getNotice());
    }

    public static OutPacket allianceMemberOnline(int alliance, int gid, int id, boolean online) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x0E);
        outPacket.encodeInt(alliance);
        outPacket.encodeInt(gid);
        outPacket.encodeInt(id);
        outPacket.encodeByte(online ? 1 : 0);

        return outPacket;
    }

    public static OutPacket updateAlliance(MapleGuildCharacter mgc, int allianceid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x18);
        outPacket.encodeInt(allianceid);
        outPacket.encodeInt(mgc.getGuildId());
        outPacket.encodeInt(mgc.getId());
        outPacket.encodeInt(mgc.getLevel());
        outPacket.encodeInt(mgc.getJobId());

        return outPacket;
    }

    public static OutPacket updateAllianceRank(int allianceid, MapleGuildCharacter mgc) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x1B);
        outPacket.encodeInt(allianceid);
        outPacket.encodeInt(mgc.getId());
        outPacket.encodeInt(mgc.getAllianceRank());

        return outPacket;
    }

    public static OutPacket disbandAlliance(int alliance) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        outPacket.encodeByte(0x1D);
        outPacket.encodeInt(alliance);

        return outPacket;
    }

    public static OutPacket BBSThreadList(final List<MapleBBSThread> bbs, int start) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BBS_OPERATION.getValue());
        outPacket.encodeByte(6);

        if (bbs == null) {
            outPacket.encodeByte(0);
            outPacket.encodeLong(0);
            return outPacket;
        }
        int threadCount = bbs.size();
        MapleBBSThread notice = null;
        for (MapleBBSThread b : bbs) {
            if (b.isNotice()) { //notice
                notice = b;
                break;
            }
        }
        outPacket.encodeByte(notice == null ? 0 : 1);
        if (notice != null) { //has a notice
            addThread(outPacket, notice);
        }
        if (threadCount < start) { //seek to the thread before where we start
            //uh, we're trying to start at a place past possible
            start = 0;
        }
        //each page has 10 threads, start = page # in packet but not here
        outPacket.encodeInt(threadCount);
        final int pages = Math.min(10, threadCount - start);
        outPacket.encodeInt(pages);

        for (int i = 0; i < pages; i++) {
            addThread(outPacket, bbs.get(start + i)); //because 0 = notice
        }
        return outPacket;
    }

    private static void addThread(OutPacket outPacket, MapleBBSThread rs) {
        outPacket.encodeInt(rs.localthreadID);
        outPacket.encodeInt(rs.ownerID);
        outPacket.encodeString(rs.name);
        outPacket.encodeLong(PacketHelper.getKoreanTimestamp(rs.timestamp));
        outPacket.encodeInt(rs.icon);
        outPacket.encodeInt(rs.getReplyCount());
    }

    public static OutPacket showThread(MapleBBSThread thread) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BBS_OPERATION.getValue());
        outPacket.encodeByte(7);

        outPacket.encodeInt(thread.localthreadID);
        outPacket.encodeInt(thread.ownerID);
        outPacket.encodeLong(PacketHelper.getKoreanTimestamp(thread.timestamp));
        outPacket.encodeString(thread.name);
        outPacket.encodeString(thread.text);
        outPacket.encodeInt(thread.icon);
        outPacket.encodeInt(thread.getReplyCount());
        for (MapleBBSReply reply : thread.replies.values()) {
            outPacket.encodeInt(reply.replyid);
            outPacket.encodeInt(reply.ownerID);
            outPacket.encodeLong(PacketHelper.getKoreanTimestamp(reply.timestamp));
            outPacket.encodeString(reply.content);
        }
        return outPacket;
    }

    public static OutPacket showGuildRanks(int npcid, List<GuildRankingInfo> all) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x50);
        outPacket.encodeInt(npcid);
        //this error 38s and official servers have it removed
        outPacket.encodeInt(all.size());

        for (GuildRankingInfo info : all) {
            outPacket.encodeString(info.getName());
            outPacket.encodeInt(info.getGP());
			outPacket.encodeInt(info.getGP());
            outPacket.encodeInt(info.getLogo());
            outPacket.encodeInt(info.getLogoColor());
            outPacket.encodeInt(info.getLogoBg());
            outPacket.encodeInt(info.getLogoBgColor());
        }

        return outPacket;
    }

    public static OutPacket updateGP(int gid, int GP, int glevel) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GUILD_OPERATION.getValue());
        outPacket.encodeByte(0x4F);
        outPacket.encodeInt(gid);
        outPacket.encodeInt(GP); //2nd int = guild level or something
        outPacket.encodeInt(glevel);

        return outPacket;
    }

    public static OutPacket skillEffect(MapleCharacter from, int skillId, byte level, byte flags, byte speed, byte unk) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SKILL_EFFECT.getValue());
        outPacket.encodeInt(from.getId());
        outPacket.encodeInt(skillId);
        outPacket.encodeByte(level);
        outPacket.encodeByte(flags);
        outPacket.encodeByte(speed);
        outPacket.encodeByte(unk); // Direction ??

        return outPacket;
    }

    public static OutPacket skillCancel(MapleCharacter from, int skillId) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CANCEL_SKILL_EFFECT.getValue());
        outPacket.encodeInt(from.getId());
        outPacket.encodeInt(skillId);

        return outPacket;
    }

    public static OutPacket showMagnet(int mobid, byte success) { // Monster Magnet
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_MAGNET.getValue());
        outPacket.encodeInt(mobid);
        outPacket.encodeByte(success);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket sendHint(String hint, int width, int height) {
        

        if (width < 1) {
            width = hint.length() * 10;
            if (width < 40) {
                width = 40;
            }
        }
        if (height < 5) {
            height = 5;
        }
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_HINT.getValue());
        outPacket.encodeString(hint);
        outPacket.encodeShort(width);
        outPacket.encodeShort(height);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket messengerInvite(String from, int messengerid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MESSENGER.getValue());
        outPacket.encodeByte(0x03);
        outPacket.encodeString(from);
        outPacket.encodeByte(0x00);
        outPacket.encodeInt(messengerid);
        outPacket.encodeByte(0x00);

        return outPacket;
    }

    public static OutPacket addMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MESSENGER.getValue());
        outPacket.encodeByte(0x00);
        outPacket.encodeByte(position);
        PacketHelper.addCharLook(outPacket, chr, true);
        outPacket.encodeString(from);
        outPacket.encodeShort(channel);

        return outPacket;
    }

    public static OutPacket removeMessengerPlayer(int position) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MESSENGER.getValue());
        outPacket.encodeByte(0x02);
        outPacket.encodeByte(position);

        return outPacket;
    }

    public static OutPacket updateMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MESSENGER.getValue());
        outPacket.encodeByte(0x07);
        outPacket.encodeByte(position);
        PacketHelper.addCharLook(outPacket, chr, true);
        outPacket.encodeString(from);
        outPacket.encodeShort(channel);

        return outPacket;
    }

    public static OutPacket joinMessenger(int position) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MESSENGER.getValue());
        outPacket.encodeByte(0x01);
        outPacket.encodeByte(position);

        return outPacket;
    }

    public static OutPacket messengerChat(String text) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MESSENGER.getValue());
        outPacket.encodeByte(0x06);
        outPacket.encodeString(text);

        return outPacket;
    }

    public static OutPacket messengerNote(String text, int mode, int mode2) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MESSENGER.getValue());
        outPacket.encodeByte(mode);
        outPacket.encodeString(text);
        outPacket.encodeByte(mode2);

        return outPacket;
    }

    public static OutPacket getFindReplyWithCS(String target, final boolean buddy) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.WHISPER.getValue());
        outPacket.encodeByte(buddy ? 72 : 9);
        outPacket.encodeString(target);
        outPacket.encodeByte(2);
        outPacket.encodeInt(-1);

        return outPacket;
    }

    public static OutPacket getFindReplyWithMTS(String target, final boolean buddy) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.WHISPER.getValue());
        outPacket.encodeByte(buddy ? 72 : 9);
        outPacket.encodeString(target);
        outPacket.encodeByte(0);
        outPacket.encodeInt(-1);

        return outPacket;
    }

    public static OutPacket showEquipEffect() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_EQUIP_EFFECT.getValue());

        return outPacket;
    }

    public static OutPacket showEquipEffect(int team) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_EQUIP_EFFECT.getValue());
        outPacket.encodeShort(team);
        return outPacket;
    }

    public static OutPacket summonSkill(int cid, int summonSkillId, int newStance) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SUMMON_SKILL.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(summonSkillId);
        outPacket.encodeByte(newStance);

        return outPacket;
    }

    public static OutPacket skillCooldown(int sid, int time) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.COOLDOWN.getValue());
        outPacket.encodeInt(sid);
        outPacket.encodeInt(time);

        return outPacket;
    }

    public static OutPacket useSkillBook(MapleCharacter chr, int skillid, int maxlevel, boolean canuse, boolean success) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.USE_SKILL_BOOK.getValue());
        outPacket.encodeByte(0); //?
        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(1);
        outPacket.encodeInt(skillid);
        outPacket.encodeInt(maxlevel);
        outPacket.encodeByte(canuse ? 1 : 0);
        outPacket.encodeByte(success ? 1 : 0);

        return outPacket;
    }

    public static OutPacket getMacros(SkillMacro[] macros) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SKILL_MACRO.getValue());
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (macros[i] != null) {
                count++;
            }
        }
        outPacket.encodeByte(count); // number of macros
        for (int i = 0; i < 5; i++) {
            SkillMacro macro = macros[i];
            if (macro != null) {
                outPacket.encodeString(macro.getName());
                outPacket.encodeByte(macro.getShout());
                outPacket.encodeInt(macro.getSkill1());
                outPacket.encodeInt(macro.getSkill2());
                outPacket.encodeInt(macro.getSkill3());
            }
        }
        return outPacket;
    }

    public static OutPacket updateAriantPQRanking(String name, int score, boolean empty) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ARIANT_PQ_START.getValue());
        outPacket.encodeByte(empty ? 0 : 1);
        if (!empty) {
            outPacket.encodeString(name);
            outPacket.encodeInt(score);
        }
        return outPacket;
    }

    public static OutPacket catchMonster(int mobid, int itemid, byte success) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CATCH_MONSTER.getValue());
        outPacket.encodeInt(mobid);
        outPacket.encodeInt(itemid);
        outPacket.encodeByte(success);

        return outPacket;
    }

    public static OutPacket catchMob(int mobid, int itemid, byte success) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CATCH_MOB.getValue());
        outPacket.encodeByte(success);
        outPacket.encodeInt(itemid);
        outPacket.encodeInt(mobid);


        return outPacket;
    }

    public static OutPacket showAriantScoreBoard() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ARIANT_SCOREBOARD.getValue());

        return outPacket;
    }

    public static OutPacket boatPacket(int effect) {
        

        // 1034: balrog boat comes, 1548: boat comes, 3: boat leaves
        OutPacket outPacket = new OutPacket(SendPacketOpcode.BOAT_EFFECT.getValue());
        outPacket.encodeShort(effect); // 0A 04 balrog
        //this packet had 3: boat leaves

        return outPacket;
    }

    public static OutPacket boatEffect(int effect) {
        

        // 1034: balrog boat comes, 1548: boat comes, 3: boat leaves
        OutPacket outPacket = new OutPacket(SendPacketOpcode.BOAT_EFF.getValue());
        outPacket.encodeShort(effect); // 0A 04 balrog
        //this packet had the other ones o.o

        return outPacket;
    }

    public static OutPacket removeItemFromDuey(boolean remove, int Package) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DUEY.getValue());
        outPacket.encodeByte(0x18);
        outPacket.encodeInt(Package);
        outPacket.encodeByte(remove ? 3 : 4);

        return outPacket;
    }

    public static OutPacket sendDuey(byte operation, List<MapleDueyActions> packages) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DUEY.getValue());
        outPacket.encodeByte(operation);

        switch (operation) {
            case 9: { // Request 13 Digit AS
                outPacket.encodeByte(1);
                // 0xFF = error
                break;
            }
            case 10: { // Open duey
                outPacket.encodeByte(0);
                outPacket.encodeByte(packages.size());

                for (MapleDueyActions dp : packages) {
                    outPacket.encodeInt(dp.getPackageId());
                    outPacket.encodeString(dp.getSender(), 13);
                    outPacket.encodeInt(dp.getMesos());
                    outPacket.encodeLong(PacketHelper.getTime(dp.getSentTime()));
                    outPacket.encodeZeroBytes(205);

                    if (dp.getItem() != null) {
                        outPacket.encodeByte(1);
                        PacketHelper.addItemInfo(outPacket, dp.getItem(), true, true);
                    } else {
                        outPacket.encodeByte(0);
                    }
                    //System.out.println("Package has been sent in packet: " + dp.getPackageId());
                }
                outPacket.encodeByte(0);
                break;
            }
        }
        return outPacket;
    }

    public static OutPacket Mulung_DojoUp2() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(9); //AFTERSHOCK: 10? MAYBE

        return outPacket;
    }

    public static OutPacket showQuestMsg(final String msg) {
        return serverNotice(5, msg);
    }

    public static OutPacket Mulung_Pts(int recv, int total) {
        return showQuestMsg("You have received " + recv + " training points, for the accumulated total of " + total + " training points.");
    }

    public static OutPacket showOXQuiz(int questionSet, int questionId, boolean askQuestion) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.OX_QUIZ.getValue());
        outPacket.encodeByte(askQuestion ? 1 : 0);
        outPacket.encodeByte(questionSet);
        outPacket.encodeShort(questionId);
        return outPacket;
    }

    public static OutPacket leftKnockBack() {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.LEFT_KNOCK_BACK.getValue());
        return outPacket;
    }

    public static OutPacket rollSnowball(int type, MapleSnowballs ball1, MapleSnowballs ball2) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.ROLL_SNOWBALL.getValue());
        outPacket.encodeByte(type); // 0 = normal, 1 = rolls from start to end, 2 = down disappear, 3 = up disappear, 4 = move
        outPacket.encodeInt(ball1 == null ? 0 : (ball1.getSnowmanHP() / 75));
        outPacket.encodeInt(ball2 == null ? 0 : (ball2.getSnowmanHP() / 75));
        outPacket.encodeShort(ball1 == null ? 0 : ball1.getPosition());
        outPacket.encodeByte(0);
        outPacket.encodeShort(ball2 == null ? 0 : ball2.getPosition());
        outPacket.encodeZeroBytes(11);
        return outPacket;
    }

    public static OutPacket enterSnowBall() {
        return rollSnowball(0, null, null);
    }

    public static OutPacket hitSnowBall(int team, int damage, int distance, int delay) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.HIT_SNOWBALL.getValue());
        outPacket.encodeByte(team);// 0 is down, 1 is up
        outPacket.encodeShort(damage);
        outPacket.encodeByte(distance);
        outPacket.encodeByte(delay);
        return outPacket;
    }

    public static OutPacket snowballMessage(int team, int message) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SNOWBALL_MESSAGE.getValue());
        outPacket.encodeByte(team);// 0 is down, 1 is up
        outPacket.encodeInt(message);
        return outPacket;
    }

    public static OutPacket finishedSort(int type) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.FINISH_SORT.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeByte(type);
        return outPacket;
    }

    // 00 01 00 00 00 00
    public static OutPacket coconutScore(int[] coconutscore) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.COCONUT_SCORE.getValue());
        outPacket.encodeShort(coconutscore[0]);
        outPacket.encodeShort(coconutscore[1]);
        return outPacket;
    }

    public static OutPacket hitCoconut(boolean spawn, int id, int type) {
        // FF 00 00 00 00 00 00
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.HIT_COCONUT.getValue());
        if (spawn) {
            outPacket.encodeByte(0);
            outPacket.encodeInt(0x80);
        } else {
            outPacket.encodeInt(id);
            outPacket.encodeByte(type); // What action to do for the coconut.
        }
        return outPacket;
    }

    public static OutPacket finishedGather(int type) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.FINISH_GATHER.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeByte(type);
        return outPacket;
    }

    public static OutPacket yellowChat(String msg) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.YELLOW_CHAT.getValue());
        outPacket.encodeByte(-1); //could be something like mob displaying message.
        outPacket.encodeString(msg);
        return outPacket;
    }

    public static OutPacket getPeanutResult(int itemId, short quantity, int itemId2, short quantity2, int ourItem) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PIGMI_REWARD.getValue());
        outPacket.encodeInt(itemId);
        outPacket.encodeShort(quantity);
        outPacket.encodeInt(ourItem);
        outPacket.encodeInt(itemId2);
        outPacket.encodeInt(quantity2);

        return outPacket;
    }

    public static OutPacket sendLevelup(boolean family, int level, String name) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.LEVEL_UPDATE.getValue());
        outPacket.encodeByte(family ? 1 : 2);
        outPacket.encodeInt(level);
        outPacket.encodeString(name);

        return outPacket;
    }

    public static OutPacket sendMarriage(boolean family, String name) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MARRIAGE_UPDATE.getValue());
        outPacket.encodeByte(family ? 1 : 0);
        outPacket.encodeString(name);

        return outPacket;
    }

    public static OutPacket sendJobup(boolean family, int jobid, String name) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.JOB_UPDATE.getValue());
        outPacket.encodeByte(family ? 1 : 0);
        outPacket.encodeInt(jobid); //or is this a short
        outPacket.encodeString((GameConstants.GMS && !family ? "> " : "") + name);

        return outPacket;
    }

    public static OutPacket showHorntailShrine(boolean spawned, int time) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.HORNTAIL_SHRINE.getValue());
        outPacket.encodeByte(spawned ? 1 : 0);
        outPacket.encodeInt(time);
        return outPacket;
    }

    public static OutPacket showChaosZakumShrine(boolean spawned, int time) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CHAOS_ZAKUM_SHRINE.getValue());
        outPacket.encodeByte(spawned ? 1 : 0);
        outPacket.encodeInt(time);
        return outPacket;
    }

    public static OutPacket showChaosHorntailShrine(boolean spawned, int time) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CHAOS_HORNTAIL_SHRINE.getValue());
        outPacket.encodeByte(spawned ? 1 : 0);
        outPacket.encodeInt(time);
        return outPacket;
    }

    public static OutPacket stopClock() {
        return getPacketFromHexString(Integer.toHexString(SendPacketOpcode.STOP_CLOCK.getValue()) + " 00"); //does the header not work?
    }

    public static OutPacket spawnDragon(MapleDragon d) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.DRAGON_SPAWN.getValue());
        outPacket.encodeInt(d.getOwner());
        outPacket.encodeInt(d.getPosition().x);
        outPacket.encodeInt(d.getPosition().y);
        outPacket.encodeByte(d.getStance()); //stance?
        outPacket.encodeShort(0);
        outPacket.encodeShort(d.getJobId());
        return outPacket;
    }

    public static OutPacket removeDragon(int chrid) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.DRAGON_REMOVE.getValue());
        outPacket.encodeInt(chrid);
        return outPacket;
    }

    public static OutPacket moveDragon(MapleDragon d, Point oldPos, Point oldVPos, List<ILifeMovementFragment> moves) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DRAGON_MOVE.getValue()); //not sure
        outPacket.encodeInt(d.getOwner());
        outPacket.encodePosition(oldPos);
        outPacket.encodePosition(oldVPos);
        PacketHelper.serializeMovementList(outPacket, moves);

        return outPacket;
    }

    public static final OutPacket temporaryStats_Aran() {
        final Map<MapleStat.Temp, Integer> stats = new EnumMap<MapleStat.Temp, Integer>(MapleStat.Temp.class);
        stats.put(MapleStat.Temp.STR, 999);
        stats.put(MapleStat.Temp.DEX, 999);
        stats.put(MapleStat.Temp.INT, 999);
        stats.put(MapleStat.Temp.LUK, 999);
        stats.put(MapleStat.Temp.WATK, 255);
        stats.put(MapleStat.Temp.ACC, 999);
        stats.put(MapleStat.Temp.AVOID, 999);
        stats.put(MapleStat.Temp.SPEED, 140);
        stats.put(MapleStat.Temp.JUMP, 120);
        return temporaryStats(stats);
    }

    public static final OutPacket temporaryStats_Balrog(final MapleCharacter chr) {
        final Map<MapleStat.Temp, Integer> stats = new EnumMap<MapleStat.Temp, Integer>(MapleStat.Temp.class);
        int offset = 1 + (chr.getLevel() - 90) / 20;
        //every 20 levels above 90, +1
        stats.put(MapleStat.Temp.STR, chr.getStat().getTotalStr() / offset);
        stats.put(MapleStat.Temp.DEX, chr.getStat().getTotalDex() / offset);
        stats.put(MapleStat.Temp.INT, chr.getStat().getTotalInt() / offset);
        stats.put(MapleStat.Temp.LUK, chr.getStat().getTotalLuk() / offset);
        stats.put(MapleStat.Temp.WATK, chr.getStat().getTotalWatk() / offset);
        stats.put(MapleStat.Temp.MATK, chr.getStat().getTotalMagic() / offset);
        return temporaryStats(stats);
    }

    public static final OutPacket temporaryStats(final Map<MapleStat.Temp, Integer> mystats) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.TEMP_STATS.getValue());
        //str 0x1, dex 0x2, int 0x4, luk 0x8
        //level 0x10 = 255
        //0x100 = 999
        //0x200 = 999
        //0x400 = 120
        //0x800 = 140
        int updateMask = 0;
        for (MapleStat.Temp statupdate : mystats.keySet()) {
            updateMask |= statupdate.getValue();
        }
        outPacket.encodeInt(updateMask);
        Integer value;

        for (final Entry<MapleStat.Temp, Integer> statupdate : mystats.entrySet()) {
            value = statupdate.getKey().getValue();

            if (value >= 1) {
                if (value <= 0x200) { //level 0x10 - is this really short or some other? (FF 00)
                    outPacket.encodeShort(statupdate.getValue().shortValue());
                } else {
                    outPacket.encodeByte(statupdate.getValue().byteValue());
                }
            }
        }
        return outPacket;
    }

    public static final OutPacket temporaryStats_Reset() {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.TEMP_STATS_RESET.getValue());
        return outPacket;
    }

    //its likely that durability items use this
    public static final OutPacket showHpHealed(final int cid, final int amount) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(0x0C); //bb +2
        outPacket.encodeInt(amount);
        return outPacket;
    }

    public static final OutPacket showOwnHpHealed(final int amount) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(0x0C);  //bb +2
        outPacket.encodeInt(amount);
        return outPacket;
    }

    public static final OutPacket sendRepairWindow(int npc) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.OPEN_UI_OPTION.getValue());
        outPacket.encodeInt(0x21); //sending 0x20 here opens evan skill window o.o
        outPacket.encodeInt(npc);
        return outPacket;
    }

    public static final OutPacket sendProfessionWindow(int type) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.OPEN_UI_OPTION.getValue());
        outPacket.encodeInt(0x2A/*0x2E*/); //sending 0x20 here opens evan skill window o.o
        outPacket.encodeInt(type);
        return outPacket;
    }

    public static final OutPacket sendPVPWindow(int npc) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.OPEN_UI_OPTION.getValue());
        outPacket.encodeInt(0x32);
        outPacket.encodeInt(npc);
        return outPacket;
    }

    public static final OutPacket sendPVPMaps() {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_INFO.getValue());
        outPacket.encodeByte(3); //max amount of players
        for (int i = 0; i < 3; i++) {
            outPacket.encodeInt(10); //how many peoples in each map
            outPacket.encodeZeroBytes(48);
        }
        outPacket.encodeShort(150); ////PVP 1.5 EVENT!
        outPacket.encodeByte(1);
        return outPacket;
    }

    public static final OutPacket sendPyramidUpdate(final int amount) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PYRAMID_UPDATE.getValue());
        outPacket.encodeInt(amount); //1-132 ?
        return outPacket;
    }

    public static final OutPacket sendPyramidResult(final byte rank, final int amount) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PYRAMID_RESULT.getValue());
        outPacket.encodeByte(rank);
        outPacket.encodeInt(amount); //1-132 ?
        return outPacket;
    }

    //show_status_info - 01 53 1E 01
    //10/08/14/19/11
    //update_quest_info - 08 53 1E 00 00 00 00 00 00 00 00
    //show_status_info - 01 51 1E 01 01 00 30
    //update_quest_info - 08 51 1E 00 00 00 00 00 00 00 00
    public static final OutPacket sendPyramidEnergy(final String type, final String amount) {
        return sendString(1, type, amount);
    }

    public static final OutPacket sendString(final int type, final String object, final String amount) {
        OutPacket outPacket = new OutPacket();
        switch (type) {
            case 1:
                outPacket = new OutPacket(SendPacketOpcode.ENERGY.getValue());
                break;
            case 2:
                outPacket = new OutPacket(SendPacketOpcode.GHOST_POINT.getValue());
                break;
            case 3:
                outPacket = new OutPacket(SendPacketOpcode.GHOST_STATUS.getValue());
                break;
        }
        outPacket.encodeString(object); //massacre_hit, massacre_cool, massacre_miss, massacre_party, massacre_laststage, massacre_skill
        outPacket.encodeString(amount);
        return outPacket;
    }

    public static final OutPacket sendGhostPoint(final String type, final String amount) {
        return sendString(2, type, amount); //PRaid_Point (0-1500???)
    }

    public static final OutPacket sendGhostStatus(final String type, final String amount) {
        return sendString(3, type, amount); //Red_Stage(1-5), Blue_Stage, blueTeamDamage, redTeamDamage
    }

    public static OutPacket MulungEnergy(int energy) {
        return sendPyramidEnergy("energy", String.valueOf(energy));
    }

    public static OutPacket getPollQuestion() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GAME_POLL_QUESTION.getValue());
        outPacket.encodeInt(1);
        outPacket.encodeInt(14);
        outPacket.encodeString(ServerConstants.Poll_Question);
        outPacket.encodeInt(ServerConstants.Poll_Answers.length); // pollcount
        for (byte i = 0; i < ServerConstants.Poll_Answers.length; i++) {
            outPacket.encodeString(ServerConstants.Poll_Answers[i]);
        }

        return outPacket;
    }

    public static OutPacket getPollReply(String message) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GAME_POLL_REPLY.getValue());
        outPacket.encodeString(message);

        return outPacket;
    }

    public static OutPacket getEvanTutorial(String data) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.NPC_TALK.getValue());

        outPacket.encodeByte(8);
        outPacket.encodeInt(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeString(data);

        return outPacket;
    }

    public static OutPacket showEventInstructions() {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.GMEVENT_INSTRUCTIONS.getValue());
        outPacket.encodeByte(0);
        return outPacket;
    }

    public static OutPacket getOwlOpen() { //best items! hardcoded
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.OWL_OF_MINERVA.getValue());
        outPacket.encodeByte(7);
        outPacket.encodeByte(GameConstants.owlItems.length);
        for (int i : GameConstants.owlItems) {
            outPacket.encodeInt(i);
        } //these are the most searched items. too lazy to actually make
        return outPacket;
    }

    public static OutPacket getOwlSearched(final int itemSearch, final List<HiredMerchant> hms) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.OWL_OF_MINERVA.getValue());
        outPacket.encodeByte(6);
        outPacket.encodeInt(0);
        outPacket.encodeInt(itemSearch);
        int size = 0;

        for (HiredMerchant hm : hms) {
            size += hm.searchItem(itemSearch).size();
        }
        outPacket.encodeInt(size);
        for (HiredMerchant hm : hms) {
            final List<MaplePlayerShopItem> items = hm.searchItem(itemSearch);
            for (MaplePlayerShopItem item : items) {
                outPacket.encodeString(hm.getOwnerName());
                outPacket.encodeInt(hm.getMap().getId());
                outPacket.encodeString(hm.getDescription());
                outPacket.encodeInt(item.item.getQuantity()); //I THINK.
                outPacket.encodeInt(item.bundles); //I THINK.
                outPacket.encodeInt(item.price);
                switch (InventoryHandler.OWL_ID) {
                    case 0:
                        outPacket.encodeInt(hm.getOwnerId()); //store ID
                        break;
                    case 1:
                        outPacket.encodeInt(hm.getStoreId());
                        break;
                    default:
                        outPacket.encodeInt(hm.getObjectId());
                        break;
                }
                outPacket.encodeByte(hm.getFreeSlot() == -1 ? 1 : 0);
                outPacket.encodeByte(GameConstants.getInventoryType(itemSearch).getType()); //position?
                if (GameConstants.getInventoryType(itemSearch) == MapleInventoryType.EQUIP) {
                    PacketHelper.addItemInfo(outPacket, item.item, true, true);
                }
            }
        }
        return outPacket;
    }

    public static OutPacket getRPSMode(byte mode, int mesos, int selection, int answer) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.RPS_GAME.getValue());
        outPacket.encodeByte(mode);
        switch (mode) {
            case 6: { //not enough mesos
                if (mesos != -1) {
                    outPacket.encodeInt(mesos);
                }
                break;
            }
            case 8: { //open (npc)
                outPacket.encodeInt(9000019);
                break;
            }
            case 11: { //selection vs answer
                outPacket.encodeByte(selection);
                outPacket.encodeByte(answer); // FF = lose, or if selection = answer then lose ???
                break;
            }
        }
        return outPacket;
    }

    public static final OutPacket getSlotUpdate(byte invType, byte newSlots) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_INVENTORY_SLOT.getValue());
        outPacket.encodeByte(invType);
        outPacket.encodeByte(newSlots);
        return outPacket;
    }

    public static OutPacket followRequest(int chrid) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.FOLLOW_REQUEST.getValue());
        outPacket.encodeInt(chrid);
        return outPacket;
    }

    public static OutPacket followEffect(int initiator, int replier, Point toMap) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.FOLLOW_EFFECT.getValue());
        outPacket.encodeInt(initiator);
        outPacket.encodeInt(replier);
        if (replier == 0) { //cancel
            outPacket.encodeByte(toMap == null ? 0 : 1); //1 -> x (int) y (int) to change map
            if (toMap != null) {
                outPacket.encodeInt(toMap.x);
                outPacket.encodeInt(toMap.y);
            }
        }
        return outPacket;
    }

    public static OutPacket getFollowMsg(int opcode) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.FOLLOW_MSG.getValue());
        outPacket.encodeInt(opcode); //5 = canceled request.
        outPacket.encodeInt(0);
        return outPacket;
    }

    public static OutPacket moveFollow(Point otherStart, Point myStart, Point otherEnd, List<ILifeMovementFragment> moves) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.FOLLOW_MOVE.getValue());
        outPacket.encodePosition(otherStart);
        outPacket.encodePosition(myStart);
        PacketHelper.serializeMovementList(outPacket, moves);
        outPacket.encodeByte(0x11); //what? could relate to movePlayer
        for (int i = 0; i < 8; i++) {
            outPacket.encodeByte(0); //?? sometimes 0x44 sometimes 0x88 sometimes 0x4.. etc.. buffstat or what
        }
        outPacket.encodeByte(0); //?
        outPacket.encodePosition(otherEnd);
        outPacket.encodePosition(otherStart);

        return outPacket;
    }

    public static final OutPacket getFollowMessage(final String msg) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPOUSE_MESSAGE.getValue());
        outPacket.encodeShort(0x0B); //?
        outPacket.encodeString(msg); //white in gms, but msea just makes it pink.. waste
        return outPacket;
    }

    public static final OutPacket getNodeProperties(final MapleMonster objectid, final MapleMap map) {
        //idk.
        if (objectid.getNodePacket() != null) {
            return objectid.getNodePacket();
        }

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MONSTER_PROPERTIES.getValue());
        outPacket.encodeInt(objectid.getObjectId()); //?
        outPacket.encodeInt(map.getNodes().size());
        outPacket.encodeInt(objectid.getPosition().x);
        outPacket.encodeInt(objectid.getPosition().y);
        for (MapleNodeInfo mni : map.getNodes()) {
            outPacket.encodeInt(mni.x);
            outPacket.encodeInt(mni.y);
            outPacket.encodeInt(mni.attr);
            if (mni.attr == 2) { //msg
                outPacket.encodeInt(500); //? talkMonster
            }
        }
        outPacket.encodeInt(0);
        boolean unk = false;
        outPacket.encodeByte(unk);
        if (unk) {
            outPacket.encodeInt(0);
        }
        outPacket.encodeByte(0);
        objectid.setNodePacket(outPacket);
        return objectid.getNodePacket();
    }

    public static final OutPacket getMovingPlatforms(final MapleMap map) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MOVE_PLATFORM.getValue());
        outPacket.encodeInt(map.getPlatforms().size());
        for (MaplePlatform mp : map.getPlatforms()) {
            outPacket.encodeString(mp.name);
            outPacket.encodeInt(mp.start);
            outPacket.encodeInt(mp.SN.size());
            for (int x = 0; x < mp.SN.size(); x++) {
                outPacket.encodeInt(mp.SN.get(x));
            }
            outPacket.encodeInt(mp.speed);
            outPacket.encodeInt(mp.x1);
            outPacket.encodeInt(mp.x2);
            outPacket.encodeInt(mp.y1);
            outPacket.encodeInt(mp.y2);
            outPacket.encodeInt(mp.x1);//?
            outPacket.encodeInt(mp.y1);
            outPacket.encodeShort(mp.r);
        }
        return outPacket;
    }

    public static final OutPacket getUpdateEnvironment(final MapleMap map) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_ENV.getValue());
        outPacket.encodeInt(map.getEnvironment().size());
        for (Entry<String, Integer> mp : map.getEnvironment().entrySet()) {
            outPacket.encodeString(mp.getKey());
            outPacket.encodeInt(mp.getValue());
        }
        return outPacket;
    }

    public static OutPacket sendEngagementRequest(String name, int cid) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.ENGAGE_REQUEST.getValue());
        outPacket.encodeByte(0); //mode, 0 = engage, 1 = cancel, 2 = answer.. etc
        outPacket.encodeString(name); // name
        outPacket.encodeInt(cid); // playerid
        return outPacket;
    }

    /**
     *
     * @param type - (0:Light&Long 1:Heavy&Short)
     * @param delay - seconds
     * @return
     */
    public static OutPacket trembleEffect(int type, int delay) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.BOSS_ENV.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeByte(type);
        outPacket.encodeInt(delay);
        return outPacket;
    }

    public static OutPacket sendEngagement(final byte msg, final int item, final MapleCharacter male, final MapleCharacter female) {
        
        // 0B = Engagement has been concluded.
        // 0D = The engagement is cancelled.
        // 0E = The divorce is concluded.
        // 10 = The marriage reservation has been successsfully made.
        // 12 = Wrong character name
        // 13 = The party in not in the same map.
        // 14 = Your inventory is full. Please empty your E.T.C window.
        // 15 = The person's inventory is full.
        // 16 = The person cannot be of the same gender.
        // 17 = You are already engaged.
        // 18 = The person is already engaged.
        // 19 = You are already married.
        // 1A = The person is already married.
        // 1B = You are not allowed to propose.
        // 1C = The person is not allowed to be proposed to.
        // 1D = Unfortunately, the one who proposed to you has cancelled his proprosal.
        // 1E = The person had declined the proposal with thanks.
        // 1F = The reservation has been cancelled. Try again later.
        // 20 = You cannot cancel the wedding after reservation.
        // 22 = The invitation card is ineffective.
        OutPacket outPacket = new OutPacket(SendPacketOpcode.ENGAGE_RESULT.getValue());
        outPacket.encodeByte(msg); // 1103 custom quest
        switch (msg) {
            case 11: {
                outPacket.encodeInt(0); // ringid or uniqueid
                outPacket.encodeInt(male.getId());
                outPacket.encodeInt(female.getId());
                outPacket.encodeShort(1); //always
                outPacket.encodeInt(item);
                outPacket.encodeInt(item); // wtf?repeat?
                outPacket.encodeString(male.getName(), 13);
                outPacket.encodeString(female.getName(), 13);
                break;
            }
        }
        return outPacket;
    }

    public static OutPacket getPartyListing(final PartySearchType pst) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PARTY_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x93 : 0x4D);
        outPacket.encodeInt(pst.id);
        final List<PartySearch> parties = World.Party.searchParty(pst);
        outPacket.encodeInt(parties.size());
        for (PartySearch party : parties) {
            outPacket.encodeInt(0); //ive no clue,either E8 72 94 00 or D8 72 94 00 
            outPacket.encodeInt(2); //again, no clue, seems to remain constant?
            if (pst.exped) {
                MapleExpedition me = World.Party.getExped(party.getId());
                outPacket.encodeInt(me.getType().maxMembers);
                outPacket.encodeInt(party.getId());
                outPacket.encodeString(party.getName(), 48);
                for (int i = 0; i < 5; i++) { //all parties in the exped other than the leader
                    if (i < me.getParties().size()) {
                        MapleParty part = World.Party.getParty(me.getParties().get(i));
                        if (part != null) {
                            addPartyStatus(-1, part, outPacket, false, true);
                        } else {
                            outPacket.encodeZeroBytes(202); //length of the addPartyStatus.
                        }
                    } else {
                        outPacket.encodeZeroBytes(202); //length of the addPartyStatus.
                    }
                }
            } else {
                outPacket.encodeInt(0);
                outPacket.encodeInt(party.getId());
                outPacket.encodeString(party.getName(), 48);
                addPartyStatus(-1, World.Party.getParty(party.getId()), outPacket, false, true); //if exped, send 0, if not then skip
            }

            outPacket.encodeShort(0); //wonder if this goes here or at bottom
        }

        return outPacket;
    }

    public static OutPacket partyListingAdded(final PartySearch ps) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PARTY_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x91 : 0x4B);
        outPacket.encodeInt(ps.getType().id);
        outPacket.encodeInt(0); //ive no clue,either 48 DB 60 00 or 18 DB 60 00
        outPacket.encodeInt(1);
        if (ps.getType().exped) {
            MapleExpedition me = World.Party.getExped(ps.getId());
            outPacket.encodeInt(me.getType().maxMembers);
            outPacket.encodeInt(ps.getId());
            outPacket.encodeString(ps.getName(), 48);
            for (int i = 0; i < 5; i++) { //all parties in the exped other than the leader
                if (i < me.getParties().size()) {
                    MapleParty party = World.Party.getParty(me.getParties().get(i));
                    if (party != null) {
                        addPartyStatus(-1, party, outPacket, false, true);
                    } else {
                        outPacket.encodeZeroBytes(202); //length of the addPartyStatus.
                    }
                } else {
                    outPacket.encodeZeroBytes(202); //length of the addPartyStatus.
                }
            }
        } else {
            outPacket.encodeInt(0); //doesn't matter
            outPacket.encodeInt(ps.getId());
            outPacket.encodeString(ps.getName(), 48);
            addPartyStatus(-1, World.Party.getParty(ps.getId()), outPacket, false, true); //if exped, send 0, if not then skip
        }
        outPacket.encodeShort(0); //wonder if this goes here or at bottom

        return outPacket;
    }

    public static OutPacket expeditionStatus(final MapleExpedition me, boolean created) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        outPacket.encodeByte(created ? (GameConstants.GMS ? 0x81 : 0x3B) : (GameConstants.GMS ? 0x7F : 0x39));
        outPacket.encodeInt(me.getType().exped);
        outPacket.encodeInt(0); //eh?
        for (int i = 0; i < 5; i++) { //all parties in the exped other than the leader
            if (i < me.getParties().size()) {
                MapleParty party = World.Party.getParty(me.getParties().get(i));
                if (party != null) {
                    addPartyStatus(-1, party, outPacket, false, true);
                } else {
                    outPacket.encodeZeroBytes(202); //length of the addPartyStatus.
                }
            } else {
                outPacket.encodeZeroBytes(202); //length of the addPartyStatus.
            }
        }
        outPacket.encodeShort(0); //wonder if this goes here or at bottom

        return outPacket;
    }

    public static OutPacket expeditionError(final int errcode, final String name) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x8F : 0x49);
        outPacket.encodeInt(errcode); //0 = not found, 1 = admin, 2 = already in a part, 3 = not right lvl, 4 = blocked, 5 = taking another, 6 = already in, 7 = all good
        outPacket.encodeString(name);

        return outPacket;
    }

    public static OutPacket expeditionJoined(final String name) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x7C : 0x3C);
        outPacket.encodeString(name);

        return outPacket;
    }

    public static OutPacket expeditionLeft(final String name) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x82 : 0x42);
        outPacket.encodeString(name);

        return outPacket;
    }

    public static OutPacket expeditionMessage(final int code) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        outPacket.encodeByte(code + (GameConstants.GMS ? 76 : 6)); //0x3B = left, 0x3E = disbanded

        return outPacket;
    }

    public static OutPacket expeditionLeaderChanged(final int newLeader) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x85 : 0x45);
        outPacket.encodeInt(newLeader);
        return outPacket;
    }

    //can only update one party in the expedition.
    public static OutPacket expeditionUpdate(final int partyIndex, final MapleParty party) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x86 : 0x46);
        outPacket.encodeInt(0); //lol?
        outPacket.encodeInt(partyIndex);
        if (party == null) {
            outPacket.encodeZeroBytes(178); //length of the addPartyStatus.
        } else {
            addPartyStatus(-1, party, outPacket, false, true);
        }
        return outPacket;
    }

    public static OutPacket expeditionInvite(MapleCharacter from, int exped) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x88 : 0x48);
        outPacket.encodeInt(from.getLevel());
        outPacket.encodeInt(from.getJob());
        outPacket.encodeString(from.getName());
        outPacket.encodeInt(exped);

        return outPacket;
    }

    public static OutPacket updateJaguar(MapleCharacter from) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_JAGUAR.getValue());
        PacketHelper.addJaguarInfo(outPacket, from);

        return outPacket;
    }

    public static OutPacket teslaTriangle(int cid, int sum1, int sum2, int sum3) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.TESLA_TRIANGLE.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(sum1);
        outPacket.encodeInt(sum2);
        outPacket.encodeInt(sum3);
        return outPacket;
    }

    public static OutPacket mechPortal(Point pos) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MECH_PORTAL.getValue());
        outPacket.encodePosition(pos);
        return outPacket;
    }

    public static OutPacket spawnMechDoor(MechDoor md, boolean animated) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MECH_DOOR_SPAWN.getValue());
        outPacket.encodeByte(animated ? 0 : 1);
        outPacket.encodeInt(md.getOwnerId());
        outPacket.encodePosition(md.getTruePosition());
        outPacket.encodeByte(md.getId());
        outPacket.encodeInt(md.getPartyId());
        return outPacket;
    }

    public static OutPacket removeMechDoor(MechDoor md, boolean animated) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MECH_DOOR_REMOVE.getValue());
        outPacket.encodeByte(animated ? 0 : 1);
        outPacket.encodeInt(md.getOwnerId());
        outPacket.encodeByte(md.getId());
        return outPacket;
    }

    public static OutPacket useSPReset(int cid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SP_RESET.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeInt(cid);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket playerDamaged(int cid, int dmg) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_DAMAGED.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(dmg);

        return outPacket;
    }

    public static OutPacket pamsSongEffect(int cid) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PAMS_SONG_EFFECT.getValue());
        outPacket.encodeInt(cid);

        return outPacket;
    }

    public static OutPacket pamsSongUI() {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PAMS_SONG_UI.getValue());
        outPacket.encodeShort(0); //doesn't seem to change it

        return outPacket;
    }

    public static OutPacket englishQuizMsg(String msg) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ENGLISH_QUIZ.getValue());
        outPacket.encodeInt(20); //?
        outPacket.encodeString(msg);

        return outPacket;
    }

    public static OutPacket report(int err) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.REPORT.getValue());
        outPacket.encodeByte(err); //0 = success
        if (GameConstants.GMS && err == 2) {
            outPacket.encodeByte(0);
            outPacket.encodeInt(1);
        }
        return outPacket;
    }

    public static OutPacket ultimateExplorer() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ULTIMATE_EXPLORER.getValue());

        return outPacket;
    }

    public static OutPacket GMPoliceMessage() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GM_POLICE.getValue());
        outPacket.encodeInt(0); //no clue
        return outPacket;
    }

    public static OutPacket pamSongUI() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PAMS_SONG_UI.getValue());
        outPacket.encodeInt(0); //no clue
        return outPacket;
    }

    public static OutPacket dragonBlink(int portalId) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DRAGON_BLINK.getValue());
        outPacket.encodeByte(portalId);
        return outPacket;
    }

    public static OutPacket showTraitGain(MapleTraitType trait, int amount) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(0x12);
        outPacket.encodeLong(trait.getStat().getValue());
        outPacket.encodeInt(amount);
        return outPacket;
    }

    public static OutPacket showTraitMaxed(MapleTraitType trait) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(0x13);
        outPacket.encodeLong(trait.getStat().getValue());
        return outPacket;
    }

    public static OutPacket harvestMessage(int oid, int msg) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.HARVEST_MESSAGE.getValue());
        outPacket.encodeInt(oid);
        outPacket.encodeInt(msg);
        return outPacket;
    }

    public static OutPacket showHarvesting(int cid, int tool) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_HARVEST.getValue());
        outPacket.encodeInt(cid);
        if (tool > 0) {
            outPacket.encodeByte(1);
            outPacket.encodeInt(0); // update time
            outPacket.encodeInt(tool);
        } else {
            outPacket.encodeByte(0);
        }
        return outPacket;
    }

    public static OutPacket harvestResult(int cid, boolean success) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.HARVESTED.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(success ? 1 : 0);
        return outPacket;
    }

    public static OutPacket makeExtractor(int cid, String cname, Point pos, int timeLeft, int itemId, int fee) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_EXTRACTOR.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeString(cname);
        outPacket.encodeInt(pos.x);
        outPacket.encodeInt(pos.y);
        outPacket.encodeShort(timeLeft); //fh or time left, dunno
        outPacket.encodeInt(itemId); //3049000, 3049001...
        outPacket.encodeInt(fee);
        return outPacket;
    }

    public static OutPacket removeExtractor(int cid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.REMOVE_EXTRACTOR.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(1); //probably 1 = animation, 2 = make something?
        return outPacket;
    }

    public static OutPacket spouseMessage(String msg, boolean white) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPOUSE_MESSAGE.getValue());
        outPacket.encodeShort(white ? (GameConstants.GMS ? 11 : 10) : 6); //12 = the blue message thing, 7/8 = yellow
        outPacket.encodeString(msg);
        return outPacket;
    }

    public static OutPacket openBag(int index, int itemId, boolean firstTime) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.OPEN_BAG.getValue());
        outPacket.encodeInt(index);
        outPacket.encodeInt(itemId);
        outPacket.encodeShort(firstTime ? 1 : 0); //this might actually be 2 bytes
        return outPacket;
    }

    public static OutPacket showOwnCraftingEffect(String effect, int time, int mode) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(0x1F);
        outPacket.encodeString(effect);
        outPacket.encodeInt(time);
        outPacket.encodeInt(mode);

        return outPacket;
    }

    public static OutPacket showCraftingEffect(int cid, String effect, int time, int mode) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(0x1F);
        outPacket.encodeString(effect);
        outPacket.encodeInt(time);
        outPacket.encodeInt(mode);

        return outPacket;
    }

    public static OutPacket craftMake(int cid, int something, int time) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CRAFT_EFFECT.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(something); // 角色動作ID
        outPacket.encodeInt(time);
        
        return outPacket;
    }

    public static OutPacket craftFinished(int cid, int craftID, int ranking, int itemId, int quantity, int exp) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CRAFT_COMPLETE.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(craftID);
        outPacket.encodeInt(ranking);
        outPacket.encodeInt(itemId);
        outPacket.encodeInt(quantity);
        outPacket.encodeInt(exp);
        return outPacket;
    }

    public static OutPacket shopDiscount(int percent) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOP_DISCOUNT.getValue());
        outPacket.encodeByte(percent);
        return outPacket;
    }

    public static OutPacket spawnAndroid(MapleCharacter cid, MapleAndroid android) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ANDROID_SPAWN.getValue());
        outPacket.encodeInt(cid.getId());
        outPacket.encodeByte(android.getItemId() - 1661999); //type of android, 1-4
        outPacket.encodePosition(android.getPos());
        outPacket.encodeByte(android.getStance());
        outPacket.encodeInt(0); //no clue, FH or something
        outPacket.encodeShort(android.getHair() - 30000);
        outPacket.encodeShort(android.getFace() - 20000);
        outPacket.encodeString(android.getName());
        for (short i = -1200; i > -1207; i--) {
            final Item item = cid.getInventory(MapleInventoryType.EQUIPPED).getItem(i);
            outPacket.encodeInt(item != null ? item.getItemId() : 0);
        }
        return outPacket;
    }

    public static OutPacket moveAndroid(int cid, Point oldPos, Point oldVPos, List<ILifeMovementFragment> res) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ANDROID_MOVE.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodePosition(oldPos);
        outPacket.encodePosition(oldVPos);
        PacketHelper.serializeMovementList(outPacket, res);
        return outPacket;
    }

    public static OutPacket showAndroidEmotion(int cid, int animation) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ANDROID_EMOTION.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(0);
        outPacket.encodeByte(animation); //1234567 = default smiles, 8 = throwing up, 11 = kiss, 14 = googly eyes, 17 = wink...
        return outPacket;
    }

    public static OutPacket removeAndroid(int cid, int animation) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ANDROID_REMOVE.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(animation);
        outPacket.encodeInt(0);
        return outPacket;
    }

    public static OutPacket deactivateAndroid(int cid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ANDROID_DEACTIVATED.getValue());
        outPacket.encodeInt(cid);
        return outPacket;
    }

    public static OutPacket changeCardSet(int set) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CARD_SET.getValue());
        outPacket.encodeInt(set);
        return outPacket;
    }

    public static OutPacket getCard(int itemid, int level) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GET_CARD.getValue());
        outPacket.encodeByte(itemid > 0 ? 1 : 0);
        if (itemid > 0) {
            outPacket.encodeInt(itemid);
            outPacket.encodeInt(level);
        }
        return outPacket;
    }

    public static OutPacket upgradeBook(Item book, MapleCharacter chr) { //slot -55
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BOOK_STATS.getValue());
        outPacket.encodeInt(book.getPosition()); //negative or not
        PacketHelper.addItemInfo(outPacket, book, true, true, false, false, chr);
        return outPacket;
    }

    public static OutPacket pendantSlot(boolean p) { //slot -59
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PENDANT_SLOT.getValue());
        outPacket.encodeByte(p ? 1 : 0);
        return outPacket;
    }

    public static OutPacket getMonsterBookInfo(MapleCharacter chr) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.BOOK_INFO.getValue());
        outPacket.encodeInt(chr.getId());
        outPacket.encodeInt(chr.getLevel());
        chr.getMonsterBook().writeCharInfoPacket(outPacket);
        
        return outPacket;
    }

    public static OutPacket getBuffBar(long millis) { //You can use the buff again _ seconds later. + bar above head
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BUFF_BAR.getValue());
        outPacket.encodeLong(millis);
        return outPacket;
    }

    /**
     * Makes any NPC in the game scriptable.
     * @param npcId - The NPC's ID, found in WZ files/MCDB
     * @param description - If the NPC has quests, this will be the text of the menu item
     * @return 
     */
    public static OutPacket setNPCScriptable(List<Pair<Integer, String>> npcs) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.NPC_SCRIPTABLE.getValue());
        outPacket.encodeByte(npcs.size());
        for (Pair<Integer, String> s : npcs) {
            outPacket.encodeInt(s.left);
            outPacket.encodeString(s.right);
            outPacket.encodeInt(0); // start time
            outPacket.encodeInt(Integer.MAX_VALUE); // end time
        }
        return outPacket;
    }

    public static OutPacket showMidMsg(String s, int l) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MID_MSG.getValue());
        outPacket.encodeByte(l); //i think this is the line.. or soemthing like that. 1 = lower than 0
        outPacket.encodeString(s);
        outPacket.encodeByte(s.length() > 0 ? 0 : 1); //remove?
        return outPacket;
    }

    public static OutPacket showMemberSearch(List<MapleCharacter> chr) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MEMBER_SEARCH.getValue());
        outPacket.encodeByte(chr.size());
        for (MapleCharacter c : chr) {
            outPacket.encodeInt(c.getId());
            outPacket.encodeString(c.getName());
            outPacket.encodeShort(c.getJob());
            outPacket.encodeByte(c.getLevel());
        }
        return outPacket;
    }

    public static OutPacket showPartySearch(List<MapleParty> chr) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PARTY_SEARCH.getValue());
        outPacket.encodeByte(chr.size());
        for (MapleParty c : chr) {
            outPacket.encodeInt(c.getId());
            outPacket.encodeString(c.getLeader().getName());
            outPacket.encodeByte(c.getLeader().getLevel());
            outPacket.encodeByte(c.getLeader().isOnline() ? 1 : 0);
            outPacket.encodeByte(c.getMembers().size());
            for (MaplePartyCharacter ch : c.getMembers()) {
                outPacket.encodeInt(ch.getId());
                outPacket.encodeString(ch.getName());
                outPacket.encodeShort(ch.getJobId());
                outPacket.encodeByte(ch.getLevel());
                outPacket.encodeByte(ch.isOnline() ? 1 : 0);
            }
        }
        return outPacket;
    }

    public static OutPacket showBackgroundEffect(String eff, int value) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.VISITOR.getValue());
        outPacket.encodeString(eff); //"Visitor"
        outPacket.encodeByte(value);
        return outPacket;
    }

    public static OutPacket loadGuildName(MapleCharacter chr) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.LOAD_GUILD_NAME.getValue());
        outPacket.encodeInt(chr.getId());

        if (chr.getGuildId() <= 0) {
            outPacket.encodeShort(0);
        } else {
            final MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                outPacket.encodeString(gs.getName());
            } else {
                outPacket.encodeShort(0);
            }
        }
        return outPacket;
    }

    public static OutPacket loadGuildIcon(MapleCharacter chr) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.LOAD_GUILD_ICON.getValue());
        outPacket.encodeInt(chr.getId());

        if (chr.getGuildId() <= 0) {
            outPacket.encodeZeroBytes(6);
        } else {
            final MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                outPacket.encodeShort(gs.getLogoBG());
                outPacket.encodeByte(gs.getLogoBGColor());
                outPacket.encodeShort(gs.getLogo());
                outPacket.encodeByte(gs.getLogoColor());
            } else {
                outPacket.encodeZeroBytes(6);
            }
        }
        return outPacket;
    }

    public static OutPacket updateGender(MapleCharacter chr) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_GENDER.getValue());
        outPacket.encodeByte(chr.getGender());
        return outPacket;
    }

    public static OutPacket registerFamiliar(MonsterFamiliar mf) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.REGISTER_FAMILIAR.getValue());
        outPacket.encodeLong(mf.getId());
        mf.writeRegisterPacket(outPacket, false);
        outPacket.encodeShort(mf.getVitality() >= 3 ? 1 : 0);

        return outPacket;
    }

    public static OutPacket touchFamiliar(int cid, byte unk, int objectid, int type, int delay, int damage) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.TOUCH_FAMILIAR.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(0);
        outPacket.encodeByte(unk);
        outPacket.encodeInt(objectid);
        outPacket.encodeInt(type);
        outPacket.encodeInt(delay);
        outPacket.encodeInt(damage);
        return outPacket;
    }

    public static OutPacket familiarAttack(int cid, byte unk, List<Triple<Integer, Integer, List<Integer>>> attackPair) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.ATTACK_FAMILIAR.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(0);
        outPacket.encodeByte(unk);
        outPacket.encodeByte(attackPair.size());
        for (Triple<Integer, Integer, List<Integer>> s : attackPair) {
            outPacket.encodeInt(s.left);
            outPacket.encodeByte(s.mid);
            outPacket.encodeByte(s.right.size());
            for (int damage : s.right) {
                outPacket.encodeInt(damage);
            }
        }
        return outPacket;
    }

    public static OutPacket updateFamiliar(MonsterFamiliar mf) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_FAMILIAR.getValue());
        outPacket.encodeInt(mf.getCharacterId());
        outPacket.encodeInt(mf.getFamiliar());
        outPacket.encodeInt(mf.getFatigue());
        outPacket.encodeLong(PacketHelper.getTime(mf.getVitality() >= 3 ? System.currentTimeMillis() : -2));
        return outPacket;
    }

    public static OutPacket removeFamiliar(int cid) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_FAMILIAR.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeShort(0);
        outPacket.encodeByte(0);
        return outPacket;
    }

    public static OutPacket spawnFamiliar(MonsterFamiliar mf, boolean spawn) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_FAMILIAR.getValue());
        outPacket.encodeInt(mf.getCharacterId());
        outPacket.encodeShort(spawn ? 1 : 0);
        outPacket.encodeByte(0);
        if (spawn) {
            outPacket.encodeInt(mf.getFamiliar());
            outPacket.encodeInt(mf.getFatigue());
            outPacket.encodeInt(mf.getVitality() * 300); //max fatigue
            outPacket.encodeString(mf.getName());
            outPacket.encodePosition(mf.getTruePosition());
            outPacket.encodeByte(mf.getStance());
            outPacket.encodeShort(mf.getFh());
        }
        return outPacket;
    }

    public static OutPacket moveFamiliar(int cid, Point oldPos, Point oldVPos, List<ILifeMovementFragment> moves) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MOVE_FAMILIAR.getValue()); //not sure
        outPacket.encodeInt(cid);
        outPacket.encodeByte(0); // slot
        outPacket.encodePosition(oldPos);
        outPacket.encodePosition(oldVPos);

        PacketHelper.serializeMovementList(outPacket, moves);

        return outPacket;
    }

    public static OutPacket achievementRatio(int amount) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ACHIEVEMENT_RATIO.getValue()); //not sure
        outPacket.encodeInt(amount);

        return outPacket;
    }

    public static OutPacket createUltimate(int amount) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CREATE_ULTIMATE.getValue());
        outPacket.encodeInt(amount); //2 = no slots, 1 = success, 0 = failed

        return outPacket;
    }

    public static OutPacket professionInfo(String skil, int level1, int level2, int chance) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PROFESSION_INFO.getValue());
        outPacket.encodeString(skil);
        outPacket.encodeInt(level1);
        outPacket.encodeInt(level2);
        outPacket.encodeByte(1);
        outPacket.encodeInt(skil.startsWith("9200") || skil.startsWith("9201") ? 100 : chance); //100% chance

        return outPacket;
    }

    public static OutPacket quickSlot(String skil) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.QUICK_SLOT.getValue());
        outPacket.encodeByte(skil == null ? 0 : 1);
        if (skil != null) {
            for (int i = 0; i < skil.length(); i++) { // 8
                outPacket.encodeString(skil.substring(i, i + 1), 1);
                outPacket.encodeZeroBytes(3); //really hacky
            }
        }

        return outPacket;
    }

    public static OutPacket getFamiliarInfo(MapleCharacter chr) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.FAMILIAR_INFO.getValue());
        outPacket.encodeInt(chr.getFamiliars().size()); //size
        for (MonsterFamiliar mf : chr.getFamiliars().values()) {
            mf.writeRegisterPacket(outPacket, true);
        }
        List<Pair<Integer, Long>> size = new ArrayList<Pair<Integer, Long>>();
        for (Item i : chr.getInventory(MapleInventoryType.USE).list()) {
            if (i.getItemId() / 10000 == 287) { //expensif
                StructFamiliar f = MapleItemInformationProvider.getInstance().getFamiliarByItem(i.getItemId());
                if (f != null) {
                    size.add(new Pair<Integer, Long>(f.familiar, i.getInventoryId()));
                }
            }
        }
        outPacket.encodeInt(size.size());
        for (Pair<Integer, Long> s : size) {
            outPacket.encodeInt(chr.getId());
            outPacket.encodeInt(s.left);
            outPacket.encodeLong(s.right);
            outPacket.encodeByte(0); //activated or not, troll
        }
        size.clear();
        return outPacket;
    }

    public static OutPacket updateImp(MapleImp imp, int mask, int index, boolean login) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.ITEM_POT.getValue());
        outPacket.encodeByte(login ? 0 : 1); //0 = unchanged, 1 = changed
        outPacket.encodeInt(index + 1);
        outPacket.encodeInt(mask);
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0) {
            final Pair<Integer, Integer> i = MapleItemInformationProvider.getInstance().getPot(imp.getItemId());
            if (i == null) {
                return enableActions();
            }
            outPacket.encodeInt(i.left);
            outPacket.encodeByte(imp.getLevel()); //probably type
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.STATE.getValue()) != 0) {
            outPacket.encodeByte(imp.getState());
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.FULLNESS.getValue()) != 0) {
            outPacket.encodeInt(imp.getFullness());
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.CLOSENESS.getValue()) != 0) {
            outPacket.encodeInt(imp.getCloseness());
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.CLOSENESS_LEFT.getValue()) != 0) {
            outPacket.encodeInt(1); //how much closeness is available to get right now
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.MINUTES_LEFT.getValue()) != 0) {
            outPacket.encodeInt(0); //how much mins till next closeness
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.LEVEL.getValue()) != 0) {
            outPacket.encodeByte(1); //k idk
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.FULLNESS_2.getValue()) != 0) {
            outPacket.encodeInt(imp.getFullness()); //idk
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.UPDATE_TIME.getValue()) != 0) {
            outPacket.encodeLong(PacketHelper.getTime(System.currentTimeMillis()));
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.CREATE_TIME.getValue()) != 0) {
            outPacket.encodeLong(PacketHelper.getTime(System.currentTimeMillis()));
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.AWAKE_TIME.getValue()) != 0) {
            outPacket.encodeLong(PacketHelper.getTime(System.currentTimeMillis()));
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.SLEEP_TIME.getValue()) != 0) {
            outPacket.encodeLong(PacketHelper.getTime(System.currentTimeMillis()));
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.MAX_CLOSENESS.getValue()) != 0) {
            outPacket.encodeInt(100); //max closeness available to be gotten
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.MAX_DELAY.getValue()) != 0) {
            outPacket.encodeInt(1000); //idk, 1260?
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.MAX_FULLNESS.getValue()) != 0) {
            outPacket.encodeInt(1000);
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.MAX_ALIVE.getValue()) != 0) {
            outPacket.encodeInt(1); //k ive no idea
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.MAX_MINUTES.getValue()) != 0) {
            outPacket.encodeInt(10); //max minutes?
        }
        outPacket.encodeByte(0); //or 1 then lifeID of affected pot, OR IS THIS 0x80000?
        return outPacket;
    }

    public static final OutPacket spawnFlags(List<Pair<String, Integer>> flags) { //Flag_R_1 to 0, etc

        OutPacket outPacket = new OutPacket(SendPacketOpcode.LOGIN_WELCOME.getValue());
        outPacket.encodeByte(flags == null ? 0 : flags.size());
        if (flags != null) {
            for (Pair<String, Integer> f : flags) {
                outPacket.encodeString(f.left);
                outPacket.encodeByte(f.right);
            }
        }

        return outPacket;
    }

    public static final OutPacket getPVPScoreboard(List<Pair<Integer, MapleCharacter>> flags, int type) { //Flag_R_1 to 0, etc

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_SCOREBOARD.getValue());
        outPacket.encodeShort(flags.size());
        for (Pair<Integer, MapleCharacter> f : flags) {
            outPacket.encodeInt(f.right.getId());
            outPacket.encodeString(f.right.getName());
            outPacket.encodeInt(f.left);
            outPacket.encodeByte(type == 0 ? 0 : (f.right.getTeam() + 1));
        }

        return outPacket;
    }

    public static final OutPacket getPVPResult(List<Pair<Integer, MapleCharacter>> flags, int exp, int winningTeam, int playerTeam) { //Flag_R_1 to 0, etc

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_RESULT.getValue());
        outPacket.encodeInt(flags.size());
        for (Pair<Integer, MapleCharacter> f : flags) {
            outPacket.encodeInt(f.right.getId());
            outPacket.encodeString(f.right.getName());
            outPacket.encodeInt(f.left);
            outPacket.encodeByte(f.right.getTeam() + 1); //??? 1 = bold
            outPacket.encodeByte(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(exp);
        outPacket.encodeByte(0);
        outPacket.encodeShort(100); //exp modifier
        outPacket.encodeInt(0); //delay 600
        outPacket.encodeInt(0);
        // if mode == 1 || mode == 2
        outPacket.encodeByte(winningTeam); //losing team?
        outPacket.encodeByte(playerTeam); //player's team

        return outPacket;
    }

    public static final OutPacket showStatusMessage(final String info, final String data) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(0x16);
        outPacket.encodeString(info); //name got Shield.
        outPacket.encodeString(data); //Shield applied to name.

        return outPacket;
    }

    public static final OutPacket showOwnChampionEffect() {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(0x21/*!GameConstants.GMS ? 0x22 : 0x20*/); //i think
        outPacket.encodeInt(30000); //seconds

        return outPacket;
    }

    public static final OutPacket showChampionEffect(final int from_playerid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        outPacket.encodeInt(from_playerid);
        outPacket.encodeByte(0x21/*!GameConstants.GMS ? 0x22 : 0x20*/);
        outPacket.encodeInt(30000);

        return outPacket;
    }

    public static final OutPacket enablePVP(final boolean enabled) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_ENABLED.getValue());
        outPacket.encodeByte(enabled ? 1 : 2);

        return outPacket;
    }

    public static final OutPacket getPVPMode(final int mode) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_MODE.getValue());
        outPacket.encodeByte(mode); //11 = starting, 0 = started, 4 = ended??? 8 = blue team win???

        return outPacket;
    }

    public static final OutPacket getPVPType(final int type, List<Pair<Integer, String>> players1, final int team, boolean enabled, final int lvl) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_TYPE.getValue());
        outPacket.encodeByte(type); //type is really a byte
        outPacket.encodeByte(lvl); //0 = rookie, 1 = gladiator, etc
        outPacket.encodeByte(enabled ? 1 : 0); //no idea
        if (type > 0) {
            outPacket.encodeByte(team); //the team of the player
            outPacket.encodeInt(players1.size());
            for (Pair<Integer, String> pl : players1) {
                outPacket.encodeInt(pl.left);
                outPacket.encodeString(pl.right);
                outPacket.encodeByte(100);
                outPacket.encodeByte(10);
            }
        }

        return outPacket;
    }

    public static final OutPacket getPVPTeam(List<Pair<Integer, String>> players) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_TEAM.getValue());
        outPacket.encodeInt(players.size());
        for (Pair<Integer, String> pl : players) {
            outPacket.encodeInt(pl.left);
            outPacket.encodeString(pl.right);
            outPacket.encodeByte(100);
            outPacket.encodeByte(10);
        }

        return outPacket;
    }

    public static final OutPacket getPVPScore(int score, boolean kill) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_SCORE.getValue());
        outPacket.encodeInt(score);
        outPacket.encodeByte(kill ? 1 : 0);

        return outPacket;
    }

    public static final OutPacket getPVPIceGage(int score) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_ICEGAGE.getValue());
        outPacket.encodeInt(score);

        return outPacket;
    }

    public static final OutPacket getPVPKilled(String lastWords) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_KILLED.getValue());
        outPacket.encodeString(lastWords); //____ defeated ____.

        return outPacket;
    }

    public static final OutPacket getPVPPoints(int p1, int p2) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_POINTS.getValue());

        outPacket.encodeInt(p1);
        outPacket.encodeInt(p2);

        return outPacket;
    }

    public static final OutPacket getPVPHPBar(int cid, int hp, int maxHp) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_HP.getValue());

        outPacket.encodeInt(cid);
        outPacket.encodeInt(hp);
        outPacket.encodeInt(maxHp);

        return outPacket;
    }

    public static final OutPacket getPVPIceHPBar(int hp, int maxHp) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_ICEKNIGHT.getValue());

        outPacket.encodeInt(hp);
        outPacket.encodeInt(maxHp);

        return outPacket;
    }

    public static final OutPacket getPVPMist(int cid, int mistSkill, int mistLevel, int damage) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_MIST.getValue());
        //DOT
        outPacket.encodeInt(cid);
        outPacket.encodeInt(mistSkill);
        outPacket.encodeByte(mistLevel);
        outPacket.encodeInt(damage);
        outPacket.encodeByte(8); //skill delay
        outPacket.encodeInt(1000);

        return outPacket;
    }

    public static final OutPacket getCaptureFlags(MapleMap map) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CAPTURE_FLAGS.getValue());
        outPacket.encodeRectInt(map.getArea(0));
        outPacket.encodeInt(map.getGuardians().get(0).left.x);
        outPacket.encodeInt(map.getGuardians().get(0).left.y);
        outPacket.encodeRectInt(map.getArea(1));
        outPacket.encodeInt(map.getGuardians().get(1).left.x);
        outPacket.encodeInt(map.getGuardians().get(1).left.y);
        return outPacket;
    }

    public static final OutPacket getCapturePosition(MapleMap map) { //position of flags if they are still at base
        
        final Point p1 = map.getPointOfItem(2910000);
        final Point p2 = map.getPointOfItem(2910001);
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CAPTURE_POSITION.getValue());
        outPacket.encodeByte(p1 == null ? 0 : 1);
        if (p1 != null) {
            outPacket.encodeInt(p1.x);
            outPacket.encodeInt(p1.y);
        }
        outPacket.encodeByte(p2 == null ? 0 : 1);
        if (p2 != null) {
            outPacket.encodeInt(p2.x);
            outPacket.encodeInt(p2.y);
        }

        return outPacket;
    }

    public static final OutPacket resetCapture() {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CAPTURE_RESET.getValue());

        return outPacket;
    }

    public static final OutPacket pvpAttack(int cid, int playerLevel, int skill, int skillLevel, int speed, int mastery, int projectile, int attackCount, int chargeTime, int stance, int direction, int range, int linkSkill, int linkSkillLevel, boolean movementSkill, boolean pushTarget, boolean pullTarget, List<AttackPair> attack) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_ATTACK.getValue());
        outPacket.encodeInt(cid);
        
        outPacket.encodeByte(playerLevel);
        outPacket.encodeInt(skill);
        outPacket.encodeByte(skillLevel);
        outPacket.encodeInt(linkSkill != skill ? linkSkill : 0);
        outPacket.encodeByte(linkSkillLevel != skillLevel ? linkSkillLevel : 0);
        outPacket.encodeByte(direction);
        outPacket.encodeByte(movementSkill ? 1 : 0);
        outPacket.encodeByte(pushTarget ? 1 : 0);
        outPacket.encodeByte(pullTarget ? 1 : 0); //afaik only chains of hell does chains
        outPacket.encodeByte(0); // unk
        outPacket.encodeShort(stance); // 動作

        outPacket.encodeByte(speed);
        outPacket.encodeByte(mastery);
        outPacket.encodeInt(projectile);
        outPacket.encodeInt(chargeTime);
        outPacket.encodeInt(range);
        outPacket.encodeByte(attack.size());
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(attackCount);
        outPacket.encodeByte(0); //idk: probably does something like immobilize target
        for (AttackPair p : attack) {
            outPacket.encodeInt(p.objectid);
            outPacket.encodeInt(0);
            outPacket.encodePosition(p.point);
            outPacket.encodeByte(0);
            outPacket.encodeInt(0);
            for (Pair<Integer, Boolean> atk : p.attack) {
                outPacket.encodeInt(atk.left);
                outPacket.encodeInt(0);
                outPacket.encodeByte(atk.right ? 1 : 0);
                outPacket.encodeByte(0); //1 = no hit
                outPacket.encodeByte(0);
            }
        }

        return outPacket;
    }

    public static final OutPacket pvpSummonAttack(int cid, int playerLevel, int oid, int animation, Point pos, List<AttackPair> attack) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_SUMMON.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(oid);
        outPacket.encodeByte(playerLevel);
        outPacket.encodeByte(animation);
        outPacket.encodePosition(pos);
        outPacket.encodeInt(0); //<-- delay
        outPacket.encodeByte(attack.size());
        for (AttackPair p : attack) {
            outPacket.encodeInt(p.objectid);
            outPacket.encodePosition(p.point);
            outPacket.encodeShort(p.attack.size());
            for (Pair<Integer, Boolean> atk : p.attack) {
                outPacket.encodeInt(atk.left);
            }
        }

        return outPacket;
    }

    public static final OutPacket pvpCool(int cid, List<Integer> attack) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_COOL.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(attack.size());
        for (int b : attack) {
            outPacket.encodeInt(b);
        }
        return outPacket;
    }

    public static OutPacket getPVPClock(int type, int time) { // time in seconds

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CLOCK.getValue());
        outPacket.encodeByte(3);
        outPacket.encodeByte(type);
        outPacket.encodeInt(time);

        return outPacket;
    }

    public static OutPacket getPVPTransform(int type) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PVP_TRANSFORM.getValue());
        outPacket.encodeByte(type); //2?

        return outPacket;
    }

    public static OutPacket changeTeam(int cid, int type) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.LOAD_TEAM.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeByte(type); //2?

        return outPacket;
    }
	
    public static OutPacket getPublicNPCInfo() {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PUBLIC_NPC.getValue());
        outPacket.encodeByte(0);
	    for (int i = 0; i < GameConstants.publicNpcIds.length; i++) {
        	outPacket.encodeInt(GameConstants.publicNpcIds[i]);
		    outPacket.encodeLong(i); //0, level needed
         	outPacket.encodeString(GameConstants.publicNpcs[i]);
		    outPacket.encodeShort(0);
	    }

        return outPacket;
    }
	
    public static OutPacket gainForce(int oid, int gain, int max) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.GAIN_FORCE.getValue());
	outPacket.encodeByte(1);
	outPacket.encodeInt(oid);
	outPacket.encodeByte(1);
	outPacket.encodeInt(gain); //total
	outPacket.encodeInt(max); //gained
	outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket TraitShow() {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.TRAIT_SHOW.getValue());

        outPacket.encodeInt(5); // 性向 0=領導力 1=洞察 2=意志 3=手藝 4=感性 5=魅力
        outPacket.encodeInt(6); // 性向等級 100等是6

        return outPacket;
    }

    public static OutPacket TraitShowForeign(int cid) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.TRAIT_SHOW_FOREIGN.getValue());

        outPacket.encodeInt(cid);
        outPacket.encodeInt(4/*5*/); // 性向 0=領導力 1=洞察 2=意志 3=手藝 4=感性 5=魅力
        outPacket.encodeInt(6); // 性向等級 100等是6

        return outPacket;
    }
}
