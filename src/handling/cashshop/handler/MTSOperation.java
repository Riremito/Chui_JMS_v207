package handling.cashshop.handler;

import client.inventory.Equip;
import constants.GameConstants;
import client.inventory.Item;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import constants.ServerConstants;
import java.util.Calendar;
import server.MTSCart;
import server.MTSStorage;
import server.MTSStorage.MTSItemInfo;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import connection.InPacket;
import tools.packet.MTSCSPacket;

public class MTSOperation {

    public static void MTSOperation(final InPacket inPacket, final MapleClient c) {
        final MTSCart cart = MTSStorage.getInstance().getCart(c.getPlayer().getId());
        //System.out.println(slea.toString());
        if (inPacket.getUnreadAmount() <= 0) {
            doMTSPackets(cart, c);
            return;
        }
        final byte op = inPacket.decodeByte();
        if (op == 2) { //put up for sale
            final byte invType = inPacket.decodeByte(); //1 = equip 2 = everything else
            if (invType != 1 && invType != 2) { //pet?
                c.write(MTSCSPacket.getMTSFailSell());
                doMTSPackets(cart, c);
                return;
            }
            final int itemid = inPacket.decodeInt(); //itemid
            if (inPacket.decodeByte() != 0) {
                c.write(MTSCSPacket.getMTSFailSell());
                doMTSPackets(cart, c);
                return;//we don't like uniqueIDs
            }
            inPacket.skip(12); //expiration, -1, don't matter
            short stars = 1, quantity = 1;
            byte slot = 0;
            if (invType == 1) {
                inPacket.skip(32);
            } else {
                stars = inPacket.decodeShort(); //the entire quantity of the item
            }
            inPacket.decodeString();//owner
            //again? =/
            if (invType == 1) {
                inPacket.skip(50);
                slot = (byte) inPacket.decodeInt();
                inPacket.skip(4); //skip the quantity int, equips are always 1
            } else {
                inPacket.decodeShort(); //flag
                if (GameConstants.isThrowingStar(itemid) || GameConstants.isBullet(itemid)) {
                    inPacket.skip(8);//recharge ID thing
                }
                slot = (byte) inPacket.decodeInt();
                if (GameConstants.isThrowingStar(itemid) || GameConstants.isBullet(itemid)) {
                    quantity = stars; //this is due to stars you need to use the entire quantity, not specified
                    inPacket.skip(4); //so just skip the quantity int
                } else {
                    quantity = (short) inPacket.decodeInt(); //specified quantity
                }
            }
            final int price = inPacket.decodeInt();
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final MapleInventoryType type = GameConstants.getInventoryType(itemid);
            final Item item = c.getPlayer().getInventory(type).getItem(slot).copy();
            if (ii.isCash(itemid) || quantity <= 0 || item == null || item.getQuantity() <= 0 || item.getItemId() != itemid || item.getUniqueId() > 0 || item.getQuantity() < quantity || price < ServerConstants.MIN_MTS || c.getPlayer().getMeso() < ServerConstants.MTS_MESO || cart.getNotYetSold().size() >= 10 || ii.isDropRestricted(itemid) || ii.isAccountShared(itemid) || item.getExpiration() > -1 || item.getFlag() > 0) {
                c.write(MTSCSPacket.getMTSFailSell());
                doMTSPackets(cart, c);
                return;
            }
            if (type == MapleInventoryType.EQUIP) {
                final Equip eq = (Equip) item;
                if (eq.getState() > 0 || eq.getEnhance() > 0 || eq.getDurability() > -1) {
                    c.write(MTSCSPacket.getMTSFailSell());
                    doMTSPackets(cart, c);
                    return;
                }
            }
            if (quantity >= 50 && item.getItemId() == 2340000) {
                c.setMonitored(true); //hack check
            }
            final long expiration = (System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000));
            item.setQuantity(quantity);
            MTSStorage.getInstance().addToBuyNow(cart, item, price, c.getPlayer().getId(), c.getPlayer().getName(), expiration);
            MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
            c.getPlayer().gainMeso(-ServerConstants.MTS_MESO, false);
            c.write(MTSCSPacket.getMTSConfirmSell());
        } else if (op == 5) { //change page/tab
            cart.changeInfo(inPacket.decodeInt(), inPacket.decodeInt(), inPacket.decodeInt());
	} else if (op == 6) { //search
	    cart.changeInfo(inPacket.decodeInt(), inPacket.decodeInt(), 0);
	    inPacket.decodeInt(); //always 0?
	    cart.changeCurrentView(MTSStorage.getInstance().getSearch(inPacket.decodeInt() > 0, inPacket.decodeString(), cart.getType(), cart.getTab()));
        } else if (op == 7) { //cancel sale
            if (!MTSStorage.getInstance().removeFromBuyNow(inPacket.decodeInt(), c.getPlayer().getId(), true)) {
                c.write(MTSCSPacket.getMTSFailCancel());
            } else {
                c.write(MTSCSPacket.getMTSConfirmCancel());
                sendMTSPackets(cart, c, true);
                return;
            }
        } else if (op == 8) { //transfer item
            final int id = Integer.MAX_VALUE - inPacket.decodeInt(); //fake id
            if (id >= cart.getInventory().size()) {
                c.getPlayer().dropMessage(1, "Please try it again later.");
                sendMTSPackets(cart, c, true);
                return;
            }
            final Item item = cart.getInventory().get(id); //by index
            //System.out.println("NumItems: " + cart.getInventory().size() + ", ID: " + id + ", ItemExists?: " + (item != null));
            if (item != null && item.getQuantity() > 0 && MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                Item item_ = item.copy();
                short pos = MapleInventoryManipulator.addbyItem(c, item_, true);
                if (pos >= 0) {
                    if (item_.getPet() != null) {
                        item_.getPet().setInventoryPosition(pos);
                        c.getPlayer().addPet(item_.getPet());
                    }
                    cart.removeFromInventory(item);
                    c.write(MTSCSPacket.getMTSConfirmTransfer(GameConstants.getInventoryType(item_.getItemId()).getType(), pos)); //IF this is actually pos and pos
                    sendMTSPackets(cart, c, true);
                    return;
                } else {
                    //System.out.println("addByItem is less than 0");
                    c.write(MTSCSPacket.getMTSFailBuy());
                }
            } else {
                //System.out.println("CheckSpace return false");
                c.write(MTSCSPacket.getMTSFailBuy());
            }
        } else if (op == 9) { //add to cart
            final int id = inPacket.decodeInt();
            if (MTSStorage.getInstance().checkCart(id, c.getPlayer().getId()) && cart.addToCart(id)) {
                c.write(MTSCSPacket.addToCartMessage(false, false));
            } else {
                c.write(MTSCSPacket.addToCartMessage(true, false));
            }
        } else if (op == 10) { //delete from cart
            final int id = inPacket.decodeInt();
            if (cart.getCart().contains(id)) {
                cart.removeFromCart(id);
                c.write(MTSCSPacket.addToCartMessage(false, true));
            } else {
                c.write(MTSCSPacket.addToCartMessage(true, true));
            }
        } else if (op == 16 || op == 17) { //buyNow, buy from cart
            final MTSItemInfo mts = MTSStorage.getInstance().getSingleItem(inPacket.decodeInt());
            if (mts != null && mts.getCharacterId() != c.getPlayer().getId()) {
                if (c.getPlayer().getCSPoints(1) > mts.getRealPrice()) {
                    if (MTSStorage.getInstance().removeFromBuyNow(mts.getId(), c.getPlayer().getId(), false)) {
                        c.getPlayer().modifyCSPoints(1, -mts.getRealPrice(), false);
                        MTSStorage.getInstance().getCart(mts.getCharacterId()).increaseOwedNX(mts.getPrice());
                        c.write(MTSCSPacket.getMTSConfirmBuy());
                        sendMTSPackets(cart, c, true);
                        return;
                    } else {
                        c.write(MTSCSPacket.getMTSFailBuy());
                    }
                } else {
                    c.write(MTSCSPacket.getMTSFailBuy());
                }
            } else {
                c.write(MTSCSPacket.getMTSFailBuy());
            }
        } else if (c.getPlayer().isAdmin()) {
            //System.out.println("New MTS Op " + op + ", \n" + slea.toString());
        }
        doMTSPackets(cart, c);
    }

    public static void MTSUpdate(final MTSCart cart, final MapleClient c) {
		final int a = MTSStorage.getInstance().getCart(c.getPlayer().getId()).getSetOwedNX();
        c.getPlayer().modifyCSPoints(1, GameConstants.GMS ? (a * 2) : a, false);
        c.write(MTSCSPacket.getMTSWantedListingOver(0, 0));
        doMTSPackets(cart, c);
    }

    private static void doMTSPackets(final MTSCart cart, final MapleClient c) {
        sendMTSPackets(cart, c, false);
    }

    private static void sendMTSPackets(final MTSCart cart, final MapleClient c, final boolean changed) {
        c.write(MTSStorage.getInstance().getCurrentMTS(cart));
        c.write(MTSStorage.getInstance().getCurrentNotYetSold(cart));
        c.write(MTSStorage.getInstance().getCurrentTransfer(cart, changed));
        c.write(MTSCSPacket.showMTSCash(c.getPlayer()));
        c.write(MTSCSPacket.enableCSUse());
        MTSStorage.getInstance().checkExpirations();
    }
}
