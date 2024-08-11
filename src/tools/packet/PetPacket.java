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
import client.MapleStat;
import client.inventory.Item;
import client.inventory.MaplePet;
import connection.OutPacket;
import handling.SendPacketOpcode;
import server.movement.ILifeMovementFragment;

import java.awt.*;
import java.util.List;

public class PetPacket {

    public static final OutPacket updatePet(final MaplePet pet, final Item item, final boolean active) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeByte(2);
        outPacket.encodeByte(3);
        outPacket.encodeByte(5);
        outPacket.encodeShort(pet.getInventoryPosition());
        outPacket.encodeByte(0);
        outPacket.encodeByte(5);
        outPacket.encodeShort(pet.getInventoryPosition());
        outPacket.encodeByte(3);
        outPacket.encodeInt(pet.getPetItemId());
        outPacket.encodeByte(1);
        outPacket.encodeLong(pet.getUniqueId());
        PacketHelper.addPetItemInfo(outPacket, item, pet, active);
        
        return outPacket;
    }

    public static final OutPacket showPet(final MapleCharacter chr, final MaplePet pet, final boolean remove, final boolean hunger) {
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_PET.getValue());
        outPacket.encodeInt(chr.getId());
	    outPacket.encodeInt(chr.getPetIndex(pet));
        if (remove) {
            outPacket.encodeByte(0);
            outPacket.encodeByte(hunger ? 1 : 0);
        } else {
            outPacket.encodeByte(1);
            outPacket.encodeByte(0); //1?
            outPacket.encodeInt(pet.getPetItemId());
            outPacket.encodeString(pet.getName());
            outPacket.encodeLong(pet.getUniqueId());
            outPacket.encodeShort(pet.getPos().x);
            outPacket.encodeShort(pet.getPos().y - 20);
            outPacket.encodeByte(pet.getStance());
            outPacket.encodeShort(pet.getFh());
//            outPacket.encodeShort(0);
        }
        
        return outPacket;
    }

    public static final OutPacket removePet(final int cid, final int index) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SPAWN_PET.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(index);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        
        return outPacket;
    }

    public static final OutPacket movePet(final int cid, final int pid, Point oldPos, Point oldVPos, final byte slot, final List<ILifeMovementFragment> moves) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MOVE_PET.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(slot);

//        outPacket.encodeLong(pid);
        outPacket.encodePosition(oldPos);
        outPacket.encodePosition(oldVPos);
        PacketHelper.serializeMovementList(outPacket, moves);

        return outPacket;
    }

    public static final OutPacket petChat(final int cid, final int un, final String text, final byte slot) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PET_CHAT.getValue());
        outPacket.encodeInt(cid);
        outPacket.encodeInt(slot);
        outPacket.encodeShort(un);
        outPacket.encodeString(text);
        outPacket.encodeByte(0); //hasQuoteRing

        return outPacket;
    }

    public static final OutPacket commandResponse(final int cid, final byte command, final byte slot, final boolean success, final boolean food) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PET_COMMAND.getValue());
        outPacket.encodeInt(cid);
	outPacket.encodeInt(slot);
	outPacket.encodeByte(command == 1 ? 1 : 0);
	outPacket.encodeByte(command);
        if (command == 1) {
	    outPacket.encodeByte(0);

        } else {
            outPacket.encodeShort(success ? 1 : 0);
	}
        return outPacket;
    }

    public static final OutPacket showOwnPetLevelUp(final byte index) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(6);
        outPacket.encodeByte(0);
        outPacket.encodeInt(index); // Pet Index

        return outPacket;
    }

    public static final OutPacket showPetLevelUp(final MapleCharacter chr, final byte index) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(6);
        outPacket.encodeByte(0);
        outPacket.encodeInt(index);

        return outPacket;
    }

    public static final OutPacket showPetUpdate(final MapleCharacter chr, final int uniqueId, final byte index) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PET_UPDATE.getValue());
        outPacket.encodeInt(chr.getId());
	outPacket.encodeInt(index);
	outPacket.encodeLong(uniqueId);
        outPacket.encodeByte(0); //not sure, probably not it

        return outPacket;
    }

    public static final OutPacket petStatUpdate(final MapleCharacter chr) {
        final 

        OutPacket outPacket = new OutPacket(SendPacketOpcode.UPDATE_STATS.getValue());
        outPacket.encodeByte(0);
        
        outPacket.encodeLong(MapleStat.PET.getValue());

        byte count = 0;
        for (final MaplePet pet : chr.getPets()) {
            if (pet.getSummoned()) {
                outPacket.encodeLong(pet.getUniqueId());
                count++;
            }
        }
        while (count < 3) {
            outPacket.encodeZeroBytes(8);
            count++;
        }
        outPacket.encodeByte(0);
	    outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }
}
