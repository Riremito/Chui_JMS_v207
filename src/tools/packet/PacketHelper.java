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

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import client.inventory.Equip;
import client.Skill;
import constants.GameConstants;
import client.inventory.MapleRing;
import client.inventory.MaplePet;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleCoolDownValueHolder;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.MapleQuestStatus;
import client.MapleTrait.MapleTraitType;
import client.MonsterFamiliar;
import client.inventory.Item;
import client.SkillEntry;
import handling.Buffstat;
import java.util.Collection;
import java.util.Date;
import java.util.SimpleTimeZone;
import server.MapleItemInformationProvider;
import server.MapleShop;
import server.MapleShopItem;
import server.Randomizer;
import tools.Pair;
import server.movement.ILifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.AbstractPlayerStore;
import server.shops.IMaplePlayerShop;
import tools.BitTools;
import tools.StringUtil;
import tools.Triple;
import connection.OutPacket;

public class PacketHelper {

    public final static long FT_UT_OFFSET = 116444592000000000L; // EDT
    public final static long MAX_TIME = 150842304000000000L; //00 80 05 BB 46 E6 17 02
    public final static long ZERO_TIME = 94354848000000000L; //00 40 E0 FD 3B 37 4F 01
	public final static long PERMANENT = 150841440000000000L; // 00 C0 9B 90 7D E5 17 02

    public static final long getKoreanTimestamp(final long realTimestamp) {
        return getTime(realTimestamp);
    }

    public static final long getTime(long realTimestamp) {
        if (realTimestamp == -1) {
            return MAX_TIME;
        } else if (realTimestamp == -2) {
            return ZERO_TIME;
		} else if (realTimestamp == -3) {
			return PERMANENT;
        }
        return ((realTimestamp * 10000) + FT_UT_OFFSET);
    }
	
    public static long getFileTimestamp(long timeStampinMillis, boolean roundToMinutes) {
        if (SimpleTimeZone.getDefault().inDaylightTime(new Date())) {
            timeStampinMillis -= 3600000L;
        }
        long time;
        if (roundToMinutes) {
            time = (timeStampinMillis / 1000 / 60) * 600000000;
        } else {
            time = timeStampinMillis * 10000;
        }
        return time + FT_UT_OFFSET;
    }

    public static void addQuestInfo(final OutPacket outPacket, final MapleCharacter chr) {
        final List<MapleQuestStatus> started = chr.getStartedQuests();
        outPacket.encodeByte(true); //idk

        outPacket.encodeShort(started.size());
        for (final MapleQuestStatus q : started) {
	    outPacket.encodeShort(q.getQuest().getId());
            if (q.hasMobKills()) {
                final StringBuilder sb = new StringBuilder();
                for (final int kills : q.getMobKills().values()) {
                    sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
                }
                outPacket.encodeString(sb.toString());
            } else {
                outPacket.encodeString(q.getCustomData() == null ? "" : q.getCustomData());
            }
        }
        
        outPacket.encodeShort(0); // -> str str

//	    outPacket.encodeByte(true); //idk

        final List<MapleQuestStatus> completed = chr.getCompletedQuests();
        outPacket.encodeShort(completed.size());
        for (final MapleQuestStatus q : completed) {
	        outPacket.encodeShort(q.getQuest().getId());
            outPacket.encodeLong(getTime(q.getCompletionTime()));
        }
    }

