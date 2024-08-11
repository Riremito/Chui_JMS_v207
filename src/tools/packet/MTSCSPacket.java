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

import java.sql.SQLException;
import java.sql.ResultSet;


import java.util.List;
import client.MapleClient;
import client.MapleCharacter;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import server.CashShop;
import server.CashItemFactory;
import server.CashItemInfo.CashModInfo;

import handling.SendPacketOpcode;
import constants.ServerConstants;
import java.util.Collection;
import tools.Pair;
import java.util.Map;
import java.util.Map.Entry;
import server.MTSStorage.MTSItemInfo;
import tools.HexTool;
import connection.OutPacket;

public class MTSCSPacket {

//    private static final byte[] warpCS = HexTool.getByteArrayFromHexString("00 00 00 02 00 00 00 30 00 00 00 90 00 06 00 A6 00 08 07 A0 EB 60 06 20 26CE 05 04 00 02 00 A4 00 0A 07 90 35 13 10 98 B5 12 10 78 00 4C 00 65 00 76 00 65 00 6C 00 00 00 00 00 02 00 06 00 98 01 08 07 02 00 00 00 30 00 00 00 02 00 08 00 9E 01 08 07 02 00 00 00 31 00 00 00 05 00 0A 00 9C 00 08 07 A0 01 15 00 40 5A 11 10 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00");
//    private static final byte[] warpCS_GMS = HexTool.getByteArrayFromHexString("00 00 00 00 00 02 00 00 00 31 00 00 00 0A 00 10 00 12 00 0E 07 E0 3B 8B 0B 60 CE 8A 0B 69 00 6C 00 6C 00 2F 00 35 00 33 00 32 00 30 00 30 00 31 00 31 00 2F 00 73 00 75 00 6D 00 6D 00 6F 00 6E 00 2F 00 61 00 74 00 74 00 61 00 63 00 6B 00 31 00 2F 00 31 0000 00 00 00 00 00 00 00 02 00 1A 00 04 01 08 07 02 00 00 00 32 00 00 00 05 00 1C 00 06 00 08 07 A0 01 2E 00 58 CD 8A 0B");
    
    public static OutPacket warpCS(MapleClient c) {
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPEN.getValue());

        PacketHelper.addCharacterInfo(outPacket, c.getPlayer());
        
        outPacket.encodeString(c.getAccountName());

	    outPacket.encodeInt(0); //amount of new items? then each SN | size: 0x70, writes ids from 10300107 to 10300218
        Collection<CashModInfo> cmi = CashItemFactory.getInstance().getAllModInfo();
        outPacket.encodeShort(cmi.size());
        for (CashModInfo cm : cmi) {
            addModCashItemInfo(outPacket, cm);
        }
        outPacket.encodeShort(0);
        outPacket.encodeByte(0);

        outPacket.encodeZeroBytes(120);
//        outPacket.encodeArr(GameConstants.GMS ? warpCS_GMS : warpCS);
        int[] itemz = CashItemFactory.getInstance().getBestItems();
        for (int i = 1; i <= 8; i++) { // 960
            for (int j = 0; j <= 1; j++) {
                for (int item = 0; item < itemz.length; item++) {
                    outPacket.encodeInt(i);
                    outPacket.encodeInt(j);
                    outPacket.encodeInt(itemz[item]);
                }
            }
        }
        outPacket.encodeShort(0);
        outPacket.encodeLong(0);
        outPacket.encodeShort(0);
        outPacket.encodeLong(0);
        
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
//        outPacket.encodeZeroBytes(7);

