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

import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.MapleClient;
import client.MapleCharacter;
import constants.GameConstants;
import client.MapleQuestStatus;
import client.RockPaperScissors;
import client.inventory.ItemFlag;
import handling.SendPacketOpcode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import server.AutobanManager;
import server.MapleShop;
import server.MapleInventoryManipulator;
import server.MapleStorage;
import server.life.MapleNPC;
import server.movement.MovementKind;
import server.quest.MapleQuest;
import scripting.NPCScriptManager;
import scripting.NPCConversationManager;
import server.MapleItemInformationProvider;
import server.maps.MapScriptMethods;
import tools.MaplePacketCreator;
import tools.Pair;
import connection.InPacket;
import connection.OutPacket;
import java.awt.Point;
import tools.packet.PacketHelper;
import util.Position;

public class NPCHandler {

    public static final void NPCAnimation(final InPacket inPacket, final MapleClient c) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.NPC_ACTION.getValue());
        
        int npcid = inPacket.decodeInt();
        byte u1 = inPacket.decodeByte(); // FF
        byte u2 = inPacket.decodeByte(); // FF
        int u3 = inPacket.decodeInt(); // 00 00 00 00

        outPacket.encodeInt(npcid);
        outPacket.encodeByte(u1);
        outPacket.encodeByte(u2);
        outPacket.encodeInt(u3);

        if (inPacket.getUnreadAmount() > 0) {
//            Point u4 = inPacket.decodePosition();
            Point oldPos = inPacket.decodePosition();
            Point oldVPos = inPacket.decodePosition();
            List res;
            try {
                res = MovementParse.parseMovement(inPacket, oldPos, MovementKind.NPC_MOVMENT);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println(new StringBuilder().append("AIOBE Type1:").toString());
                return;
            }

//            mplew.writePos(u4);
            outPacket.encodePosition(oldPos);
            outPacket.encodePosition(oldVPos);
            PacketHelper.serializeMovementList(outPacket, res);
        }
        c.write(outPacket);
    }

    public static final void NPCShop(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        final byte bmode = inPacket.decodeByte();
        if (chr == null) {
            return;
        }

        switch (bmode) {
            case 0: {
                final MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                inPacket.skip(3/*2*/); // 06 00 00
                final int itemId = inPacket.decodeInt();
                final short quantity = inPacket.decodeShort();
                shop.buy(c, itemId, quantity);
                break;
            }
            case 1: {
                final MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                final byte slot = (byte) inPacket.decodeShort();
                final int itemId = inPacket.decodeInt();
                final short quantity = inPacket.decodeShort();
                shop.sell(c, GameConstants.getInventoryType(itemId), slot, quantity);
                break;
            }
            case 2: {
                final MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                final byte slot = (byte) inPacket.decodeShort();
                shop.recharge(c, slot);
                break;
            }
            default:
                chr.setConversation(0);
                break;
        }
    }

    public static final void NPCTalk(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.getBattle() != null) {
            return;
        }
        final MapleNPC npc = chr.getMap().getNPCByOid(inPacket.decodeInt());

        if (npc == null) {
            return;
        }
        if (chr.hasBlockedInventory()) {
//            chr.dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
            return;
        }
        c.getPlayer().updateTick(inPacket.decodeInt());
        if (npc.hasShop()) {
            chr.setConversation(1);
            npc.sendShop(c);
        } else {
            NPCScriptManager.getInstance().start(c, npc.getId());
        }
    }

    public static final void QuestAction(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        final byte action = inPacket.decodeByte();
        final int quest = inPacket.decodeUShort();
        if (chr == null) {
            return;
        }
        final MapleQuest q = MapleQuest.getInstance(quest);
        switch (action) {
            case 0: { // Restore lost item
                //chr.updateTick(inPacket.decodeInt());
                inPacket.decodeInt();
                final int itemid = inPacket.decodeInt();
                q.RestoreLostItem(chr, itemid);
                break;
            }
            case 1: { // Start Quest
                final int npc = inPacket.decodeInt();
                if (!q.hasStartScript()) {
                    q.start(chr, npc);
                }
                break;
            }
            case 2: { // Complete Quest
                final int npc = inPacket.decodeInt();
                //chr.updateTick(inPacket.decodeInt());
                inPacket.decodeInt();
                if (q.hasEndScript()) {
                    return;
                }
                if (inPacket.getUnreadAmount() >= 4) {
                    q.complete(chr, npc, inPacket.decodeInt());
                } else {
                    q.complete(chr, npc);
                }
                // c.write(MaplePacketCreator.completeQuest(c.getPlayer(), quest));
                //c.write(MaplePacketCreator.updateQuestInfo(c.getPlayer(), quest, npc, (byte)14));
                // 6 = start quest
                // 7 = unknown error
                // 8 = equip is full
                // 9 = not enough mesos
                // 11 = due to the equipment currently being worn wtf o.o
                // 12 = you may not posess more than one of this item
                break;
            }
            case 3: { // Forefit Quest
                if (GameConstants.canForfeit(q.getId())) {
                    q.forfeit(chr);
                } else {
                    chr.dropMessage(1, "You may not forfeit this quest.");
                }
                break;
            }
            case 4: { // Scripted Start Quest
                final int npc = inPacket.decodeInt();
                if (chr.hasBlockedInventory()) {
//                    chr.dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
                    return;
                }
                //c.getPlayer().updateTick(inPacket.decodeInt());
                NPCScriptManager.getInstance().startQuest(c, npc, quest);
                break;
            }
            case 5: { // Scripted End Quest
                final int npc = inPacket.decodeInt();
                if (chr.hasBlockedInventory()) {
//                    chr.dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
                    return;
                }
                //c.getPlayer().updateTick(inPacket.decodeInt());
                NPCScriptManager.getInstance().endQuest(c, npc, quest, false);
                c.write(MaplePacketCreator.showSpecialEffect(13)); // Quest completion
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showSpecialEffect(chr.getId(), 13), false);
                break;
            }
        }
    }

    public static final void Storage(final InPacket inPacket, final MapleClient c, final MapleCharacter chr) {
        final byte mode = inPacket.decodeByte();
        if (chr == null) {
            return;
        }
        final MapleStorage storage = chr.getStorage();

        switch (mode) {
            case 3: { // Take Out
                final byte type = inPacket.decodeByte();
                final byte slot = storage.getSlot(MapleInventoryType.getByType(type), inPacket.decodeByte());
                final Item item = storage.takeOut(slot);

                if (item != null) {
                    if (!MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                        storage.store(item);
                        chr.dropMessage(1, "Your inventory is full");
                    } else {
                        MapleInventoryManipulator.addFromDrop(c, item, false);
                    }
                    storage.sendTakenOut(c, GameConstants.getInventoryType(item.getItemId()));
                } else {
                    c.write(MaplePacketCreator.enableActions());
                    return;
                }
                break;
            }
            case 4: { // Store
                final byte slot = (byte) inPacket.decodeShort();
                final int itemId = inPacket.decodeInt();
                MapleInventoryType type = GameConstants.getInventoryType(itemId);
                short quantity = inPacket.decodeShort();
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (quantity < 1) {
                    //AutobanManager.getInstance().autoban(c, "Trying to store " + quantity + " of " + itemId);
                    return;
                }
                if (storage.isFull()) {
                    c.write(MaplePacketCreator.getStorageFull());
                    return;
                }
                if (chr.getInventory(type).getItem(slot) == null) {
                    c.write(MaplePacketCreator.enableActions());
                    return;
                }

                if (chr.getMeso() < 100) {
                    chr.dropMessage(1, "You don't have enough mesos to store the item");
                } else {
                    Item item = chr.getInventory(type).getItem(slot).copy();

                    if (GameConstants.isPet(item.getItemId())) {
                        c.write(MaplePacketCreator.enableActions());
                        return;
                    }
                    final short flag = item.getFlag();
                    if (ii.isPickupRestricted(item.getItemId()) && storage.findById(item.getItemId()) != null) {
                        c.write(MaplePacketCreator.enableActions());
                        return;
                    }
                    if (item.getItemId() == itemId && (item.getQuantity() >= quantity || GameConstants.isThrowingStar(itemId) || GameConstants.isBullet(itemId))) {
                        if (ii.isDropRestricted(item.getItemId())) {
                            if (ItemFlag.KARMA_EQ.check(flag)) {
                                item.setFlag((short) (flag - ItemFlag.KARMA_EQ.getValue()));
                            } else if (ItemFlag.KARMA_USE.check(flag)) {
                                item.setFlag((short) (flag - ItemFlag.KARMA_USE.getValue()));
                            } else if (ItemFlag.KARMA_ACC.check(flag)) {
                                item.setFlag((short) (flag - ItemFlag.KARMA_ACC.getValue()));
                            } else if (ItemFlag.KARMA_ACC_USE.check(flag)) {
                                item.setFlag((short) (flag - ItemFlag.KARMA_ACC_USE.getValue()));
                            } else {
                                c.write(MaplePacketCreator.enableActions());
                                return;
                            }
                        }
                        if (GameConstants.isThrowingStar(itemId) || GameConstants.isBullet(itemId)) {
                            quantity = item.getQuantity();
                        }
                        chr.gainMeso(-100, false, false);
                        MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                        item.setQuantity(quantity);
                        storage.store(item);
                    } else {
                        AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to store non-matching itemid (" + itemId + "/" + item.getItemId() + ") or quantity not in posession (" + quantity + "/" + item.getQuantity() + ")");
                        return;
                    }
                }
                storage.sendStored(c, GameConstants.getInventoryType(itemId));
                break;
            }
            case 5: { //arrange
                storage.arrange();
                storage.update(c);
                break;
            }
            case 6: {
                int meso = inPacket.decodeInt();
                final int storageMesos = storage.getMeso();
                final int playerMesos = chr.getMeso();

                if ((meso > 0 && storageMesos >= meso) || (meso < 0 && playerMesos >= -meso)) {
                    if (meso < 0 && (storageMesos - meso) < 0) { // storing with overflow
                        meso = -(Integer.MAX_VALUE - storageMesos);
                        if ((-meso) > playerMesos) { // should never happen just a failsafe
                            return;
                        }
                    } else if (meso > 0 && (playerMesos + meso) < 0) { // taking out with overflow
                        meso = (Integer.MAX_VALUE - playerMesos);
                        if ((meso) > storageMesos) { // should never happen just a failsafe
                            return;
                        }
                    }
                    storage.setMeso(storageMesos - meso);
                    chr.gainMeso(meso, false, false);
                } else {
                    AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to store or take out unavailable amount of mesos (" + meso + "/" + storage.getMeso() + "/" + c.getPlayer().getMeso() + ")");
                    return;
                }
                storage.sendMeso(c);
                break;
            }
            case 7: {
                storage.close();
                chr.setConversation(0);
                break;
            }
            default:
                System.out.println("Unhandled Storage mode : " + mode);
                break;
        }
    }

    public static final void NPCMoreTalk(final InPacket inPacket, final MapleClient c) {
        final byte lastMsg = inPacket.decodeByte(); // 00 (last msg type I think)
        final byte action = inPacket.decodeByte(); // 00 = end chat, 01 == follow

        //todo legend
        if (((lastMsg == 0x12 && c.getPlayer().getDirection() >= 0) || (lastMsg == 0x13 && c.getPlayer().getDirection() == -1)) && action == 1 && GameConstants.GMS) {
            MapScriptMethods.startDirectionInfo(c.getPlayer(), lastMsg == 0x13);
            return;
        }

        final NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);

        if (cm == null || c.getPlayer().getConversation() == 0 || cm.getLastMsg() != lastMsg) {
            return;
        }
        cm.setLastMsg((byte) -1);
        if (lastMsg == 3) {
            if (action != 0) {
                cm.setGetText(inPacket.decodeString());
                if (cm.getType() == 0) {
                    NPCScriptManager.getInstance().startQuest(c, action, lastMsg, -1);
                } else if (cm.getType() == 1) {
                    NPCScriptManager.getInstance().endQuest(c, action, lastMsg, -1);
                } else {
                    NPCScriptManager.getInstance().action(c, action, lastMsg, -1);
                }
            } else {
                cm.dispose();
            }
        } else {
            int selection = -1;
            if (inPacket.getUnreadAmount() >= 4) {
                selection = inPacket.decodeInt();
            } else if (inPacket.getUnreadAmount() > 0) {
                selection = inPacket.decodeByte();
            }
            if (lastMsg == 4 && selection == -1) {
                cm.dispose();
                return;//h4x
            }
            if (selection >= -1 && action != -1) {
                if (cm.getType() == 0) {
                    NPCScriptManager.getInstance().startQuest(c, action, lastMsg, selection);
                } else if (cm.getType() == 1) {
                    NPCScriptManager.getInstance().endQuest(c, action, lastMsg, selection);
                } else {
                    NPCScriptManager.getInstance().action(c, action, lastMsg, selection);
                }
            } else {
                cm.dispose();
            }
        }
    }

    public static final void repairAll(final MapleClient c) {
        if (c.getPlayer().getMapId() != 240000000) {
            return;
        }
        Equip eq;
        double rPercentage;
        int price = 0;
        Map<String, Integer> eqStats;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Map<Equip, Integer> eqs = new HashMap<Equip, Integer>();
        final MapleInventoryType[] types = {MapleInventoryType.EQUIP, MapleInventoryType.EQUIPPED};
        for (MapleInventoryType type : types) {
            for (Item item : c.getPlayer().getInventory(type).newList()) {
                if (item instanceof Equip) { //redundant
                    eq = (Equip) item;
                    if (eq.getDurability() >= 0) {
                        eqStats = ii.getEquipStats(eq.getItemId());
                        if (eqStats.containsKey("durability") && eqStats.get("durability") > 0 && eq.getDurability() < eqStats.get("durability")) {
                            rPercentage = (100.0 - Math.ceil((eq.getDurability() * 1000.0) / (eqStats.get("durability") * 10.0)));
                            eqs.put(eq, eqStats.get("durability"));
                            price += (int) Math.ceil(rPercentage * ii.getPrice(eq.getItemId()) / (ii.getReqLevel(eq.getItemId()) < 70 ? 100.0 : 1.0));
                        }
                    }
                }
            }
        }
        if (eqs.size() <= 0 || c.getPlayer().getMeso() < price) {
            return;
        }
        c.getPlayer().gainMeso(-price, true);
        Equip ez;
        for (Entry<Equip, Integer> eqqz : eqs.entrySet()) {
            ez = eqqz.getKey();
            ez.setDurability(eqqz.getValue());
            c.getPlayer().forceReAddItem(ez.copy(), ez.getPosition() < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP);
        }
    }

    public static final void repair(final InPacket inPacket, final MapleClient c) {
        if (c.getPlayer().getMapId() != 240000000 || inPacket.getUnreadAmount() < 4) { //leafre for now
            return;
        }
        final int position = inPacket.decodeInt(); //who knows why this is a int
        final MapleInventoryType type = position < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP;
        final Item item = c.getPlayer().getInventory(type).getItem((byte) position);
        if (item == null) {
            return;
        }
        final Equip eq = (Equip) item;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Map<String, Integer> eqStats = ii.getEquipStats(item.getItemId());
        if (eq.getDurability() < 0 || !eqStats.containsKey("durability") || eqStats.get("durability") <= 0 || eq.getDurability() >= eqStats.get("durability")) {
            return;
        }
        final double rPercentage = (100.0 - Math.ceil((eq.getDurability() * 1000.0) / (eqStats.get("durability") * 10.0)));
        //drpq level 105 weapons - ~420k per %; 2k per durability point
        //explorer level 30 weapons - ~10 mesos per %
        final int price = (int) Math.ceil(rPercentage * ii.getPrice(eq.getItemId()) / (ii.getReqLevel(eq.getItemId()) < 70 ? 100.0 : 1.0)); // / 100 for level 30?
        //TODO: need more data on calculating off client
        if (c.getPlayer().getMeso() < price) {
            return;
        }
        c.getPlayer().gainMeso(-price, false);
        eq.setDurability(eqStats.get("durability"));
        c.getPlayer().forceReAddItem(eq.copy(), type);
    }

    public static final void UpdateQuest(final InPacket inPacket, final MapleClient c) {
        final MapleQuest quest = MapleQuest.getInstance(inPacket.decodeShort());
        if (quest != null) {
            c.getPlayer().updateQuest(c.getPlayer().getQuest(quest), true);
        }
    }

    public static final void UseItemQuest(final InPacket inPacket, final MapleClient c) {
        final short slot = inPacket.decodeShort();
        final int itemId = inPacket.decodeInt();
        final Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem(slot);
        final int qid = inPacket.decodeInt();
        final MapleQuest quest = MapleQuest.getInstance(qid);
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Pair<Integer, List<Integer>> questItemInfo = null;
        boolean found = false;
        for (Item i : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
            if (i.getItemId() / 10000 == 422) {
                questItemInfo = ii.questItemInfo(i.getItemId());
                if (questItemInfo != null && questItemInfo.getLeft() == qid && questItemInfo.getRight() != null && questItemInfo.getRight().contains(itemId)) {
                    found = true;
                    break; //i believe it's any order
                }
            }
        }
        if (quest != null && found && item != null && item.getQuantity() > 0 && item.getItemId() == itemId) {
            final int newData = inPacket.decodeInt();
            final MapleQuestStatus stats = c.getPlayer().getQuestNoAdd(quest);
            if (stats != null && stats.getStatus() == 1) {
                stats.setCustomData(String.valueOf(newData));
                c.getPlayer().updateQuest(stats, true);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
            }
        }
    }

    public static final void RPSGame(final InPacket inPacket, final MapleClient c) {
        if (inPacket.getUnreadAmount() == 0 || c.getPlayer() == null || c.getPlayer().getMap() == null || !c.getPlayer().getMap().containsNPC(9000019)) {
            if (c.getPlayer() != null && c.getPlayer().getRPS() != null) {
                c.getPlayer().getRPS().dispose(c);
            }
            return;
        }
        final byte mode = inPacket.decodeByte();
        switch (mode) {
            case 0: //start game
            case 5: //retry
                if (c.getPlayer().getRPS() != null) {
                    c.getPlayer().getRPS().reward(c);
                }
                if (c.getPlayer().getMeso() >= 1000) {
                    c.getPlayer().setRPS(new RockPaperScissors(c, mode));
                } else {
                    c.write(MaplePacketCreator.getRPSMode((byte) 0x08, -1, -1, -1));
                }
                break;
            case 1: //answer
                if (c.getPlayer().getRPS() == null || !c.getPlayer().getRPS().answer(c, inPacket.decodeByte())) {
                    c.write(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
            case 2: //time over
                if (c.getPlayer().getRPS() == null || !c.getPlayer().getRPS().timeOut(c)) {
                    c.write(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
            case 3: //continue
                if (c.getPlayer().getRPS() == null || !c.getPlayer().getRPS().nextRound(c)) {
                    c.write(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
            case 4: //leave
                if (c.getPlayer().getRPS() != null) {
                    c.getPlayer().getRPS().dispose(c);
                } else {
                    c.write(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
        }

    }

    public static final void OpenPublicNpc(final InPacket inPacket, final MapleClient c) {
        final int npcid = inPacket.decodeInt();
        if (c.getPlayer().hasBlockedInventory() || c.getPlayer().isInBlockedMap() || c.getPlayer().getLevel() < 10) {
//            c.getPlayer().dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
            return;
        }
        for (int i = 0; i < GameConstants.publicNpcIds.length; i++) {
            if (GameConstants.publicNpcIds[i] == npcid) { //for now
                NPCScriptManager.getInstance().start(c, npcid);
                return;
            }
        }
    }
}