    public static final void addSkillInfo(final OutPacket outPacket, final MapleCharacter chr) {
        final Map<Skill, SkillEntry> skills = chr.getSkills();
        outPacket.encodeShort(skills.size());
        for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
            outPacket.encodeInt(skill.getKey().getId());
            outPacket.encodeInt(skill.getValue().skillevel);
            addExpirationTime(outPacket, skill.getValue().expiration);

            if (skill.getKey().isFourthJob()) {
                outPacket.encodeInt(skill.getValue().masterlevel);
            }
        }
    }

    public static final void addCoolDownInfo(final OutPacket outPacket, final MapleCharacter chr) {
        final List<MapleCoolDownValueHolder> cd = chr.getCooldowns();
        outPacket.encodeShort(cd.size());
        for (final MapleCoolDownValueHolder cooling : cd) {
            outPacket.encodeInt(cooling.skillId);
            outPacket.encodeShort((int) (cooling.length + cooling.startTime - System.currentTimeMillis()) / 1000);
        }
    }

    public static final void addRocksInfo(final OutPacket outPacket, final MapleCharacter chr) {
        final int[] mapz = chr.getRegRocks();
        for (int i = 0; i < 5; i++) { // VIP teleport map
            outPacket.encodeInt(mapz[i]);
        }

        final int[] map = chr.getRocks();
        for (int i = 0; i < 10; i++) { // VIP teleport map
            outPacket.encodeInt(map[i]);
        }

        final int[] maps = chr.getHyperRocks();
        for (int i = 0; i < 13; i++) { // VIP teleport map
            outPacket.encodeInt(maps[i]);
        }
/*        for (int i = 0; i < 13; i++) { // VIP teleport map
            outPacket.encodeInt(maps[i]);
        }*/
    }

    public static final void addRingInfo(final OutPacket outPacket, final MapleCharacter chr) {
        outPacket.encodeShort(0); // minigame
        //01 00 = size
        //01 00 00 00 = gametype?
        //03 00 00 00 = win
        //00 00 00 00 = tie/loss
        //01 00 00 00 = tie/loss
        //16 08 00 00 = points
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> aRing = chr.getRings(true);
        List<MapleRing> cRing = aRing.getLeft();
        outPacket.encodeShort(cRing.size());
        for (MapleRing ring : cRing) {
            outPacket.encodeInt(ring.getPartnerChrId());
            outPacket.encodeString(ring.getPartnerName(), 13);
            outPacket.encodeLong(ring.getRingId());
            outPacket.encodeLong(ring.getPartnerRingId());
        }
        List<MapleRing> fRing = aRing.getMid();
        outPacket.encodeShort(fRing.size());
        for (MapleRing ring : fRing) {
            outPacket.encodeInt(ring.getPartnerChrId());
            outPacket.encodeString(ring.getPartnerName(), 13);
            outPacket.encodeLong(ring.getRingId());
            outPacket.encodeLong(ring.getPartnerRingId());
            outPacket.encodeInt(ring.getItemId());
        }
        List<MapleRing> mRing = aRing.getRight();
        outPacket.encodeShort(mRing.size());
        int marriageId = 30000;
        for (MapleRing ring : mRing) {
            outPacket.encodeInt(marriageId);
            outPacket.encodeInt(chr.getId());
            outPacket.encodeInt(ring.getPartnerChrId());
            outPacket.encodeShort(3); //1 = engaged 3 = married
            outPacket.encodeInt(ring.getItemId());
            outPacket.encodeInt(ring.getItemId());
            outPacket.encodeString(chr.getName(), 13);
            outPacket.encodeString(ring.getPartnerName(), 13);
        }
    }

    public static void addInventoryInfo(OutPacket outPacket, MapleCharacter chr) {
        outPacket.encodeInt(chr.getMeso()); // mesos
        outPacket.encodeInt(chr.getId());
        outPacket.encodeLong(0); // 小鋼珠
        outPacket.encodeInt(0);
        outPacket.encodeLong(0);

        outPacket.encodeInt(0); // 1+1 藥水

        outPacket.encodeByte(chr.getInventory(MapleInventoryType.EQUIP).getSlotLimit()); // equip slots
        outPacket.encodeByte(chr.getInventory(MapleInventoryType.USE).getSlotLimit()); // use slots
        outPacket.encodeByte(chr.getInventory(MapleInventoryType.SETUP).getSlotLimit()); // set-up slots
        outPacket.encodeByte(chr.getInventory(MapleInventoryType.ETC).getSlotLimit()); // etc slots
        outPacket.encodeByte(chr.getInventory(MapleInventoryType.CASH).getSlotLimit()); // cash slots

        final MapleQuestStatus stat = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        if (stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()) {
            outPacket.encodeLong(getTime(Long.parseLong(stat.getCustomData())));
        } else {
            outPacket.encodeLong(getTime(-2));
        }
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        for (Item item : equipped) {
            if (item.getPosition() < 0 && item.getPosition() > -100) {
                addItemInfo(outPacket, item, false, false, false, false, chr);
            }
        }
        outPacket.encodeShort(0); // start of equipped nx
        for (Item item : equipped) {
            if (item.getPosition() <= -100 && item.getPosition() > -1000) {
                addItemInfo(outPacket, item, false, false, false, false, chr);
            }
        }

        outPacket.encodeShort(0); // start of equip inventory
        iv = chr.getInventory(MapleInventoryType.EQUIP);
        for (Item item : iv.list()) {
            addItemInfo(outPacket, item, false, false, false, false, chr);
        }
        outPacket.encodeShort(0); //start of evan equips

        for (Item item : equipped) {
            if (item.getPosition() <= -1000 && item.getPosition() > -1100) {
                addItemInfo(outPacket, item, false, false, false, false, chr);
            }
        }
        outPacket.encodeShort(0); //start of mechanic equips, ty KDMS

        for (Item item : equipped) {
            if (item.getPosition() <= -1100 && item.getPosition() > -1200) {
                addItemInfo(outPacket, item, false, false, false, false, chr);
            }
        }
        outPacket.encodeShort(0); // start of android equips
        for (Item item : equipped) {
            if (item.getPosition() <= -1200) {
                addItemInfo(outPacket, item, false, false, false, false, chr);
            }
        }
        outPacket.encodeShort(0); // start of use inventory
        iv = chr.getInventory(MapleInventoryType.USE);
        for (Item item : iv.list()) {
            addItemInfo(outPacket, item, false, false, false, false, chr);
        }
        outPacket.encodeByte(0); // start of set-up inventory
        iv = chr.getInventory(MapleInventoryType.SETUP);
        for (Item item : iv.list()) {
            addItemInfo(outPacket, item, false, false, false, false, chr);
        }
        outPacket.encodeByte(0); // start of etc inventory
        iv = chr.getInventory(MapleInventoryType.ETC);
        for (Item item : iv.list()) {
            if (item.getPosition() < 100) {
                addItemInfo(outPacket, item, false, false, false, false, chr);
            }
        }
        outPacket.encodeByte(0); // start of cash inventory
        iv = chr.getInventory(MapleInventoryType.CASH);
        for (Item item : iv.list()) {
            addItemInfo(outPacket, item, false, false, false, false, chr);
        }
        outPacket.encodeByte(0);

        for (int i = 0; i < chr.getExtendedSlots().size(); i++) {
            outPacket.encodeInt(i);
            outPacket.encodeInt(chr.getExtendedSlot(i));
            for (Item item : chr.getInventory(MapleInventoryType.ETC).list()) {
                if (item.getPosition() > (i * 100 + 100) && item.getPosition() < (i * 100 + 200)) {
                    addItemInfo(outPacket, item, false, false, false, true, chr);
                }
            }
            outPacket.encodeInt(-1);
        }

        outPacket.encodeInt(-1);
        
//        outPacket.encodeInt(0);

        outPacket.encodeInt(0);

        outPacket.encodeByte(0);
    }

    public static final void addCharStats(final OutPacket outPacket, final MapleCharacter chr) {
        outPacket.encodeInt(chr.getId()); // character id
        outPacket.encodeString(chr.getName(), 13);
        outPacket.encodeByte(chr.getGender()); // gender (0 = male, 1 = female)
        outPacket.encodeByte(chr.getSkinColor()); // skin color
        outPacket.encodeInt(chr.getFace()); // face
        outPacket.encodeInt(chr.getHair()); // hair
        outPacket.encodeZeroBytes(24);
        outPacket.encodeByte(chr.getLevel()); // level
        outPacket.encodeShort(chr.getJob()); // job
        chr.getStat().connectData(outPacket);
        outPacket.encodeShort(chr.getRemainingAp()); // remaining ap
        if (GameConstants.isEvan(chr.getJob()) || GameConstants.isResist(chr.getJob()) || GameConstants.isMercedes(chr.getJob())) {
            final int size = chr.getRemainingSpSize();
            outPacket.encodeByte(size);
            for (int i = 0; i < chr.getRemainingSps().length; i++) {
                if (chr.getRemainingSp(i) > 0) {
                    outPacket.encodeByte(i + 1);
                    outPacket.encodeByte(chr.getRemainingSp(i));
                }
            }
        } else {
            outPacket.encodeShort(chr.getRemainingSp()); // remaining sp
        }
        outPacket.encodeInt(chr.getExp()); // exp
        outPacket.encodeInt(chr.getFame()); // fame
        outPacket.encodeInt(chr.getGachExp()); // Gachapon exp
        outPacket.encodeInt(chr.getMapId()); // current map id
        outPacket.encodeByte(chr.getInitialSpawnpoint()); // spawnpoint
        outPacket.encodeShort(chr.getSubcategory()); //1 here = db
/*        if (GameConstants.isDemon(chr.getJob())) {
            mplew.writeInt(chr.getDemonMarking());
        }*/
        outPacket.encodeByte(chr.getFatigue());
        outPacket.encodeInt(GameConstants.getCurrentDate());
        for (MapleTraitType t : MapleTraitType.values()) {
            outPacket.encodeInt(chr.getTrait(t).getTotalExp());
        }
        for (MapleTraitType t : MapleTraitType.values()) {
             outPacket.encodeShort(0); // 今天獲得的性向EXP
        }
        outPacket.encodeInt(chr.getStat().pvpExp); //pvp exp
        outPacket.encodeByte(chr.getStat().pvpRank); //pvp rank
        outPacket.encodeInt(chr.getBattlePoints()); //pvp points
        outPacket.encodeByte(5); //idk
        outPacket.encodeInt(0); // JUMP之後才有 金槍戰鬥機 綠能活動
        outPacket.encodeLong(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
    }

    public static final void addCharLook(final OutPacket outPacket, final MapleCharacter chr, final boolean mega) {
        outPacket.encodeByte(chr.getGender());
        outPacket.encodeByte(chr.getSkinColor());
        outPacket.encodeInt(chr.getFace());
        outPacket.encodeInt(chr.getJob());
        outPacket.encodeByte(mega ? 0 : 1);
        outPacket.encodeInt(chr.getHair());

        final Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
        final Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
        MapleInventory equip = chr.getInventory(MapleInventoryType.EQUIPPED);

        for (final Item item : equip.newList()) {
            if (item.getPosition() < -127) { //not visible
                continue;
            }
            byte pos = (byte) (item.getPosition() * -1);

            if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, item.getItemId());
            } else if (pos > 100 && pos != 111) {
                pos = (byte) (pos - 100);
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getItemId());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getItemId());
            }
        }
        for (final Entry<Byte, Integer> entry : myEquip.entrySet()) {
            outPacket.encodeByte(entry.getKey());
            outPacket.encodeInt(entry.getValue());
        }
        outPacket.encodeByte(0xFF); // end of visible itens
        // masked itens
        for (final Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            outPacket.encodeByte(entry.getKey());
            outPacket.encodeInt(entry.getValue());
        }
        outPacket.encodeByte(0xFF); // ending markers

        final Item cWeapon = equip.getItem((byte) -111);
        outPacket.encodeInt(cWeapon != null ? cWeapon.getItemId() : 0);

        outPacket.encodeByte(GameConstants.isMercedes(chr.getJob()) ? 1 : 0); // 耳朵
        
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
/*        if (GameConstants.isDemon(chr.getJob())) {
            outPacket.encodeInt(chr.getDemonMarking());
        }*/
    }

    public static final void addExpirationTime(final OutPacket outPacket, final long time) {
        outPacket.encodeLong(getTime(time));
    }

    public static final void addItemInfo(final OutPacket outPacket, final Item item, final boolean zeroPosition, final boolean leaveOut) {
        addItemInfo(outPacket, item, zeroPosition, leaveOut, false, false, null);
    }

    public static final void addItemInfo(final OutPacket outPacket, final Item item, final boolean zeroPosition, final boolean leaveOut, final boolean trade) {
        addItemInfo(outPacket, item, zeroPosition, leaveOut, trade, false, null);
    }

    public static final void addItemInfo(final OutPacket outPacket, final Item item, final boolean zeroPosition, final boolean leaveOut, final boolean trade, final boolean bagSlot, final MapleCharacter chr) {
        short pos = item.getPosition();
        if (zeroPosition) {
            if (!leaveOut) {
                outPacket.encodeByte(0);
            }
        } else {
            if (pos <= -1) {
                pos *= -1;
                if (pos > 100 && pos < 1000) {
                    pos -= 100;
                }
            }
            if (bagSlot) {
                outPacket.encodeInt((pos % 100) - 1);
            } else if (!trade && item.getType() == 1) {
                outPacket.encodeShort(pos);
            } else {
                outPacket.encodeByte(pos);
            }
        }
        outPacket.encodeByte(item.getPet() != null ? 3 : item.getType());
        outPacket.encodeInt(item.getItemId());
        boolean hasUniqueId = item.getUniqueId() > 0 && !GameConstants.isMarriageRing(item.getItemId()) && item.getItemId() / 10000 != 166;
        //marriage rings arent cash items so dont have uniqueids, but we assign them anyway for the sake of rings
        outPacket.encodeByte(hasUniqueId ? 1 : 0);
        if (hasUniqueId) {
            outPacket.encodeLong(item.getUniqueId());
        }

        if (item.getPet() != null) { // Pet
            addPetItemInfo(outPacket, item, item.getPet(), true);
        } else {
            addExpirationTime(outPacket, item.getExpiration());
            outPacket.encodeInt(chr == null ? -1 : chr.getExtendedSlots().indexOf(item.getItemId()));
            if (item.getType() == 1) {
                final Equip equip = (Equip) item;
                outPacket.encodeByte(equip.getUpgradeSlots());
                outPacket.encodeByte(equip.getLevel());
                outPacket.encodeShort(equip.getStr());
                outPacket.encodeShort(equip.getDex());
                outPacket.encodeShort(equip.getInt());
                outPacket.encodeShort(equip.getLuk());
                outPacket.encodeShort(equip.getHp());
                outPacket.encodeShort(equip.getMp());
                outPacket.encodeShort(equip.getWatk());
                outPacket.encodeShort(equip.getMatk());
                outPacket.encodeShort(equip.getWdef());
                outPacket.encodeShort(equip.getMdef());
                outPacket.encodeShort(equip.getAcc());
                outPacket.encodeShort(equip.getAvoid());
                outPacket.encodeShort(equip.getHands());
                outPacket.encodeShort(equip.getSpeed());
                outPacket.encodeShort(equip.getJump());
                outPacket.encodeString(equip.getOwner());
                outPacket.encodeShort(equip.getFlag());
                outPacket.encodeByte(equip.getIncSkill() > 0 ? 1 : 0);
                outPacket.encodeByte(Math.max(equip.getBaseLevel(), equip.getEquipLevel())); // Item level
                outPacket.encodeInt(equip.getExpPercentage() * 100000); // Item Exp... 10000000 = 100%
                outPacket.encodeInt(equip.getDurability());
                outPacket.encodeInt(equip.getViciousHammer());
                outPacket.encodeShort(equip.getPVPDamage()); //OR is it below MPR? TODOO
                outPacket.encodeByte(equip.getState()); //7 = unique for the lulz
                outPacket.encodeByte(equip.getEnhance());
                outPacket.encodeShort(equip.getPotential1());
                outPacket.encodeShort(equip.getPotential2());
                outPacket.encodeShort(equip.getPotential3());
                outPacket.encodeShort(equip.getHpR());
                outPacket.encodeShort(equip.getMpR());
                if (!hasUniqueId) {
//                    System.out.println(equip.getInventoryId());
                    outPacket.encodeLong(equip.getInventoryId() <= 0 ? -1 : equip.getInventoryId()); //some tracking ID
                }
                outPacket.encodeLong(getTime(-2));
                outPacket.encodeInt(-1);

                // 下面只有日版有
                outPacket.encodeInt(0);
                outPacket.encodeByte(0);
                outPacket.encodeByte(1/*0*/); // ブランド印章 flag要等於0x80還有這個等於1才會顯示「owner」の渾身の力作
                outPacket.encodeShort(0); // 魂武
            } else {
                outPacket.encodeShort(item.getQuantity());
                outPacket.encodeString(item.getOwner());
                outPacket.encodeShort(item.getFlag());
                final int itemid = item.getItemId();
                if (GameConstants.isThrowingStar(itemid) || GameConstants.isBullet(itemid) || itemid / 10000 == 287 || itemid / 10000 == 288 || itemid / 10000 == 289) {
                    outPacket.encodeLong(item.getInventoryId() <= 0 ? -1 : item.getInventoryId());
		        }
                // 下面是訓獸師的familiar
                outPacket.encodeInt(0);
                outPacket.encodeShort(0);
                outPacket.encodeShort(0);
                outPacket.encodeShort(0);
                outPacket.encodeShort(0);
                outPacket.encodeLong(0);
                outPacket.encodeLong(0);
                outPacket.encodeLong(0);
                outPacket.encodeLong(0);
                outPacket.encodeInt(0);
                outPacket.encodeShort(0);
            }
        }
    }

    public static final void serializeMovementList(final OutPacket outPacket, final List<ILifeMovementFragment> moves) {
        outPacket.encodeByte(moves.size());
        for (ILifeMovementFragment move : moves) {
            move.serialize(outPacket);
        }
    }

    public static final void addAnnounceBox(final OutPacket outPacket, final MapleCharacter chr) {
        if (chr.getPlayerShop() != null && chr.getPlayerShop().isOwner(chr) && chr.getPlayerShop().getShopType() != 1 && chr.getPlayerShop().isAvailable()) {
            addInteraction(outPacket, chr.getPlayerShop());
        } else {
            outPacket.encodeByte(0);
        }
    }

    public static final void addInteraction(final OutPacket outPacket, IMaplePlayerShop shop) {
        outPacket.encodeByte(shop.getGameType());
        outPacket.encodeInt(((AbstractPlayerStore) shop).getObjectId());
        outPacket.encodeString(shop.getDescription());
        if (shop.getShopType() != 1) {
            outPacket.encodeByte(shop.getPassword().length() > 0 ? 1 : 0); //password = false
        }
        outPacket.encodeByte(shop.getItemId() % 10);
        outPacket.encodeByte(shop.getSize()); //current size
        outPacket.encodeByte(shop.getMaxSize()); //full slots... 4 = 4-1=3 = has slots, 1-1=0 = no slots
        if (shop.getShopType() != 1) {
            outPacket.encodeByte(shop.isOpen() ? 0 : 1);
        }
    }

    public static final void addCharacterInfo(final OutPacket outPacket, final MapleCharacter chr) {
//        long mask = 0xFF_FF_F7_FF_FF_BB_BD_FFL;
        long mask = 0xFF_FF_FF_FE_FF_FF_FF_FFL;
        outPacket.encodeLong(mask/*-1*/);
        outPacket.encodeZeroBytes(7); // 1 + 1 + 4 + 1
        addCharStats(outPacket, chr);
        outPacket.encodeByte(chr.getBuddylist().getCapacity());
        // Bless
        if (chr.getBlessOfFairyOrigin() != null) {
            outPacket.encodeByte(1);
            outPacket.encodeString(chr.getBlessOfFairyOrigin());
        } else {
            outPacket.encodeByte(0);
        }
        if (chr.getBlessOfEmpressOrigin() != null) {
            outPacket.encodeByte(1);
            outPacket.encodeString(chr.getBlessOfEmpressOrigin());
        } else {
            outPacket.encodeByte(0);
        }
        final MapleQuestStatus ultExplorer = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER));
        if (ultExplorer != null && ultExplorer.getCustomData() != null) {
            outPacket.encodeByte(1);
            outPacket.encodeString(ultExplorer.getCustomData());
        } else {
            outPacket.encodeByte(0);
        }
        //AFTERSHOCK: EMPRESS ORIGIN (same structure)
        //AFTERSHOCK: UA LINK TO CYGNUS (same structure)
        // End
        addInventoryInfo(outPacket, chr);
        addSkillInfo(outPacket, chr);
        addCoolDownInfo(outPacket, chr);
        addQuestInfo(outPacket, chr);
        addRingInfo(outPacket, chr);
        addRocksInfo(outPacket, chr);
        outPacket.encodeShort(0);
        addMonsterBookInfo(outPacket, chr); // 0x20000 0x10000 0x40000000
        outPacket.encodeShort(0); // 0x80000000