        return outPacket;
    }

    public static OutPacket playCashSong(int itemid, String name) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CASH_SONG.getValue());
        outPacket.encodeInt(itemid);
        outPacket.encodeString(name);
        return outPacket;
    }

    public static OutPacket useCharm(byte charmsleft, byte daysleft) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(8);
        outPacket.encodeByte(1);
        outPacket.encodeByte(charmsleft);
        outPacket.encodeByte(daysleft);

        return outPacket;
    }

    public static OutPacket useWheel(byte charmsleft) { // 運命の車輪 原地復活術
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(22);
        outPacket.encodeLong(charmsleft);

        return outPacket;
    }

    public static OutPacket itemExpired(int itemid) {
        
        // 1E 00 02 83 C9 51 00

        // 21 00 08 02
        // 50 62 25 00
        // 50 62 25 00
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(2);
        outPacket.encodeInt(itemid);

        return outPacket;
    }

    public static OutPacket ViciousHammer(boolean start, int hammered) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        outPacket.encodeByte(start ? 61 : 65);
        outPacket.encodeInt(0);
        outPacket.encodeInt(hammered);
        return outPacket;
    }

    public static OutPacket changePetFlag(int uniqueId, boolean added, int flagAdded) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PET_FLAG_CHANGE.getValue());

        outPacket.encodeLong(uniqueId);
        outPacket.encodeByte(added ? 1 : 0);
        outPacket.encodeShort(flagAdded);

        return outPacket;
    }

    public static OutPacket changePetName(MapleCharacter chr, String newname, int slot) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PET_NAMECHANGE.getValue());

        outPacket.encodeInt(chr.getId());
        outPacket.encodeInt(slot);
        outPacket.encodeString(newname);

        return outPacket;
    }

    public static OutPacket showNotes(ResultSet notes, int count) throws SQLException {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_NOTES.getValue());
        outPacket.encodeByte(3);
        outPacket.encodeByte(count);
        for (int i = 0; i < count; i++) {
            outPacket.encodeInt(notes.getInt("id"));
            outPacket.encodeString(notes.getString("from"));
            outPacket.encodeString(notes.getString("message"));
            outPacket.encodeLong(PacketHelper.getKoreanTimestamp(notes.getLong("timestamp")));
            outPacket.encodeByte(notes.getInt("gift"));
            notes.next();
        }

        return outPacket;
    }

    public static OutPacket useChalkboard(final int charid, final String msg) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CHALKBOARD.getValue());

        outPacket.encodeInt(charid);
        if (msg == null || msg.length() <= 0) {
            outPacket.encodeByte(0);
        } else {
            outPacket.encodeByte(1);
            outPacket.encodeString(msg);
        }

        return outPacket;
    }

    public static OutPacket getTrockRefresh(MapleCharacter chr, byte vip, boolean delete) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.TROCK_LOCATIONS.getValue());
        outPacket.encodeByte(delete ? 2 : 3);
        outPacket.encodeByte(vip);
        if (vip == 1) {
            int[] map = chr.getRocks();
            for (int i = 0; i < 10; i++) {
                outPacket.encodeInt(map[i]);
            }
        } else if (vip >= 2) {
            int[] map = chr.getHyperRocks();
            for (int i = 0; i < 13; i++) {
                outPacket.encodeInt(map[i]);
            }
        } else {
            int[] map = chr.getRegRocks();
            for (int i = 0; i < 5; i++) {
                outPacket.encodeInt(map[i]);
            }
        }
        return outPacket;
    }

    public static OutPacket sendWishList(MapleCharacter chr, boolean update) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte((update ? 0x5F : 0x5B)); //+12
        int[] list = chr.getWishlist();
        for (int i = 0; i < 10; i++) {
            outPacket.encodeInt(list[i] != -1 ? list[i] : 0);
        }
        return outPacket;
    }

    public static OutPacket showNXMapleTokens(MapleCharacter chr) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_UPDATE.getValue());
        outPacket.encodeInt(GameConstants.GMS ? 0 : chr.getCSPoints(1)); // A-cash or NX Credit
        outPacket.encodeInt(chr.getCSPoints(2)); // MPoint
