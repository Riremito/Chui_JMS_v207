package handling.cashshop.handler;

import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

import constants.GameConstants;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.inventory.MapleInventoryType;
import client.inventory.MapleRing;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.Item;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.CharacterTransfer;
import handling.world.World;
import java.util.List;
import server.CashItemFactory;
import server.CashItemInfo;
import server.MTSCart;
import server.MTSStorage;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.packet.MTSCSPacket;
import tools.Triple;
import connection.InPacket;
import handling.channel.handler.InterServerHandler;

public class CashShopOperation {

    public static void LeaveCS(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        CashShopServer.getPlayerStorageMTS().deregisterPlayer(chr);
        CashShopServer.getPlayerStorage().deregisterPlayer(chr);
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        try {
            World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), c.getChannel());
//            c.write(MaplePacketCreator.getChannelChange(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1])));
        } finally {
//	    final String s = c.getSessionIPAddress();
//	    LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));      
//            chr.saveToDB(false, true);
            c.setPlayer(null);
            c.setReceiving(false);
            
            c.disconnect(true, false);
            InterServerHandler.Loggedin(chr.getId(), c);
        }
    }

    public static void EnterCS(final int playerid, final MapleClient c, CharacterTransfer transfer) {
//        CharacterTransfer transfer = CashShopServer.getPlayerStorage().getPendingCharacter(playerid);
        boolean mts = false;
        if (transfer == null) {
            transfer = CashShopServer.getPlayerStorageMTS().getPendingCharacter(playerid);
            mts = true;
            if (transfer == null) {
                c.close();
                return;
            }
        }
        MapleCharacter chr = MapleCharacter.ReconstructChr(transfer, c, false);

        c.setPlayer(chr);
        c.setAccID(chr.getAccountID());

        if (!c.CheckIPAddress()) { // Remote hack
            c.close();
            return;
        }

        final int state = c.getLoginState();
        boolean allowLogin = false;
        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
            if (!World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()))) {
                allowLogin = true;
            }
        }
        if (!allowLogin) {
            c.setPlayer(null);
            c.close();
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        if (mts) {
            CashShopServer.getPlayerStorageMTS().registerPlayer(chr);
            c.write(MTSCSPacket.startMTS(chr));
	    final MTSCart cart = MTSStorage.getInstance().getCart(c.getPlayer().getId());
	    cart.refreshCurrentView();
            MTSOperation.MTSUpdate(cart, c);
        } else {
            CashShopServer.getPlayerStorage().registerPlayer(chr);
            c.write(MTSCSPacket.warpCS(c));
            CSUpdate(c);
        }

    }

    public static void CSUpdate(final MapleClient c) {
        c.write(MTSCSPacket.getCSGifts(c));
        doCSPackets(c);
        c.write(MTSCSPacket.sendWishList(c.getPlayer(), false));
    }

    public static void CouponCode(final String code, final MapleClient c) {
	if (code.length() <= 0) {
	    return;
	}
	Triple<Boolean, Integer, Integer> info = null;
        try {
            info = MapleCharacterUtil.getNXCodeInfo(code);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (info != null && info.left) {
	    int type = info.mid, item = info.right;
            try {
                MapleCharacterUtil.setNXCodeUsed(c.getPlayer().getName(), code);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            /*
             * Explanation of type!
             * Basically, this makes coupon codes do
             * different things!
             *
             * Type 1: A-Cash,
             * Type 2: Maple Points
             * Type 3: Item.. use SN
             * Type 4: Mesos
             */
            Map<Integer, Item> itemz = new HashMap<Integer, Item>();
            int maplePoints = 0, mesos = 0;
            switch (type) {
                case 1:
                case 2:
                    c.getPlayer().modifyCSPoints(type, item, false);
                    maplePoints = item;
                    break;
                case 3:
                    CashItemInfo itez = CashItemFactory.getInstance().getItem(item);
                    if (itez == null) {
                        c.write(MTSCSPacket.sendCSFail(0));
                        return;
                    }
                    byte slot = MapleInventoryManipulator.addId(c, itez.getId(), (short) 1, "", "Cash shop: coupon code" + " on " + FileoutputUtil.CurrentReadable_Date());
                    if (slot <= -1) {
                        c.write(MTSCSPacket.sendCSFail(0));
                        return;
                    } else {
                        itemz.put(item, c.getPlayer().getInventory(GameConstants.getInventoryType(item)).getItem(slot));
                    }
                    break;
                case 4:
                    c.getPlayer().gainMeso(item, false);
                    mesos = item;
                    break;
            }
            c.write(MTSCSPacket.showCouponRedeemedItem(itemz, mesos, maplePoints, c));
        } else {
            c.write(MTSCSPacket.sendCSFail(info == null ? 0xA7 : 0xA5)); //A1, 9F
        }
    }

    public static final void BuyCashItem(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        final int action = inPacket.decodeByte();
        if (action == 0) {
            inPacket.skip(2);
            CouponCode(inPacket.decodeString(), c);
        } else if (action == 3) {
	    final int toCharge = (inPacket.decodeByte() + 1);
            final CashItemInfo item = CashItemFactory.getInstance().getItem(inPacket.decodeInt());
            if (item != null && chr.getCSPoints(toCharge) >= item.getPrice()) {
                if (!item.genderEquals(c.getPlayer().getGender())) {
                    c.write(MTSCSPacket.sendCSFail(0xA6));
                    doCSPackets(c);
                    return;
                } else if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                    c.write(MTSCSPacket.sendCSFail(0xB1));
                    doCSPackets(c);
                    return;
                }
                for (int i : GameConstants.cashBlock) {
                    if (item.getId() == i) {
                        c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                        doCSPackets(c);
                        return;
                    }
                }
                chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                Item itemz = chr.getCashInventory().toItem(item);
                if (itemz != null && itemz.getUniqueId() > 0 && itemz.getItemId() == item.getId() && itemz.getQuantity() == item.getCount()) {
                    chr.getCashInventory().addToInventory(itemz);
                    //c.write(MTSCSPacket.confirmToCSInventory(itemz, c.getAccID(), item.getSN()));
                    c.write(MTSCSPacket.showBoughtCSItem(itemz, item.getSN(), c.getAccID()));
                } else {
                    c.write(MTSCSPacket.sendCSFail(0));
                }
            } else {
                c.write(MTSCSPacket.sendCSFail(0));
            }
        } else if (action == 4 || action == (GameConstants.GMS ? 34 : 33)) { //gift, package
            inPacket.decodeString(); // NEXON ID
            final CashItemInfo item = CashItemFactory.getInstance().getItem(inPacket.decodeInt());
            String partnerName = inPacket.decodeString();
            String msg = inPacket.decodeString();
            if (item == null || c.getPlayer().getCSPoints(1) < item.getPrice() || msg.length() > 73 || msg.length() < 1) { //dont want packet editors gifting random stuff =P
                c.write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, c.getPlayer().getWorld());
            if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == c.getPlayer().getId() || info.getMid().intValue() == c.getAccID()) {
                c.write(MTSCSPacket.sendCSFail(0xA2)); //9E v75
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(info.getRight().intValue())) {
                c.write(MTSCSPacket.sendCSFail(0xA3));
                doCSPackets(c);
                return;
            } else {
                for (int i : GameConstants.cashBlock) {
                    if (item.getId() == i) {
                        c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                        doCSPackets(c);
                        return;
                    }
                }
                c.getPlayer().getCashInventory().gift(info.getLeft().intValue(), c.getPlayer().getName(), msg, item.getSN(), MapleInventoryIdentifier.getInstance());
                c.getPlayer().modifyCSPoints(1, -item.getPrice(), false);
                c.write(MTSCSPacket.sendGift(item.getPrice(), item.getId(), item.getCount(), partnerName));
            }
        } else if (action == 5) { // Wishlist
            chr.clearWishlist();
            if (inPacket.getUnreadAmount() < 40) {
                c.write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            int[] wishlist = new int[10];
            for (int i = 0; i < 10; i++) {
                wishlist[i] = inPacket.decodeInt();
            }
            chr.setWishlist(wishlist);
            c.write(MTSCSPacket.sendWishList(chr, true));

        } else if (action == 6) { // Increase inv
	    inPacket.skip(1);
	    final int toCharge = GameConstants.GMS ? inPacket.decodeInt() : 1;
            final boolean coupon = inPacket.decodeByte() > 0;
            if (coupon) {
                final MapleInventoryType type = getInventoryType(inPacket.decodeInt());

                if (chr.getCSPoints(toCharge) >= (GameConstants.GMS ? 6000 : 12000) && chr.getInventory(type).getSlotLimit() < 89) {
                    chr.modifyCSPoints(toCharge, (GameConstants.GMS ? -6000 : -12000), false);
                    chr.getInventory(type).addSlot((byte) 8);
                    chr.dropMessage(1, "Slots has been increased to " + chr.getInventory(type).getSlotLimit());
                } else {
                    c.write(MTSCSPacket.sendCSFail(0xA4));
                }
            } else {
                final MapleInventoryType type = MapleInventoryType.getByType(inPacket.decodeByte());
                if (chr.getCSPoints(toCharge) >= (GameConstants.GMS ? 4000 : 8000) && chr.getInventory(type).getSlotLimit() < 93) {
                    chr.modifyCSPoints(toCharge, (GameConstants.GMS ? -4000 : -8000), false);
                    chr.getInventory(type).addSlot((byte) 4);
                    chr.dropMessage(1, "Slots has been increased to " + chr.getInventory(type).getSlotLimit());
                } else {
                    c.write(MTSCSPacket.sendCSFail(0xA4));
                }
            }
        } else if (action == 7) { // Increase slot space
	    inPacket.skip(1);
	    final int toCharge = GameConstants.GMS ? inPacket.decodeInt() : 1;
	    final int coupon = inPacket.decodeByte() > 0 ? 2 : 1;
            if (chr.getCSPoints(toCharge) >= (GameConstants.GMS ? 4000 : 8000) * coupon && chr.getStorage().getSlots() < (49 - (4 * coupon))) {
                chr.modifyCSPoints(toCharge, (GameConstants.GMS ? -4000 : -8000) * coupon, false);
                chr.getStorage().increaseSlots((byte) (4 * coupon));
                chr.getStorage().saveToDB();
                chr.dropMessage(1, "Storage slots increased to: " + chr.getStorage().getSlots());
            } else {
                c.write(MTSCSPacket.sendCSFail(0xA4));
            }
        } else if (action == 8) { //...9 = pendant slot expansion
            inPacket.skip(1);
	    final int toCharge = GameConstants.GMS ? inPacket.decodeInt() : 1;
            CashItemInfo item = CashItemFactory.getInstance().getItem(inPacket.decodeInt());
            int slots = c.getCharacterSlots();
            if (item == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || slots > 15 || item.getId() != 5430000) {
                c.write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            if (c.gainCharacterSlot()) {
		c.getPlayer().modifyCSPoints(toCharge, -item.getPrice(), false);
                chr.dropMessage(1, "Character slots increased to: " + (slots+1));
            } else {
                c.write(MTSCSPacket.sendCSFail(0));
            }
        /*} else if (action == 9) { //...9 = pendant slot expansion
            inPacket.decodeByte();
	    final int sn = inPacket.decodeInt();
            CashItemInfo item = CashItemFactory.getInstance().getItem(sn);
            int slots = c.getCharacterSlots();
            if (item == null || c.getPlayer().getCSPoints(1) < item.getPrice() || item.getId() / 10000 != 555) {
                c.write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            MapleQuestStatus marr = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
	    if (marr != null && marr.getCustomData() != null && Long.parseLong(marr.getCustomData()) >= System.currentTimeMillis()) {
                c.write(MTSCSPacket.sendCSFail(0));
	    } else {
		c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)).setCustomData(String.valueOf(System.currentTimeMillis() + ((long)item.getPeriod() * 24 * 60 * 60000)));
		c.getPlayer().modifyCSPoints(1, -item.getPrice(), false);
                chr.dropMessage(1, "Additional pendant slot gained.");
            }*/
        } else if (action == 14) { //get item from csinventory
            //uniqueid, 00 01 01 00, type->position(short)
            Item item = c.getPlayer().getCashInventory().findByCashId((int) inPacket.decodeLong());
            if (item != null && item.getQuantity() > 0 && MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                Item item_ = item.copy();
                short pos = MapleInventoryManipulator.addbyItem(c, item_, true);
                if (pos >= 0) {
                    if (item_.getPet() != null) {
                        item_.getPet().setInventoryPosition(pos);
                        c.getPlayer().addPet(item_.getPet());
                    }
                    c.getPlayer().getCashInventory().removeFromInventory(item);
                    c.write(MTSCSPacket.confirmFromCSInventory(item_, pos));
                } else {
                    c.write(MTSCSPacket.sendCSFail(0xB1));
                }
            } else {
                c.write(MTSCSPacket.sendCSFail(0xB1));
            }
        } else if (action == 15) { //put item in cash inventory
            int uniqueid = (int) inPacket.decodeLong();
            MapleInventoryType type = MapleInventoryType.getByType(inPacket.decodeByte());
            Item item = c.getPlayer().getInventory(type).findByUniqueId(uniqueid);
            if (item != null && item.getQuantity() > 0 && item.getUniqueId() > 0 && c.getPlayer().getCashInventory().getItemsSize() < 100) {
                Item item_ = item.copy();
                MapleInventoryManipulator.removeFromSlot(c, type, item.getPosition(), item.getQuantity(), false);
                if (item_.getPet() != null) {
                    c.getPlayer().removePetCS(item_.getPet());
                }
                item_.setPosition((byte) 0);
                c.getPlayer().getCashInventory().addToInventory(item_);
                //warning: this d/cs
                //c.write(MTSCSPacket.confirmToCSInventory(item, c.getAccID(), c.getPlayer().getCashInventory().getSNForItem(item)));
            } else {
                c.write(MTSCSPacket.sendCSFail(0xB1));
            }
        } else if (action == (GameConstants.GMS ? 32 : 31) || action == (GameConstants.GMS ? 38 : 37)) { //36 = friendship, 30 = crush
            //c.write(MTSCSPacket.sendCSFail(0));
			if (GameConstants.GMS) {
				inPacket.skip(4);
			} else {
				inPacket.decodeString(); // as13
			}
			final int toCharge = GameConstants.GMS ? inPacket.decodeInt() : 1;
            final CashItemInfo item = CashItemFactory.getInstance().getItem(inPacket.decodeInt());
            final String partnerName = inPacket.decodeString();
            final String msg = inPacket.decodeString();
            if (item == null || !GameConstants.isEffectRing(item.getId()) || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || msg.length() > 73 || msg.length() < 1) {
                c.write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(c.getPlayer().getGender())) {
                c.write(MTSCSPacket.sendCSFail(0xA6));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                c.write(MTSCSPacket.sendCSFail(0xB1));
                doCSPackets(c);
                return;
            }
            for (int i : GameConstants.cashBlock) { //just incase hacker
                if (item.getId() == i) {
                    c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                    doCSPackets(c);
                    return;
                }
            }
            Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, c.getPlayer().getWorld());
            if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == c.getPlayer().getId()) {
                c.write(MTSCSPacket.sendCSFail(0xB4)); //9E v75
                doCSPackets(c);
                return;
            } else if (info.getMid().intValue() == c.getAccID()) {
                c.write(MTSCSPacket.sendCSFail(0xA3)); //9D v75
                doCSPackets(c);
                return;
            } else {
                if (info.getRight().intValue() == c.getPlayer().getGender() && action == 30) {
                    c.write(MTSCSPacket.sendCSFail(0xA1)); //9B v75
                    doCSPackets(c);
                    return;
                }

                int err = MapleRing.createRing(item.getId(), c.getPlayer(), partnerName, msg, info.getLeft().intValue(), item.getSN());

                if (err != 1) {
                    c.write(MTSCSPacket.sendCSFail(0)); //9E v75
                    doCSPackets(c);
                    return;
                }
                c.getPlayer().modifyCSPoints(toCharge, -item.getPrice(), false);
                //c.write(MTSCSPacket.showBoughtCSItem(itemz, item.getSN(), c.getAccID()));
                c.write(MTSCSPacket.sendGift(item.getPrice(), item.getId(), item.getCount(), partnerName));
            }


        } else if (action == (GameConstants.GMS ? 33 : 32)) {
            inPacket.skip(1);
			final int toCharge = GameConstants.GMS ? inPacket.decodeInt() : 1;
            final CashItemInfo item = CashItemFactory.getInstance().getItem(inPacket.decodeInt());
            List<Integer> ccc = null;
            if (item != null) {
                ccc = CashItemFactory.getInstance().getPackageItems(item.getId());
            }
            if (item == null || ccc == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice()) {
                c.write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(c.getPlayer().getGender())) {
                c.write(MTSCSPacket.sendCSFail(0xA6));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getCashInventory().getItemsSize() >= (100 - ccc.size())) {
                c.write(MTSCSPacket.sendCSFail(0xB1));
                doCSPackets(c);
                return;
            }
            for (int iz : GameConstants.cashBlock) {
                if (item.getId() == iz) {
                    c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                    doCSPackets(c);
                    return;
                }
            }
            Map<Integer, Item> ccz = new HashMap<Integer, Item>();
            for (int i : ccc) {
		final CashItemInfo cii = CashItemFactory.getInstance().getSimpleItem(i);
		if (cii == null) {
		    continue;
		}
                Item itemz = c.getPlayer().getCashInventory().toItem(cii);
                if (itemz == null || itemz.getUniqueId() <= 0) {
                    continue;
                }
                for (int iz : GameConstants.cashBlock) {
                    if (itemz.getItemId() == iz) {
                        continue;
                    }
                }
                ccz.put(i, itemz);
                c.getPlayer().getCashInventory().addToInventory(itemz);
            }
            chr.modifyCSPoints(toCharge, -item.getPrice(), false);
            c.write(MTSCSPacket.showBoughtCSPackage(ccz, c.getAccID()));

        } else if (action == (GameConstants.GMS ? 35 : 34)) {
            final CashItemInfo item = CashItemFactory.getInstance().getItem(inPacket.decodeInt());
            if (item == null || !MapleItemInformationProvider.getInstance().isQuestItem(item.getId())) {
                c.write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getMeso() < item.getPrice()) {
                c.write(MTSCSPacket.sendCSFail(0xB8));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getInventory(GameConstants.getInventoryType(item.getId())).getNextFreeSlot() < 0) {
                c.write(MTSCSPacket.sendCSFail(0xB1));
                doCSPackets(c);
                return;
            }
            for (int iz : GameConstants.cashBlock) {
                if (item.getId() == iz) {
                    c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                    doCSPackets(c);
                    return;
                }
            }
            byte pos = MapleInventoryManipulator.addId(c, item.getId(), (short) item.getCount(), null, "Cash shop: quest item" + " on " + FileoutputUtil.CurrentReadable_Date());
            if (pos < 0) {
                c.write(MTSCSPacket.sendCSFail(0xB1));
                doCSPackets(c);
                return;
            }
            chr.gainMeso(-item.getPrice(), false);
            c.write(MTSCSPacket.showBoughtCSQuestItem(item.getPrice(), (short) item.getCount(), pos, item.getId()));
		} else if (action == (GameConstants.GMS ? 46 : 45)) { //idk
			c.write(MTSCSPacket.redeemResponse());
        } else {
            c.write(MTSCSPacket.sendCSFail(0));

        }
        doCSPackets(c);
    }

    private static final MapleInventoryType getInventoryType(final int id) {
        switch (id) {
            case 50200075:
                return MapleInventoryType.EQUIP;
            case 50200074:
                return MapleInventoryType.USE;
            case 50200073:
                return MapleInventoryType.ETC;
            default:
                return MapleInventoryType.UNDEFINED;
        }
    }

    public static final void doCSPackets(MapleClient c) {
        c.write(MTSCSPacket.getCSInventory(c));
        c.write(MTSCSPacket.showNXMapleTokens(c.getPlayer()));
        c.write(MTSCSPacket.enableCSUse());
        c.getPlayer().getCashInventory().checkExpire(c);
    }
}
