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

import client.MapleCharacter;
import client.inventory.Item;
import client.MapleClient;

import constants.GameConstants;
import handling.SendPacketOpcode;
import java.util.List;
import server.MerchItemPackage;
import server.shops.AbstractPlayerStore.BoughtItem;
import server.shops.HiredMerchant;
import server.shops.IMaplePlayerShop;
import server.shops.MapleMiniGame;
import server.shops.MaplePlayerShop;
import server.shops.MaplePlayerShopItem;
import tools.Pair;
import connection.OutPacket;

public class PlayerShopPacket {

    public static final OutPacket addCharBox(final MapleCharacter c, final int type) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        outPacket.encodeInt(c.getId());
        PacketHelper.addAnnounceBox(outPacket, c);

        return outPacket;
    }

    public static final OutPacket removeCharBox(final MapleCharacter c) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        outPacket.encodeInt(c.getId());
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static final OutPacket sendTitleBox() {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SEND_TITLE_BOX.getValue());
        outPacket.encodeByte(7);

        return outPacket;
    }

    public static final OutPacket sendPlayerShopBox(final MapleCharacter c) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        outPacket.encodeInt(c.getId());
        PacketHelper.addAnnounceBox(outPacket, c);

        return outPacket;
    }

    public static final OutPacket getHiredMerch(final MapleCharacter chr, final HiredMerchant merch, final boolean firstTime) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());

        outPacket.encodeByte(GameConstants.GMS ? 10 : 5);
        outPacket.encodeByte(5);
        outPacket.encodeByte(4);
        outPacket.encodeShort(merch.getVisitorSlot(chr));
        outPacket.encodeInt(merch.getItemId());
        outPacket.encodeString("Hired Merchant");

        for (final Pair<Byte, MapleCharacter> storechr : merch.getVisitors()) {
            outPacket.encodeByte(storechr.left);
            PacketHelper.addCharLook(outPacket, storechr.right, false);
            outPacket.encodeString(storechr.right.getName());
            outPacket.encodeShort(storechr.right.getJob());
        }
        outPacket.encodeByte(-1);
        outPacket.encodeShort(0);
        outPacket.encodeString(merch.getOwnerName());
        if (merch.isOwner(chr)) {
            outPacket.encodeInt(merch.getTimeLeft());
            outPacket.encodeByte(firstTime ? 1 : 0);
            outPacket.encodeByte(merch.getBoughtItems().size());
            for (BoughtItem SoldItem : merch.getBoughtItems()) {
                outPacket.encodeInt(SoldItem.id);
                outPacket.encodeShort(SoldItem.quantity); // number of purchased
                outPacket.encodeInt(SoldItem.totalPrice); // total price
                outPacket.encodeString(SoldItem.buyer); // name of the buyer
            }
            outPacket.encodeInt(merch.getMeso());
			if (GameConstants.GMS) {
				outPacket.encodeInt(0);
			}
        }
        outPacket.encodeString(merch.getDescription());
        outPacket.encodeByte(10);
        outPacket.encodeInt(merch.getMeso()); // meso
        outPacket.encodeByte(merch.getItems().size());

        for (final MaplePlayerShopItem item : merch.getItems()) {
            outPacket.encodeShort(item.bundles);
            outPacket.encodeShort(item.item.getQuantity());
            outPacket.encodeInt(item.price);
            PacketHelper.addItemInfo(outPacket, item.item, true, true);
        }
        return outPacket;
    }

    public static final OutPacket getPlayerStore(final MapleCharacter chr, final boolean firstTime) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        IMaplePlayerShop ips = chr.getPlayerShop();
		outPacket.encodeByte(GameConstants.GMS ? 10 : 5);
        switch (ips.getShopType()) {
            case 2:
                outPacket.encodeByte(4);
                outPacket.encodeByte(4);
                break;
            case 3:
                outPacket.encodeByte(2);
                outPacket.encodeByte(2);
                break;
            case 4:
                outPacket.encodeByte(1);
                outPacket.encodeByte(2);
                break;
        }
        outPacket.encodeShort(ips.getVisitorSlot(chr));
        PacketHelper.addCharLook(outPacket, ((MaplePlayerShop) ips).getMCOwner(), false);
        outPacket.encodeString(ips.getOwnerName());
        outPacket.encodeShort(((MaplePlayerShop) ips).getMCOwner().getJob());
        for (final Pair<Byte, MapleCharacter> storechr : ips.getVisitors()) {
            outPacket.encodeByte(storechr.left);
            PacketHelper.addCharLook(outPacket, storechr.right, false);
            outPacket.encodeString(storechr.right.getName());
            outPacket.encodeShort(storechr.right.getJob());
        }
        outPacket.encodeByte(0xFF);
        outPacket.encodeString(ips.getDescription());
        outPacket.encodeByte(10);
        outPacket.encodeByte(ips.getItems().size());

        for (final MaplePlayerShopItem item : ips.getItems()) {
            outPacket.encodeShort(item.bundles);
            outPacket.encodeShort(item.item.getQuantity());
            outPacket.encodeInt(item.price);
            PacketHelper.addItemInfo(outPacket, item.item, true, true);
        }
        return outPacket;
    }

    public static final OutPacket shopChat(final String message, final int slot) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 14 : 6);
        outPacket.encodeByte(GameConstants.GMS ? 15 : 8);
        outPacket.encodeByte(slot);
        outPacket.encodeString(message);

        return outPacket;
    }

    public static final OutPacket shopErrorMessage(final int error, final int type) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 18 : 0x0A);
        outPacket.encodeByte(type);
        outPacket.encodeByte(error);

        return outPacket;
    }

    public static final OutPacket spawnHiredMerchant(final HiredMerchant hm) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_HIRED_MERCHANT.getValue());
        outPacket.encodeInt(hm.getOwnerId());
        outPacket.encodeInt(hm.getItemId());
        outPacket.encodePosition(hm.getTruePosition());
        outPacket.encodeShort(0);
        outPacket.encodeString(hm.getOwnerName());
        PacketHelper.addInteraction(outPacket, hm);

        return outPacket;
    }

    public static final OutPacket destroyHiredMerchant(final int id) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DESTROY_HIRED_MERCHANT.getValue());
        outPacket.encodeInt(id);

        return outPacket;
    }

    public static final OutPacket shopItemUpdate(final IMaplePlayerShop shop) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 43 : 0x17);
        if (shop.getShopType() == 1) {
            outPacket.encodeInt(0);
        }
        outPacket.encodeByte(shop.getItems().size());

        for (final MaplePlayerShopItem item : shop.getItems()) {
            outPacket.encodeShort(item.bundles);
            outPacket.encodeShort(item.item.getQuantity());
            outPacket.encodeInt(item.price);
            PacketHelper.addItemInfo(outPacket, item.item, true, true);
        }
        return outPacket;
    }

    public static final OutPacket shopVisitorAdd(final MapleCharacter chr, final int slot) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 9 : 4);
        outPacket.encodeByte(slot);
        PacketHelper.addCharLook(outPacket, chr, false);
        outPacket.encodeString(chr.getName());
        outPacket.encodeShort(chr.getJob());

        return outPacket;
    }

    public static final OutPacket shopVisitorLeave(final byte slot) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 18 : 0x0A);
        outPacket.encodeByte(slot);

        return outPacket;
    }

    public static final OutPacket Merchant_Buy_Error(final byte message) {
        final 

        // 2 = You have not enough meso
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 42 : 0x18);
        outPacket.encodeByte(message);

        return outPacket;
    }

    public static final OutPacket updateHiredMerchant(final HiredMerchant shop) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_HIRED_MERCHANT.getValue());
        outPacket.encodeInt(shop.getOwnerId());
        PacketHelper.addInteraction(outPacket, shop);

        return outPacket;
    }

    public static final OutPacket merchItem_Message(final byte op) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MERCH_ITEM_MSG.getValue());
        outPacket.encodeByte(op);

        return outPacket;
    }

    public static final OutPacket merchItemStore(final byte op) {
        final 
        // [28 01] [22 01] - Invalid Asiasoft Passport
        // [28 01] [22 00] - Open Asiasoft pin typing
        OutPacket outPacket = new OutPacket(SendPacketOpcode.MERCH_ITEM_STORE.getValue());
        outPacket.encodeByte(op);

        switch (op) {
            case 0x24:
			case 0x25:
			case 0x26:
                outPacket.encodeZeroBytes(8);
                break;
            default:
                outPacket.encodeByte(0);
                break;
        }
        return outPacket;
    }


    public static final OutPacket merchItemStore_ItemData(final MerchItemPackage pack) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MERCH_ITEM_STORE.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x24 : 0x23);
        outPacket.encodeInt(9030000); // Fredrick
        outPacket.encodeInt(32272); // always the same..?
        outPacket.encodeZeroBytes(5);
        outPacket.encodeInt(pack.getMesos());
        outPacket.encodeByte(0);
        outPacket.encodeByte(pack.getItems().size());

        for (final Item item : pack.getItems()) {
            PacketHelper.addItemInfo(outPacket, item, true, true);
        }
        outPacket.encodeZeroBytes(3);

        return outPacket;
    }

    public static OutPacket getMiniGame(MapleClient c, MapleMiniGame minigame) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 10 : 5);
        outPacket.encodeByte(minigame.getGameType());
        outPacket.encodeByte(minigame.getMaxSize());
        outPacket.encodeShort(minigame.getVisitorSlot(c.getPlayer()));
        PacketHelper.addCharLook(outPacket, minigame.getMCOwner(), false);
        outPacket.encodeString(minigame.getOwnerName());
        outPacket.encodeShort(minigame.getMCOwner().getJob());
        for (Pair<Byte, MapleCharacter> visitorz : minigame.getVisitors()) {
            outPacket.encodeByte(visitorz.getLeft());
            PacketHelper.addCharLook(outPacket, visitorz.getRight(), false);
            outPacket.encodeString(visitorz.getRight().getName());
            outPacket.encodeShort(visitorz.getRight().getJob());
        }
        outPacket.encodeByte(-1);
        outPacket.encodeByte(0);
        addGameInfo(outPacket, minigame.getMCOwner(), minigame);
        for (Pair<Byte, MapleCharacter> visitorz : minigame.getVisitors()) {
            outPacket.encodeByte(visitorz.getLeft());
            addGameInfo(outPacket, visitorz.getRight(), minigame);
        }
        outPacket.encodeByte(-1);
        outPacket.encodeString(minigame.getDescription());
        outPacket.encodeShort(minigame.getPieceType());
        return outPacket;
    }

    public static OutPacket getMiniGameReady(boolean ready) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? (ready ? 0x3B : 0x3C) : (ready ? 0x38 : 0x39));
        return outPacket;
    }

    public static OutPacket getMiniGameExitAfter(boolean ready) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? (ready ? 0x39 : 0x3A) : ready ? 0x36 : 0x37);
        return outPacket;
    }

    public static OutPacket getMiniGameStart(int loser) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x3E : 0x3B);
        outPacket.encodeByte(loser == 1 ? 0 : 1);
        return outPacket;
    }

    public static OutPacket getMiniGameSkip(int slot) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x40 : 0x3D);
        //owner = 1 visitor = 0?
        outPacket.encodeByte(slot);
        return outPacket;
    }

    public static OutPacket getMiniGameRequestTie() {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x33 : 0x30);
        return outPacket;
    }

    public static OutPacket getMiniGameDenyTie() {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x32 : 0x31);
        return outPacket;
    }

    public static OutPacket getMiniGameFull() {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeShort(GameConstants.GMS ? 10 : 5);
        outPacket.encodeByte(2);
        return outPacket;
    }

    public static OutPacket getMiniGameMoveOmok(int move1, int move2, int move3) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x41 : 0x3E);
        outPacket.encodeInt(move1);
        outPacket.encodeInt(move2);
        outPacket.encodeByte(move3);
        return outPacket;
    }

    public static OutPacket getMiniGameNewVisitor(MapleCharacter c, int slot, MapleMiniGame game) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 9 : 4);
        outPacket.encodeByte(slot);
        PacketHelper.addCharLook(outPacket, c, false);
        outPacket.encodeString(c.getName());
        outPacket.encodeShort(c.getJob());
        addGameInfo(outPacket, c, game);
        return outPacket;
    }

    public static void addGameInfo(OutPacket outPacket, MapleCharacter chr, MapleMiniGame game) {
        outPacket.encodeInt(game.getGameType()); // start of visitor; unknown
        outPacket.encodeInt(game.getWins(chr));
        outPacket.encodeInt(game.getTies(chr));
        outPacket.encodeInt(game.getLosses(chr));
        outPacket.encodeInt(game.getScore(chr)); // points
    }

    public static OutPacket getMiniGameClose(byte number) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 18 : 0xA);
        outPacket.encodeByte(1);
        outPacket.encodeByte(number);
        return outPacket;
    }

    public static OutPacket getMatchCardStart(MapleMiniGame game, int loser) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x3E : 0x3B);
        outPacket.encodeByte(loser == 1 ? 0 : 1);
        int times = game.getPieceType() == 1 ? 20 : (game.getPieceType() == 2 ? 30 : 12);
        outPacket.encodeByte(times);
        for (int i = 1; i <= times; i++) {
            outPacket.encodeInt(game.getCardId(i));
        }
        return outPacket;
    }

    public static OutPacket getMatchCardSelect(int turn, int slot, int firstslot, int type) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x45 : 0x42);
        outPacket.encodeByte(turn);
        outPacket.encodeByte(slot);
        if (turn == 0) {
            outPacket.encodeByte(firstslot);
            outPacket.encodeByte(type);
        }
        return outPacket;
    }

    public static OutPacket getMiniGameResult(MapleMiniGame game, int type, int x) {
        
        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 0x3F : 0x3C);
        outPacket.encodeByte(type); //lose = 0, tie = 1, win = 2
        game.setPoints(x, type);
        if (type != 0) {
            game.setPoints(x == 1 ? 0 : 1, type == 2 ? 0 : 1);
        }
        if (type != 1) {
            if (type == 0) {
                outPacket.encodeByte(x == 1 ? 0 : 1); //who did it?
            } else {
                outPacket.encodeByte(x);
            }
        }
        addGameInfo(outPacket, game.getMCOwner(), game);
        for (Pair<Byte, MapleCharacter> visitorz : game.getVisitors()) {
            addGameInfo(outPacket, visitorz.right, game);
        }

        return outPacket;
    }

    public static final OutPacket MerchantBlackListView(final List<String> blackList) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 37 : 45);
        outPacket.encodeShort(blackList.size());
        for (String visit : blackList) {
            outPacket.encodeString(visit);
        }
        return outPacket;
    }

    public static final OutPacket MerchantVisitorView(List<String> visitor) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        outPacket.encodeByte(GameConstants.GMS ? 36 : 44);
        outPacket.encodeShort(visitor.size());
        for (String visit : visitor) {
            outPacket.encodeString(visit);
            outPacket.encodeInt(1); /////for the lul
        }
        return outPacket;
    }
}