//	outPacket.encodeInt(GameConstants.GMS ? chr.getCSPoints(1) : 0); //something or NX Prepaid

        return outPacket;
    }

    public static OutPacket showBoughtCSPackage(Map<Integer, Item> ccc, int accid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0xA3 : 0x97); //use to be 7a
        outPacket.encodeByte(ccc.size());
        int size = 0;
        for (Entry<Integer, Item> sn : ccc.entrySet()) {
            addCashItemInfo(outPacket, sn.getValue(), accid, sn.getKey().intValue());
            if (GameConstants.isPet(sn.getValue().getItemId()) || GameConstants.getInventoryType(sn.getValue().getItemId()) == MapleInventoryType.EQUIP) {
                size++;
            }
        }
        if (ccc.size() > 0) {
            outPacket.encodeInt(size);
            for (Item itemz : ccc.values()) {
                if (GameConstants.isPet(itemz.getItemId()) || GameConstants.getInventoryType(itemz.getItemId()) == MapleInventoryType.EQUIP) {
                    PacketHelper.addItemInfo(outPacket, itemz, true, true);
                }
            }
        }
        outPacket.encodeShort(0);

        return outPacket;
    }

    public static OutPacket showBoughtCSItem(int itemid, int sn, int uniqueid, int accid, int quantity, String giftFrom, long expire) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(0x61); //use to be 4a
        addCashItemInfo(outPacket, uniqueid, accid, itemid, sn, quantity, giftFrom, expire);

        return outPacket;
    }

    public static OutPacket showBoughtCSItem(Item item, int sn, int accid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(0x61);
        addCashItemInfo(outPacket, item, accid, sn);

        return outPacket;
    }

    public static OutPacket showXmasSurprise(int idFirst, Item item, int accid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.XMAS_SURPRISE.getValue());
        outPacket.encodeByte(0xE6);
        outPacket.encodeLong(idFirst); //uniqueid of the xmas surprise itself
        outPacket.encodeInt(0);
        addCashItemInfo(outPacket, item, accid, 0); //info of the new item, but packet shows 0 for sn?
        outPacket.encodeInt(item.getItemId());
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static final void addCashItemInfo(OutPacket outPacket, Item item, int accId, int sn) {
        addCashItemInfo(outPacket, item, accId, sn, true);
    }

    public static final void addCashItemInfo(OutPacket outPacket, Item item, int accId, int sn, boolean isFirst) {
        addCashItemInfo(outPacket, item.getUniqueId(), accId, item.getItemId(), sn, item.getQuantity(), item.getGiftFrom(), item.getExpiration(), isFirst); //owner for the lulz
    }

    public static final void addCashItemInfo(OutPacket outPacket, int uniqueid, int accId, int itemid, int sn, int quantity, String sender, long expire) {
        addCashItemInfo(outPacket, uniqueid, accId, itemid, sn, quantity, sender, expire, true);
    }

    public static final void addCashItemInfo(OutPacket outPacket, int uniqueid, int accId, int itemid, int sn, int quantity, String sender, long expire, boolean isFirst) {
        outPacket.encodeLong(uniqueid > 0 ? uniqueid : 0);
        outPacket.encodeLong(accId);
        outPacket.encodeInt(itemid);
        outPacket.encodeInt(isFirst ? sn : 0);
        outPacket.encodeShort(quantity);
        outPacket.encodeString(sender, 13); //owner for the lulzlzlzl
        PacketHelper.addExpirationTime(outPacket, expire);
        outPacket.encodeLong(isFirst ? 0 : sn);
        outPacket.encodeZeroBytes(10);
        //additional 4 bytes for some stuff?
        //if (isFirst && uniqueid > 0 && GameConstants.isEffectRing(itemid)) {
        //	MapleRing ring = MapleRing.loadFromDb(uniqueid);
        //	if (ring != null) { //or is this only for friendship rings, i wonder. and does isFirst even matter
        //		outPacket.encodeString(ring.getPartnerName());
        //		outPacket.encodeInt(itemid);
        //		outPacket.encodeShort(quantity);
        //	}
        //}
    }

    public static void addModCashItemInfo(OutPacket outPacket, CashModInfo item) {
        int flags = item.flags;
        outPacket.encodeInt(item.sn);
        outPacket.encodeInt(flags);
        if ((flags & 0x1) != 0) {
            outPacket.encodeInt(item.itemid);
        }
        if ((flags & 0x2) != 0) {
            outPacket.encodeShort(item.count);
        }
        if ((flags & 0x4) != 0) {
            outPacket.encodeInt(item.discountPrice);
        }
        if ((flags & 0x8) != 0) {
            outPacket.encodeByte(item.unk_1 - 1);
        }
        if ((flags & 0x10) != 0) {
            outPacket.encodeByte(item.priority);
        }
        if ((flags & 0x20) != 0) {
            outPacket.encodeShort(item.period);
        }
        if ((flags & 0x40) != 0) {
            outPacket.encodeInt(0);
        }
        if ((flags & 0x80) != 0) {
            outPacket.encodeInt(item.meso);
        }
        if ((flags & 0x100) != 0) {
            outPacket.encodeByte(item.unk_2 - 1);
        }
        if ((flags & 0x200) != 0) {
            outPacket.encodeByte(item.gender);
        }
        if ((flags & 0x400) != 0) {
            outPacket.encodeByte(item.showUp ? 1 : 0);
        }
        if ((flags & 0x800) != 0) {
            outPacket.encodeByte(item.mark);
        }
        if ((flags & 0x1000) != 0) {
            outPacket.encodeByte(item.unk_3 - 1);
        }
        if ((flags & 0x2000) != 0) {
            outPacket.encodeShort(0);
        }
        if ((flags & 0x4000) != 0) {
            outPacket.encodeShort(0);
        }
        if ((flags & 0x8000) != 0) {
            outPacket.encodeShort(0);
        }
        if ((flags & 0x10000) != 0) {
            outPacket.encodeShort(0);
        }
        if ((flags & 0x20000) != 0) {
            outPacket.encodeShort(0);
        }
        if ((flags & 0x40000) != 0) {
            List<Integer> pack = CashItemFactory.getInstance().getPackageItems(item.sn);
            if (pack == null) {
                outPacket.encodeByte(0);
            } else {
                outPacket.encodeByte(pack.size());
                for (int i = 0; i < pack.size(); i++) {
                    outPacket.encodeInt(pack.get(i));
                }
            }
        }
        if ((flags & 0x80000) != 0) {
            outPacket.encodeInt(0);
        }
        if ((flags & 0x100000) != 0) {
            outPacket.encodeInt(0);
        }
    }

    public static OutPacket showBoughtCSQuestItem(int price, short quantity, byte position, int itemid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0xA7 : 0x9B);
        outPacket.encodeInt(price);
        outPacket.encodeShort(quantity);
        outPacket.encodeShort(position);
        outPacket.encodeInt(itemid);

        return outPacket;
    }

    public static OutPacket sendCSFail(int err) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x7D : 0x71);
        outPacket.encodeByte(err);

        return outPacket;
    }

    public static OutPacket showCouponRedeemedItem(int itemid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeShort(GameConstants.GMS ? 0x75 : 0x69);
        outPacket.encodeInt(0);
        outPacket.encodeInt(1);
        outPacket.encodeShort(1);
        outPacket.encodeShort(0x1A);
        outPacket.encodeInt(itemid);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket showCouponRedeemedItem(Map<Integer, Item> items, int mesos, int maplePoints, MapleClient c) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x75 : 0x69); //use to be 4c
        outPacket.encodeByte(items.size());
        for (Entry<Integer, Item> item : items.entrySet()) {
            addCashItemInfo(outPacket, item.getValue(), c.getAccID(), item.getKey().intValue());
        }
        outPacket.encodeLong(maplePoints);
        outPacket.encodeInt(mesos);

        return outPacket;
    }

    public static OutPacket enableCSUse() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_USE.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket getCSInventory(MapleClient c) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(0x57); // use to be 3e
        CashShop mci = c.getPlayer().getCashInventory();
        int size = 0;
        outPacket.encodeShort(mci.getItemsSize());
        for (Item itemz : mci.getInventory()) {
            addCashItemInfo(outPacket, itemz, c.getAccID(), 0); //test
            if (GameConstants.isPet(itemz.getItemId()) || GameConstants.getInventoryType(itemz.getItemId()) == MapleInventoryType.EQUIP) {
                size++;
            }
        }
        if (mci.getInventory().size() > 0) {
            outPacket.encodeInt(size);
            for (Item itemz : mci.getInventory()) {
                if (GameConstants.isPet(itemz.getItemId()) || GameConstants.getInventoryType(itemz.getItemId()) == MapleInventoryType.EQUIP) {
                    PacketHelper.addItemInfo(outPacket, itemz, true, true);
                }
            }
        }
        outPacket.encodeShort(c.getPlayer().getStorage().getSlots());
        outPacket.encodeInt(c.getCharacterSlots());
        outPacket.encodeShort(4); //00 00 04 00 <-- added?

        return outPacket;
    }

    //work on this packet a little more
    public static OutPacket getCSGifts(MapleClient c) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());

        outPacket.encodeByte(0x59); //use to be 40
        List<Pair<Item, String>> mci = c.getPlayer().getCashInventory().loadGifts();
        outPacket.encodeShort(mci.size());
        for (Pair<Item, String> mcz : mci) {
            outPacket.encodeLong(mcz.getLeft().getUniqueId());
            outPacket.encodeInt(mcz.getLeft().getItemId());
            outPacket.encodeString(mcz.getLeft().getGiftFrom(), 13);
            outPacket.encodeString(mcz.getRight(), 73);
        }

        return outPacket;
    }

    public static OutPacket cashItemExpired(int uniqueid) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x86 : 0x78); //use to be 5d
        outPacket.encodeLong(uniqueid);
        return outPacket;
    }

    public static OutPacket sendGift(int price, int itemid, int quantity, String receiver) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0xA4 : 0x99); //use to be 7C
        outPacket.encodeString(receiver);
        outPacket.encodeInt(itemid);
        outPacket.encodeShort(quantity);
        outPacket.encodeShort(0); //maplePoints
        outPacket.encodeInt(price);

        return outPacket;
    }

    public static OutPacket increasedInvSlots(int inv, int slots) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x78 : 0x6C);
        outPacket.encodeByte(inv);
        outPacket.encodeShort(slots);

        return outPacket;
    }

    //also used for character slots !
    public static OutPacket increasedStorageSlots(int slots) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x7A : 0x6E);
        outPacket.encodeShort(slots);

        return outPacket;
    }

    public static OutPacket confirmToCSInventory(Item item, int accId, int sn) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x82 : 0x76);
        addCashItemInfo(outPacket, item, accId, sn, false);

        return outPacket;
    }

    public static OutPacket confirmFromCSInventory(Item item, short pos) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x80 : 0x74);
        outPacket.encodeShort(pos);
        PacketHelper.addItemInfo(outPacket, item, true, true);
		outPacket.encodeInt(0);

        return outPacket;
    }
	
    public static OutPacket getBoosterFamiliar(int cid, int familiar, int id) { //item IDs
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BOOSTER_FAMILIAR.getValue());
		outPacket.encodeInt(cid);
		outPacket.encodeInt(familiar);
		outPacket.encodeLong(id);
		outPacket.encodeByte(0);

        return outPacket;
    }
	
    public static OutPacket getBoosterPack(int f1, int f2, int f3) { //item IDs
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BOOSTER_PACK.getValue());
        outPacket.encodeByte(0xD7);
		outPacket.encodeInt(f1);
		outPacket.encodeInt(f2);
		outPacket.encodeInt(f3);

        return outPacket;
    }
	
    public static OutPacket getBoosterPackClick() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BOOSTER_PACK.getValue());
        outPacket.encodeByte(0xD5);

        return outPacket;
    }
	
    public static OutPacket getBoosterPackReveal() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.BOOSTER_PACK.getValue());
        outPacket.encodeByte(0xD6);

        return outPacket;
    }
	
    public static OutPacket redeemResponse() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CS_OPERATION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0xBA : 0xAE);
        outPacket.encodeInt(0);
		outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket sendMesobagFailed() {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MESOBAG_FAILURE.getValue());
        return outPacket;
    }

    public static OutPacket sendMesobagSuccess(int mesos) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MESOBAG_SUCCESS.getValue());
        outPacket.encodeInt(mesos);
        return outPacket;
    }

