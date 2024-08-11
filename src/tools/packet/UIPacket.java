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

import constants.GameConstants;
import handling.SendPacketOpcode;
import tools.MaplePacketCreator;
import connection.OutPacket;

public class UIPacket {

    public static final OutPacket EarnTitleMsg(final String msg) {
        final 

// "You have acquired the Pig's Weakness skill."
        OutPacket outPacket = new OutPacket(SendPacketOpcode.EARN_TITLE_MSG.getValue());
        outPacket.encodeString(msg);

        return outPacket;
    }

    public static OutPacket getSPMsg(byte sp, short job) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(4);
        outPacket.encodeShort(job);
        outPacket.encodeByte(sp);

        return outPacket;
    }

    public static OutPacket getGPMsg(int itemid) {
        

        // Temporary transformed as a dragon, even with the skill ......
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(7);
        outPacket.encodeInt(itemid);

        return outPacket;
    }

    public static OutPacket getBPMsg(int amount) {
        return getBPMsg(amount, 0);
    }

    public static OutPacket getBPMsg(int amount, int pvpexp) {

        // Temporary transformed as a dragon, even with the skill ......
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(22);
        outPacket.encodeInt(amount);
        outPacket.encodeInt(pvpexp);

        return outPacket;
    }

    public static OutPacket getGPContribution(int itemid) {
        

        // Temporary transformed as a dragon, even with the skill ......
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(8);
        outPacket.encodeInt(itemid);

        return outPacket;
    }

    public static OutPacket getTopMsg(String msg) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.TOP_MSG.getValue());
        outPacket.encodeString(msg);

        return outPacket;
    }

    public static OutPacket getMidMsg(String msg, boolean keep, int index) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.MID_MSG.getValue());
        outPacket.encodeByte(index); //where the message should appear on the screen
        outPacket.encodeString(msg);
        outPacket.encodeByte(keep ? 0 : 1);

        return outPacket;
    }

    public static OutPacket clearMidMsg() {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CLEAR_MID_MSG.getValue());

        return outPacket;
    }

    public static OutPacket getStatusMsg(int itemid) {
        

        // Temporary transformed as a dragon, even with the skill ......
        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        outPacket.encodeByte(9);
        outPacket.encodeInt(itemid);

        return outPacket;
    }

    public static final OutPacket MapEff(final String path) {
        return MaplePacketCreator.environmentChange(path, 3);
    }

    public static final OutPacket MapNameDisplay(final int mapid) {
        return MaplePacketCreator.environmentChange("maplemap/enter/" + mapid, 3);
    }

    public static final OutPacket Aran_Start() {
        return MaplePacketCreator.environmentChange("Aran/balloon", 4);
    }

    public static final OutPacket AranTutInstructionalBalloon(final String data) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(0x19);
        outPacket.encodeString(data);
        outPacket.encodeInt(1);

        return outPacket;
    }

    public static final OutPacket ShowWZEffect(final String data) {

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        outPacket.encodeByte(0x16); //bb +2
        outPacket.encodeString(data);

        return outPacket;
    }

    public static final OutPacket playMovie(final String data, boolean show) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.PLAY_MOVIE.getValue());
        outPacket.encodeString(data);
        outPacket.encodeByte(show ? 1 : 0);

        return outPacket;
    }

    public static OutPacket summonHelper(boolean summon) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SUMMON_HINT.getValue());
        outPacket.encodeByte(summon ? 1 : 0);

        return outPacket;
    }

    public static OutPacket summonMessage(int type) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SUMMON_HINT_MSG.getValue());
        outPacket.encodeByte(1);
        outPacket.encodeInt(type);
        outPacket.encodeInt(7000); // probably the delay

        return outPacket;
    }

    public static OutPacket summonMessage(String message) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.SUMMON_HINT_MSG.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeString(message);
        outPacket.encodeInt(200); // IDK
        outPacket.encodeShort(0);
        outPacket.encodeInt(10000); // Probably delay

        return outPacket;
    }

    public static OutPacket IntroLock(boolean enable) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CYGNUS_INTRO_LOCK.getValue());
        outPacket.encodeByte(enable ? 1 : 0);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket getDirectionStatus(boolean enable) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DIRECTION_STATUS.getValue());
        outPacket.encodeByte(enable ? 1 : 0);

        return outPacket;
    }

    public static OutPacket getDirectionInfo(int type, int value) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DIRECTION_INFO.getValue());
        outPacket.encodeByte(type);
        outPacket.encodeLong(value);

        return outPacket;
    }

    public static OutPacket getDirectionInfo(String data, int value, int x, int y, int pro) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.DIRECTION_INFO.getValue());
        outPacket.encodeByte(2);
        outPacket.encodeString(data);
        outPacket.encodeInt(value);
        outPacket.encodeInt(x);
        outPacket.encodeInt(y);
        outPacket.encodeShort(pro);
        outPacket.encodeInt(0); //only if pro is > 0

        return outPacket;
    }

    public static OutPacket IntroEnableUI(int wtf) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CYGNUS_INTRO_ENABLE_UI.getValue());
        outPacket.encodeByte(wtf > 0 ? 1 : 0);
        if (wtf > 0) {
            outPacket.encodeShort(wtf);
        }

        return outPacket;
    }

    public static OutPacket IntroDisableUI(boolean enable) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.CYGNUS_INTRO_DISABLE_UI.getValue());
        outPacket.encodeByte(enable ? 1 : 0);

        return outPacket;
    }

    public static OutPacket fishingUpdate(byte type, int id) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.FISHING_BOARD_UPDATE.getValue());
        outPacket.encodeByte(type);
        outPacket.encodeInt(id);

        return outPacket;
    }

    public static OutPacket fishingCaught(int chrid) {
        

        OutPacket outPacket = new OutPacket(SendPacketOpcode.FISHING_CAUGHT.getValue());
        outPacket.encodeInt(chrid);

        return outPacket;
    }
}