//        addFamiliarInfo(outPacket, chr); //
        chr.QuestInfoPacket(outPacket); // 0x40000 for every questinfo: int16_t questid, string questdata
        if (chr.getJob() >= 3300 && chr.getJob() <= 3312) { //wh
            addJaguarInfo(outPacket, chr);
        }
        outPacket.encodeShort(0); // [short]+[long]
        outPacket.encodeShort(0);
        outPacket.encodeShort(0); // [short]+[short]
    }

    public static final void addMonsterBookInfo(final OutPacket outPacket, final MapleCharacter chr) {
        outPacket.encodeInt(0); //something
        if (chr.getMonsterBook().getSetScore() > 0) {
            chr.getMonsterBook().writeFinished(outPacket);
        } else {
            chr.getMonsterBook().writeUnfinished(outPacket);
        }
        outPacket.encodeInt(chr.getMonsterBook().getSet());
    }
    
    public static final void addFamiliarInfo(final OutPacket outPacket, final MapleCharacter chr) {
        outPacket.encodeInt(chr.getFamiliars().size()); //size
        int totalVitality = 0;
        for (MonsterFamiliar mf : chr.getFamiliars().values()) {
            mf.writeRegisterPacket(outPacket, true);
            totalVitality += mf.getVitality();
        }
        outPacket.encodeInt(0/*totalVitality*/); //size of ALL not just stacked
/*        for (MonsterFamiliar mf : chr.getFamiliars().values()) {
            for (int i = 0; i < mf.getVitality(); i++) {
                outPacket.encodeInt(chr.getId());
                outPacket.encodeInt(mf.getFamiliar());
                outPacket.encodeLong(mf.getId() + (100000 * i)); //fake it like a pro
                outPacket.encodeByte(1);
            }
        }*/
    }

    public static final void addPetItemInfo(final OutPacket outPacket, final Item item, final MaplePet pet, final boolean active) {
        //PacketHelper.addExpirationTime(outPacket, -1); //always
        if (item == null) {
            outPacket.encodeLong(PacketHelper.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
        } else {
            PacketHelper.addExpirationTime(outPacket, item.getExpiration() <= System.currentTimeMillis() ? -1 : item.getExpiration());
        }
        outPacket.encodeInt(-1);
        outPacket.encodeString(pet.getName(), 13);
        outPacket.encodeByte(pet.getLevel());
        outPacket.encodeShort(pet.getCloseness());
        outPacket.encodeByte(pet.getFullness());
        if (item == null) {
            outPacket.encodeLong(PacketHelper.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
        } else {
            PacketHelper.addExpirationTime(outPacket, item.getExpiration() <= System.currentTimeMillis() ? -1 : item.getExpiration());
        }
        outPacket.encodeShort(0);
        outPacket.encodeShort(pet.getFlags());
        outPacket.encodeInt(pet.getPetItemId() == 5000054 && pet.getSecondsLeft() > 0 ? pet.getSecondsLeft() : 0); //in seconds, 3600 = 1 hr.
        outPacket.encodeShort(0);
        outPacket.encodeByte(active ? (pet.getSummoned() ? pet.getSummonedValue() : 0) : 0); // 1C 5C 98 C6 01
        outPacket.encodeInt(0); //0x40 before, changed to 0?
    }

    public static final void addShopInfo(final OutPacket outPacket, final MapleShop shop, final MapleClient c) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        outPacket.encodeByte(shop.getRanks().size() > 0 ? 1 : 0);
        if (shop.getRanks().size() > 0) {
            outPacket.encodeByte(shop.getRanks().size());
            for (Pair<Integer, String> s : shop.getRanks()) {
                outPacket.encodeInt(s.left);
                outPacket.encodeString(s.right);
            }
        }
        outPacket.encodeShort(shop.getItems().size() + c.getPlayer().getRebuy().size()); // item count
        for (MapleShopItem item : shop.getItems()) {
            addShopItemInfo(outPacket, item, shop, ii, null);
        }
        for (Item i : c.getPlayer().getRebuy()) {
            addShopItemInfo(outPacket, new MapleShopItem(i.getItemId(), (int) ii.getPrice(i.getItemId()), i.getQuantity()), shop, ii, i);
        }
    }

    public static final void addShopItemInfo(final OutPacket outPacket, final MapleShopItem item, final MapleShop shop, final MapleItemInformationProvider ii, final Item i) {
        outPacket.encodeInt(item.getItemId());
        outPacket.encodeInt(item.getPrice());
        outPacket.encodeInt(item.getReqItem());
        outPacket.encodeInt(item.getReqItemQ());
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0); //category? 7 = special
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        
        if (!GameConstants.isThrowingStar(item.getItemId()) && !GameConstants.isBullet(item.getItemId())) {
            outPacket.encodeShort(1); // stacksize o.o
	        outPacket.encodeShort(item.getBuyable());
        } else {
            outPacket.encodeZeroBytes(6);
            outPacket.encodeShort(BitTools.doubleToShortBits(ii.getPrice(item.getItemId())));
	        outPacket.encodeShort(ii.getSlotMax(item.getItemId()));
        }
	//outPacket.encodeShort(ii.getSlotMax(item.getItemId())); //its this for both for official servers, who cares though
	    if (GameConstants.GMS) { // 前面還有一個道具循環沒這個
	        outPacket.encodeByte(i == null ? 0 : 1);
	        if (i != null) {
		        addItemInfo(outPacket, i, true, true);
	        }
	    }

        if (shop.getRanks().size() > 0) {
            outPacket.encodeByte(item.getRank() >= 0 ? 1 : 0);
            if (item.getRank() >= 0) {
                outPacket.encodeByte(item.getRank());
            }
        }
    }

    public static final void addJaguarInfo(final OutPacket outPacket, final MapleCharacter chr) {
        outPacket.encodeByte(chr.getIntNoRecord(GameConstants.JAGUAR));
        outPacket.encodeZeroBytes(20); //probably mobID of the 5 mobs that can be captured.
    }

    public static <E extends Buffstat> void writeSingleMask(OutPacket outPacket, E statup) {
        for (int i = GameConstants.MAX_BUFFSTAT; i >= 1; i--) {
            outPacket.encodeInt(i == statup.getPosition() ? statup.getValue() : 0);
        }
    }

    public static <E extends Buffstat> void writeMask(OutPacket outPacket, Collection<E> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups) {
            mask[statup.getPosition() - 1] |= statup.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            outPacket.encodeInt(mask[i - 1]);
        }
    }

    public static <E extends Buffstat> void writeBuffMask(OutPacket outPacket, Collection<Pair<E, Integer>> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (Pair<E, Integer> statup : statups) {
            mask[statup.left.getPosition() - 1] |= statup.left.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            outPacket.encodeInt(mask[i - 1]);
        }
    }

    public static <E extends Buffstat> void writeBuffMask(OutPacket outPacket, Map<E, Integer> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups.keySet()) {
            mask[statup.getPosition() - 1] |= statup.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            outPacket.encodeInt(mask[i - 1]);
        }
    }
}