//======================================MTS===========================================
    public static final OutPacket startMTS(final MapleCharacter chr) {
        final 
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPEN.getValue());

        PacketHelper.addCharacterInfo(outPacket, chr);
        outPacket.encodeString(chr.getClient().getAccountName());
        outPacket.encodeInt(ServerConstants.MTS_MESO);
        outPacket.encodeInt(ServerConstants.MTS_TAX);
        outPacket.encodeInt(ServerConstants.MTS_BASE);
        outPacket.encodeInt(24);
        outPacket.encodeInt(168);
        outPacket.encodeLong(PacketHelper.getTime(System.currentTimeMillis()));
        return outPacket;
    }

    public static final OutPacket sendMTS(final List<MTSItemInfo> items, final int tab, final int type, final int page, final int pages) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPERATION.getValue());
        outPacket.encodeByte(0x15); //operation
        outPacket.encodeInt(pages); //total items
        outPacket.encodeInt(items.size()); //number of items on this page
        outPacket.encodeInt(tab);
        outPacket.encodeInt(type);
        outPacket.encodeInt(page);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);

        for (MTSItemInfo item : items) {
            addMTSItemInfo(outPacket, item);
        }
        outPacket.encodeByte(0); //0 or 1?


        return outPacket;
    }

    public static final OutPacket showMTSCash(final MapleCharacter p) {
        final 
        OutPacket outPacket = new OutPacket(SendPacketOpcode.GET_MTS_TOKENS.getValue());
        outPacket.encodeInt(p.getCSPoints(1));
        outPacket.encodeInt(p.getCSPoints(2));
        return outPacket;
    }

    public static final OutPacket getMTSWantedListingOver(final int nx, final int items) {
        final 
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPERATION.getValue());
        outPacket.encodeByte(0x3D);
        outPacket.encodeInt(nx);
        outPacket.encodeInt(items);
        return outPacket;
    }

    public static final OutPacket getMTSConfirmSell() {
        final 
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPERATION.getValue());
        outPacket.encodeByte(0x1D);
        return outPacket;
    }

    public static final OutPacket getMTSFailSell() {
        final 
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPERATION.getValue());
        outPacket.encodeByte(0x1E);
        outPacket.encodeByte(0x42);
        return outPacket;
    }

    public static final OutPacket getMTSConfirmBuy() {
        final 
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPERATION.getValue());
        outPacket.encodeByte(0x33);
        return outPacket;
    }

    public static final OutPacket getMTSFailBuy() {
        final 
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPERATION.getValue());
        outPacket.encodeByte(0x34);
        outPacket.encodeByte(0x42);
        return outPacket;
    }

    public static final OutPacket getMTSConfirmCancel() {
        final 
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPERATION.getValue());
        outPacket.encodeByte(0x25);
        return outPacket;
    }

    public static final OutPacket getMTSFailCancel() {
        final 
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPERATION.getValue());
        outPacket.encodeByte(0x26);
        outPacket.encodeByte(0x42);
        return outPacket;
    }

    public static final OutPacket getMTSConfirmTransfer(final int quantity, final int pos) {
        final 
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPERATION.getValue());
        outPacket.encodeByte(0x27);
        outPacket.encodeInt(quantity);
        outPacket.encodeInt(pos);
        return outPacket;
    }

    private static final void addMTSItemInfo(final OutPacket outPacket, final MTSItemInfo item) {
        PacketHelper.addItemInfo(outPacket, item.getItem(), true, true);
        outPacket.encodeInt(item.getId()); //id
        outPacket.encodeInt(item.getTaxes()); //this + below = price
        outPacket.encodeInt(item.getPrice()); //price
        outPacket.encodeZeroBytes(GameConstants.GMS ? 4 : 8);
        outPacket.encodeLong(PacketHelper.getTime(item.getEndingDate()));
        outPacket.encodeString(item.getSeller()); //account name (what was nexon thinking?)
        outPacket.encodeString(item.getSeller()); //char name
        outPacket.encodeZeroBytes(28);
    }

    public static final OutPacket getNotYetSoldInv(final List<MTSItemInfo> items) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPERATION.getValue());
        outPacket.encodeByte(0x23);

        outPacket.encodeInt(items.size());

        for (MTSItemInfo item : items) {
            addMTSItemInfo(outPacket, item);
        }

        return outPacket;
    }

    public static final OutPacket getTransferInventory(final List<Item> items, final boolean changed) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPERATION.getValue());
        outPacket.encodeByte(0x21);

        outPacket.encodeInt(items.size());
        int i = 0;
        for (Item item : items) {
            PacketHelper.addItemInfo(outPacket, item, true, true);
            outPacket.encodeInt(Integer.MAX_VALUE - i); //fake ID
            outPacket.encodeZeroBytes(GameConstants.GMS ? 52 : 56); //really just addMTSItemInfo
            i++;
        }
        outPacket.encodeInt(-47 + i - 1);
        outPacket.encodeByte(changed ? 1 : 0);

        return outPacket;
    }

    public static final OutPacket addToCartMessage(boolean fail, boolean remove) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MTS_OPERATION.getValue());
        if (remove) {
            if (fail) {
                outPacket.encodeByte(0x2C);
                outPacket.encodeInt(-1);
            } else {
                outPacket.encodeByte(0x2B);
            }
        } else {
            if (fail) {
                outPacket.encodeByte(0x2A);
                outPacket.encodeInt(-1);
            } else {
                outPacket.encodeByte(0x29);
            }
        }

        return outPacket;
    }
}
