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

import handling.SendPacketOpcode;
import server.MapleCarnivalParty;
import connection.OutPacket;

public class MonsterCarnivalPacket {

    public static OutPacket startMonsterCarnival(final MapleCharacter chr, final int enemyavailable, final int enemytotal) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MONSTER_CARNIVAL_START.getValue());
        final MapleCarnivalParty friendly = chr.getCarnivalParty();
        outPacket.encodeByte(friendly.getTeam());
        outPacket.encodeShort(chr.getAvailableCP());
        outPacket.encodeShort(chr.getTotalCP());
        outPacket.encodeShort(friendly.getAvailableCP());
        outPacket.encodeShort(friendly.getTotalCP());
        outPacket.encodeShort(enemyavailable);
        outPacket.encodeShort(enemytotal);
        outPacket.encodeLong(0);
        outPacket.encodeShort(0);

        return outPacket;
    }

    public static OutPacket playerDiedMessage(String name, int lostCP, int team) { //CPQ
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MONSTER_CARNIVAL_DIED.getValue());
        outPacket.encodeByte(team); //team
        outPacket.encodeString(name);
        outPacket.encodeByte(lostCP);

        return outPacket;
    }

    public static OutPacket CPUpdate(boolean party, int curCP, int totalCP, int team) {
        
        OutPacket outPacket;
        
        if (!party) {
            outPacket = new OutPacket(SendPacketOpcode.MONSTER_CARNIVAL_OBTAINED_CP.getValue());
        } else {
            outPacket = new OutPacket(SendPacketOpcode.MONSTER_CARNIVAL_PARTY_CP.getValue());
            outPacket.encodeByte(team);
        }
        outPacket.encodeShort(curCP);
        outPacket.encodeShort(totalCP);

        return outPacket;
    }

    public static OutPacket playerSummoned(String name, int tab, int number) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
        outPacket.encodeByte(tab);
        outPacket.encodeByte(number);
        outPacket.encodeString(name);

        return outPacket;
    }
}
